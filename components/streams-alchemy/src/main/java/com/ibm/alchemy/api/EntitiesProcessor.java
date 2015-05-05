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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.streams.components.http.HttpConfigurator;
import org.apache.streams.components.http.HttpProcessorConfiguration;
import org.apache.streams.components.http.processor.SimpleHTTPGetProcessor;
import org.apache.streams.config.StreamsConfigurator;
import org.apache.streams.core.StreamsDatum;
import org.apache.streams.pojo.json.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Enrich actor with account type
 */
public class EntitiesProcessor extends SimpleHTTPGetProcessor {

    public final static String STREAMS_ID = "EntitiesProcessor";

    private final static Logger LOGGER = LoggerFactory.getLogger(AuthorsProcessor.class);

    public EntitiesProcessor() {
        this(HttpConfigurator.detectProcessorConfiguration(StreamsConfigurator.config.getConfig("alchemy")));
    }

    public EntitiesProcessor(HttpProcessorConfiguration alchemyConfiguration) {
        super(alchemyConfiguration);
        LOGGER.info("creating EntitiesProcessor");
        configuration.setEntity(HttpProcessorConfiguration.Entity.ACTIVITY);
    }

    @Override
    public List<StreamsDatum> process(StreamsDatum entry) {

        List<StreamsDatum> result = Lists.newArrayList();

        ObjectNode rootDocument = getRootDocument(entry);

        if( !rootDocument.has("links") ||
                !rootDocument.get("links").isArray() ||
                ((ArrayNode)(rootDocument.get("links"))).size() == 0 )

            return Lists.newArrayList();

        else

            return super.process(entry);

    }
    /**
     Override this to add parameters to the request
     */
    @Override
    protected Map<String, String> prepareParams(StreamsDatum entry) {
        Activity activity = mapper.convertValue(entry.getDocument(), Activity.class);
        Map<String, String> params = Maps.newHashMap();
        params.put("outputMode", "json");
        params.put("url", activity.getLinks().get(0));
        params.put("sentiment", "1");
        params.put("sourceText", "cleaned");
        params.put("knowledgeGraph", "1");
        return params;
    }
};
