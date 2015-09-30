from pox.core import core
import pox.openflow.libopenflow_01 as of

ETH_ARP = 0x0806
ETH_IP =  0x0800

# Lifetime of rules, in seconds.
FLOW_LIFE = 30

"""
	Everything received at a port is forwarded thru the other.
	The bridge learns the src ARP address that comes thru each port, so to filter
	messages doing a loop.

	knownMACs["srcMAC"] = switch_port
"""

class BasicBridge(object):
	def __init__(self, connection):
		self.connection = connection
		self.knownMACs = {}

		self.connection.addListeners(self)

	def installFlowForwarding(self, event, packet, outport, duration = 0):
		msg = of.ofp_flow_mod()
		msg.match = of.ofp_match.from_packet(packet, event.port)
		msg.actions.append(of.ofp_action_output(port = outport))
		msg.data = event.ofp
		if duration > 0:
			msg.hard_timeout = duration
			msg.idle_timeout = duration
		self.connection.send(msg)

	def drop(self, event):
		# Does not install a rule. Just drops this packet.
		if event.ofp.buffer_id is not None:
			msg = of.ofp_packet_out()
			msg.buffer_id = event.ofp.buffer_id
			msg.in_port = event.port
			self.connection.send(msg)

	def forwardPacket(self, event, outPort):
		# Does not install a rule. Just forwards this packet.
		if event.ofp.buffer_id is not None:
			msg = of.ofp_packet_out()
			msg.actions.append(of.ofp_action_output(port = outPort))
			msg.buffer_id = event.ofp.buffer_id
			msg.in_port = event.port
			self.connection.send(msg)

	def _handle_PacketIn(self, event):
		packet = event.parsed
		outPort = 1 if event.port == 2 else 2
		forwarded = False

		if packet.type == ETH_IP:
			self.installFlowForwarding(event, packet, outPort, FLOW_LIFE)
			forwarded = True
		elif packet.type == ETH_ARP:
			srcMac = packet.src.toStr()
			try:
				port = self.knownMACs[srcMac]
				if port == event.port:
					self.forwardPacket(event, outPort)
					forwarded = True
			except KeyError:
				self.knownMACs[srcMac] = event.port
				self.forwardPacket(event, outPort)
				forwarded = True

		if not forwarded:
			self.drop(event)

