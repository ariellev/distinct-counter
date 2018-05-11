#!/bin/bash
# script to push docker image

source $(dirname "$0")/common.sh

while [[ $# -gt 1 ]]
do
key="$1"

case $key in
    -r|--registry)
    registry="$2"
    shift # past argument
    ;;
    -f|--file)
    dockerfile="$2"
    shift # past argument
    ;;
    -i|--image)
    image="$2"
    shift # past argument
    ;;
    *)
					# unknown option
    ;;
esac
shift # past argument or value
done

#cd $(dirname "$0")

if [[ -z "$registry" ]]; then
  registry=localhost:5000
fi

if [[ -z "$dockerfile" ]]; then
  dockerfile=.
fi

echo "------------------------------------------------"
echo "$(bold registry)    ${registry}"
echo "$(bold dockerfile)  ${dockerfile}"
echo "$(bold image)       ${image}"
echo "------------------------------------------------"

docker build $docker_args -t $registry/$image $dockerfile
docker push $registry/$image
