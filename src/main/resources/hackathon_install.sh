#!/usr/bin/env bash

sudo apt-get install docker.io

sudo apt-get install openjdk-7-jre

sudo apt-get install git

sudo apt-get install maven2

docker login quay.io

docker pull quay.io/peoplepattern/neo4j:2.2.0.M4

docker pull quay.io/peoplepattern/elasticsearch:1.5.0

docker run -d --name neo4j -p 7474:7474 quay.io/steveblackmon/neo4j:2.2.0.M4

docker run -d --name elasticsearch1 -p 9200:9200 -p 9300:9300 -e PLUGINS=mobz/elasticsearch-head,elasticsearch/marvel/latest quay.io/peoplepattern/elasticsearch:1.5.0

docker run -d --name elasticsearch2 -p 9201:9200 -p 9301:9300 quay.io/peoplepattern/elasticsearch:1.5.0


