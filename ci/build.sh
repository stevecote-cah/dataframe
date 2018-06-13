#!/bin/bash

export TERM=${TERM:-dumb}
cd dataframe
./gradlew --no-daemon build