#!/bin/bash

# Clean mininet
sudo ~/pyretic/mininet/mn -c

# Load the topology specified in the script below
cd ./Topology && sudo ./UC2_IITSystem.py && cd ../
