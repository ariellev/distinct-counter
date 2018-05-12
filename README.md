# Distinct Counter

Stream data from Kafka and count unique things within this data.

## Prerequisites
1. Docker, Java, Gradle, [Protobuf compiler](https://github.com/google/protobuf) are installed.
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
6. Upon changes to the messaging model, you may want to generate the protobuf sources.\
 `protoc --proto_path=src/main/proto --java_out=src/main/java src/main/proto/model.proto`

## Run
Create a `data` Folder and copy the input file to `data/stream.jsonl.gz`.\
Alternatively specify a path using the command line option `--in `

```
> distinct-count.sh start

-------------------------------------------------------
distinct-counter           start
-------------------------------------------------------
Creating Data Folder, path=output/data
Creating Log Folder, path=output/logs
Starting Component Doorman.
Starting Component Worker.
Starting Component CSVSink.
Starting Component JsonSink.
Starting Metric Worker frames-processed.
Starting Metric Worker props-ingested.
Starting MetricSink.
Sleeping 10s..
Sending Data..
```
The tool creates an `output` folder containing logs and csv files.
```
output/
├── data
│   ├── sink-card.csv
│   ├── sink-card.json
│   └── sink-metric.csv
└── logs
    ├── CSVSink.log
    ├── Doorman.log
    ├── JsonSink.log
    ├── MetricSink.log
    └── Worker.log
```

* `sink-card.csv` contains cardinality counts in `csv` format.
```
> head output/data/sink-card.csv | csvlook -I

| date                | property | method | window | cardinality | error   |
| ------------------- | -------- | ------ | ------ | ----------- | ------- |
| 2016-07-11 15:39:44 | uid      | PCSA   | 60     | 5635        | 0.04875 |
| 2016-07-11 15:40:44 | uid      | PCSA   | 60     | 5499        | 0.04875 |
| 2016-07-11 15:41:44 | uid      | PCSA   | 60     | 5727        | 0.04875 |
| 2016-07-11 15:42:44 | uid      | PCSA   | 60     | 5885        | 0.04875 |
| 2016-07-11 15:43:44 | uid      | PCSA   | 60     | 5382        | 0.04875 |
| 2016-07-11 15:44:44 | uid      | PCSA   | 60     | 5681        | 0.04875 |
| 2016-07-11 15:45:44 | uid      | PCSA   | 60     | 5696        | 0.04875 |
| 2016-07-11 15:46:44 | uid      | PCSA   | 60     | 5650        | 0.04875 |
| 2016-07-11 15:47:44 | uid      | PCSA   | 60     | 5605        | 0.04875 |
```
* `sink-card.json` contains cardinality counts in `json line` format.
* `sink-metric.csv` contains non-functional metrics.


```
> head output/data/sink-metric.csv | csvlook -I

| date                | property         | method | window | cardinality | error |
| ------------------- | ---------------- | ------ | ------ | ----------- | ----- |
| 2018-05-11 22:20:09 | frames-processed | Head   | 1      | 6           | 0.0   |
| 2018-05-11 22:20:09 | props-ingested   | Head   | 1      | 6           | 0.0   |
| 2018-05-11 22:20:10 | frames-processed | Head   | 1      | 39          | 0.0   |
| 2018-05-11 22:20:10 | props-ingested   | Head   | 1      | 39          | 0.0   |
| 2018-05-11 22:20:11 | frames-processed | Head   | 1      | 48          | 0.0   |
| 2018-05-11 22:20:11 | props-ingested   | Head   | 1      | 48          | 0.0   |
| 2018-05-11 22:20:12 | frames-processed | Head   | 1      | 65          | 0.0   |
| 2018-05-11 22:20:12 | props-ingested   | Head   | 1      | 65          | 0.0   |
| 2018-05-11 22:20:13 | frames-processed | Head   | 1      | 74          | 0.0   |
```


# Stop
```
> distinct-count.sh stop
-------------------------------------------------------
distinct-counter           stop
-------------------------------------------------------
```
Tip: If you keep on getting `Killed: 9` each time stopping the tool, run `set +m` to suppress the message.

# Design

### Overview
![Design](https://github.com/ariellev/distinct-counter/blob/master/distinct-counter.png?raw=true)

| Component        | Description | Remarks
| ---------------- | -------- | ------ |
| Doorman          | Filter on json properties. Enforcing ingress rules & value validation. Example: `uid = "[a-z0-9]{19}"` enforces the `uid` property to contain exactly 19 alphanumeric characters.    | Rules can be applied only to primitive fields
| Worker           | Aggregates, downsamples the data, counter container      | For the sake of simplicity and granularity : 1 counter per json property per topic. This can be generalized to N counter per topic.
| Sink             | Outputs records to csv files      | -

* De- and Serialization are implemented with Protobuf. See: [model.proto](https://github.com/ariellev/distinct-counter/blob/master/src/main/proto/model.proto)
* Configuration is based on [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md). See: [application.conf](https://github.com/ariellev/distinct-counter/blob/master/src/main/resources/application.conf)

### Add more cardinality estimators
The API currently supports 2 basic implementation: Exact & PCSA (Probabilistic Counting with Stochastic Averaging).
To add other estimators such as LogLog, SuperLogLog, HyperLogLog follow the steps below:
1. Implement [ICounter.java](https://github.com/ariellev/distinct-counter/blob/master/src/main/java/org/some/thing/counter/ICounter.java)
2. Start a worker while setting the `worker.method` system property to the newly created coutner class, for example:
`java -Dworker.method=MyNewCounter -cp build/libs/distinct-count-all.jar org.some.thing.component.Worker`
# Resources
* [Probabilistic Counting with Stochastic Averaging](https://research.neustar.biz/2013/04/02/sketch-of-the-day-probabilistic-counting-with-stochastic-averaging-pcsa/)
* [An evaluation of streaming algorithms for distinct counting over a sliding window](https://www.frontiersin.org/articles/10.3389/fict.2015.00023/full)
* [Counting Large Numbers of Events in Small Registers](https://www.inf.ed.ac.uk/teaching/courses/exc/reading/morris.pdf)