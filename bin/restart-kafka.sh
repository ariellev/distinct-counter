#!/bin/bash
source $(dirname "$0")/common.sh
CID=$(dirname "$0")/../.cid

compose.sh down -f $CID/localhost-compose.yml
sleep 2

docker volume ls | tr -s " "  | cut -d " " -f2 | xargs -I{} docker volume rm {} 2>/dev/null
compose.sh up -f $CID/localhost-compose.yml
sleep 6

ZK_HOST=localhost:2181
echo -------------------------------------------------------
echo "Creating Kafka Topics"
echo -------------------------------------------------------

TOPICS=ingest,test,cardinalities,frames-processed,props-ingested,uid
TOPICS=(${TOPICS//,/ })

for t in ${TOPICS[@]}; do
    topic=distinct-counter-$t
#    echo "Creating topic $(bold $topic)."
    kafka-topics --create --zookeeper $ZK_HOST --replication-factor 1 --partitions 1 --topic $topic
done


