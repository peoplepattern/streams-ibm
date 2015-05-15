#! /bin/bash

docker pull quay.io/peoplepattern/twitter-follow-graph:latest

docker pull quay.io/peoplepattern/twitter-history-elasticsearch:latest

docker pull quay.io/peoplepattern/streams-ibm:latest

# Collect statuses from seed user
docker run --name statuses -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/twitter-history-elasticsearch:latest java -Dlogback.configuration=/streams/logback.xml -Dconfig.file=/streams/pipelines/src/main/resources/statuses.conf -cp twitter-history-elasticsearch/target/twitter-history-elasticsearch-0.2-SNAPSHOT.jar org.apache.streams.example.twitter.TwitterHistoryElasticsearch

# Collect friends of seed user
docker run --name friends.graph -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/twitter-follow-graph:latest java -Dlogback.configurationFile=/streams/logback.xml -Dconfig.file=/streams/pipelines/src/main/resources/friends.conf -cp twitter-follow-graph-0.2-incubating-SNAPSHOT.jar org.apache.streams.example.graph.TwitterFollowGraph

# Collect statuses from friends
docker run --name friends.history -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/twitter-history-elasticsearch:latest java -Dlogback.configurationFile=/streams/logback.xml -Dconfig.file=/streams/pipelines/src/main/resources/friends.statuses.conf -cp twitter-follow-graph-0.2-SNAPSHOT.jar org.apache.streams.example.twitter.TwitterHistoryElasticsearch

# Apply Personality Insights Enrichment
docker run --name users.personality -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/streams-ibm:latest java -Dlogback.configurationFile=/streams/logback.xml -Dconfig.file=/streams/pipelines/src/main/resources/reindex.conf -cp elasticsearch-reindex-0.2-incubating-SNAPSHOT.jar org.apache.streams.elasticsearch.example.ElasticsearchReindex

# Collect friends of friends
docker run --name followers.history -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/twitter-history-elasticsearch:latest java -Dlogback.configurationFile=/streams/logback.xml -Dconfig.file=/streams/pipelines/src/main/resources/followers.conf -cp twitter-follow-graph-0.2-incubating-SNAPSHOT.jar org.apache.streams.example.graph.TwitterFollowGraph

# Apply Sentiment Enrichment
docker run --name statuses.sentiment -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/streams-ibm:latest java -Dlogback.configurationFile=/streams/logback.xml -Dconfig.file=/streams/pipelines/src/main/resources/sentiment.conf -cp pipelines-0.3-incubating-SNAPSHOT-jar-with-dependencies.jar com.ibm.hackathon.AlchemyPipeline

# Apply Keywords Enrichment
docker run --name statuses.keywords -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/streams-ibm:latest java -Dlogback.configurationFile=/streams/logback.xml -Dconfig.file=/streams/pipelines/src/main/resources/keywords.conf -cp pipelines-0.3-incubating-SNAPSHOT-jar-with-dependencies.jar com.ibm.hackathon.AlchemyPipeline

# Apply Taxonomy Enrichment
docker run --name statuses.taxonomy -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/streams-ibm:latest java -Dlogback.configurationFile=/streams/logback.xml -Dconfig.file=/streams/pipelines/src/main/resources/taxonomy.conf -cp pipelines-0.3-incubating-SNAPSHOT-jar-with-dependencies.jar com.ibm.hackathon.AlchemyPipeline

# Apply Entities Enrichment
docker run --name statuses.entities -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/streams-ibm:latest java -Dlogback.configurationFile=/streams/logback.xml -Dconfig.file=/streams/pipelines/src/main/resources/entities.conf -cp pipelines-0.3-incubating-SNAPSHOT-jar-with-dependencies.jar com.ibm.hackathon.AlchemyPipeline

# Build Disambiguated Index
docker run --name entity.index -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/streams-ibm:latest java -Dlogback.configurationFile=/streams/logback.xml -Dconfig.file=/streams/pipelines/src/main/resources/disambiguated.conf -cp pipelines-0.3-incubating-SNAPSHOT-jar-with-dependencies.jar com.ibm.hackathon.EntityIndexPipeline

