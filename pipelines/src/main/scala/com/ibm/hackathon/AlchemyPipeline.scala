/*
Copyright 2015 People Pattern Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.ibm.hackathon

// Config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.ibm.alchemy.api.{EntitiesProcessor, TaxonomyProcessor, KeywordsProcessor}
import org.apache.streams.components.http.HttpProcessorConfiguration
import org.apache.streams.config.{ComponentConfigurator, StreamsConfigurator}
import org.apache.streams.converter.{ActivityConverterProcessor, HoconConverterUtil, ActivityConverterUtil, TypeConverterUtil}
import org.apache.streams.core.{StreamsProcessor, StreamBuilder}
import org.apache.streams.elasticsearch._
import org.apache.streams.hdfs.WebHdfsPersistReader
import org.apache.streams.jackson.StreamsJacksonMapper
import org.apache.streams.local.builders.LocalStreamBuilder
import org.apache.streams.pojo.json.{Image, Activity, Actor}
import org.apache.streams.twitter.pojo.User
import com.ibm.hackathon.PipelineConfiguration
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.{mutable, Map}
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
 * adds keywords metadata to documents
 */
object AlchemyPipeline {

  def main(args: Array[String]) = {
    val job: AlchemyPipeline = new AlchemyPipeline()
    new Thread(job).start
  }
}

class AlchemyPipeline(config: PipelineConfiguration) extends Runnable with java.io.Serializable {

  def this() = {
    this(
      new ComponentConfigurator[PipelineConfiguration](classOf[PipelineConfiguration])
        .detectConfiguration(StreamsConfigurator.getConfig)
    )
  }

  private val LOGGER: Logger = LoggerFactory.getLogger(classOf[PipelineConfiguration])

  val STREAMS_ID: String = "AlchemyPipeline"

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
    val esPersistWriter: ElasticsearchPersistWriter = new ElasticsearchPersistWriter(esWriterConfig)

    val alchemyConfig = mapper.convertValue(config.getAlchemy, classOf[HttpProcessorConfiguration])

    var processor : StreamsProcessor = null;
    if( alchemyConfig.getExtension.equals("keywords"))
      processor = new KeywordsProcessor()
    if( alchemyConfig.getExtension.equals("taxonomy"))
      processor = new TaxonomyProcessor()
    if( alchemyConfig.getExtension.equals("entities"))
      processor = new EntitiesProcessor()

    val streamConfig: scala.collection.mutable.Map[String,AnyRef] = scala.collection.mutable.Map[String,AnyRef]()
    val timeout: Int = (7 * 24 * 60 * 1000)

    streamConfig(LocalStreamBuilder.STREAM_IDENTIFIER_KEY) = STREAMS_ID
    streamConfig(LocalStreamBuilder.TIMEOUT_KEY) = timeout.asInstanceOf[AnyRef]

    val javaConfig : java.util.Map[String,Object] = streamConfig.asJava

    val builder: StreamBuilder = new LocalStreamBuilder(10000, javaConfig)
    builder.newPerpetualStream(ElasticsearchPersistReader.STREAMS_ID, esPersistReader)
    builder.addStreamsProcessor(KeywordsProcessor.STREAMS_ID, processor, 1, ElasticsearchPersistReader.STREAMS_ID)
    builder.addStreamsPersistWriter(ElasticsearchPersistWriter.STREAMS_ID, esPersistWriter, 1, KeywordsProcessor.STREAMS_ID)
    builder.start

  }
}