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
###        Name: Commons.py
###      Author: Georgios Katsikas - katsikas@imdea.org
### Description: Global variables for Pyretic Modules
###############################################################################################

# Pyretic libraries
from pyretic.lib import *
from pyretic.lib.corelib import *
from pyretic.lib.std import *

################### IP Setup ##################
# Internet side
ipp1 = IPPrefix('10.0.0.0/24')
# DNS Server
ipp2 = IPAddr('10.0.1.17')
# Web Server
ipp3 = IPAddr('10.0.1.18')
# Intranet
ipp4 = IPAddr('10.0.1.32')

# Load Balancer configuration (Internet side)
LB_Server_1 = IPAddr('10.0.0.1')
LB_Server_2 = IPAddr('10.0.0.2')
PublicIP    = IPAddr('10.0.0.100')
###############################################

# Middleboxes' IDs
Firewall_1 = 9
Firewall_2 = 10
LB_Device  = 4

# Protocols
ICMP = 1
TCP  = 6
UDP  = 17
IP   = 0x0800

# Allowed ports
DNS_PORT  = 53
HTTP_PORT = 80

# Messages
ERROR = -1

# Monitoring Interval period (in seconds)
MonitoringInterval = 5
