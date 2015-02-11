package net.floodlightcontroller.monitor;
/**
* Used to alert modules registered to {@link IMonitorService}
* @author K.Phemius<br>
* DT/CEA/TAI Lab<br>
* Copyright (c) 2014 Thales Communications & Security<br>
* 4 av. des Louvresses - 92230 Gennevilliers - France<br>
* All rights reserved
*
**/
public interface IMonitorListener {
	/**
	 * Alert registered listener of a latency change
	 */
	void latencyAlert();
}
