#!/bin/bash
source $(dirname "$0")/common.sh
set -e

cmd=$1

USAGE="Usage: $(basename $0) [COMMAND] [options]"

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

    echo "Creating Data Folder, path=$(bold $data_folder)"
    mkdir -p $data_folder

    echo "Creating Log Folder, path=$(bold $log_folder)"
    rm -rf $log_folder
    mkdir -p $log_folder

    COMPONENTS=Doorman,Worker,CardinalitySink
    COMPONENTS=(${COMPONENTS//,/ })

    for component in ${COMPONENTS[@]}; do
        echo "Starting Component $(bold $component)."
        java -cp ${jar_file} org.some.thing.component.${component} >> $log_folder/$component.log 2>&1 &
        sleep 1
    done

    # creating metric workers
    METRICS=frames-processed,frames-ingested
    METRICS=(${METRICS//,/ })
    component=Worker

    for metric in ${METRICS[@]}; do
        echo "Starting Metric Worker $(bold $metric)."
        java -Dworker.window=1s -Dworker.method=Exact -Dworker.property=${metric} -Dworker.isMetric=true -cp ${jar_file} org.some.thing.component.${component} >> $log_folder/$component.log 2>&1 &
        sleep 1
    done

    echo "Starting $(bold MetricSink)."
    java -Dsink.name=metric -cp ${jar_file} org.some.thing.component.CardinalitySink >> $log_folder/MetricSink.log 2>&1 &

    echo "Sleeping 10s.."

    sleep 10

    echo $(bold "Sending Data..")
    java -cp ${jar_file} org.some.thing.component.Source >> $log_folder/Source.log 2>&1
    echo "done."
fi

