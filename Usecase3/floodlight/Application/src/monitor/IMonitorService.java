package net.floodlightcontroller.monitor;

import java.util.List;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.monitor.stuctures.MonitorSwitchContainer;

/**
 * Used to interact with Monitor 
 * @author K.Phemius<br>
 * DT/CEA/TAI Lab<br>
 * Copyright (c) 2014 Thales Communications & Security<br>
 * 4 av. des Louvresses - 92230 Gennevilliers - France<br>
 * All rights reserved
 *
**/
public interface IMonitorService  extends IFloodlightService  {
	/**
	 * Add a new Monitor listener
	 * @param listener The listener
	 */
	public void addListener(IMonitorListener listener);
	/**
	 * @return The size of the history
	 */
	public int getHistorySize();
	/**
	 * @param history The new history size
	 */
	public void setHistorySize(int history);
	/**
	 * @return The update interval
	 */
	public int getUpdate();
	/**
	 * @return The latency update interval
	 */
	public int getLatUpdate();
	/**
	 * Changes the latency update interval
	 * @param update the new rate
	 */
	public void setLatUpdate(int update);
	/**
	 * @return the list of monitored switches
	 */
	List<MonitorSwitchContainer> getSwList();
	/**
	 * @param id the ID of the switch to be found
	 * @return a monitored switch container or <b>null</b> if it doesn't exist
	 */
	MonitorSwitchContainer getSwitchContainer(long id);
}