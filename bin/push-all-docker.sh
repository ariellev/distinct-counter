#!/bin/bash

source $(dirname "$0")/common.sh

# generic script to build and push all docker images
while [[ $# -gt 1 ]]
do
key="$1"

case $key in
    -r|--registry)
    registry="$2"
    shift # past argument
    ;;
    -g|--group)
    group="$2"
    shift # past argument
    ;;
    -t|--tag)
    tag="$2"
    shift # past argument
    ;;
    *)
					# unknown option
    ;;
esac
shift # past argument or value
done

# default values
if [ ${group} ]; then
	group=$group/
fi

if [ -z ${tag} ]; then
    tag=latest
fi

if [[ -z "$registry" ]]; then
  registry=localhost:5000
fi

for f in $(find . -name "Dockerfile" | grep -v node_modules | grep -v src); do
  docker_folder=$(dirname "$(realpath $f)")
  image=$(echo $docker_folder | rev | cut -d "/" -f1 | rev)
  push-docker.sh -r $registry -f $docker_folder -i $group$image:$tag
done
