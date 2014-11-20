#!/usr/bin/python
 
""" 
 Copyright (c) 2014, NetIDE Consortium (Create-Net (CN), Telefonica Investigacion Y Desarrollo SA (TID), Fujitsu 
 Technology Solutions GmbH (FTS), Thales Communications & Security SAS (THALES), Fundacion Imdea Networks (IMDEA),
 Universitaet Paderborn (UPB), Intel Research & Innovation Ireland Ltd (IRIIL) )
 
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/epl-v10.html
 
 Authors:
	Georgios Katsikas
"""


###############################################################################################
###        Name: UC1_DataCenter.py
###      Author: Georgios Katsikas - katsikas@imdea.org
### Description: NetIDE UC1 Topology
###############################################################################################

from os import popen
import logging

# Mininet libraries
from mininet.cli  import CLI
from mininet.net  import Mininet
from mininet.topo import Topo
from mininet.util import errRun
from mininet.node import Controller, OVSKernelSwitch, RemoteController

# Configuration Files
IPCONFIG = './IPCONFIG.dat'

class DCTopology (Topo):

	### Topology class members
	Devices = dict()

	### Topology constructor
	def __init__ (self):
		## Initialize Topology
		super(DCTopology, self).__init__()

		logger = logging.getLogger('NetIDE Logger')

		# Load Balancer
		SW0       = self.addSwitch('s1')
		LB_Srv1   = self.addSwitch('s2')
		LB_Srv2   = self.addSwitch('s3')
		LB        = self.addSwitch('s4')

		# Switches and Routers
		R1        = self.addSwitch('s5')
		SW1       = self.addSwitch('s6')
		SW2       = self.addSwitch('s7')
		SW3       = self.addSwitch('s8')

		# Firewalls
		FW1       = self.addSwitch('s9')
		FW2       = self.addSwitch('s10')

		# Hosts
		ExtH      = self.addHost('h1')
		IntH      = self.addHost('h4')

		# Servers
		DNS       = self.addHost('h2')
		Web       = self.addHost('h3')

		# Add links
		self.addLink(ExtH,       SW0)
		self.addLink(SW0,    LB_Srv1)
		self.addLink(SW0,    LB_Srv2)
		self.addLink(LB_Srv1,     LB)
		self.addLink(LB_Srv2,     LB)
		self.addLink(LB,          R1)
		self.addLink(R1,         SW1)
		self.addLink(SW1,        FW1)
		self.addLink(FW1,        SW2)
		self.addLink(SW2,        DNS)
		self.addLink(SW2,        Web)
		self.addLink(SW2,        FW2)
		self.addLink(FW2,        SW3)
		self.addLink(SW3,       IntH)

		logger.info('NetIDE - UC1 Topology created')

	### Mutator for Devices
	def SetDevices(self, Devices):
		self.Devices = Devices

	### Assign IPs, Masks and Routes to devices
	def SetIPConfiguration(self, Devices, net):
		net.get("h1").setIP("10.0.0.100")
		net.get("h2").setIP("10.0.1.17")
		net.get("h3").setIP("10.0.1.18")
		net.get("h4").setIP("10.0.1.32")

		try:
			with open(IPCONFIG, 'r') as f:
				for command in f:
					if( len(command.split()) == 0):
						break

					token = command.split('#')
					for host in Devices:
						if token[0] == host.name:
							for tok in token[1:len(token)]:
								tok.replace("\n", "")
								host.popen(tok)
							break

				logger.info('Successfully enforced initial ip settings to the UC topology')
				f.close()
				
		except EnvironmentError:
			logger.error("Couldn't load config file for ip addresses, check whether %s exists" % IPCONFIG)

### Create a logger and configure it
def ConfigureLogger():
	# Create a log instance
	logger = logging.getLogger('NetIDE Logger')

	# Set logging level
	logger.setLevel(logging.INFO)

	handler = logging.StreamHandler()
	handler.setLevel(logging.INFO)

	# Create a logging format
	formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
	handler.setFormatter(formatter)

	# Add the handlers to the logger
	logger.addHandler(handler)

	return logger

### Network Emulator method
def UC1_DC_Network():

	logger.info('=================== Mininet Topology SetUp ===================')

	# Create UC Topology instance
	DCTopo = DCTopology()

	# Start mininet and load the topology
	net = Mininet( topo=DCTopo, controller=RemoteController, autoSetMacs = True )
	net.start()

	# Get the devices
	Devices = dict()
	Devices = net.get('h1', 'h2', 'h3', 'h4', 's1', 's2', 's3', 's4', 's5', 's6', 's7', 's8', 's9', 's10')
	DCTopo.SetDevices(Devices)
	logger.info('==============================================================\n')

	# Enforce IP configuration
	logger.info('====================== IP Configuration ======================')
	DCTopo.SetIPConfiguration(Devices, net)
	logger.info('==============================================================\n')

	# Start mininet CLI
	CLI( net )

	# Destroy network after exiting the CLI
	net.stop()


#########################################################################################################
### Main
#########################################################################################################
if __name__ == '__main__':
	# Setup the logger
	logger = ConfigureLogger()

	# Start network
	UC1_DC_Network()
#########################################################################################################


#########################################################################################################
### Load this module from mininet externally
#########################################################################################################
logger = ConfigureLogger()
topos = { 'DC': ( lambda: UC1_DC_Network() ) }
#########################################################################################################
