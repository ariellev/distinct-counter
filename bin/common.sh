#!/bin/bash

# source this file at the begining of your bash script to import
# example: source $(dirname "$0")/common.sh

bold_text=$(tput bold)
normal_text=$(tput sgr0)

bold() {
    echo ${bold_text}"$@"${normal_text}
}

set +m