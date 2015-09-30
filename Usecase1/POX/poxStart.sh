#!/bin/bash

echo "========================================= NetIDE poxApp =========================================="

# Update the appropriate modules into pox's /ext/netide/ directory
echo "========================================================"
if [ ! -d "$HOME/pox/ext/netide" ]; then
  mkdir $HOME/pox/ext/netide
fi
cp ./Application/*.py $HOME/pox/ext/netide/
if [ "$?" -ne "0" ]
	then
	echo "===== pox/ext directory update error"
else
	echo "===== pox/ext/ directory is successfully updated"
fi
echo "========================================================"
echo ""

# Compile the modules
echo "========================================================"
python -m compileall $HOME/pox/ext/netide/
if [ "$?" -ne "0" ]
	then
	echo "===== pox/ext Modules' compilation error"
else
	echo "===== pox/ext Modules are compiled"
fi
echo "========================================================"
echo ""

# Start pox with netide modules:
$HOME/pox/pox.py netide.UC1_DC_pox

echo "=================================================================================================="
