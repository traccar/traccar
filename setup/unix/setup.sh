#!/bin/sh

UNIX_PATH="/opt/traccar"

if which java &>/dev/null
then
  if [ $(java -version 2>&1 | grep -i version | sed 's/.*version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q') -lt 17 ]
  then
    echo "Java 7 or higher required"
  else
    mkdir -p $UNIX_PATH
    cp -rf * $UNIX_PATH
    chmod -R go+rX $UNIX_PATH
    $UNIX_PATH/bin/traccar install
    rm $UNIX_PATH/setup.sh
  fi
else
  echo "Java runtime is required"
fi
