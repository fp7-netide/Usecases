import sys
from pox.core import core
import pox.openflow.libopenflow_01 as of

# Use the pox example for an l2_learning switch directly:
from pox.forwarding.l2_learning import LearningSwitch
# Use our simplified firewall:
from netide.basic_firewall import BasicFirewall
from netide.basic_bridge import BasicBridge
from netide.basic_loadbalancer import BasicLoadBalancer

PROTO_TCP = 6
PROTO_UDP = 17
ETH_ARP = 0x0806
ETH_IP = 0x0800
PORT_DNS = 53
PORT_WEB = 80

HOST_EXT = "10.0.0.100"
HOST_INT = "10.0.1.32"
HOST_DNS = "10.0.1.17"
HOST_WEB = "10.0.1.18"

log = core.getLogger()

def ForceWrite(message):
	sys.stdout.write(message)
	sys.stdout.flush()

class NetIdeApp(object):
	def __init__(self):
		ForceWrite("Alive!\n")
		core.openflow.addListeners(self)

	def Configure_FW1(self, event):
		# Forward DNS (UDP:53) to H3
		match = of.ofp_match(dl_type = ETH_IP, nw_dst = HOST_DNS, 
					nw_proto = PROTO_UDP, tp_dst = PORT_DNS)
		action = of.ofp_action_output(port = 2)
		msg = of.ofp_flow_mod(match = match, actions = [action], command = of.OFPFC_ADD)
		event.connection.send(msg)

		# Forward WEB (TCP:80) to H4
		match = of.ofp_match(dl_type = ETH_IP, nw_dst = HOST_WEB, 
					nw_proto = PROTO_TCP, tp_dst = PORT_WEB)
		action = of.ofp_action_output(port = 2)
		msg = of.ofp_flow_mod(match = match, actions = [action], command = of.OFPFC_ADD)
		event.connection.send(msg)

	def _handle_ErrorIn(self, event):
		ForceWrite("Error: %s\n", event.ofp.asString())

	def _handle_ConnectionUp(self, event):
		ForceWrite("NetIDEApp::Connection {0} - DPID: {1}\n".format(event.connection, event.dpid))
		if event.dpid == 1: #SW0
			LearningSwitch(event.connection, False)
			ForceWrite("SW0 configured...\n")
		elif event.dpid == 2: #LB_Srv1
			BasicBridge(event.connection)
			ForceWrite("LB_Srv1 configured...\n")
		elif event.dpid == 3: #LB_Srv2
			BasicBridge(event.connection)
			ForceWrite("LB_Srv2 configured...\n")
		elif event.dpid == 4: #LB
			BasicLoadBalancer(event.connection)
			ForceWrite("LB configured...\n")
		elif event.dpid == 5: #R1
			LearningSwitch(event.connection, False)
			ForceWrite("R1 configured...\n")
		elif event.dpid == 6: #SW1
			LearningSwitch(event.connection, False)
			ForceWrite("SW1 configured...\n")
		elif event.dpid == 9: #FW1
			self.Configure_FW1(event) # Open TCP and UDP ports statically
			BasicFirewall(event.connection)
			ForceWrite("FW1 configured...\n")
		elif event.dpid == 7: #SW2
			LearningSwitch(event.connection, False)
			ForceWrite("SW2 configured...\n")
		elif event.dpid == 10: #FW2
			BasicFirewall(event.connection)
			ForceWrite("FW2 configured...\n")
		elif event.dpid == 8: #SW3
			LearningSwitch(event.connection, False)
			ForceWrite("SW3 configured...\n")

		else:
			ForceWrite("NetIDEApp::Event from unknown dpid: {0}\n".format(event.dpid))


def launch():
	ForceWrite("Registering...\n")
	core.registerNew(NetIdeApp)
