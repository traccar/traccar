#!/bin/sh

UNIX_PATH="/opt/traccar"

if [ $(java -version 2>&1 | grep -i version | sed 's/.*version "\(.*\)\.\(.*\)\..*"/\1\2/; 1q') -lt 17 ]
then
  echo "Please install Java version 7 or higher"
else
  mkdir -p $UNIX_PATH
  cp -rf * $UNIX_PATH
  chmod -R go+rX $UNIX_PATH
  if [ -z "`readelf -A /proc/self/exe | grep Tag_ABI_VFP_args`" ]
  then
    mv $UNIX_PATH/bin/wrapper-linux-armel-32 $UNIX_PATH/bin/wrapper
    mv $UNIX_PATH/lib/libwrapper-linux-armel-32.so $UNIX_PATH/lib/libwrapper.so
  else
    mv $UNIX_PATH/bin/wrapper-linux-armhf-32 $UNIX_PATH/bin/wrapper
    mv $UNIX_PATH/lib/libwrapper-linux-armhf-32.so $UNIX_PATH/lib/libwrapper.so
  fi
  $UNIX_PATH/bin/traccar install
  rm $UNIX_PATH/setup.sh
fi
