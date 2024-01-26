#!/usr/bin/bash

set -o errexit   # abort on nonzero exitstatus
set -o nounset   # abort on unbound variable
set -o pipefail  # don't hide errors within pipes

mvn -Prelease verify
mvn release:prepare
mvn release:perform || mvn release:clean
git pull
