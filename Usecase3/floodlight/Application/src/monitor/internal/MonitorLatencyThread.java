package net.floodlightcontroller.monitor.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.monitor.stuctures.MonitorSwitchContainer;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;

import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

/**
 * Monitor the latencies of the connected switches.<br>
 * This only measure the latency on <i>INTERNAL</i> links (i.e, between switches, not between hosts and switches).
 * @author K.Phemius<br>
 * DT/CEA/TAI Lab<br>
 * Copyright (c) 2014 Thales Communications & Security<br>
 * 4 av. des Louvresses - 92230 Gennevilliers - France<br>
 * All rights reserved
 *
 **/
public class MonitorLatencyThread extends Thread{
	private Monitor m;
	public MonitorLatencyThread(Monitor monitor) {
		m = monitor;
	}
	/**
	 * Main loop<br>
	 */
	@Override
	public void run() {
		try {
			Thread.sleep(3000); // let the thread sleep for 3s after init to let the system boot properly
		} catch (InterruptedException e) {}
		while (true) {
			try{
				// we use micro sleeps to update the links sequentially instead of all at once 
				ArrayList<SwitchPort> map = new ArrayList<SwitchPort>();
				for(IOFSwitch sw : m.floodlightProvider.getSwitches().values())
					for(OFPhysicalPort p : sw.getPorts()){
						if(p.getPortNumber()!=-2 && p.getState()==0) // if the port is UP and is not the internal port TODO : add device AP check
							map.add(new SwitchPort(sw.getId(), p.getPortNumber()));
					}
				boolean miniSleep = m.latencyUpdateInterval > map.size();
				if(map.size()!=0){
					for(SwitchPort sp : map){
						getLatency(m.floodlightProvider.getSwitches().get(sp.getSwitchDPID()), (short) sp.getPort());
						/** half-sleep **/
						if(miniSleep){
							try {
								Thread.sleep(m.latencyUpdateInterval/map.size());
							} catch (InterruptedException e) {break;}
						}
					}
					/** sleep **/
					if(!miniSleep){
						try {
							Thread.sleep(m.latencyUpdateInterval);
						} catch (InterruptedException e) {break;}
					}
					if(!map.isEmpty())
						m.informListeners();
				}else{
					try {
						Thread.sleep(m.latencyUpdateInterval); // no links yet
					} catch (InterruptedException e) {break;}
				}
			}catch(java.lang.NullPointerException e){}
		}
	}
	/**
	 * Sends a message to check the latency on a switch's port
	 * @param sw The switch to test
	 * @param p The port to test
	 */
	public void getLatency(IOFSwitch sw, short p){
		if(sw==null) return;
		if(m.getSwitchContainer(sw.getId())==null){
			MonitorSwitchContainer msc = new MonitorSwitchContainer(m,sw.getId());
			m.switchList.add(msc);
		}
		if(m.getUpdate()<m.getLatUpdate()) // to ensure that only one of the two threads will check (prevents concurrent access)
			m.getSwitchContainer(sw.getId()).checkPort();
		try{
			Data rawData = null;
			// create action
			List<OFAction> actions = new ArrayList<OFAction>(1);      
			actions.add(new OFActionOutput(p, (short) 0));
			// create data
			Ethernet eth = new Ethernet();
			eth.setSourceMACAddress(sw.getPort(p).getHardwareAddress());
			eth.setDestinationMACAddress(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff,(byte)0xff, (byte)0xff, (byte)0xff});
			eth.setEtherType((short)0x07c3);
			// create packet_out
			OFPacketOut pa = new OFPacketOut();
			short l = (short)OFPacketOut.MINIMUM_LENGTH;
			pa.setBufferId(0xffffffff);
			pa.setInPort((short)-1);
			pa.setXid(m.getId());
			pa.setActionsLength((short)OFActionOutput.MINIMUM_LENGTH);
			l += OFActionOutput.MINIMUM_LENGTH;
			pa.setActions(actions);
			rawData = new Data().setData(MonitorUtils.buildLatencyPayload(Long.valueOf(m.getControllerId()),(short) 0,sw.getId(),p,MonitorUtils.longToByteArray(System.currentTimeMillis()))); // timestamp @ the last possible moment
			eth.setPayload(rawData);
			eth.setPad(true);
			byte[] data = eth.serialize();
			pa.setPacketData(data); 
			l += (short)data.length;
			pa.setLength(l); 
			//send everything
			try { 
				sw.write(pa, null); 
				sw.flush();
			} catch (IOException e) {
				if(m.logger.isErrorEnabled())
					m.logger.error("Failed to write packet to switch "+sw+". Cause: "+e);
			}
		}catch(java.lang.NullPointerException e){}//Switch is down
	}
}