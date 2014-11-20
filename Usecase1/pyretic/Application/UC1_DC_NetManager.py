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
###        Name: UC1_DC_NetManager.py
###      Author: Georgios Katsikas - katsikas@imdea.org
### Description: Pyretic Implementation of NetIDE UC1 - Main Module
###############################################################################################

import os

# Pyretic libraries
from pyretic.lib.std     import *
from pyretic.lib.corelib import *

# Generic Pyretic Modules
from pyretic.modules.Commons          import *
from pyretic.modules.Monitor          import Monitor
from pyretic.modules.Firewall         import Firewall
from pyretic.modules.mac_learner      import mac_learner
from pyretic.modules.LoadBalancer     import LoadBalancer

### Main class for UC1 DataCenter Implementation
class UC1_DC_NetManager(DynamicPolicy):
	def __init__(self):
		super(UC1_DC_NetManager, self).__init__()
		self.FW = Firewall()
		self.policy = None

		# Initialize and Start
		self.SetInitialState()

	# Initial configuration of DC Application
	def SetInitialState(self):
		# LB configuration
		self.LB_Device = LB_Device
		self.PublicIP  = PublicIP
		self.ServerIPs = [LB_Server_1, LB_Server_2]
		self.ClientIPs = [ipp2, ipp3, ipp4]

		# Firewall configuration
		self.FWDevices = [Firewall_1, Firewall_2]

		return self.Start()

	# Dynamically update enforced policy based on the last values of all the modules
	def Start(self):
		# Handle ARP
		ARPPkt = match(ethtype=ARP_TYPE)

		# Instantiate Firewalls
		AccessControl = self.FW.ApplyFirewall()

		# Instantiate Load Balancer
		LB  = LoadBalancer(self.LB_Device, self.ClientIPs, self.ServerIPs, self.PublicIP)

		self.policy = 	(
					( ARPPkt >> mac_learner() ) +				# ARP - L2 Learning Switches
					( LB >> mac_learner() >> AccessControl ) +	# Load Balancer + Firewall
					Monitor(MonitoringInterval)					# Monitoring
				)

		return self.policy

################################################################################
### Bootstrap Use Case
################################################################################
def main():
	return UC1_DC_NetManager()
