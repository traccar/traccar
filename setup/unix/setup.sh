#!/bin/sh

UNIX_PATH="/opt/traccar"

if [ $(java -version 2>&1 | grep -i version | sed 's/.*version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q') -lt 17 ]
then
  echo "Please install Java version 7 or higher"
else
  mkdir -p $UNIX_PATH
  cp -rf * $UNIX_PATH
  chmod -R go+rX $UNIX_PATH
  $UNIX_PATH/bin/traccar install
  rm $UNIX_PATH/setup.sh
fi
