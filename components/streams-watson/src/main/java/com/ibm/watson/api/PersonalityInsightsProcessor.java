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

package com.ibm.watson.api;

import com.google.common.collect.Maps;
import org.apache.streams.components.http.HttpConfigurator;
import org.apache.streams.components.http.HttpProcessorConfiguration;
import org.apache.streams.components.http.processor.SimpleHTTPGetProcessor;
import org.apache.streams.config.StreamsConfigurator;
import org.apache.streams.core.StreamsDatum;
import org.apache.streams.pojo.extensions.ExtensionUtil;
import org.apache.streams.pojo.json.Activity;
import org.apache.streams.pojo.json.ActivityObject;
import org.apache.streams.pojo.json.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Enrich actor with demographics
 */
public class PersonalityInsightsProcessor extends SimpleHTTPGetProcessor {

    public final static String STREAMS_ID = "PersonalityInsightsProcessor";

    private final static Logger LOGGER = LoggerFactory.getLogger(PersonalityInsightsProcessor.class);

    public PersonalityInsightsProcessor() {
        this(HttpConfigurator.detectProcessorConfiguration(StreamsConfigurator.config.getConfig("watson")));
    }

    public PersonalityInsightsProcessor(HttpProcessorConfiguration watsonConfiguration) {
        super(watsonConfiguration);
        LOGGER.info("creating PersonalityInsightsProcessor");
        configuration.setProtocol("https");
        configuration.setResourcePath("/v2/profile");
        configuration.setEntity(HttpProcessorConfiguration.Entity.ACTOR);
        configuration.setExtension("personality");
    }

    /**
     Override this to add parameters to the request
     */
    @Override
    protected Map<String, String> prepareParams(StreamsDatum entry) {
        Activity activity = mapper.convertValue(entry.getDocument(), Activity.class);
        Actor actor = activity.getActor();
        ActivityObject actorObject = mapper.convertValue(actor, ActivityObject.class);
        String username = (String) ExtensionUtil.getExtension(actorObject, "screenName");
        Map<String, String> params = Maps.newHashMap();
        params.put("id", actor.getId());
        params.put("name", actor.getDisplayName());
        params.put("username", username);
        params.put("description", actor.getSummary());
        return params;
    }

};
