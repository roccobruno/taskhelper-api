#!/usr/bin/env bash

#####################################################
#
# Dummy gradle wrapper that actually uses sbt instead
#
#####################################################

# Change to the directory of this script
cd `dirname $0`

########################
# Download and unzip SBT
########################


if [ -x /sbin/md5 ] ; then
	CHECKSUM=$(md5 -q $0)
else
	# grumble grumble linux
	CHECKSUM=$(md5sum $0 | cut -d' ' -f1)
fi

GRADLEW=gradlew-sbt-$CHECKSUM

if [ ! -d $GRADLEW ]; then
  mkdir $GRADLEW
  curl http://repo.scala-sbt.org/scalasbt/sbt-native-packages/org/scala-sbt/sbt/0.13.1/sbt.zip -o $GRADLEW/sbt.zip
  unzip $GRADLEW/sbt.zip -d $GRADLEW
  
  # Turn off colours to make Jenkins output cleaner
  echo "-no-colors" >> $GRADLEW/sbt/conf/sbtopts
fi

########################
# Run the build
########################

SBT_ARGS=""

while test $# -gt 0; do
  case "$1" in

    clean)
      SBT_ARGS="$SBT_ARGS clean"
    ;;

    test)
      SBT_ARGS="$SBT_ARGS test"
    ;;

    package)
      SBT_ARGS="$SBT_ARGS package rpm:package-bin"
    ;;

    artifactoryPublish)
      SBT_ARGS="$SBT_ARGS publish"
    ;;


  esac

  shift
done

$GRADLEW/sbt/bin/sbt $SBT_ARGS
