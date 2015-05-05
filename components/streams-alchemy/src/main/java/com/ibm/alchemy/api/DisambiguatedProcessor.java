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

package com.ibm.alchemy.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.streams.components.http.HttpConfigurator;
import org.apache.streams.components.http.HttpProcessorConfiguration;
import org.apache.streams.components.http.processor.SimpleHTTPGetProcessor;
import org.apache.streams.config.StreamsConfigurator;
import org.apache.streams.core.StreamsDatum;
import org.apache.streams.core.StreamsProcessor;
import org.apache.streams.jackson.StreamsJacksonMapper;
import org.apache.streams.pojo.json.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Enrich actor with account type
 */
public class DisambiguatedProcessor implements StreamsProcessor {

    public final static String STREAMS_ID = "DisambiguatedProcessor";

    private final static Logger LOGGER = LoggerFactory.getLogger(AuthorsProcessor.class);

    private ObjectMapper MAPPER;

    public DisambiguatedProcessor() {
    }

    @Override
    public List<StreamsDatum> process(StreamsDatum entry) {

        List<StreamsDatum> result = Lists.newArrayList();

        ObjectNode rootDocument = MAPPER.convertValue(entry.getDocument(), ObjectNode.class);

        try {
            ArrayNode entities = (ArrayNode) rootDocument.get("extensions").get("entities").get("entities");

            for( JsonNode entity : entities ) {
                ObjectNode objectNode = (ObjectNode) entity;
                ObjectNode disambiguated = (ObjectNode) entity.get("disambiguated");

                result.add(new StreamsDatum(disambiguated, disambiguated.get("name").asText()));
            }
        } catch( Throwable e ) {

        } finally {
            return result;
        }

    }

    @Override
    public void prepare(Object configurationObject) {
        MAPPER = StreamsJacksonMapper.getInstance();
    }

    @Override
    public void cleanUp() {

    }
};
