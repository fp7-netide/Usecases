#!/bin/bash

# Clean mininet
sudo ~/pyretic/mininet/mn -c

# Load the topology specified in the script below
#cd ./Topology && sudo mn --custom ./UC1_DataCenter.py --topo DC --switch ovsk --controller pox --mac --arp && cd ../
cd ./Topology && sudo ./UC1_DataCenter.py && cd ../
