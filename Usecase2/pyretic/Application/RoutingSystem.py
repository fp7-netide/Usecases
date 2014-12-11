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
	Elisa Rojas
"""

###############################################################################################
###        Name: RoutingSystem.py
###      Author: Elisa Rojas - elisa.rojas@imdea.org
### Description: Routing System for the UC2 (TODO: Still to be improved)
###############################################################################################

# Pyretic libraries
from pyretic.lib.std     import *
from pyretic.lib.query   import *
from pyretic.lib.corelib import *

##################################################################
# Simple up/down routing applied to the architecture in UC2      #
#                                                                #
# This implementation will drop the first packet of each flow.   #
# An easy fix would be to use network.inject_packet to send the  #
# packet to its final destination.   ###                         #
##################################################################
class RoutingSystem(DynamicPolicy):
	def __init__(self, SwitchIDs, HostIPs):
	    super(RoutingSystem, self).__init__()
	    print("[Routing System]: __init__: %s %s" %(SwitchIDs, HostIPs)) ###
	    
	    self.topology       = None
	    self.flood          = flood()

	    self.SwitchIDs      = SwitchIDs
	    self.SwitchStates   = None
	    self.reset_network_state()
	    self.HostIPs        = HostIPs
            self.nSwitch        = len(SwitchIDs)
            self.nHost          = len(HostIPs)

	    self.SwitchPolicies = [None, None, None, None, None, None, None, None]
	    self.EgressPolicy = drop
		 
            self.set_initial_state()

    	def set_initial_state(self):
            print("[Routing System]: set_initial_state") ###
            # Start a query of all the packets arriving in order to route them in the topology
	    self.query = packets(1, ['srcip','switch']) #packets() #self.query = packets(1, ['srcip']) #1, ['srcip', 'switch'] # Start a packet query
	    self.query.register_callback(self.RoutingSystemPolicy) # Handle events using callback function
            self.forward = flood() # Initially floods all packet via ST #identity - return all packets (unmodified), which will later go to mac_learner
         
	    # Capture all packets but ARP ones
	    #self.all_packets = ~match(ethtype=ARP_TYPE) >> self.query
            self.all_packets = identity >> self.query #takes all packets, but only non ARP packets are routed following the system and ARP packets are just used for learning egress locations and updating their routing policies

	    self.update_policy()

	def reset_network_state(self):
            #Switches state (first value is switch state, while the rest are ports states) #1:UP, 0:DOWN

            self.SW1State = [0, [0, 0, 0, 0, 0, 0, 0, 0, 0]]
            self.SW2State = [0, [0, 0, 0, 0, 0, 0, 0, 0, 0]]
            self.SW3State = [0, [0, 0, 0, 0, 0, 0, 0, 0, 0]]
            self.SW4State = [0, [0, 0, 0, 0, 0, 0, 0, 0, 0]]
            self.SW1bState = [0, [0, 0, 0, 0, 0, 0, 0, 0, 0]]
       	    self.SW2bState = [0, [0, 0, 0, 0, 0, 0, 0, 0, 0]]
            self.SW3bState = [0, [0, 0, 0, 0, 0, 0, 0, 0, 0]]
            self.SW4bState = [0, [0, 0, 0, 0, 0, 0, 0, 0, 0]]
	    self.SwitchStates = [self.SW1State, self.SW2State, self.SW3State, self.SW4State, 
				self.SW1bState, self.SW2bState, self.SW3bState, self.SW4bState] 

	    """
            self.SW1State = [1, [1, 1, 1, 1, 1, 1, 1, 1, 1]]
            self.SW2State = [1, [1, 1, 1, 1, 1, 1, 1, 1, 1]]
            self.SW3State = [1, [1, 1, 1, 1, 1, 1, 1, 1, 1]]
            self.SW4State = [1, [1, 1, 1, 1, 1, 1, 1, 1, 1]]
            self.SW1bState = [1, [1, 1, 1, 1, 1, 1, 1, 1, 1]]
       	    self.SW2bState = [1, [1, 1, 1, 1, 1, 1, 1, 1, 1]]
            self.SW3bState = [1, [1, 1, 1, 1, 1, 1, 1, 1, 1]]
            self.SW4bState = [1, [1, 1, 1, 1, 1, 1, 1, 1, 1]]
	    self.SwitchStates = [self.SW1State, self.SW2State, self.SW3State, self.SW4State, 
				self.SW1bState, self.SW2bState, self.SW3bState, self.SW4bState] 
	    """

	def default_routing(self):
	    """Returns 'true' if the network uses default routing (no faulty links/switches)"""
	    for state in self.SwitchStates:
		if state[0] == 0:
		    return false
	    return true

	def add_policy(self,switch,policy):
            print("[Routing System]: add_policy")
	    """Update policy for a specific switch"""
	    if self.SwitchPolicies[switch] is None:
		self.SwitchPolicies[switch] = policy
	    else:
		self.SwitchPolicies[switch] = self.SwitchPolicies[switch] + policy
            #print("  %s<-%s = %s" %(switch, policy, self.SwitchPolicies[switch]))

	def update_policy(self):
	    print("[Routing System]: update_policy")
	    """Update policy (all switches)"""
	    i=0

	    if self.EgressPolicy is not None:
		self.forward = self.EgressPolicy
		i=1

	    for policy in self.SwitchPolicies:
		#Only add policy if it is not None
		if policy is not None:
		    if i==0:
		    	self.forward = policy
		    	i=1
		    else:
		    	self.forward = self.forward + policy
		#If policy is None -> discard packets (i.e. no policy added to main policy)

	    self.policy = if_(~match(ethtype=ARP_TYPE),self.forward,drop) + self.all_packets #we just forward non ARP packets (ARP packets are dropped, since they are just used for learning egress locations)
	    print("  self.policy %s" %self.policy)

    	def set_network(self,network):
            print("[Routing System]: set_network") ###
            #TODO: See what's changed in the network, update self.SwitchStates and policy (if applicable)

	    """Save network information for later forwarding"""
            changed = False
            if not network is None:
            	updated_network = network.topology
            	if not self.topology is None:
                    if self.topology != updated_network:
                    	self.topology = updated_network
                    	changed = True
                else:
                    self.topology = updated_network
                    changed = True
            if changed:
            	self.flood = parallel([
                    match(switch=switch) >>
                    parallel(map(xfwd,ports))
                    for switch,ports 
                    in self.topology.switch_with_port_ids_list()])

	        print("  self.flood: %s" %self.flood)
	    	print("  self.topology: %s" %self.topology)

		"""Update network state"""
		self.reset_network_state()
        	for switch in self.topology.nodes():
		    self.SwitchStates[switch-1][0] = 1 #If the switch is active, becomes UP (1)
		dr = self.default_routing()

		"""Reset all switch policies"""
                self.SwitchPolicies = [None, None, None, None, None, None, None, None]
		mainBranch = [0, 0, 0, 0] #default routing has 4 main branches to distribute all the traffic, if some of them is down (either link's down or switch's down), we should add an alternative route 


		"""Update policies for every switch with new network information (inter-switch routing)"""
		for (s1,s2,port_nos) in self.topology.edges(data=True):
		    print("  edge: %s %s %s" %(s1,s2,port_nos))

                    #Default routing
		    if s1==self.SwitchIDs[0] and s2==self.SwitchIDs[1]:
		    	p1 = (match(switch=s1,dstip=self.HostIPs[1]) | match(switch=s1,dstip=self.HostIPs[3])) >> xfwd(port_nos[s1])
		    	p2 = (match(switch=s2,dstip=self.HostIPs[0]) | match(switch=s2,dstip=self.HostIPs[2])) >> xfwd(port_nos[s2])
		    	self.add_policy(s1,p1)
			self.add_policy(s2,p2)
                        mainBranch[0] = 1 #mainBranch 1 is up (S1-S2)
		    elif s1==self.SwitchIDs[0] and s2==self.SwitchIDs[2]:
			p3 = match(switch=s1,dstip=self.HostIPs[4]) >> xfwd(port_nos[s1])
			p4 = (match(switch=s2,dstip=self.HostIPs[0]) | match(switch=s2,dstip=self.HostIPs[2])) >> xfwd(port_nos[s2])
			self.add_policy(s1,p3)
			self.add_policy(s2,p4)
                        mainBranch[1] = 1 #mainBranch 2 is up (S1-S3)
	            elif s1==self.SwitchIDs[1] and s2==self.SwitchIDs[2]:
			p5 = match(switch=s1,dstip=self.HostIPs[4]) >> xfwd(port_nos[s1])
			p6 = (match(switch=s2,dstip=self.HostIPs[1]) | match(switch=s2,dstip=self.HostIPs[3])) >> xfwd(port_nos[s2])
			self.add_policy(s1,p5)
			self.add_policy(s2,p6)
                        mainBranch[2] = 1 #mainBranch 3 is up (s2-S3)
		    elif s1==self.SwitchIDs[2] and s2==self.SwitchIDs[3]:
			p7 = match(switch=s1,dstip=self.HostIPs[4]) >> xfwd(port_nos[s1])
			p8 = (match(switch=s2,dstip=self.HostIPs[0]) | match(switch=s2,dstip=self.HostIPs[1]) \
			    | match(switch=s2,dstip=self.HostIPs[2]) | match(switch=s2,dstip=self.HostIPs[3])) \
			    >> xfwd(port_nos[s2])
			self.add_policy(s1,p7)
			self.add_policy(s2,p8)
                        mainBranch[3] = 1 #mainBranch 4 is up (S3-S4)

                for (s1,s2,port_nos) in self.topology.edges(data=True):
		    print("  edge: %s %s %s" %(s1,s2,port_nos))

                    #Non-default routing ###REVISAR (hay más opciones de caídas por cada enlace.......)
                    if mainBranch[0] == 0:
                        if s1==self.SwitchIDs[4] and s2==self.SwitchIDs[5]:
                            p1 = (match(switch=s1,dstip=self.HostIPs[1]) | match(switch=s1,dstip=self.HostIPs[3])) >> xfwd(port_nos[s1])
		    	    p2 = (match(switch=s2,dstip=self.HostIPs[0]) | match(switch=s2,dstip=self.HostIPs[2])) >> xfwd(port_nos[s2])
		    	    self.add_policy(s1,p1)
			    self.add_policy(s2,p2)
                    if mainBranch[1] == 0:
		        if s1==self.SwitchIDs[4] and s2==self.SwitchIDs[2]: 
			    p3 = match(switch=s1,dstip=self.HostIPs[4]) >> xfwd(port_nos[s1])
			    p4 = (match(switch=s2,dstip=self.HostIPs[0]) | match(switch=s2,dstip=self.HostIPs[2])) >> xfwd(port_nos[s2])
			    self.add_policy(s1,p3)
			    self.add_policy(s2,p4)
                    if mainBranch[2] == 0:
	                if s1==self.SwitchIDs[5] and s2==self.SwitchIDs[2]:
			    p5 = match(switch=s1,dstip=self.HostIPs[4]) >> xfwd(port_nos[s1])
			    p6 = (match(switch=s2,dstip=self.HostIPs[1]) | match(switch=s2,dstip=self.HostIPs[3])) >> xfwd(port_nos[s2])
			    self.add_policy(s1,p5)
			    self.add_policy(s2,p6)



		self.update_policy()	
            
	def RoutingSystemPolicy(self, pkt):

            SwitchID = pkt['switch']
            print("[Routing System]: Switch ID: %s; Input port: %s" %(SwitchID,pkt['inport']))
	    #print pkt

	    if pkt['ethtype'] == IP_TYPE:
		print "  Ethernet packet"
	    
            elif pkt['ethtype'] == ARP_TYPE:
	        print "  ARP packet"

                """Update policy with MAC learning for egress locations (host-switch routing)"""
	        #egress = "%s[%s]" %(pkt['switch'],pkt['inport'])
	        #print("  egress: %s -> %s" %(egress,self.topology.egress_locations(pkt['switch'])))
	        for el in self.topology.egress_locations(pkt['switch']):
		    print("    el: %s -> %s" %(el,el.port_no))
	            if pkt['inport'] == el.port_no:
		        #self.EgressPolicy = if_(match(dstmac=pkt['srcmac'],switch=pkt['switch']),fwd(pkt['inport']),self.EgressPolicy) #weird MAC for secondary interfaces
                        self.EgressPolicy = if_(match(dstip=pkt['srcip'],switch=pkt['switch']),fwd(pkt['inport']),self.EgressPolicy) 
		        print("      self.EgressPolicy: %s" %self.EgressPolicy)

            # If switch is backup and not active
            if SwitchID in self.SwitchIDs:
                SwitchIndex = self.SwitchIDs.index(SwitchID)

		# If "backup" switch, we first need to know if the other switch is not available, otherwise, discard packet
                if SwitchIndex >= self.nSwitch/2 and self.SwitchStates[SwitchIndex-self.nSwitch/2][0]: 
                    print("[Routing System]: Discarding packet...")

		# If not "backup" or "backup" active (because the other switch is not available)

            else:
                print("[Routing System]: inconsistency ERROR!")
                return
	   
	    self.update_policy()

            #print("  self.topology: %s" %self.topology)


