#!/bin/bash
TRACCAR_VERSION=${1:-5.10}

echo "starting build for traccar $TRACCAR_VERSION"

# Build trccar server
{
    echo 'build server binaries'
    ./gradlew build
}

# Build traccar-web
{
    echo 'build client output...'
    ./track-web/tools/package.sh
}


# package build outputs
{
    echo 'package build outputs...'
    cd setup
    ./package.sh $TRACCAR_VERSION other
}
