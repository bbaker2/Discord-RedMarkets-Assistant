#!/bin/bash
# Update the WORKSPACE to point the the git directory
WORKSPACE=../
# Update the TRG_DIR to point to the folder location
# you want the compiled jar to output too and the
# database to be created in
TRG_DIR=/update/to/folder/where/you/want/bot/to/exeute/from
# Update the ARGS to point to the property file that
# holds the launch arguments needed for the bot
ARGS=/update/to/folder/where/bot/config/exists/bot.properties

# That is
NC="\033[0m"
RD="\033[0;31m"
GN="\033[0;32m"

JAR=discord_redmarket-jar-with-dependencies.jar

# Read the user arguments to see if they wish to
# refresh, build, and/or launch
while [ $# -ne 0 ]
do
	arg="$1"
	case "$arg" in
		refresh) refresh=true;;
		build) build=true;;
		launch) launch=true;;
	esac
	shift
done

if [[ $refresh || $build ]]
then
	# Moved to the workspace
	echo Working with repo: ${WORKSPACE}
	pushd ${WORKSPACE}
fi

if [ $refresh ]
then
	# Grab the latest from the remote, then check the status
	git fetch > /dev/null 2>&1

	# Use the status result to determin if we can short circuit
	remoteBranch=$(git rev-parse @{u})

	# check to see if the local branch matches the remote branch
	if [ $(git rev-parse @) = $remoteBranch ]; then
		echo -e ${GN}No code change detected${NC}
	else
		# update git to the remote branch
		echo -e ${GN}New change detected${NC}
		git reset --hard ${remoteBranch}
		git clean -f
		# since a change was detected, force a new build
		build=true
	fi
fi

if [ $build ]
then
	echo Building with Maven
	mvn -f pom.xml clean package
	echo -e Copying ${GN}${JAR}${NC} to ${GN}${TRG_DIR}${NC}
	cp target/${JAR} ${TRG_DIR}
fi

if [[ $refresh || $build ]]
then
	echo Exiting from ${WORKSPACE}
	popd
fi

if [ $launch ]
then
	echo Entering ${TRG_DIR}
	pushd ${TRG_DIR}
	echo -e ${GN}Starting up Gnat${NC}
	echo "java -jar ${JAR} ${ARGS}"
	java -jar ${JAR} ${ARGS}
	popd
fi
