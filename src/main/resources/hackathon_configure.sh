#! /bin/bash
export ES_HOST=localhost
export ES_PORT=9200
curl -X PUT $ES_HOST:$ES_PORT/_template/defaults -d @defaults.json
curl -X PUT $ES_HOST:$ES_PORT/ibmwatson_activity
curl -X PUT $ES_HOST:$ES_PORT/ibmwatson_activity/_mapping/activity -d @activity_nested.json
