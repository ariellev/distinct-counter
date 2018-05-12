#!/bin/bash
source $(dirname "$0")/common.sh
set -e

cmd=$1

USAGE="Usage: $(basename $0) [start|stop] [options]"

if [ "$#" == "0" ]; then
	echo "$USAGE"
	exit 1
fi

# shift once to skip COMMAND
shift

while [[ $# -gt 1 ]]
do
key="$1"

case $key in
    -i|--in)
    input="$2"
    shift # past argument
    ;;
    -j|--jar)
    jar_file="$2"
    shift # past argument
    ;;
    -l|--logs)
    log_folder="$2"
    shift # past argument
    ;;
    -o|--output)
    output_folder="$2"
    shift # past argument
    ;;
    *)
					# unknown option
    ;;
esac
shift # past argument or value
done

# default values
if [ -z ${input} ]; then
    input=data/stream.jsonl.gz
fi

if [ -z ${jar_file} ]; then
    jar_file=build/libs/distinct-count-all.jar
fi

if [ -z ${output_folder} ]; then
    output_folder=output
fi

if [ -z ${data_folder} ]; then
    data_folder=${output_folder}/data
fi

if [ -z ${log_folder} ]; then
    log_folder=${output_folder}/logs
fi

echo -------------------------------------------------------
echo "distinct-counter           $(bold $cmd)"
echo -------------------------------------------------------

if [ "${cmd}" = "stop" ]; then
    ps aux | grep distinct | tr -s " " | cut -d" " -f2 | xargs -I{} kill -9 {} >/dev/null 2>&1
else

    echo "Creating $(bold Data) Folder, path=$data_folder"
    mkdir -p $data_folder

    echo "Creating $(bold Log) Folder, path=$log_folder"
    rm -rf $log_folder
    mkdir -p $log_folder

    COMPONENTS=Doorman,Worker,CSVSink,JsonSink
    COMPONENTS=(${COMPONENTS//,/ })

    for component in ${COMPONENTS[@]}; do
        echo "Starting Component $(bold $component)."
        java -cp ${jar_file} org.some.thing.component.${component} >> $log_folder/$component.log 2>&1 &
        sleep 1
    done

    # creating metric workers
    METRICS=frames-processed,props-ingested
    METRICS=(${METRICS//,/ })
    component=Worker

    for metric in ${METRICS[@]}; do
        echo "Starting Metric Worker $(bold $metric)."
        java -Dworker.window=1s -Dworker.method=Head -Dworker.property=${metric} -Dworker.metric=true -cp ${jar_file} org.some.thing.component.${component} >> $log_folder/$component.log 2>&1 &
        sleep 1
    done

    echo "Starting $(bold MetricSink)."
    java -Dsink.name=metric -Dsink.metric=true -cp ${jar_file} org.some.thing.component.CSVSink >> $log_folder/MetricSink.log 2>&1 &

    echo "Sleeping 10s.."

    sleep 10

    echo $(bold "Sending Data") input=${input}
    java -Dsource.path=${input} -cp ${jar_file} org.some.thing.component.Source 1>> $log_folder/Source.log
    echo "done."
fi

