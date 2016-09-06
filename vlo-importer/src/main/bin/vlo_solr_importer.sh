#!/bin/sh

VMOPTS="-Xmx4G"

# Get the script's source directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Filter of the Error stream from which to prevent the admins from getting daily
#  "can't find java" emails
JAVA=`which java 2>/dev/null`
if [  -z $JAVA ]; then
#set java for mpi servers
JAVA=/lat/java/bin/java
fi

#default configuration file
DFLT_CONFIG=${DIR}/"../config/VloConfig.xml"

LOGDIR=${DIR}/../log/
echo Logging in ${LOGDIR}

# Please specify the configuration to the importer via a system property or via
# the command line. In case of a property add 
#
# -DconfigFile=<pathToConfigFile>
#
# and in case of the command line, add
#
# -c <pathToConfigFile>
#
# to the JAVA command. Please note the a specification on the command line will
# take preference over a specification as a property.
# 
# to process only subset of data roots from configuration file pass the list via command line:
# -l path OR -l "path1 path2 ..."
#

$JAVA ${VMOPTS} \
    -cp "${DIR}:${DIR}/vlo-importer-${project.version}-importer.jar" \
	-DconfigFile=${DFLT_CONFIG} \
    -DIMPORTER_LOG_DIR=${LOGDIR} \
    eu.clarin.cmdi.vlo.importer.MetadataImporter "$@"

