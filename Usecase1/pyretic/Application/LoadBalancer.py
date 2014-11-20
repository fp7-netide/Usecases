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
###        Name: LoadBalancer.py
###      Author: Omid Alipourfard  - omida@cs.princeton.edu
###      Editor: Georgios Katsikas - katsikas@imdea.org
### Description: Round Robin Load Balancer
###############################################################################################

# Pyretic libraries
from pyretic.lib.std     import *
from pyretic.lib.query   import *
from pyretic.lib.corelib import *

################################################
# Translate from 
#   client -> public address : client -> server
#   server -> client : public address -> client
################################################
def Translate(c, s, p):
	cp = match(srcip=c, dstip=p)
	sc = match(srcip=s, dstip=c)

	return ((cp >> modify(dstip=s)) +
		(sc >> modify(srcip=p)) +
		(~cp & ~sc))

##################################################################
# Simple round-robin load balancing policy                       #
#                                                                #
# This implementation will drop the first packet of each flow.   #
# An easy fix would be to use network.inject_packet to send the  #
# packet to its final destination.                               #
##################################################################
class LoadBalancer(DynamicPolicy):
	def __init__(self, Device, Clients, Servers, PublicIP):
		super(LoadBalancer, self).__init__()
		#print("[Load Balancer]: Device ID: %s" %(Device))
		#print("[Load Balancer]: Server addresses: %s %s" %(Servers[0], Servers[1]))

		self.Device    = Device
		self.Clients   = Clients
		self.Servers   = Servers
		self.PublicIP  = PublicIP
		self.Index     = 0

		# Start a packet query
		self.Query     = packets(1, ['srcip'])
		# Handle events using callback function
		self.Query.register_callback(self.LoadBalancingPolicy)

		# Capture packets that arrive at LB and go to Internet
		self.Public_to_Controller = (match(dstip=self.PublicIP, switch=self.Device)>> self.Query)
		self.LB_Policy = None
		self.policy    = self.Public_to_Controller

	def UpdatePolicy(self):
		self.policy = self.LB_Policy + self.Public_to_Controller

	def LoadBalancingPolicy(self, pkt):
		Client = pkt['srcip']

		# Be careful not to redirect servers on themselves
		if Client in self.Servers: return

		# Round-robin, per-flow load balancing
		Server = self.NextServer()
		p = Translate(Client, Server, self.PublicIP)
		print("[Load Balancer]: Mapping c:%s to s:%s" % (Client, Server))

		# Apply the modifications
		if self.LB_Policy:
			self.LB_Policy = self.LB_Policy >> p
		else:
			self.LB_Policy = p

		# Update LB policy object
		self.UpdatePolicy()

	# Round-robin
	def NextServer(self):
		Server = self.Servers[self.Index % len(self.Servers)]
		self.Index += 1
		return Server

################################################################################
### Test the LB functionality (Outside the UC scope)
################################################################################
def main(Clients, Servers):
	from pyretic.modules.mac_learner import mac_learner

	Clients   = int(Clients)
	Servers   = int(Servers)
	Device    = 4

	IP_Prefix = "10.0.0."
	PublicIP = IP(IP_Prefix + "100")
	print("Public ip address is %s." % PublicIP)
    
	ClientIPs = [IP(IP_Prefix+str(i)) for i in range(1, Clients+1)]
	ServerIPs = [IP(IP_Prefix+str(i)) for i in range(1+Clients, Clients+Servers+1)]
    
	return LoadBalancer(Device, ClientIPs, ServerIPs, PublicIP) >> mac_learner()