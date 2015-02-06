#!/usr/bin/python

#########################################################################################################
###   File: UC1_DataCenter.py
### Author: Georgios Katsikas - katsikas@imdea.org
###   Date: 24/03/2014
#########################################################################################################

from os import popen
from os import environ

import logging

from mininet.util import errRun
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Controller, OVSKernelSwitch, RemoteController
from mininet.cli import CLI

# Configuration Files
#POXDIR = environ[ 'HOME' ] + '/pox/'
IPCONFIG = './IPCONFIG_2.dat'
TOPOLOGY = './TOPOLOGY.dat'

class DCTopology (Topo):

	### Topology class members
	Devices = dict()
	DeviceIface = dict()

	### Topology constructor
	def __init__ (self):
		## Initialize Topology
		super(DCTopology, self).__init__()

		logger = logging.getLogger('NetIDE Logger')

		# Switches and Routers
		R0  = self.addSwitch('R0')
		SW1 = self.addSwitch('SW1')
		SW2 = self.addSwitch('SW2')
		SW3 = self.addSwitch('SW3')

		# Firewalls
		FW1 = self.addSwitch('F4')
		FW2 = self.addSwitch('F5')

		# Servers
		DNS = self.addHost('DNS6')
		Web = self.addHost('Web7')

		# Hosts
		ExtH = self.addHost('ExtH')
		IntH = self.addHost('IntH')

		# Add links
		self.addLink(ExtH, R0)
		self.addLink(R0,  SW1)
		self.addLink(SW1, FW1)
		self.addLink(FW1, SW2)
		self.addLink(SW2, DNS)
		self.addLink(SW2, Web)
		self.addLink(SW2, FW2)
		self.addLink(FW2, SW3)
		self.addLink(SW3, IntH)

		logger.info('NetIDE - UC1 Topology created')

	### Mutator for Devices
	def SetDevices(self, Devices):
		self.Devices = Devices

	### Assign IPs, Masks and Routes to devices
	def SetIPConfiguration(self, Devices):
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

	### Read topology file and load into memory
	def GetTopology(self):
		try:
			with open(TOPOLOGY, 'r') as f:
				for line in f:
					if( len(line.split()) == 0):
						break

					token = line.split()
					self.DeviceIface[token[0]] = list()

					for ip in token[1:len(token)]:
						self.DeviceIface[token[0]].append(ip)

				f.close()
				logger.info('Successfully loaded UC topology \n %s' % self.DeviceIface)

		except EnvironmentError:
			logger.error("Couldn't load config file for the topology, check whether %s exists" % TOPOLOGY)

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
	net = Mininet( topo=DCTopo, controller=None)
	# In case of an out of the box controller, uncomment the following
	net.addController( 'c0', controller=RemoteController, ip='127.0.0.1', port=6633 )
	net.start()
	# Get the devices
	Devices = dict()
	Devices = net.get('ExtH', 'R0', 'SW1', 'F4', 'SW2', 'DNS6', 'Web7', 'F5', 'SW3', 'IntH')
	DCTopo.SetDevices(Devices)

	# Read the Topology configuration
	DCTopo.GetTopology()
	logger.info('==============================================================\n')

	logger.info('====================== IP Configuration ======================')
	# Enforce IP configuration
	DCTopo.SetIPConfiguration(Devices)
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
### Load this module from mininet externally
#########################################################################################################
logger = ConfigureLogger()
topos = { 'DC': ( lambda: UC1_DC_Network() ) }
