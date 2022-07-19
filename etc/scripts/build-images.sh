#!/bin/bash

DIRS=$(find . -name "connectors" -type d -print0 | grep -v target | xargs -0 -n1  basename src/generated/resources/META-INF/connectors)

for C in $DIRS; do
    echo $C
done