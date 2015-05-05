#!/usr/bin/env bash

docker pull quay.io/peoplepattern/twitter-follow-graph:latest

docker pull quay.io/peoplepattern/twitter-history-elasticsearch:latest

docker pull quay.io/peoplepattern/streams-ibm:latest

# Collect statuses from self
docker run --name statuses -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/twitter-history-elasticsearch:0.2-incubating-SNAPSHOT java -Dlogback.configuration=/streams/logback-console-info.xml -Dconfig.file=/Users/sblackmon/git/streams-ibm/pipelines/statuses.conf -cp /Users/sblackmon/git/incubator-streams-examples/local/twitter-history-elasticsearch/target/twitter-history-elasticsearch-0.2-SNAPSHOT.jar org.apache.streams.example.twitter.TwitterHistoryElasticsearch

# Collect friends
docker run --name friends.graph -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/twitter-follow-graph:0.2-incubating-SNAPSHOT java -cp jars/twitter-follow-graph-0.2-incubating-SNAPSHOT.jar -Dconfig.file=/home/ubuntu/streams-ibm/pipelines/src/main/resources/friends.conf org.apache.streams.example.graph.TwitterFollowGraph

# Collect statuses from friends
docker run --name friends.history -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/twitter-history-elasticsearch:0.2-incubating-SNAPSHOT java -Dlogback.configurationFile=/streams/logback-console-info.xml -Dconfig.file=/streams/pipelines/friends.statuses.conf -cp jars/twitter-follow-graph-0.2-SNAPSHOT.jar org.apache.streams.example.twitter.TwitterHistoryElasticsearch

# Collect followers
docker run --name followers.history -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/twitter-history-elasticsearch:0.2-incubating-SNAPSHOT java -Dlogback.configurationFile=/streams/logback-console-info.xml -Dconfig.file=/home/ubuntu/streams-ibm/pipelines/followers.conf -cp jars/twitter-follow-graph-0.2-incubating-SNAPSHOT.jar org.apache.streams.example.graph.TwitterFollowGraph

# Collect statuses from followers
docker run --name followers.history -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/twitter-history-elasticsearch:0.2-incubating-SNAPSHOT java -Dlogback.configurationFile=/streams/logback-console-info.xml -Dconfig.file=/streams/pipelines/followers.statuses.conf -cp /twitter-0.2-SNAPSHOT.jar org.apache.streams.example.twitter.TwitterHistoryElasticsearch

# Apply Sentiment Enrichment
docker run --name statuses.sentiment -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/streams-ibm:latest java -Dlogback.configurationFile=/home/ubuntu/streams/logback-console-info.xml -Dconfig.file=/home/ubuntu/streams-ibm/pipelines/src/main/resources/sentiment.conf -cp /home/ubuntu/jars/pipelines-0.3-incubating-SNAPSHOT-jar-with-dependencies.jar com.ibm.hackathon.AlchemyPipeline

# Apply Keywords Enrichment
docker run --name statuses.keywords -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/streams-ibm:latest java -Dlogback.configurationFile=/streams/logback-console-info.xml -Dconfig.file=/home/ubuntu/streams-ibm/pipelines/src/main/resources/keywords.conf -cp /home/ubuntu/jars/pipelines-0.3-incubating-SNAPSHOT-jar-with-dependencies.jar com.ibm.hackathon.AlchemyPipeline

# Apply Taxonomy Enrichment
docker run --name statuses.taxonomy -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/streams-ibm:latest java -Dlogback.configurationFile=/streams/logback-console-info.xml -Dconfig.file=/home/ubuntu/streams-ibm/pipelines/src/main/resources/taxonomy.conf -cp /home/ubuntu/jars/pipelines-0.3-incubating-SNAPSHOT-jar-with-dependencies.jar com.ibm.hackathon.AlchemyPipeline

# Apply Entities Enrichment
docker run --name statuses.entities -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/streams-ibm:latest java -Dlogback.configurationFile=/streams/logback-console-info.xml -Dconfig.file=/home/ubuntu/streams-ibm/pipelines/src/main/resources/entities.conf -cp /home/ubuntu/jars/pipelines-0.3-incubating-SNAPSHOT-jar-with-dependencies.jar com.ibm.hackathon.AlchemyPipeline

# Apply Reindex
docker run --name elasticsearch.reindex -v `pwd`/streams-ibm:/streams quay.io/peoplepattern/streams-ibm:latest java -Dlogback.configurationFile=/home/ubuntu/streams/logback-console-info.xml -Dconfig.file=/home/ubuntu/streams-ibm/pipelines/src/main/resources/reindex.conf -cp /home/ubuntu/jars/elasticsearch-reindex-0.2-incubating-SNAPSHOT.jar org.apache.streams.elasticsearch.example.ElasticsearchReindex