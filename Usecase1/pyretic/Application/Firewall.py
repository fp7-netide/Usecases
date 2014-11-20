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
###        Name: Firewall.py
###      Author: Georgios Katsikas - katsikas@imdea.org
### Description: Firewall configuration for the two firewalls of UC1
###############################################################################################

import os

# Pyretic libraries
from pyretic.lib.std     import *
from pyretic.lib.corelib import *

# Pyretic modules
from pyretic.modules.Commons     import *
from pyretic.modules.mac_learner import mac_learner

class Firewall(DynamicPolicy):
	def __init__(self):
		super(Firewall, self).__init__()

		# Initial policy objects are empty
		self.Blocked = None
		self.Allowed = None
		self.policy  = None

	def ApplyFirewall(self):
		# Set rules to devices
		ac1 = self.ConfigureFW1()
		ac2 = self.ConfigureFW2()

		# Update block object
		if self.Blocked:
			self.Blocked = self.Blocked | ac1 | ac2
		else:
			self.Blocked = ac1 | ac2

		# The packets tha match Block object are dropped. The rest are forwarded.
		self.Allowed = if_(self.Blocked, drop, mac_learner())

		# Update policy object
		return self.UpdatePolicy()

	def UpdatePolicy(self):
		self.policy = self.Allowed
		return self.policy

	def ConfigureFW1(self):
		# Only UDP at port 53
		ICMPtoDNS 	=	(
						match(ethtype=IP, protocol=ICMP, switch=Firewall_1, srcip=ipp1, dstip=ipp2)
					)
		TCPtoDNS	= 	(
						match(ethtype=IP, protocol=TCP,  switch=Firewall_1, srcip=ipp1, dstip=ipp2)
					)
		DNSToInternet	=	(
						match(ethtype=IP, protocol=UDP,  switch=Firewall_1, srcip=ipp1, dstip=ipp2) & ~match(dstport=DNS_PORT)
					)

		DNSToInternet_Reply = 	(
						match(ethtype=IP, protocol=UDP,  switch=Firewall_1, srcip=ipp2, dstip=ipp1) & ~match(srcport=DNS_PORT)
					)

		# Only TCP at port 80
		ICMPtoWeb	=	(
						match(ethtype=IP, protocol=ICMP, switch=Firewall_1, srcip=ipp1, dstip=ipp3)
					)

		UDPtoWeb	=	(
						match(ethtype=IP, protocol=UDP,  switch=Firewall_1, srcip=ipp1, dstip=ipp3)
					)

		HTTPToInternet =	(
						match(ethtype=IP, protocol=TCP,  switch=Firewall_1, srcip=ipp1, dstip=ipp3) & ~match(dstport=HTTP_PORT)
					)

		HTTPToInternet_Reply =	(
						match(ethtype=IP, protocol=TCP,  switch=Firewall_1, srcip=ipp3, dstip=ipp1) & ~match(srcport=HTTP_PORT)
					)

		# Compose the above rules --> FW1 functionality
		p =     (
				DNSToInternet  | DNSToInternet_Reply  | ICMPtoDNS | TCPtoDNS |
				HTTPToInternet | HTTPToInternet_Reply | ICMPtoWeb | UDPtoWeb
			)

		return p

	def ConfigureFW2(self):	
		# Incoming is blocked --> FW2 functionality
		p = 	(
				match(ethtype=IP, dstip=ipp4, switch=Firewall_2)
			)

		return p		

################################################################################
### Test the Firewall functionality
################################################################################
def main():
	fw = Firewall()
	return fw.ApplyFirewall()
