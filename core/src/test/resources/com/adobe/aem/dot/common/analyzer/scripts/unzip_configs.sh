#!/bin/bash

#
#    Copyright 2021 Adobe. All rights reserved.
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

###############################################################################
# Unzip configuration files into a folder.  The configurations should all be
# in a zip file - that zip file should hold all the files in ine folder named
# to provide some kind of identifier.
#
# Instructions
# 1. Extract collection of zips (say Sample-Dispatcher-Configs-1.zip) to a folder
#    (say Sample-Dispatcher-Configs-1) in the 'dispatcher/configurations' folder
#    to produce a few folders in 'configurations' containing many zips.
# 2. cd to dispatcher/configurations
# 3. For each folder, enter "../scripts/unzip_configs.sh <folder_name>"
# 4. The zips files will be extracted in the original <folder_name>
# 5. All the zip files will be copied to a "zipfiles" folder.
###############################################################################

if [ -z "$1" ]; then
	echo USAGE: The first parameter must be a subfolder, full of config zips.
	exit
elif [ ! -d "$1" ]; then
	echo USAGE: The first parameter is not a subfolder, full of config zips.
	exit
fi

if [ ! -d "zipfiles" ]; then
	mkdir zipfiles
fi

FILES=$1/*.zip

for cf in $FILES
do
	filename=$(basename -- "$cf")
	extension="${filename##*.}"
	filename="${filename%.*}"

	nextzip=$cf

	if [ ! -d $1/$filename ]; then
		unzip -q -d $1 $nextzip
	fi
	mv $nextzip zipfiles

	lastChar="${cf: -1}"
	if [ $lastChar == "." ]; then
	    mv $cf "${cf::-1}"
	fi

	echo Done $cf
done

echo =====================================================================================================
echo Directories that end in a period are a problem when running IT, and cannot be copied as a resource.
echo These directories are:
find . -name "*." | grep -n ".."
echo Execute this to rename the files:
echo "find . -maxdepth 10 -name \"*.\" -type d -print0 | xargs -0 -n 1 ../scripts/remove_period.sh"

echo =====================================================================================================
echo Some configurations have .git roots defined.  IntelliJ does not like that, and we do not want to
echo accidentally check in changes, so please delete them.
echo GIT repos are as follows:
find . -name ".git"
echo Execute this to delete them:
echo "find . -name \".git\" -type d -exec rm -rf \"{}\" \;"

echo =====================================================================================================
echo Some configurations use environment variable in their include paths.  Set those before running tests.
echo ENV_NAME=dev is one such environment variable to set when testing.
