#!/bin/bash

# Clean mininet
sudo mn -c #sudo ~/pyretic/mininet/mn -c

# Load the topology specified in the script below
cd ./Topology && sudo ./UC2_IITSystem.py && cd ../
