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
import com.ibm.watson.api.{PersonalityInsightsProcessor, PersonalityInsights, LanguageIdentificationProcessor}
import org.apache.streams.components.http.HttpProcessorConfiguration
import org.apache.streams.config.{ComponentConfigurator, StreamsConfigurator}
import org.apache.streams.converter.{ActivityConverterProcessor, HoconConverterUtil, ActivityConverterUtil, TypeConverterUtil}
import org.apache.streams.core.{StreamsProcessor, StreamBuilder}
import org.apache.streams.elasticsearch._
import org.apache.streams.graph.{GraphWriterConfiguration, GraphPersistWriter}
import org.apache.streams.hdfs.WebHdfsPersistReader
import org.apache.streams.jackson.StreamsJacksonMapper
import org.apache.streams.local.builders.LocalStreamBuilder
import org.apache.streams.pojo.json.{Image, Activity, Actor}
import org.apache.streams.twitter.pojo.User
import com.ibm.hackathon.ProfilePipelineConfiguration
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.{mutable, Map}
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
 * adds keywords metadata to documents
 */
object WatsonPipeline {

  def main(args: Array[String]) = {
    val job: WatsonPipeline = new WatsonPipeline()
    new Thread(job).start
  }
}

class WatsonPipeline(config: ProfilePipelineConfiguration) extends Runnable with java.io.Serializable {

  def this() = {
    this(
      new ComponentConfigurator[ProfilePipelineConfiguration](classOf[ProfilePipelineConfiguration])
        .detectConfiguration(StreamsConfigurator.getConfig)
    )
  }

  private val LOGGER: Logger = LoggerFactory.getLogger(classOf[ProfilePipelineConfiguration])

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

    val graphWriterConfig = mapper.convertValue(config.getDestination, classOf[GraphWriterConfiguration])
    val graphPersistWriter: GraphPersistWriter = new GraphPersistWriter(graphWriterConfig)

    val watsonConfig = mapper.convertValue(config.getWatson, classOf[HttpProcessorConfiguration])

    var processor : StreamsProcessor = null;
    if( watsonConfig.getExtension.endsWith("language"))
      processor = new LanguageIdentificationProcessor()
    if( watsonConfig.getExtension.endsWith("personality"))
      processor = new PersonalityInsightsProcessor()

    val streamConfig: scala.collection.mutable.Map[String,AnyRef] = scala.collection.mutable.Map[String,AnyRef]()
    val timeout: Int = (7 * 24 * 60 * 1000)

    streamConfig(LocalStreamBuilder.STREAM_IDENTIFIER_KEY) = STREAMS_ID
    streamConfig(LocalStreamBuilder.TIMEOUT_KEY) = timeout.asInstanceOf[AnyRef]

    val javaConfig : java.util.Map[String,Object] = streamConfig.asJava

    val builder: StreamBuilder = new LocalStreamBuilder(10000, javaConfig)
    builder.newReadCurrentStream(ElasticsearchPersistReader.STREAMS_ID, esPersistReader)
    builder.addStreamsProcessor("batch", processor, 1, ElasticsearchPersistReader.STREAMS_ID)
    builder.addStreamsProcessor("processor", processor, 1, "batch")
    builder.addStreamsPersistWriter(GraphPersistWriter.STREAMS_ID, graphPersistWriter, 1, "processor")
    builder.start

  }
}