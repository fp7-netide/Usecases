package net.floodlightcontroller.monitor.stuctures;


import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.monitor.internal.Monitor;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPhysicalPort;
import org.openflow.protocol.statistics.OFStatistics;

/**
 * Structure to store the statistics of a switch
 * 
 * @author K.Phemius<br>
 *         DT/CEA/TAI Lab<br>
 *         Copyright (c) 2014 Thales Communications & Security<br>
 *         4 av. des Louvresses - 92230 Gennevilliers - France<br>
 *         All rights reserved
 * 
 **/
public class MonitorSwitchContainer {
	private Monitor m;
	/**
	 * The switch
	 */
	private long sw;
	/**
	 * A List containing a list of general statistics on the ports
	 */
	private List<List<OFStatistics>> statsPortList = new ArrayList<List<OFStatistics>>();
	/**
	 * A List containing a list of statistics on the flows
	 */
	private List<List<OFStatistics>> statsFlowList = new ArrayList<List<OFStatistics>>();
	/**
	 * A List containing the latencies between the switch and the controller
	 */
	private List<Double> statsLatList = new ArrayList<Double>();
	/**
	 * A list containing the ports of the switch
	 */
	private List<MonitorPortContainer> portList;
	/**
	 * A List containing instantaneous values for each flow of each switch registered to the Controller
	 */
	private Map<Long,SimpleEntry<OFMatch,List<Long>>> statsInstantaneousFlowList = new HashMap<Long, SimpleEntry<OFMatch,List<Long>>>();

	public MonitorSwitchContainer(Monitor m,long sw) {
		this.m=m;
		this.sw=sw;
		this.portList = new ArrayList<MonitorPortContainer>(m.getFloodlightProvider().getSwitches().get(sw).getPorts().size());
		for(OFPhysicalPort p : m.getFloodlightProvider().getSwitches().get(sw).getPorts()){ // creates a MonitorPortContainer per port
			if(p.getPortNumber()!=-2)
				portList.add(new MonitorPortContainer(p.getPortNumber()));
		}
	}
	/**
	 * Find a port container
	 * @param port the port number
	 * @return the port container
	 */
	public MonitorPortContainer getPortContainer(short port) {
		for(MonitorPortContainer p : portList){
			if(p.getPort()==port)
				return p;
		}
		return null;
	}
	/**
	 * Check that all the switch's ports are monitored (useful at startup)
	 */
	public void checkPort(){
		try{
			for(OFPhysicalPort p : m.getFloodlightProvider().getSwitches().get(sw).getPorts()){
				if(getPortContainer(p.getPortNumber())==null && p.getPortNumber()!=-2){
					MonitorPortContainer mpc = new MonitorPortContainer(p.getPortNumber());
					portList.add(mpc);
				}
			}
		}catch(NullPointerException e){} // switch is down or removed
	}
	//  Getters/Setters
	public Map<Long, SimpleEntry<OFMatch, List<Long>>> getStatsInstantaneousFlowList() {
		return statsInstantaneousFlowList;
	}
	public List<List<OFStatistics>> getStatsPortList() {
		return statsPortList;
	}
	public long getSw() {
		return sw;
	}
	public void setSw(long sw) {
		this.sw = sw;
	}
	public List<MonitorPortContainer> getPorts() {
		return portList;
	}
	public List<List<OFStatistics>> getStatsFlowList() {
		return statsFlowList;
	}
	public void setStatsFlowList(List<List<OFStatistics>> statsFlowList) {
		this.statsFlowList = statsFlowList;
	}
	public List<Double> getStatsLatList() {
		return statsLatList;
	}
	public void setStatsLatList(List<Double> statsLatList) {
		this.statsLatList = statsLatList;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MonitorSwitchContainer [sw=" + sw + ", statsPortList="
				+ statsPortList + ", statsFlowList=" + statsFlowList
				+ ", statsLatList=" + statsLatList + ", portList=" + portList
				+ ", statsInstantaneousFlowList=" + statsInstantaneousFlowList
				+ "]";
	}
}
