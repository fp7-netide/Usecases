package net.floodlightcontroller.monitor.stuctures;

import java.util.ArrayList;
import java.util.List;

/**
 * Structure to store the statistics of a port
 * 
 * @author K.Phemius<br>
 *         DT/CEA/TAI Lab<br>
 *         Copyright (c) 2014 Thales Communications & Security<br>
 *         4 av. des Louvresses - 92230 Gennevilliers - France<br>
 *         All rights reserved
 * 
 **/
public class MonitorPortContainer {
	/**
	 * The ports ID
	 */
	private short port;
	/**
	 * A List containing the latencies for the port
	 */
	private double latency;
	/**
	 * A List containing instantaneous values for the port
	 */
	List<Long> statsInstantaneousPortList = new ArrayList<Long>();
	/**
	 * The state of the port
	 */
	private long state;
	public MonitorPortContainer(short port) {
		this.port=port;
	}
	// Getters / Setters
	public List<Long> getStatsInstantaneousPortList() {
		return statsInstantaneousPortList;
	}
	public void setStatsInstantaneousPortList(List<Long> values) {
		this.statsInstantaneousPortList=values;
	}
	public void setState(long linkState) {
		this.state=linkState;
	}
	public double getLatency() {
		return latency;
	}
	public void setLatency(double statsLatency) {
		this.latency = statsLatency;
	}
	public short getPort() {
		return port;
	}
	public void setPort(short port) {
		this.port = port;
	}
	public long getState() {
		return state;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MonitorPortContainer [port=" + port + ", statsLatency="
				+ latency + ", statsInstantaneousPortList="
				+ statsInstantaneousPortList + ", state=" + state + "]";
	}
}
