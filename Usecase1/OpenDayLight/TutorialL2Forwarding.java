/*
 * Copyright (C) 2014 SDN Hub

 Licensed under the GNU GENERAL PUBLIC LICENSE, Version 3.
 You may not use this file except in compliance with this License.
 You may obtain a copy of the License at

    http://www.gnu.org/licenses/gpl-3.0.txt

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 implied.

 *
 */

package org.opendaylight.controller.tutorial_L2_forwarding.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.lang.String;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.opendaylight.controller.sal.core.Actions.ActionType;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.packet.ARP;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.FloodAll;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.action.Drop;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.IPProtocols;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Subnet;

public class TutorialL2Forwarding implements IListenDataPacket {
    private static final Logger logger = LoggerFactory
            .getLogger(TutorialL2Forwarding.class);
    private ISwitchManager switchManager = null;
    private IFlowProgrammerService programmer = null;
    private IDataPacketService dataPacketService = null;
    private Map<Node, Map<Long, NodeConnector>> mac_to_port_per_switch = new HashMap<Node, Map<Long, NodeConnector>>();
    private String function = "switch";
    
    String ipp1 = "192.168.0.0";
    String mask1 = "255.255.255.0";
	String ipp2 = "10.0.0.0";
	String mask2 = "255.255.255.240";
	String ipp3 = "10.0.200.0";
	String mask3 = "255.255.255.240";
	
	
	String SW1 = "00:00:00:00:00:00:00:01";
	String SW2 = "00:00:00:00:00:00:00:02";
	String SW3 = "00:00:00:00:00:00:00:03";
	String FW4 = "00:00:00:00:00:00:00:04";
	String FW5 = "00:00:00:00:00:00:00:05";

    void setDataPacketService(IDataPacketService s) {
        this.dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }

    public void setFlowProgrammerService(IFlowProgrammerService s)
    {
        this.programmer = s;
    }

    public void unsetFlowProgrammerService(IFlowProgrammerService s) {
        if (this.programmer == s) {
            this.programmer = null;
        }
    }

    void setSwitchManager(ISwitchManager s) {
        logger.info("SwitchManager set");
        this.switchManager = s;
    }

    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            logger.debug("SwitchManager removed!");
            this.switchManager = null;
        }
    }

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        logger.info("Initialized");
        // Disabling the SimpleForwarding and ARPHandler bundle to not conflict with this one
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        for(Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().contains("arphandler") ||
                bundle.getSymbolicName().contains("simpleforwarding")) {
                try {
                    bundle.uninstall();
                } catch (BundleException e) {
                    logger.error("Exception in Bundle uninstall "+bundle.getSymbolicName(), e); 
                }   
            }   
        }   
 
    }

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        logger.info("Started");
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        logger.info("Stopped");
    }

    private void floodPacket(RawPacket inPkt) {
        NodeConnector incoming_connector = inPkt.getIncomingNodeConnector();
        Node incoming_node = incoming_connector.getNode();

        Set<NodeConnector> nodeConnectors =
                this.switchManager.getUpNodeConnectors(incoming_node);

        for (NodeConnector p : nodeConnectors) {
            if (!p.equals(incoming_connector)) {
                try {
                    RawPacket destPkt = new RawPacket(inPkt);
                    destPkt.setOutgoingNodeConnector(p);
                    this.dataPacketService.transmitDataPacket(destPkt);
                } catch (ConstructionException e2) {
                    continue;
                }
            }
        }
    }
    
    private PacketResult addDefaultDrop(Node incoming_node){
    	Match match = new Match();
    	List<Action> actions = new ArrayList<Action>();
        actions.add(new Drop());
    	Flow f = new Flow(match, actions);
    	f.setHardTimeout((short)0);
    	f.setPriority((short)100);
    	Status status = programmer.addFlow(incoming_node, f);
        if (!status.isSuccess()) {
            logger.warn(
                    "SDN Plugin failed to program the flow: {}. The failure is: {}",
                    f, status.getDescription());
            return PacketResult.IGNORED;
        }
        logger.info("Installed flow {} in node {}",
                f, incoming_node);
        
        return PacketResult.CONSUME;
    }
    
    private PacketResult addARPAllow(Node incoming_node){
    	Match match = new Match();
    	match.setField( MatchType.DL_TYPE, EtherTypes.ARP.shortValue());  // all the other fields are wildcarded
    	List<Action> actions = new ArrayList<Action>();
    	actions.add(new FloodAll());
    	Flow f = new Flow(match, actions);
    	f.setHardTimeout((short)0);
    	f.setPriority((short)200);
    	Status status = programmer.addFlow(incoming_node, f);
        if (!status.isSuccess()) {
            logger.warn(
                    "SDN Plugin failed to program the flow: {}. The failure is: {}",
                    f, status.getDescription());
            return PacketResult.IGNORED;
        }
        logger.info("Installed flow {} in node {}",
                f, incoming_node);
        
        return PacketResult.CONSUME;
    }
    
    private PacketResult addIPAllow(Node incoming_node, String ip, String netmask, Short outPort){
    	Match match = new Match();
    	InetAddress address;
    	InetAddress mask;
    	
    	match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());
		try {
			address = InetAddress.getByName(ip);
			mask = InetAddress.getByName(netmask);
			match.setField(MatchType.NW_SRC, address,mask);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		NodeConnector dst_connector = NodeConnector.fromStringNoNode("OF", outPort.toString(), incoming_node);
    	List<Action> actions = new ArrayList<Action>();
        actions.add(new Output(dst_connector));
    	Flow f = new Flow(match, actions);
    	f.setHardTimeout((short)0);
    	f.setPriority((short)250);
    	Status status = programmer.addFlow(incoming_node, f);
        if (!status.isSuccess()) {
            logger.warn(
                    "SDN Plugin failed to program the flow: {}. The failure is: {}",
                    f, status.getDescription());
            return PacketResult.IGNORED;
        }
        logger.info("Installed flow {} in node {}",
                f, incoming_node);
        
        return PacketResult.CONSUME;
    }
    
    private PacketResult addPortAllow(Node incoming_node, String ip, String netmask, byte networkProtocol, Short tpPort, Short outPort){
    	Match match = new Match();
    	InetAddress address;
    	InetAddress mask;
    	
    	match.setField(MatchType.DL_TYPE, EtherTypes.IPv4.shortValue());
		try {
			address = InetAddress.getByName(ip);
			mask = InetAddress.getByName(netmask);
			match.setField(MatchType.NW_SRC, address,mask);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	match.setField(MatchType.NW_PROTO, networkProtocol);
    	match.setField(MatchType.TP_DST, tpPort);
    	
    	NodeConnector dst_connector = NodeConnector.fromStringNoNode("OF", outPort.toString(), incoming_node);
		List<Action> actions = new ArrayList<Action>();
		actions.add(new Output(dst_connector));
    	Flow f = new Flow(match, actions);
    	f.setHardTimeout((short)0);
    	f.setPriority((short)250);
    	Status status = programmer.addFlow(incoming_node, f);
        if (!status.isSuccess()) {
            logger.warn(
                    "SDN Plugin failed to program the flow: {}. The failure is: {}",
                    f, status.getDescription());
            return PacketResult.IGNORED;
        }
        logger.info("Installed flow {} in node {}",
                f, incoming_node);
        
        return PacketResult.CONSUME;
    }

    @Override
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        if (inPkt == null) {
            return PacketResult.IGNORED;
        }
        logger.trace("Received a frame of size: {}",
                        inPkt.getPacketData().length);

        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        NodeConnector incoming_connector = inPkt.getIncomingNodeConnector();
        Node incoming_node = incoming_connector.getNode();

        if (formattedPak instanceof Ethernet) {
            byte[] srcMAC = ((Ethernet)formattedPak).getSourceMACAddress();
            byte[] dstMAC = ((Ethernet)formattedPak).getDestinationMACAddress();
            long srcMAC_val = BitBufferHelper.toNumber(srcMAC);
            long dstMAC_val = BitBufferHelper.toNumber(dstMAC);
            // Hub implementation
//            if (function.equals("hub")) {
//                floodPacket(inPkt);
//                return PacketResult.CONSUME;
//            }
            // Switch 
            if(incoming_node.getNodeIDString().equals(SW1) ||
        		incoming_node.getNodeIDString().equals(SW2) ||
        		incoming_node.getNodeIDString().equals(SW3)) {
                Match match = new Match();
                match.setField( new MatchField(MatchType.IN_PORT, incoming_connector) );
                match.setField( new MatchField(MatchType.DL_DST, dstMAC.clone()) );

                // Set up the mapping: switch -> src MAC address -> incoming port
                if (this.mac_to_port_per_switch.get(incoming_node) == null) {
                    this.mac_to_port_per_switch.put(incoming_node, new HashMap<Long, NodeConnector>());
                }
                this.mac_to_port_per_switch.get(incoming_node).put(srcMAC_val, incoming_connector);

                NodeConnector dst_connector = this.mac_to_port_per_switch.get(incoming_node).get(dstMAC_val);

                // Do I know the destination MAC?
                if (dst_connector != null) {

                    List<Action> actions = new ArrayList<Action>();
                    actions.add(new Output(dst_connector));

                    Flow f = new Flow(match, actions);
                    f.setIdleTimeout((short)5);

                    // Modify the flow on the network node
                    Status status = programmer.addFlow(incoming_node, f);
                    if (!status.isSuccess()) {
                        logger.warn(
                                "SDN Plugin failed to program the flow: {}. The failure is: {}",
                                f, status.getDescription());
                        return PacketResult.IGNORED;
                    }
                    logger.info("Installed flow {} in node {}",
                            f, incoming_node);
                }
                else 
                    floodPacket(inPkt);
            }
            else if ((incoming_node.getNodeIDString().equals(FW4))){
            	addDefaultDrop(incoming_node);
        		addARPAllow(incoming_node);
        		addIPAllow(incoming_node, ipp2, mask2, (short)1);
        		addIPAllow(incoming_node, ipp3, mask3, (short)1);
        		addPortAllow(incoming_node, ipp1, mask1, IPProtocols.TCP.byteValue(), (short)80, (short)2);
        		addPortAllow(incoming_node, ipp1, mask1, IPProtocols.UDP.byteValue(), (short)53, (short)2);     		
            }
            else if ((incoming_node.getNodeIDString().equals(FW5))){
            	addDefaultDrop(incoming_node);
        		addARPAllow(incoming_node);
        		addIPAllow(incoming_node, ipp2, mask2, (short)2);
        		addIPAllow(incoming_node, ipp3, mask3, (short)1);
            }
        }
        return PacketResult.IGNORED;
    }
} 
