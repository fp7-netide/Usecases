from pox.core import core
import pox.openflow.libopenflow_01 as of

ETH_ARP = 0x0806
ETH_IP =  0x0800

"""
	Basic load balancer with per-flow round-robin.
	ARP up is always routed thru port 1.
	The bridge learns the src ARP address that comes thru each port, so to filter
	messages doing a loop.
	knownMACs["srcMAC"] = switch_port
"""

class BasicLoadBalancer(object):
	def __init__(self, connection):
		self.connection = connection
		self.lastUpPort = 0
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
		forwarded = False

		if packet.type == ETH_IP:
			if (event.port == 1) or (event.port == 2):
				outPort = 3
			else:
				outPort = self.lastUpPort + 1 # Returns 1 or 2
				self.lastUpPort = (self.lastUpPort + 1) % 2
			self.installFlowForwarding(event, packet, outPort)
			forwarded = True

		elif packet.type == ETH_ARP:
			srcMac = packet.src.toStr()
			if (event.port == 1) or (event.port == 2):
				outPort = 3
			else:
				outPort = 1
			try:
				port = self.knownMACs[srcMac]
				if port == event.port:
					# Forward the packet if comes from the usual port.
					self.forwardPacket(event, outPort)
					forwarded = True
			except KeyError:
				# Register this is the port for these srcMac.
				self.knownMACs[srcMac] = event.port
				self.forwardPacket(event, outPort)
				forwarded = True

			#if (event.port == 1) or (event.port == 2):
			#	outPort = 3
			#else:
			#	outPort = 1
			#self.forwardPacket(event, outPort)
			#forwarded = True

		if not forwarded:
			self.drop(event)

