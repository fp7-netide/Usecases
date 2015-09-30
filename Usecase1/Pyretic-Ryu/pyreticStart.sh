#!/bin/bash

echo "========================================= NetIDE Pyretic ========================================="

# Update the appropriate modules into pyretic's /modules/ directory
echo "========================================================"
cp pyretic.py ./pyretic/
if [ "$?" -ne "0" ]
	then
	echo "===== Pyretic Repository update error"
else
	echo "===== Pyretic Repository is successfully updated"
fi

echo "========================================================"

cp ryu_client.py ./pyretic/of_client
if [ "$?" -ne "0" ]
	then
	echo "===== Pyretic Repository update error"
else
	echo "===== Pyretic Repository is successfully updated"
fi

# Start Pyretic
./pyretic/pyretic.py -v low -c $1  pyretic.modules.mac_learner

echo "=================================================================================================="
