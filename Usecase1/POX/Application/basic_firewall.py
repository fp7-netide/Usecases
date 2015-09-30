import time

from pox.core import core
import pox.openflow.libopenflow_01 as of

ETH_ARP = 0x0806
ETH_IP =  0x0800

ICMP_FLOW_LIFE = 5	# seconds.

"""
	Active connections are remembered in a dictionary. Two types:

	[(ICMP, srcip, dstip)] = expiry time
	[(TCP/UDP, srcip, srcport, dstip, dstport)] = None
"""

class BasicFirewall(object):
	def __init__(self, connection):
		self.connection = connection
		self.activeConnections = {}

		self.connection.addListeners(self)

		# Flood ARP
		match = of.ofp_match(dl_type = ETH_ARP)
		action = of.ofp_action_output(port = of.OFPP_FLOOD)
		msg = of.ofp_flow_mod(match = match, actions = [action], command = of.OFPFC_ADD)
		self.connection.send(msg)

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

	def _handle_PacketIn(self, event):
		"""
			For ICMP packets, installs a rule to allow them. Remembers the connection
		so that the answer will be installed as well. The rules are uninstalled after a delay.
			For TCP/UDP, installs the outgoing rule for the flow. Remembers the connection
		so that when the answers arrive, the corresponding rules will also be installed.
		"""
		packet = event.parsed
		forwarded = False

		if packet.type == ETH_IP:
			ipPacket = packet.payload
			if ipPacket.protocol == ipPacket.ICMP_PROTOCOL:
				if event.port == 2:
					tupla = (ipPacket.ICMP_PROTOCOL,
							ipPacket.srcip.__str__(), ipPacket.dstip.__str__())
					self.activeConnections[tupla] = time.time() + ICMP_FLOW_LIFE
					self.installFlowForwarding(event, packet, 1, ICMP_FLOW_LIFE)
					forwarded = True
				elif event.port == 1:
					tupla = (ipPacket.ICMP_PROTOCOL,
							ipPacket.dstip.__str__(), ipPacket.srcip.__str__())
					try:
						if self.activeConnections[tupla] >= time.time():
							self.installFlowForwarding(event, packet, 2, ICMP_FLOW_LIFE)
							forwarded = True
						else:
							# Drop connection. Packet dropped below.
							self.activeConnections.pop(tupla)
					except KeyError:
						pass

			elif (ipPacket.protocol == ipPacket.TCP_PROTOCOL) or \
				 (ipPacket.protocol == ipPacket.UDP_PROTOCOL):
				if event.port == 2:
					tupla = (ipPacket.protocol,
							ipPacket.srcip.__str__(), ipPacket.payload.srcport,
							ipPacket.dstip.__str__(), ipPacket.payload.dstport)
					self.activeConnections[tupla] = None
					self.installFlowForwarding(event, packet, 1)
					forwarded = True
				elif event.port == 1:
					tupla = (ipPacket.protocol,
							ipPacket.dstip.__str__(), ipPacket.payload.dstport,
							ipPacket.srcip.__str__(), ipPacket.payload.srcport)
					if tupla in self.activeConnections:
						self.installFlowForwarding(event, packet, 2)
						forwarded = True

		if not forwarded:
			self.drop(event)

