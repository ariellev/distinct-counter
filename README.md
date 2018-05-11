# Distinct Counter

Stream data from Kafka and count unique things within this data.

## Prerequisites
1. Docker, Java, [Protobuf compiler](https://github.com/google/protobuf) are installed.
2. A private docker registry is running on localhost:5000. see [here](https://docs.docker.com/registry/deploying/)
3. That private registry is marked 'insecure': [Mac Os ](https://stackoverflow.com/questions/32808215/where-to-set-the-insecure-registry-flag-on-mac-os) | [Linux](https://docs.docker.com/registry/insecure/)

## Build
1. Include `bin` in your local PATH variable `export PATH=$(pwd)/bin:$PATH`
2. `export registry=localhost:5000`
3. Optional: Run `push-all-docker.sh` to recursively build all Dockerfiles and push them to the a docker registry.
4. Run `restart-kafka.sh` to start Kafka and Zookeeper and to set up the relevant topics.
```
-------------------------------------------------------
docker-compose           up
-------------------------------------------------------
..
Creating zookeeper ... done
Creating kafka ... done
Creating kafka ... done
..
-------------------------------------------------------
Creating Kafka Topics
-------------------------------------------------------
Created topic "distinct-counter-ingest".
Created topic "distinct-counter-test".
Created topic "distinct-counter-cardinalities".
Created topic "distinct-counter-frames-processed".
Created topic "distinct-counter-frames-ingested".
Created topic "distinct-counter-uid".
```
5. Build the JAR Artefact: `gradle shadowJar`
6. Upon changes to the messaging model, you may want to generate the protobuf sources:
 `protoc --proto_path=src/main/proto --java_out=src/main/java src/main/proto/model.proto`

## Run
```
> distinct-count.sh start
  -------------------------------------------------------
  distinct-counter           start
  -------------------------------------------------------
  Creating Data Folder, path=output/data
  Creating Log Folder, path=output/logs
  Starting Component Doorman.
  Starting Component Worker.
  Starting Component CardinalitySink.
  Starting Metric Worker frames-processed.
  Starting Metric Worker frames-ingested.
  Starting MetricSink.
  Sleeping 10s..
  Sending Data..
```
The tool creates an `output` folder containing logs and csv files.
```
output/
├── data
│   ├── sink-card.csv
│   └── sink-metric.csv
└── logs
    ├── CardinalitySink.log
    ├── Doorman.log
    ├── MetricSink.log
    └── Worker.log
```

* `sink-card.csv` contains cardinality counts
* `sink-metric.csv` contains metrics

```
> csvlook -I output/data/sink-card.csv

| date             | property | method | window | cardinality | error   |
| ---------------- | -------- | ------ | ------ | ----------- | ------- |
| 2016-07-11 15:39 | uid      | Exact  | 1      | 43598       | 0.0     |
| 2016-07-11 15:39 | uid      | PCSA   | 1      | 5635        | 0.04875 |
| 2016-07-11 15:40 | uid      | PCSA   | 1      | 5499        | 0.04875 |
| 2016-07-11 15:40 | uid      | Exact  | 1      | 41541       | 0.0     |
| 2016-07-11 15:41 | uid      | PCSA   | 1      | 5727        | 0.04875 |
```

# Design

### Overview
![Design](https://github.com/ariellev/distinct-counter/blob/master/distinct-counter.png?raw=true)
### Add more cardinality estimators
The API currently supports 2 basic implementation: Exact & PCSA (Probabilistic Counting with Stochastic Averaging).
To add other estimators such as LogLog, SuperLogLog, HyperLogLog follow the steps below:
1. Implement [ICounter.java](https://github.com/ariellev/distinct-counter/blob/master/src/main/java/org/some/thing/counter/ICounter.java)
2. Start a worker while setting the `worker.method` system property to the newly created coutner class, for example:
`java -Dworker.method=MyNewCounter -cp build/libs/distinct-count-all.jar org.some.thing.component.Worker`
# Resources
