import logging
import struct

from ryu.base import app_manager
from ryu.controller import mac_to_port
from ryu.controller import ofp_event
from ryu.controller.handler import MAIN_DISPATCHER
from ryu.controller.handler import CONFIG_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_0
from ryu.lib.mac import haddr_to_bin
from ryu.lib.packet import packet
from ryu.lib.packet import ethernet
from ryu.lib.packet import ipv4

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

FW1 = 9
FW2 = 10
SW0 = 1
SW1 = 6
SW2 = 7
SW3 = 8
R1 = 5
LB = 4
LBSRV1 = 2
LBSRV2 = 3

class UC1_DC(app_manager.RyuApp):
    OFP_VERSIONS = [ofproto_v1_0.OFP_VERSION]

    def __init__(self, *args, **kwargs):
        super(UC1_DC, self).__init__(*args, **kwargs)
        self.mac_to_port = {}
        self.knownMACs = {}
        
    def ipv4_to_int(self, ip):
        o = map(int, ip.split('.'))
        res = (16777216 * o[0]) + (65536 * o[1]) + (256 * o[2]) + o[3]
        return res

    def add_flow(self, datapath, match, actions, idle_to, hard_to):
        ofproto = datapath.ofproto

        mod = datapath.ofproto_parser.OFPFlowMod(
            datapath=datapath, match=match, cookie=0,
            command=ofproto.OFPFC_ADD, idle_timeout=idle_to, hard_timeout=hard_to,
            priority=ofproto.OFP_DEFAULT_PRIORITY,
            flags=ofproto.OFPFF_SEND_FLOW_REM, actions=actions)
        datapath.send_msg(mod)
        
    def forwardPacket(self, msg, outPort):
		# Does not install a rule. Just forwards this packet.
        datapath=msg.datapath		
        if msg.buffer_id is not None:
            po_actions = [datapath.ofproto_parser.OFPActionOutput(outPort)]
            pkt_out = datapath.ofproto_parser.OFPPacketOut(datapath=datapath, buffer_id=msg.buffer_id, in_port=msg.in_port, actions=po_actions)
            datapath.send_msg(pkt_out)
        
    def Configure_Learning_Switch(self, msg):
        datapath = msg.datapath
        ofproto = datapath.ofproto

        pkt = packet.Packet(msg.data)
        eth = pkt.get_protocol(ethernet.ethernet)

        dst = eth.dst
        src = eth.src

        dpid = datapath.id
        self.mac_to_port.setdefault(dpid, {})

        # learn a mac address to avoid FLOOD next time.
        self.mac_to_port[dpid][src] = msg.in_port

        if dst in self.mac_to_port[dpid]:
            outPort = self.mac_to_port[dpid][dst]
        else:
            outPort = ofproto.OFPP_FLOOD

        match = datapath.ofproto_parser.OFPMatch(msg.in_port, dl_dst=haddr_to_bin(dst))
        actions = [datapath.ofproto_parser.OFPActionOutput(outPort)]

        # install a flow to avoid packet_in next time
        if outPort != ofproto.OFPP_FLOOD:
            self.add_flow(datapath, match, actions, 5 , 0)

        self.forwardPacket(msg, outPort)
        
    # Static rules for the web and dns services
    def Configure_FW1(self, msg):
	
        datapath = msg.datapath
    
        inport = 1 # from outside
        outport = 2 # to inside
        actions = [datapath.ofproto_parser.OFPActionOutput(outport)]
     # Forward DNS (UDP:53) to H2        
        match = datapath.ofproto_parser.OFPMatch(in_port=inport,dl_type = ETH_IP, nw_dst = self.ipv4_to_int(HOST_DNS), 
		   nw_proto = PROTO_UDP, tp_dst = PORT_DNS)
        self.add_flow(datapath, match, actions, 0, 0)
        
     # Forward WEB (TCP:80) to H3
        match = datapath.ofproto_parser.OFPMatch(in_port=inport,dl_type = ETH_IP, nw_dst = self.ipv4_to_int(HOST_WEB), 
		   nw_proto = PROTO_TCP, tp_dst = PORT_WEB)
        self.add_flow(datapath, match, actions, 0, 0)

    def Configure_basic_firewall(self, msg):
        pkt = packet.Packet(msg.data)
        datapath = msg.datapath
        
        eth = pkt.get_protocol(ethernet.ethernet)
        hwdst = eth.dst
        hwsrc = eth.src
        
        # Forward all arp
        if eth.ethertype == ETH_ARP:
            if msg.in_port == 2:
                self.forwardPacket(msg, 1)
            if msg.in_port == 1:
                self.forwardPacket(msg, 2)        
        # Forward packets from inside to outside and also install the reverse rule with idle_to=5 sec
        elif msg.in_port == 2:
            match = datapath.ofproto_parser.OFPMatch(in_port=2, dl_type = ETH_IP, dl_src=haddr_to_bin(hwsrc), dl_dst=haddr_to_bin(hwdst))
            actions = [datapath.ofproto_parser.OFPActionOutput(1)]
            self.add_flow(datapath, match, actions, 5, 0)
            
            match = datapath.ofproto_parser.OFPMatch(in_port=1, dl_type = ETH_IP, dl_src=haddr_to_bin(hwdst), dl_dst=haddr_to_bin(hwsrc))
            actions = [datapath.ofproto_parser.OFPActionOutput(2)]
            self.add_flow(datapath, match, actions, 5, 0)
            
            # forward the packet
            self.forwardPacket(msg, 1)
		
    def Configure_load_balancer(self, msg):   
        pkt = packet.Packet(msg.data)
        datapath = msg.datapath
        
        eth = pkt.get_protocol(ethernet.ethernet)
        hwdst = eth.dst
        hwsrc = eth.src
        
        
        if (msg.in_port == 1) or (msg.in_port == 2):
            outPort = 3
        else:
            outPort = hash(str(pkt))
            #self.logger.info("load balancer outPort: %s", outPort)  
            outPort = ((outPort) % 2) + 1
            #self.logger.info("load balancer outPort: %s", outPort)  
         
        # install a flow to avoid packet_in next time  
        if eth.ethertype == ETH_IP:    
            actions = [datapath.ofproto_parser.OFPActionOutput(outPort)]
            match = datapath.ofproto_parser.OFPMatch(in_port=msg.in_port, dl_type = ETH_IP, dl_src=haddr_to_bin(hwsrc), dl_dst=haddr_to_bin(hwdst))           
            #self.logger.info("load balancer match: %s", match)  
            self.add_flow(datapath, match, actions, 5, 0)

        # forward the flow
        self.forwardPacket(msg, outPort)                  

    def Configure_basic_bridge(self, msg):
        pkt = packet.Packet(msg.data)
        datapath = msg.datapath
        outPort = 1 if msg.in_port == 2 else 2
        
        eth = pkt.get_protocol(ethernet.ethernet)
        hwdst = eth.dst
        hwsrc = eth.src
        
        try:
            port = self.knownMACs[hwsrc]
            if port == msg.in_port:
                if eth.ethertype == ETH_IP:
                    actions = [datapath.ofproto_parser.OFPActionOutput(outPort)]
                    match = datapath.ofproto_parser.OFPMatch(in_port=msg.in_port, dl_type = ETH_IP, dl_src=haddr_to_bin(hwsrc), dl_dst=haddr_to_bin(hwdst))            
                    self.add_flow(datapath, match, actions, 5, 0)
                    
                self.forwardPacket(msg, outPort)
        except KeyError:
            self.knownMACs[hwsrc] = msg.in_port
            if eth.ethertype == ETH_IP:
                actions = [datapath.ofproto_parser.OFPActionOutput(outPort)]
                match = datapath.ofproto_parser.OFPMatch(in_port=msg.in_port, dl_type = ETH_IP, dl_src=haddr_to_bin(hwsrc), dl_dst=haddr_to_bin(hwdst))           
                self.add_flow(datapath, match, actions, 5, 0)

            self.forwardPacket(msg, outPort)

    # Feature reply handler: used to install proactive actions
    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
    def _switch_features_handler(self, ev):
        msg = ev.msg
        datapath = msg.datapath
        
        if datapath.id == FW1:
            self.Configure_FW1(msg)
    
    # PacketIn handler for reactive actions
    @set_ev_cls(ofp_event.EventOFPPacketIn, MAIN_DISPATCHER)
    def _packet_in_handler(self, ev):
        msg = ev.msg
        datapath = msg.datapath
        pkt = packet.Packet(msg.data)
        
        
        if datapath.id == SW0:
            self.Configure_Learning_Switch(msg)
        if datapath.id == LBSRV1:
            self.Configure_basic_bridge(msg)
        if datapath.id == LBSRV2:
            self.Configure_basic_bridge(msg)
        if datapath.id == LB:
            self.Configure_load_balancer(msg)   
            self.Configure_Learning_Switch(msg)
        elif datapath.id == R1:
            self.Configure_Learning_Switch(msg)
        elif datapath.id == SW1:
            self.Configure_Learning_Switch(msg)
        elif datapath.id == SW2:
            #self.logger.info("packet in dpid=%s inport=%s %s", datapath.id, msg.in_port, pkt)
            self.Configure_Learning_Switch(msg)
        elif datapath.id == SW3:
            #self.logger.info("packet in dpid=%s inport=%s %s", datapath.id, msg.in_port, pkt)
            self.Configure_Learning_Switch(msg)
        elif datapath.id == FW1:
            self.Configure_basic_firewall(msg)
        elif datapath.id == FW2:
            #self.logger.info("packet in dpid=%s inport=%s %s", datapath.id, msg.in_port, pkt)
            self.Configure_basic_firewall(msg)

    @set_ev_cls(ofp_event.EventOFPPortStatus, MAIN_DISPATCHER)
    def _port_status_handler(self, ev):
        msg = ev.msg
        reason = msg.reason
        port_no = msg.desc.port_no

        ofproto = msg.datapath.ofproto
        if reason == ofproto.OFPPR_ADD:
            self.logger.info("port added %s", port_no)
        elif reason == ofproto.OFPPR_DELETE:
            self.logger.info("port deleted %s", port_no)
        elif reason == ofproto.OFPPR_MODIFY:
            self.logger.info("port modified %s", port_no)
        else:
            self.logger.info("Illegal port state %s %s", port_no, reason)
