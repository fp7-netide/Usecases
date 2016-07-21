#!/bin/bash

# Clean mininet
sudo mn -c

# Load the topology specified in the script below
cd ./Topology && sudo chmod +x UC2_IITSystem.py && sudo ./UC2_IITSystem.py && cd ../
