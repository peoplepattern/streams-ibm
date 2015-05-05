package com.ibm.hackathon

import com.ibm.alchemy.api.DisambiguatedProcessor
import org.apache.streams.components.http.HttpProcessorConfiguration
import org.apache.streams.config.{StreamsConfigurator, ComponentConfigurator}
import org.apache.streams.converter.ActivityConverterUtil
import org.apache.streams.core.{StreamBuilder, StreamsProcessor}
import org.apache.streams.elasticsearch._
import org.apache.streams.jackson.StreamsJacksonMapper
import org.apache.streams.local.builders.LocalStreamBuilder
import org.apache.streams.pojo.json.Activity
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.JavaConverters._

import scala.util.{Failure, Success, Try}

/**
 * Created by sblackmon on 5/5/15.
 */
object EntityIndexPipeline {

  def main(args: Array[String]) = {
    val job: EntityIndexPipeline = new EntityIndexPipeline()
    new Thread(job).start
  }
}

class EntityIndexPipeline(config: PostPipelineConfiguration) extends Runnable with java.io.Serializable {

  def this() = {
    this(
      new ComponentConfigurator[PostPipelineConfiguration](classOf[PostPipelineConfiguration])
        .detectConfiguration(StreamsConfigurator.getConfig)
    )
  }

  private val LOGGER: Logger = LoggerFactory.getLogger(classOf[PostPipelineConfiguration])

  val STREAMS_ID: String = "EntityIndexPipeline"

  def convertTwitterStatusJsonToStreamsPost(in: String, activityConverterUtil: ActivityConverterUtil) : Option[Activity] = {
    try {
      val activities : java.util.List[Activity] = activityConverterUtil.convert(in)
      if( activities != null ) {
        if( activities.get(0) != null )
          return Some(activities.get(0))
        else return None
      }
      else return None
    } catch {
      case e: Exception => {
        LOGGER.warn(in, e)
      }
        return None
    }
  }

  def convertTwitterStatusJsonToStreamsPost(iter: Iterator[String]) : Iterator[Activity] = {
    val activityConverterUtil = ActivityConverterUtil.getInstance
    iter.flatMap(item => convertTwitterStatusJsonToStreamsPost(item, activityConverterUtil))
  }

  def convertStreamsPostObjToJson(in: Activity, mapper: StreamsJacksonMapper) : Option[String] = {
    val out = Try(mapper.writeValueAsString(in))
    out match {
      case Success(v : String) =>
        if( out != null ) return Some(v) else return None
      case Failure(e : Throwable) =>
        LOGGER.warn(in.getId, e)
        return None
    }
  }

  def convertStreamsPostObjToJson(iter: Iterator[Activity]) : Iterator[String] = {
    val mapper = StreamsJacksonMapper.getInstance
    iter.flatMap(item => convertStreamsPostObjToJson(item, mapper))
  }

  override def run(): Unit = {

    LOGGER.info("Typesafe Config: " + StreamsConfigurator.config.toString)
    LOGGER.info("Streams Config: " + config.toString)

    val mapper = StreamsJacksonMapper.getInstance

    val esReaderConfig = mapper.convertValue(config.getSource, classOf[ElasticsearchReaderConfiguration])
    val esPersistReader: ElasticsearchPersistReader = new ElasticsearchPersistReader(esReaderConfig)

    val esWriterConfig = mapper.convertValue(config.getDestination, classOf[ElasticsearchWriterConfiguration])
    val esPersistWriter: ElasticsearchPersistUpdater = new ElasticsearchPersistUpdater(esWriterConfig)

    val alchemyConfig = mapper.convertValue(config.getAlchemy, classOf[HttpProcessorConfiguration])

    var processor : StreamsProcessor = null;
    processor = new DisambiguatedProcessor()

    val streamConfig: scala.collection.mutable.Map[String,AnyRef] = scala.collection.mutable.Map[String,AnyRef]()
    val timeout: Int = (7 * 24 * 60 * 1000)

    streamConfig(LocalStreamBuilder.STREAM_IDENTIFIER_KEY) = STREAMS_ID
    streamConfig(LocalStreamBuilder.TIMEOUT_KEY) = timeout.asInstanceOf[AnyRef]

    val javaConfig : java.util.Map[String,Object] = streamConfig.asJava

    val builder: StreamBuilder = new LocalStreamBuilder(1000, javaConfig)
    builder.newPerpetualStream(ElasticsearchPersistReader.STREAMS_ID, esPersistReader)
    builder.addStreamsProcessor(DisambiguatedProcessor.STREAMS_ID, processor, 100, ElasticsearchPersistReader.STREAMS_ID)
    builder.addStreamsPersistWriter(ElasticsearchPersistWriter.STREAMS_ID, esPersistWriter, 1, DisambiguatedProcessor.STREAMS_ID)
    builder.start

  }
}
