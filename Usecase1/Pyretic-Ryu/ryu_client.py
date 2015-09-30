#!/usr/bin/python

###############################################################################################
###        Name: ryu_client.py
###      Author: Roberto Doriguzzi Corin roberto.doriguzzi@create-net.org
### Description: Pyretic's of_client for the Ryu controller
###############################################################################################

import logging
import struct
import threading
from ryu.base import app_manager
from ryu.controller import mac_to_port
from ryu.controller import ofp_event
from ryu.controller import dpset
from ryu.controller.handler import MAIN_DISPATCHER
from ryu.controller.handler import HANDSHAKE_DISPATCHER
from ryu.controller.handler import CONFIG_DISPATCHER
from ryu.controller.handler import set_ev_cls
from ryu.ofproto import ofproto_v1_0, ether, ofproto_v1_0_parser, nx_match
from ryu.lib.mac import haddr_to_bin
from ryu.lib.ip import ipv4_to_bin
from ryu.lib.packet import packet, ethernet, lldp
from ryu.lib.packet import ethernet
from ryu.lib.packet import ipv4
from pyretic.backend.comm import *


def inport_value_hack(outport):
    if outport > 1:
        return 1
    else:
        return 2
    
class OF10Match(object):
    def __init__(self):
        self.in_port = 0
        self.dl_src = 0
        self.dl_dst = 0
        self.dl_type = 0
        self.dl_vlan = 0
        self.dl_vlan_pcp = 0
        self.nw_tos = 0
        self.nw_proto = 0
        self.nw_src = 0
        self.nw_dst = 0
        self.tp_dst = 0
        self.tp_src = 0
    
    def match_tuple(self):
        """return a tuple which can be used as *args for
        ofproto_v1_0_parser.OFPMatch.__init__().
        see Datapath.send_flow_mod.
        """
        wildcards = ofproto_v1_0.OFPFW_ALL

        if self.in_port != 0:
            wildcards &= ~ofproto_v1_0.OFPFW_IN_PORT

        if self.dl_src != 0:
            wildcards &= ~ofproto_v1_0.OFPFW_DL_SRC

        if self.dl_dst != 0:
            wildcards &= ~ofproto_v1_0.OFPFW_DL_DST

        if not self.dl_type !=0:
            wildcards &= ~ofproto_v1_0.OFPFW_DL_TYPE

        if self.dl_vlan != 0:
            wildcards &= ~ofproto_v1_0.OFPFW_DL_VLAN

        if self.dl_vlan_pcp != 0:
            wildcards &= ~ofproto_v1_0.OFPFW_DL_VLAN_PCP

        if self.nw_tos != 0:
            wildcards &= ~ofproto_v1_0.OFPFW_NW_TOS

        if self.nw_proto != 0:
            wildcards &= ~ofproto_v1_0.OFPFW_NW_PROTO

        if self.nw_src != 0:
            wildcards &= ~ofproto_v1_0.OFPFW_NW_SRC_ALL

        if self.nw_dst != 0:
            wildcards &= ~ofproto_v1_0.OFPFW_NW_DST_ALL

        if self.tp_src != 0:
            wildcards &= ~ofproto_v1_0.OFPFW_TP_SRC

        if self.tp_dst != 0:
            wildcards &= ~ofproto_v1_0.OFPFW_TP_DST

        return (wildcards, self.in_port, self.dl_src,
                self.dl_dst, self.dl_vlan, self.dl_vlan_pcp,
                self.dl_type, self.nw_tos & 0xfc,
                self.nw_proto, self.nw_src, self.nw_dst,
                self.tp_src, self.tp_dst)


class BackendChannel(asynchat.async_chat):
    """Sends messages to the server and receives responses.
    """
    def __init__(self, host, port, of_client):
        self.of_client = of_client
        self.received_data = []
        asynchat.async_chat.__init__(self)
        self.create_socket(socket.AF_INET, socket.SOCK_STREAM)
        self.connect((host, port))
        self.ac_in_buffer_size = 4096 * 3
        self.ac_out_buffer_size = 4096 * 3
        self.set_terminator(TERM_CHAR)
        return

    def handle_connect(self):
        print "Connected to pyretic frontend."
        
    def collect_incoming_data(self, data):
        """Read an incoming message from the client and put it into our outgoing queue."""
        #print "ofclient collect incoming data"
        with self.of_client.channel_lock:
            self.received_data.append(data)

    def dict2OF(self,d):
        def convert(h,val):
            if h in ['srcmac','dstmac']:
                return val #packetaddr.EthAddr(val)
            elif h in ['srcip','dstip']:
                try:
                    return val #packetaddr.IPAddr(val)
                except:
                    return val
            elif h in ['vlan_id','vlan_pcp'] and val == 'None':
                return None
            else:
                return val
        return { h : convert(h,val) for (h, val) in d.items()}
    def found_terminator(self):
        """The end of a command or message has been seen."""
        with self.of_client.channel_lock:
            msg = deserialize(self.received_data)

        #print "ofclient found terminator: ", msg
        
        # USE DESERIALIZED MSG
        #=======================================================================
        # if msg[0] == 'inject_discovery_packet':
        #     switch = msg[1]
        #     port = msg[2]
        #     self.of_client.inject_discovery_packet(switch,port)
        #=======================================================================
        if msg[0] == 'packet':
            packet = self.dict2OF(msg[1])
            self.of_client.send_to_switch(packet)
        elif msg[0] == 'install':
            pred = self.dict2OF(msg[1])
            priority = int(msg[2])
            actions = map(self.dict2OF,msg[3])
            self.of_client.install_flow(pred,priority,actions)
        elif msg[0] == 'delete':
            pred = self.dict2OF(msg[1])
            priority = int(msg[2])
            self.of_client.delete_flow(pred,priority)
        elif msg[0] == 'clear':
            switch = int(msg[1])
            self.of_client.clear(switch)
        elif msg[0] == 'barrier':
            switch = msg[1]
            self.of_client.barrier(switch)
        elif msg[0] == 'flow_stats_request':
            switch = msg[1]
            self.of_client.flow_stats_request(switch)
        else:
            print "ERROR: Unknown msg from frontend %s" % msg

class asyncore_loop(threading.Thread):
        def run(self):
            asyncore.loop()

class RYUClient(app_manager.RyuApp):
    OFP_VERSIONS = [ofproto_v1_0.OFP_VERSION]
    
    def __init__(self, *args, **kwargs):
        super(RYUClient, self).__init__(*args, **kwargs)
        print "RYUClient init"
        self.packetno = 0
        self.switches = {}
        self.ofp_port_config_rev_map = {
          'OFPPC_PORT_DOWN'    : ofproto_v1_0.OFPPC_PORT_DOWN,
          'OFPPC_NO_STP'       : ofproto_v1_0.OFPPC_NO_STP,
          'OFPPC_NO_RECV'      : ofproto_v1_0.OFPPC_NO_RECV,
          'OFPPC_NO_RECV_STP'  : ofproto_v1_0.OFPPC_NO_RECV_STP,
          'OFPPC_NO_FLOOD'     : ofproto_v1_0.OFPPC_NO_FLOOD,
          'OFPPC_NO_FWD'       : ofproto_v1_0.OFPPC_NO_FWD,
          'OFPPC_NO_PACKET_IN' : ofproto_v1_0.OFPPC_NO_PACKET_IN,
        }
        self.ofp_port_state_rev_map = {
          'OFPPS_STP_LISTEN'  : ofproto_v1_0.OFPPS_STP_LISTEN,
          'OFPPS_LINK_DOWN'   : ofproto_v1_0.OFPPS_LINK_DOWN,
          'OFPPS_STP_LEARN'   : ofproto_v1_0.OFPPS_STP_LEARN,
          'OFPPS_STP_FORWARD' : ofproto_v1_0.OFPPS_STP_FORWARD,
          'OFPPS_STP_BLOCK'   : ofproto_v1_0.OFPPS_STP_BLOCK,
        }
        self.channel_lock = threading.Lock()
        self.backend_channel = BackendChannel('127.0.0.1', BACKEND_PORT, self) 
        self.al = asyncore_loop()
        self.al.start()   
        
    def ipv4_to_int(self, string):
        ip = string.split('.')
        assert len(ip) == 4
        i = 0
        for b in ip:
            b = int(b)
            i = (i << 8) | b
        return i

    def packet_from_network(self, **kwargs):
        return kwargs
    def packet_to_network(self, packet):
        return packet['raw']
    
    def active_ofp_port_config(self,configs):
        active = []
        for (config,bit) in self.ofp_port_config_rev_map.items():
            if configs & bit:
                active.append(config)
        return active
    
    def active_ofp_port_state(self,states):
        active = []
        for (state,bit) in self.ofp_port_state_rev_map.items():
            if states & bit:
                active.append(state)
        return active
    
    def create_discovery_packet (self, dpid, port_num, port_addr):
        """
        Build discovery packet
        """
        pkt = packet.Packet()
        print "create_discovery_packet"
        chassis_id = lldp.ChassisID(subtype=lldp.ChassisID.SUB_LOCALLY_ASSIGNED, chassis_id = bytes('dpid:' + hex(long(dpid))[2:-1]))
        #chassis_id.chassis_id = bytes('dpid:' + hex(long(dpid))[2:-1])
        print "create_discovery_packet - chassis_id" , chassis_id  
        port_id = lldp.PortID(subtype=lldp.PortID.SUB_LOCALLY_ASSIGNED, port_id=str(port_num))
        print "create_discovery_packet - port_id" , port_id  
        ttl = lldp.TTL(ttl = 120)
        print "create_discovery_packet - ttl" , ttl  
   
        tlvs = (chassis_id, port_id, ttl, lldp.End())
        lldp_pkt = lldp.lldp(tlvs) 
        pkt.add_protocol(lldp_pkt)
        print "create_discovery_packet: discovery_packet", lldp_pkt
    
        dst = lldp.LLDP_MAC_NEAREST_BRIDGE
        src = port_addr
        ethertype = ether.ETH_TYPE_LLDP
        eth_pkt = ethernet.ethernet(dst, src, ethertype)
        pkt.add_protocol(eth_pkt)
        pkt.serialize()
           
        datapath = self.switches[dpid]['connection']
        print "datapath.id", datapath.id
        print "datapath.ofproto", datapath.ofproto.OFP_VERSION
        po_actions = [datapath.ofproto_parser.OFPActionOutput(port_num)]
        print "po_actions", po_actions
        
        # TODO: the returned pkt_out is NULL. We need to investigate why.
        pkt_out = datapath.ofproto_parser.OFPPacketOut(datapath=datapath, in_port=datapath.ofproto.OFPP_CONTROLLER, buffer_id=datapath.ofproto.OFP_NO_BUFFER, 
                                                       actions=po_actions, data=pkt.data)
        return pkt_out
    
    def inject_discovery_packet(self,switch, port):
        try:
            hw_addr = self.switches[switch]['ports'][port]
            print "inject_discovery_packet - hw_addr: ", hw_addr
            packet = self.create_discovery_packet(switch, port, hw_addr)
            print "inject_discovery_packet - packet: ", packet
            datapath = self.switches[switch]['connection']
            datapath.send_msg(packet)
        except KeyError:
            pass
    
    def send_to_pyretic(self,msg):
        serialized_msg = serialize(msg)
        try:
            #with self.channel_lock:
            self.backend_channel.push(serialized_msg)
        except IndexError as e:
            print "ERROR PUSHING MESSAGE %s" % msg
            pass
     
    def send_to_switch(self,packet):
        switch = packet["switch"]
        outPort = packet["outport"]
        
        #print "send_to_switch - switch", switch
        #print "send_to_switch - outPort", outPort
        #print "send_to_switch - packet", packet
        #print "send_to_switch - raw packet", self.packet_to_network(packet)
        
        try:
            inport = packet["inport"]
            if inport == -1 or inport == outPort:
                inport = inport_value_hack(outPort)
        except KeyError:
            inport = inport_value_hack(outPort)
        
        
         ## HANDLE PACKETS SEND ON LINKS THAT HAVE TIMED OUT
        try:
            datapath = self.switches[switch]['connection']
            po_actions = [datapath.ofproto_parser.OFPActionOutput(outPort)]
            pkt_out = datapath.ofproto_parser.OFPPacketOut(datapath=datapath, in_port=inport, buffer_id=ofproto_v1_0.OFP_NO_BUFFER, actions=po_actions, data = self.packet_to_network(packet))      
            datapath.send_msg(pkt_out)
        except RuntimeError, e:
            print "ERROR:send_to_switch: %s to switch %d" % (str(e),switch)
            # TODO - ATTEMPT TO RECONNECT SOCKET
        except KeyError, e:
            print "ERROR:send_to_switch: No connection to switch %d available" % switch
            # TODO - IF SOCKET RECONNECTION, THEN WAIT AND RETRY 
    
    def build_of_match(self,datapath,inport,pred):
        ### BUILD OF MATCH
        rule = OF10Match()      
        
        if inport != None:
            rule.in_port = inport
        if 'srcmac' in pred:
            rule.dl_src = haddr_to_bin(pred['srcmac'])
        if 'dstmac' in pred:
            rule.dl_dst = haddr_to_bin(pred['dstmac'])
        if 'ethtype' in pred:
            rule.dl_type = pred['ethtype']
        if 'vlan_id' in pred:
            rule.dl_vlan = pred['vlan_id']
        if 'vlan_pcp' in pred:
            rule.dl_vlan_pcp = pred['vlan_pcp']
        if 'protocol' in pred:
            rule.nw_proto = pred['protocol']
        if 'srcip' in pred:
            rule.nw_src = self.ipv4_to_int(pred['srcip'])
        if 'dstip' in pred:
            rule.nw_dst = self.ipv4_to_int(pred['dstip'])
        if 'tos' in pred:
            rule.nw_tos = pred['tos']
        if 'srcport' in pred:
            rule.tp_src = pred['srcport']
        if 'dstport' in pred:
            rule.tp_dst = pred['dstport']
        
        match_tuple = rule.match_tuple()
        print "switch: ", datapath.id
        print "match_tuple: ", match_tuple
        match = datapath.ofproto_parser.OFPMatch(*match_tuple)
        print "build_of_match", match
        print "wildcards all", ofproto_v1_0.OFPFW_ALL
        return match
    
    def build_of_actions(self,inport,action_list):
        ### BUILD OF ACTIONS
        of_actions = []
        for actions in action_list:
            outport = actions['outport']
            del actions['outport']
            if 'srcmac' in actions:
                of_actions.append(ofproto_v1_0_parser.OFPActionSetDlSrc(haddr_to_bin(actions['srcmac'])))
            if 'dstmac' in actions:
                of_actions.append(ofproto_v1_0_parser.OFPActionSetDlDst(haddr_to_bin(actions['dstmac'])))
            if 'srcip' in actions:
                of_actions.append(ofproto_v1_0_parser.OFPActionSetNwSrc(self.ipv4_to_int(actions['srcip'])))
            if 'dstip' in actions:
                of_actions.append(ofproto_v1_0_parser.OFPActionSetNwDst(self.ipv4_to_int(actions['dstip'])))
            if 'srcport' in actions:
                of_actions.append(ofproto_v1_0_parser.OFPActionSetTpSrc(actions['srcport']))
            if 'dstport' in actions:
                of_actions.append(ofproto_v1_0_parser.OFPActionSetTpDst(actions['dstport']))
            if 'vlan_id' in actions:
                if actions['vlan_id'] is None:
                    of_actions.append(ofproto_v1_0_parser.OFPActionStripVlan())
                else:
                    of_actions.append(ofproto_v1_0_parser.OFPActionVlanVid(vlan_vid=actions['vlan_id']))
            if 'vlan_pcp' in actions:
                if actions['vlan_pcp'] is None:
                    if not actions['vlan_id'] is None:
                        raise RuntimeError("vlan_id and vlan_pcp must be set together!")
                    pass
                else:
                    of_actions.append(ofproto_v1_0_parser.OFPActionVlanPcp(vlan_pcp=actions['vlan_pcp']))
            if (not inport is None) and (outport == inport):
                of_actions.append(ofproto_v1_0_parser.OFPActionOutput(ofproto_v1_0.OFPP_IN_PORT))
            else:
                of_actions.append(ofproto_v1_0_parser.OFPActionOutput(outport))
        print "build_of_actions", of_actions
        return of_actions
    
    def install_flow(self,pred,priority,action_list):
        switch = pred['switch']
        if 'inport' in pred:        
            inport = pred['inport']
        else:
            inport = None
          
        print "install_flow - pred: ", pred
        print "install_flow - priority: ", priority
        print "install_flow - action_list: ", action_list  
            
        datapath = self.switches[switch]['connection']
        ofproto = datapath.ofproto
        match = self.build_of_match(datapath,inport,pred)
        of_actions = self.build_of_actions(inport,action_list)
        msg = datapath.ofproto_parser.OFPFlowMod(idle_timeout=0, hard_timeout=0,
            datapath=datapath, match=match, cookie=0,
            command=ofproto.OFPFC_ADD,
            priority=ofproto.OFP_DEFAULT_PRIORITY,
            flags=ofproto.OFPFF_SEND_FLOW_REM, actions=of_actions)
        try:
            datapath.send_msg(msg)
        except RuntimeError, e:
            print "WARNING:install_flow: %s to switch %d" % (str(e),switch)
        except KeyError, e:
            print "WARNING:install_flow: No connection to switch %d available" % switch
     
    def delete_flow(self,pred,priority):
        switch = pred['switch']
        if 'inport' in pred:        
            inport = pred['inport']
        else:
            inport = None
           
        datapath = self.switches[switch]['connection']
        ofproto = datapath.ofproto 
        match = self.build_of_match(datapath,inport,pred)
        msg = datapath.ofproto_parser.OFPFlowMod(
            datapath=datapath, match=match, cookie=0,
            command=ofproto.OFPFC_DELETE_STRICT,
            priority=priority)
        try:
            datapath.send_msg(msg)
        except RuntimeError, e:
            print "WARNING:delete_flow: %s to switch %d" % (str(e),switch)
        except KeyError, e:
            print "WARNING:delete_flow: No connection to switch %d available" % switch
              
    def barrier(self,switch):
        datapath = self.switches[switch]['connection']
        barrier_request = datapath.ofproto_parser.OFPBarrierRequest(datapath=datapath)
        datapath.send_msg(barrier_request) 
        
    def clear(self,switch=None):
        if switch is None:
            for switch in self.switches.keys():
                self.clear(switch)
        else:
            datapath = self.switches[switch]['connection']
            match = datapath.ofproto_parser.OFPMatch()
            mod = datapath.ofproto_parser.OFPFlowMod(datapath=datapath, match=match, cookie=0, command=ofproto_v1_0.OFPFC_DELETE) 
            datapath.send_msg(mod)
        
    @set_ev_cls(ofp_event.EventOFPSwitchFeatures, CONFIG_DISPATCHER)
    def _handle_ConnectionUp(self, ev):  
        print "Connection UP"
        msg = ev.msg
        datapath = msg.datapath  
        assert datapath.id not in self.switches

        self.switches[datapath.id] = {}
        self.switches[datapath.id]['connection'] = datapath
        self.switches[datapath.id]['ports'] = {}
        
        ofproto = datapath.ofproto
        match = datapath.ofproto_parser.OFPMatch()
        actions = [datapath.ofproto_parser.OFPActionOutput(ofproto_v1_0.OFPP_CONTROLLER)]
        mod = datapath.ofproto_parser.OFPFlowMod(
            datapath=datapath, match=match, cookie=0,
            command=ofproto.OFPFC_ADD, actions=actions)
        datapath.send_msg(mod)
        
        self.send_to_pyretic(['switch','join',datapath.id,'BEGIN'])

        # port type is ofp_phy_port
        for port_nr in msg.ports:
            port = msg.ports[port_nr]
            print "OFPort: ", port.port_no
            if port.port_no <= ofproto_v1_0.OFPP_MAX:
                self.switches[datapath.id]['ports'][port.port_no] = port.hw_addr
                CONF_UP = not 'OFPPC_PORT_DOWN' in self.active_ofp_port_config(port.config)
                STAT_UP = not 'OFPPS_LINK_DOWN' in self.active_ofp_port_state(port.state)
                self.send_to_pyretic(['port','join',datapath.id, port.port_no, CONF_UP, STAT_UP])                        
   
        self.send_to_pyretic(['switch','join',datapath.id,'END'])
    
    @set_ev_cls(dpset.EventDP, CONFIG_DISPATCHER)    
    def _handle_ConnectionDown(self, ev):
        if ev.enter == False:
            print "Connection DOWN", ev.dp.id
            assert ev.dp.id in self.switches
        
            del self.switches[ev.dp.id]
            self.send_to_pyretic(['switch','part',ev.dp.id])

    def of_match_to_dict(self, m):
        h = {}
        if not m.in_port is None:
            h["inport"] = m.in_port
        if not m.dl_src is None:
            h["srcmac"] = m.dl_src
        if not m.dl_dst is None:
            h["dstmac"] = m.dl_dst
        if not m.dl_type is None:
            h["ethtype"] = m.dl_type
        if not m.dl_vlan is None:
            h["vlan_id"] = m.dl_vlan
        if not m.dl_vlan_pcp is None:
            h["vlan_pcp"] = m.dl_vlan_pcp
        if not m.nw_src is None:
            h["srcip"] = m.nw_src
        if not m.nw_dst is None:
            h["dstip"] = m.nw_dst
        if not m.nw_proto is None:
            h["protocol"] = m.nw_proto
        if not m.nw_tos is None:
            h["tos"] = m.nw_tos
        if not m.tp_src is None:
            h["srcport"] = m.tp_src
        if not m.tp_dst is None:
            h["dstport"] = m.tp_dst
        return h

    def of_actions_to_dicts(self, actions):
        action_dicts = []
        for a in actions:
            d = {}
            if a.type == ofproto_v1_0.OFPAT_OUTPUT:
                d['output'] = a.port
            elif a.type == ofproto_v1_0.OFPAT_ENQUEUE:
                d['enqueue'] = a.port
            elif a.type == ofproto_v1_0.OFPAT_STRIP_VLAN:
                d['strip_vlan_id'] = 0
            elif a.type == ofproto_v1_0.OFPAT_SET_VLAN_VID:
                d['vlan_id'] = a.vlan_vid
            elif a.type == ofproto_v1_0.OFPAT_SET_VLAN_PCP:
                d['vlan_pcp'] = a.vlan_pcp
            elif a.type == ofproto_v1_0.OFPAT_SET_DL_SRC:
                d['srcmac'] = a.dl_addr.toRaw()
            elif a.type == ofproto_v1_0.OFPAT_SET_DL_DST:
                d['dstmac'] = a.dl_addr.toRaw()
            elif a.type == ofproto_v1_0.OFPAT_SET_NW_SRC:
                d['srcip'] = a.nw_addr.toRaw()
            elif a.type == ofproto_v1_0.OFPAT_SET_NW_DST:
                d['dstip'] = a.nw_addr.toRaw()
            elif a.type == ofproto_v1_0.OFPAT_SET_NW_TOS:
                d['tos'] = a.nw_tos
            elif a.type == ofproto_v1_0.OFPAT_SET_TP_SRC:
                d['srcport'] = a.tp_port
            elif a.type == ofproto_v1_0.OFPAT_SET_TP_DST:
                d['dstport'] = a.tp_port
            action_dicts.append(d)
        return action_dicts

#TODO: test _handle_FlowStatsReceived
    @set_ev_cls(ofp_event.EventOFPFlowStatsReply, MAIN_DISPATCHER)
    def _handle_FlowStatsReceived (self, ev):
        msg = ev.msg
        body = msg.body
        datapath = msg.datapath 
        dpid = datapath.id
        def handle_ofp_flow_stat(flow_stat):
            flow_stat_dict = {}
            flow_stat_dict['table_id'] = flow_stat.table_id 
            #flow_stat.match
            flow_stat_dict['duration_sec'] = flow_stat.duration_sec
            flow_stat_dict['duration_nsec'] = flow_stat.duration_nsec
            flow_stat_dict['priority'] = flow_stat.priority
            flow_stat_dict['idle_timeout'] = flow_stat.idle_timeout
            flow_stat_dict['hard_timeout'] = flow_stat.hard_timeout
            flow_stat_dict['cookie'] = flow_stat.cookie    
            flow_stat_dict['packet_count'] = flow_stat.packet_count
            flow_stat_dict['byte_count'] = flow_stat.byte_count
            match = self.of_match_to_dict(flow_stat.match)
            flow_stat_dict['match'] = match
            actions = self.of_actions_to_dicts(flow_stat.actions)
            flow_stat_dict['actions'] = actions
            return flow_stat_dict
        flow_stats = [handle_ofp_flow_stat(s) for s in body]
        self.send_to_pyretic(['flow_stats_reply',dpid,flow_stats])
        
    @set_ev_cls(ofp_event.EventOFPPortStatus, MAIN_DISPATCHER)
    def _handle_PortStatus(self, ev):       
        msg = ev.msg
        reason = msg.reason
        datapath = msg.datapath
        port = msg.desc
        
        print "Port Status Handler - reason", reason
        
        if port.port_no <= ofproto_v1_0.OFPP_MAX:
            if reason == ofproto_v1_0.OFPPR_ADD:
                self.switches[datapath.id]['ports'][port.port_no] = port.hw_addr
                CONF_UP = not 'OFPPC_PORT_DOWN' in self.active_ofp_port_config(port.config)
                STAT_UP = not 'OFPPS_LINK_DOWN' in self.active_ofp_port_state(port.state)
                self.send_to_pyretic(['port','join',datapath.id, port.port_no, CONF_UP, STAT_UP])
            elif reason == ofproto_v1_0.OFPPR_DELETE:
                try:
                    del self.switches[datapath.id]['ports'][port.port_no] 
                except KeyError:
                    pass  # SWITCH ALREADY DELETED
                self.send_to_pyretic(['port','part',datapath.id,port.port_no])
            elif reason == ofproto_v1_0.OFPPR_MODIFY:
                CONF_UP = not 'OFPPC_PORT_DOWN' in self.active_ofp_port_config(port.config)
                STAT_UP = not 'OFPPS_LINK_DOWN' in self.active_ofp_port_state(port.state)
                self.send_to_pyretic(['port','mod',datapath.id, port.port_no, CONF_UP, STAT_UP])
            else:
                raise RuntimeException("Unknown port status event")
        
        
    def handle_lldp(self,ev):
        #print "handle_lldp"
        return    
        
        
        
    # PacketIn handler for reactive actions
    @set_ev_cls(ofp_event.EventOFPPacketIn, MAIN_DISPATCHER)
    def _packet_in_handler(self, ev):
        msg = ev.msg
        datapath = msg.datapath
        pkt = packet.Packet(msg.data)
        eth = pkt.get_protocol(ethernet.ethernet)
        
        if eth.ethertype == ether.ETH_TYPE_LLDP: 
            self.handle_lldp(ev)
            return
        elif eth.ethertype == ether.ETH_TYPE_IPV6:  # IGNORE IPV6
            return 
        
        self.packetno += 1
        #print "-------- RYU/OF RECV %d ---------------" % self.packetno
        #print "port\t%s" % msg.in_port
        #print "data\t%s" % eth
        #print "dpid\t%s" % datapath.id
        
        received = self.packet_from_network(switch=datapath.id, inport=msg.in_port, raw=msg.data)
        #print "Received: ", received 
        #print "Packet in msg.data", msg.data
        self.send_to_pyretic(['packet',received])
    