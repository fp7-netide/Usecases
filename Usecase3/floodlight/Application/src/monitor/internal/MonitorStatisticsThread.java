package net.floodlightcontroller.monitor.internal;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.monitor.stuctures.MonitorSwitchContainer;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFPortStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;
import org.openflow.protocol.statistics.OFStatisticsType;

/**
 * Monitor the statistics about the ports and the flows of the connected switches
 * 
 * @author K.Phemius<br>
 *         DT/CEA/TAI Lab<br>
 *         Copyright (c) 2014 Thales Communications & Security<br>
 *         4 av. des Louvresses - 92230 Gennevilliers - France<br>
 *         All rights reserved
 * 
 **/
public class MonitorStatisticsThread extends Thread {
	private Monitor m;
	private MonitorNanoTimer t;

	public MonitorStatisticsThread(Monitor monitor) {
		m = monitor;
		t = new MonitorNanoTimer();
	}
	/**
	 * Used as a stop watch (in nanoseconds)
	 **/
	public class MonitorNanoTimer {
		private double startTime = 0.0;
		private double endTime   = 0.0;
		/**
		 * Starts the timer
		 */
		public void start(){
			this.startTime = System.nanoTime();
		}
		/**
		 * Stops the timer
		 */
		public void stop() {
			this.endTime = System.nanoTime();
		}
		/**
		 * Get the elapsed time
		 */
		public Double getTotalTime() {
			return (this.endTime - this.startTime);
		}
	}
	/**
	 * Main loop
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		try {
			Thread.sleep(3000); // let the thread sleep for 3s after init to let the system boot properly
		} catch (InterruptedException e) {}
		while (true) {
			int nbSwitch =  m.floodlightProvider.getAllSwitchMap().keySet().size();
			if(nbSwitch!=0){
				boolean miniSleep = m.statisticsUpdateInterval > nbSwitch;
				for (IOFSwitch sw : m.floodlightProvider.getAllSwitchMap().values()) {
					if(m.getSwitchContainer(sw.getId())==null){ // updates the switch list
						MonitorSwitchContainer msc = new MonitorSwitchContainer(m,sw.getId());
						m.switchList.add(msc);
					}
					if(m.getLatUpdate()<=m.getUpdate()) // to ensure that only one of the two threads will check (prevents concurrent acces)
						m.getSwitchContainer(sw.getId()).checkPort(); // updates the port list
					getStats(sw);
					if (miniSleep) { // if we can, the thread sleeps for a short time between each query to space them out
						try {
							Thread.sleep(m.statisticsUpdateInterval / nbSwitch);
						} catch (InterruptedException e) {
							break;
						} catch (java.lang.ArithmeticException e) {
							try {
								Thread.sleep(m.statisticsUpdateInterval);
							} catch (InterruptedException e1) {
								break;
							}
						}
					}
				}
				if (!miniSleep) { // if we cannot, there is only one sleep period per cycle
					try {
						Thread.sleep(m.statisticsUpdateInterval);
					} catch (InterruptedException e) {
						break;
					}
				}
				updateInstantaneous();
				flushMaps();
			}else{
				try {
					Thread.sleep(m.statisticsUpdateInterval);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}
	/**
	 * @param m the OFMatch
	 * @return a unique hash value for a OFMatch
	 */
	private int hashPacket(OFMatch m) {
		final int prime = 131;
		int result = 1;
		result = prime * result + Arrays.hashCode(m.getDataLayerDestination());
		result = prime * result + Arrays.hashCode(m.getDataLayerSource());
		result = prime * result + m.getDataLayerType();
		result = prime * result + m.getDataLayerVirtualLan();
		result = prime * result + m.getDataLayerVirtualLanPriorityCodePoint();
		result = prime * result + m.getNetworkDestination();
		result = prime * result + m.getNetworkProtocol();
		result = prime * result + m.getNetworkSource();
		result = prime * result + m.getNetworkTypeOfService();
		result = prime * result + m.getTransportDestination();
		result = prime * result + m.getTransportSource();
		return Math.abs(result);
	}
	/**
	 * Collects statistics on the switches for each ports. <br>
	 * Statistics include : <li>Port statistics <li>Flow statistics <li>
	 * Switch-to-Controller latency <br>
	 * 
	 * @param sw
	 *            The switch
	 */
	private void getStats(IOFSwitch sw) {
		MonitorSwitchContainer msc = m.getSwitchContainer(sw.getId());
		// Port stats
		Future<List<OFStatistics>> futurePort;
		List<OFStatistics> statsPort = null;
		OFStatisticsRequest reqPort = new OFStatisticsRequest();
		reqPort.setStatisticType(OFStatisticsType.PORT);
		int requestLengthPort = reqPort.getLengthU();
		OFPortStatisticsRequest specificReqPort = new OFPortStatisticsRequest();
		specificReqPort.setPortNumber((short) OFPort.OFPP_NONE.getValue());
		reqPort.setStatistics(Collections
				.singletonList((OFStatistics) specificReqPort));
		requestLengthPort += specificReqPort.getLength();
		reqPort.setLengthU(requestLengthPort);
		try {
			futurePort = sw.queryStatistics(reqPort);
			statsPort = futurePort.get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			m.logger.error("Failure retrieving port statistics from switch "
					+ sw + ". Cause: " + e);
		}
		// Flow stats
		Future<List<OFStatistics>> futureFlow;
		List<OFStatistics> statsFlow = null;
		OFFlowStatisticsRequest specificReqFlow = new OFFlowStatisticsRequest();
		OFStatisticsRequest reqFlow = new OFStatisticsRequest();
		reqFlow.setStatisticType(OFStatisticsType.FLOW);
		int requestLengthFlow = reqFlow.getLengthU();
		OFMatch match = new OFMatch();
		match.setWildcards(0xffffffff);
		specificReqFlow.setMatch(match);
		specificReqFlow.setOutPort(OFPort.OFPP_NONE.getValue());
		specificReqFlow.setTableId((byte) 0xff);
		reqFlow.setStatistics(Collections
				.singletonList((OFStatistics) specificReqFlow));
		requestLengthFlow += specificReqFlow.getLength();
		reqFlow.setLengthU(requestLengthFlow);
		try {
			futureFlow = sw.queryStatistics(reqFlow);
			t.start();
			statsFlow = futureFlow.get(10, TimeUnit.SECONDS);
			t.stop();
		} catch (Exception e) {
			m.logger.error("Failure retrieving flow statistics from switch "
					+ sw + ". Cause: " + e);
		}
		// Tables update
		// Port map
		if (!statsPort.isEmpty()) {
			msc.getStatsPortList().add(statsPort);
		}
		// Flow map
		msc.getStatsFlowList().add(statsFlow);
		// switch latency map
		msc.getStatsLatList().add(t.getTotalTime());
	}

	/**
	 * Update instantaneous data
	 */
	private void updateInstantaneous() {
		try {
			Collection<IOFSwitch> switches = m.floodlightProvider.getAllSwitchMap()
					.values();
			if (!switches.isEmpty()) {
				for (IOFSwitch sw : switches) {
					List<List<OFStatistics>> listPStat = m.getSwitchContainer(sw.getId()).getStatsPortList();
					if (listPStat != null && !listPStat.isEmpty()
							&& listPStat.size() > 1) {
						List<OFStatistics> lastValue = (List<OFStatistics>) listPStat
								.get(listPStat.size() - 1);
						List<OFStatistics> secondToLastValue = (List<OFStatistics>) listPStat
								.get(listPStat.size() - 2);
						Map<Short, List<Long>> lastValueList = new HashMap<Short, List<Long>>();
						for (OFStatistics aLastValue : lastValue) {
							OFPortStatisticsReply s = (OFPortStatisticsReply) aLastValue;
							if (s.getPortNumber() == -2)
								continue;
							List<Long> list = new ArrayList<Long>();
							list.add(s.getReceiveBytes()); // 0
							list.add(s.getReceivePackets()); // 1
							list.add(s.getReceiveDropped()); // 2
							list.add(s.getreceiveErrors()); // 3
							list.add(s.getTransmitBytes()); // 4
							list.add(s.getTransmitPackets()); // 5
							list.add(s.getTransmitDropped()); // 6
							list.add(s.getTransmitErrors()); // 7
							lastValueList.put(s.getPortNumber(), list);
						}
						for (OFStatistics aSecondToLastValue : secondToLastValue) {
							OFPortStatisticsReply s = (OFPortStatisticsReply) aSecondToLastValue;
							if (s.getPortNumber() == -2)
								continue;
							// Some values are omitted
							List<Long> list = new ArrayList<Long>();
							list.add(s.getReceiveBytes()); // 8
							list.add(s.getReceivePackets()); // 9
							list.add(s.getReceiveDropped()); // 10
							list.add(s.getreceiveErrors()); // 11
							list.add(s.getTransmitBytes()); // 12
							list.add(s.getTransmitPackets()); // 13
							list.add(s.getTransmitDropped()); // 14
							list.add(s.getTransmitErrors()); // 15
							lastValueList.get(s.getPortNumber()).addAll(list);
						}
						for (Entry<Short, List<Long>> it4 : lastValueList
								.entrySet()) {
							List<Long> values = new ArrayList<Long>();
							values.add((it4.getValue().get(0) - it4.getValue()
									.get(8)) * 8); // data_in // 0
							values.add((it4.getValue().get(4) - it4.getValue()
									.get(12)) * 8); // data_out // 1
							values.add(it4.getValue().get(1)
									- it4.getValue().get(9)); // packet_in // 2
							values.add(it4.getValue().get(5)
									- it4.getValue().get(13)); // packet_out //
							// 3
							values.add(it4.getValue().get(2)
									- it4.getValue().get(10)); // drop_in // 4
							values.add(it4.getValue().get(6)
									- it4.getValue().get(14)); // drop_out // 5
							values.add(it4.getValue().get(3)
									- it4.getValue().get(11)); // err_in // 6
							values.add(it4.getValue().get(7)
									- it4.getValue().get(15)); // err_out // 7
							m.getSwitchContainer(sw.getId()).getPortContainer(it4.getKey()).setStatsInstantaneousPortList(
									values);
						}
					}
					try {
						List<List<OFStatistics>> listFStat = m.getSwitchContainer(sw.getId()).getStatsFlowList();
						m.getSwitchContainer(sw.getId()).getStatsInstantaneousFlowList().clear();
						if (listFStat != null && !listFStat.isEmpty()
								&& listFStat.size() > 1) {
							List<OFStatistics> lastValue = (List<OFStatistics>) listFStat
									.get(listFStat.size() - 1);
							List<OFStatistics> secondToLastValue = (List<OFStatistics>) listFStat
									.get(listFStat.size() - 2);
							if (!lastValue.isEmpty()
									&& !secondToLastValue.isEmpty()) {
								Map<Integer, List<Object>> lastValueList = new HashMap<Integer, List<Object>>();
								Iterator<OFStatistics> it = lastValue
										.iterator();
								while (it.hasNext()) {
									try {
										OFFlowStatisticsReply s = (OFFlowStatisticsReply) it
												.next();
										List<Object> list = new ArrayList<Object>();
										list.add(s.getByteCount()); // 0
										list.add(s.getPacketCount()); // 1
										lastValueList.put(
												hashPacket(s.getMatch()),
												list);
									} catch (NullPointerException e) {
										break;
									}
								}
								Iterator<OFStatistics> it2 = secondToLastValue
										.iterator();
								while (it2.hasNext()) {
									try {
										OFFlowStatisticsReply s = (OFFlowStatisticsReply) it2
												.next();
										// Some values are omitted
										List<Object> list = new ArrayList<Object>();
										list.add(s.getByteCount()); // 2
										list.add(s.getPacketCount()); // 3
										list.add((long) hashPacket(s
												.getMatch())); // 4
										list.add(s.getMatch()); //5
										lastValueList.get(
												hashPacket(s.getMatch()))
												.addAll(list);
									} catch (NullPointerException e) {
										break;
									}
								}
								Iterator<Entry<Integer, List<Object>>> it3 = lastValueList
										.entrySet().iterator();
								while (it3.hasNext()) {
									Entry<Integer, List<Object>> it4 = it3.next();
									List<Long> values = new ArrayList<Long>();
									values.add(((Long)it4.getValue().get(0) - (Long)it4
											.getValue().get(2)) * 8); // data //
									// 0
									values.add((Long)it4.getValue().get(1)
											- (Long)it4.getValue().get(3)); // packets
									// // 1
									m.getSwitchContainer(sw.getId()).getStatsInstantaneousFlowList().put(
											(Long)it4.getValue().get(4), new SimpleEntry<OFMatch,List<Long>>((OFMatch)it4.getValue().get(5),values));
								}
							}
						}
					} catch (IndexOutOfBoundsException e) {} // don't have 2 values yet
				}
			}
		} catch (java.lang.NullPointerException e) {
			if (m.logger.isErrorEnabled())
				m.logger.debug("Monitor : Link is probably down");
		} catch (java.lang.IndexOutOfBoundsException e) {}// history has just been purged
	}

	/**
	 * Flushes the maps if their sizes is bigger than {@link Monitor#historySize}<br>
	 * but keeps the last two values
	 */
	private void flushMaps() {
		for(MonitorSwitchContainer msc : m.switchList){
			if (!msc.getStatsPortList().isEmpty()
					&& msc.getStatsPortList().size() > m
					.getHistorySize()) {
				List<OFStatistics> last = msc.getStatsPortList().get(msc.getStatsPortList().size() - 1);
				List<OFStatistics> secondToLast = msc.getStatsPortList().get(msc.getStatsPortList().size() - 2);
				msc.getStatsPortList().clear();
				msc.getStatsPortList().add(secondToLast);
				msc.getStatsPortList().add(last);
			}
			if (!msc.getStatsFlowList().isEmpty()
					&& msc.getStatsFlowList().size() > m
					.getHistorySize()) {
				List<OFStatistics> last = msc.getStatsFlowList().get(msc.getStatsFlowList().size() - 1);
				List<OFStatistics> secondToLast = msc.getStatsFlowList().get(msc.getStatsFlowList().size() - 2);
				msc.getStatsFlowList().clear();
				msc.getStatsFlowList().add(secondToLast);
				msc.getStatsFlowList().add(last);
			}
			if (!msc.getStatsLatList().isEmpty()
					&& msc.getStatsLatList().size() > m
					.getHistorySize()) {
				double last = msc.getStatsLatList().get(msc.getStatsLatList().size() - 1);
				double secondToLast = msc.getStatsLatList().get(msc.getStatsLatList().size() - 2);
				msc.getStatsLatList().clear();
				msc.getStatsLatList().add(secondToLast);
				msc.getStatsLatList().add(last);

			}
		}
	}
}