#!/bin/bash

ERROR=-1

echo "========================================= NetIDE Pyretic ========================================="

# Update the appropriate modules into pyretic's /modules/ directory
echo "========================================================"
cp ./Application/*.py $HOME/pyretic/pyretic/modules/
if [ "$?" -ne "0" ]
	then
	echo "===== Pyretic Repository update error"
	exit $ERROR
else
	echo "===== Pyretic Repository is successfully updated"
fi
echo "========================================================"
echo ""

# Compile the modules
echo "========================================================"
python -m compileall $HOME/pyretic/pyretic/modules/
if [ "$?" -ne "0" ]
	then
	echo "===== Pyretic Modules' compilation error"
	exit $ERROR
else
	echo "===== Pyretic Modules are compiled"
fi
echo "========================================================"
echo ""

# Start Pyretic with either Ryu or POX
if [ "$1" == "ryu" ]
	then
	echo "===== Pyretic starts with Ryu SDN Controller"
	$HOME/pyretic/pyreticExt.py -m i -v low -c $1 pyretic.modules.UC1_DC_NetManager
elif [ "$1" == "pox" ]
	then
	$HOME/pyretic/pyretic.py -m i -v low pyretic.modules.UC1_DC_NetManager
else
	echo "===== Pyretic can start either with Ryu or POX"
	exit $ERROR
fi

echo "=================================================================================================="
