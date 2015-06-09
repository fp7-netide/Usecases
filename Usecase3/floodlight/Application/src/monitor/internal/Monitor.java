package net.floodlightcontroller.monitor.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.monitor.IMonitorListener;
import net.floodlightcontroller.monitor.IMonitorService;
import net.floodlightcontroller.monitor.stuctures.LatencyPacket;
import net.floodlightcontroller.monitor.stuctures.MonitorPortContainer;
import net.floodlightcontroller.monitor.stuctures.MonitorSwitchContainer;
import net.floodlightcontroller.monitor.web.MonitorWebRoutable;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.restserver.IRestApiService;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPortStatus;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the monitored parameters of the network by continuously updating different containers<br>
 * {@link MonitorSwitchContainer} for switches and {@link MonitorPortContainer} for ports.<br>
 * Uses {@link MonitorStatisticsThread} for general statistics and {@link MonitorLatencyThread} for latency.
 * @author K.Phemius<br>
 * DT/CEA/TAI Lab<br>
 * Copyright (c) 2014 Thales Communications & Security<br>
 * 4 av. des Louvresses - 92230 Gennevilliers - France<br>
 * All rights reserved
 *
 **/
public class Monitor implements IFloodlightModule, IMonitorService , IOFMessageListener {
	protected IFloodlightProviderService floodlightProvider;
	protected IRestApiService restApi;
	protected Logger logger;
	/**
	 * A list of modules registered to Monitor
	 */
	protected List<IMonitorListener> monitorAware;
	/**
	 * Thread to collects the statistics
	 */
	protected MonitorStatisticsThread monitorStatThread;
	/**
	 * Thread to collect the latency
	 */
	protected MonitorLatencyThread monitorLatThread;
    /**
	 * The controller's *unique* ID
	 */
	protected String controllerid;
	/**
	 * The list of monitored switches
	 */
	protected List<MonitorSwitchContainer> switchList;
	/**
	 * The size of the update history
	 */
	protected int historySize;
	/**
	 * The interval (in milliseconds) between each update
	 */
	protected int statisticsUpdateInterval;
	/**
	 * The interval (in milliseconds) between each latency update
	 */
	protected int latencyUpdateInterval;
	/* ------------------------------------  Getters / Setters  -------------------------------------*/
	public IFloodlightProviderService getFloodlightProvider() {return floodlightProvider;}
	public int getHistorySize() {return historySize;}
	public void setHistorySize(int history) {this.historySize = history;}
	public int getUpdate() {return statisticsUpdateInterval;}
	public int getLatUpdate() {return latencyUpdateInterval;}
	public void setLatUpdate(int update) {this.latencyUpdateInterval = update;}
	public void setUpdate(int update) {this.statisticsUpdateInterval = update;}
    public String getControllerId() {return controllerid;}
	/* ------------------------------------- Floodlight methods ------------------------------------ */
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = 
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IMonitorService.class);
		return l;
	}
	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		Map<Class<? extends IFloodlightService>,
		IFloodlightService> m = 
		new HashMap<Class<? extends IFloodlightService>,
		IFloodlightService>();
		// We are the class that implements the service
		m.put(IMonitorService.class, this);
		return m;
	}
	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = 
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRestApiService.class);
		return l;
	}
	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return false;
	}
	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// we are asking the module service to put Monitor BEFORE every other modules
		// in the PACKET_IN queue (to catch latency packets faster)
		return (type.equals(OFType.PACKET_IN));
	}
	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		restApi = context.getServiceImpl(IRestApiService.class);
		logger = LoggerFactory.getLogger(Monitor.class);
	} 
	@Override
	public void startUp(FloodlightModuleContext context) {
		logger.debug("Starting " + this.getClass().getCanonicalName());
		// floodlightProvider init
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this); // PACKET_IN
		floodlightProvider.addOFMessageListener(OFType.PORT_STATUS, this); // PORT_STATUS
		// REST API init
		restApi.addRestletRoutable(new MonitorWebRoutable());
		// values init 
        try{
			controllerid = Long.valueOf(context.getConfigParams(this).get("controllerid")).toString();
		}catch(java.lang.NumberFormatException e){
			logger.error("Controller ID not provided");
            logger.error("Monitor not active");
            return;
		}
		try{
			statisticsUpdateInterval = Integer.valueOf(context.getConfigParams(this).get("update"));
		}catch(java.lang.NumberFormatException e){
			statisticsUpdateInterval = 1000; // default value is 1s
		}
		try{
			latencyUpdateInterval = Integer.valueOf(context.getConfigParams(this).get("latency"));
		}catch(java.lang.NumberFormatException e){
			latencyUpdateInterval = 5000; // default value is 5s
		}
		try{
			historySize = Integer.valueOf(context.getConfigParams(this).get("history"));
		}catch(java.lang.NumberFormatException e){
			historySize = 10; // default value is 10
		}
		switchList = new ArrayList<MonitorSwitchContainer>();
		monitorAware = Collections.synchronizedList(new ArrayList<IMonitorListener>());
		// threads init
		monitorStatThread = new MonitorStatisticsThread(this);
		monitorLatThread = new MonitorLatencyThread(this);
		monitorStatThread.start();
		monitorLatThread.start();
		logger.info("" + this.getClass().getCanonicalName() + " started");
	}
	// ---------------------------------------------------------- iMonitor methods ----------------------------------------------- //
	@Override
	public String getName() {
		return "monitor";
	}
	@Override
	public void addListener(IMonitorListener listener) {
		monitorAware.add(listener);
	}
	public int getId() {
		return "Monitor".hashCode();
	}
	@Override
	public MonitorSwitchContainer getSwitchContainer(long id){
		for(MonitorSwitchContainer msc : switchList){
			if(msc.getSw()==id)
				return msc;
		}
		return null;
	}
	@Override
	public java.util.List<MonitorSwitchContainer> getSwList(){
		return switchList;
	}
	// ---------------------------------------------------------- Monitor methods ----------------------------------------------- //
	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		double now = System.currentTimeMillis();// timestamp @ the earliest possible moment
		// handle PORT_STAUS messages (whether the port is up or down)
		if(msg.getType()==OFType.PORT_STATUS){
			updatePortMap(sw,((OFPortStatus) msg).getDesc().getPortNumber(),((OFPortStatus) msg).getReason(),((OFPortStatus) msg).getDesc().getState());
			return Command.CONTINUE;
		}
		// handle PACKET_IN messages : if the EherType is 0x07C3 then update the latency 
		if(msg.getType()==OFType.PACKET_IN){
			OFMatch match = new OFMatch();
			match.loadFromPacket(((OFPacketIn) msg).getPacketData(), ((OFPacketIn) msg).getInPort());
			if(match.getDataLayerType()==0x07c3){
				if(switchList.isEmpty())
					return Command.CONTINUE; // don't bother to do anything if we have no switch yet; it will only lead to NullPointerExceptions
				Ethernet eth = new Ethernet();
				eth.deserialize(((OFPacketIn) msg).getPacketData(),0,((OFPacketIn) msg).getPacketData().length);
				LatencyPacket lp = new LatencyPacket();
				/**
				 * pre-2014 floodlight version must use this :
				lp.deserializeLatencyPacket(eth.getPayload().serialize());
				/**
				 * Latest floodlight version must use this :
				 */
				lp.deserializeLatencyPacket(eth.getPayload().serialize());
				//lp.deserializeLatencyPacket(MonitorUtils.getEthPayload(eth.getPayload().serialize()));
				
				if(lp.getId()!=Long.valueOf(controllerid)){
					// This packet comes from another controller
					if(lp.getOpCode()==(short) 0){ // This is a latency request => send a reply
						try{
							Data rawData = null;
							// create action
							List<OFAction> actions = new ArrayList<OFAction>(1);      
							actions.add(new OFActionOutput(((OFPacketIn) msg).getInPort(), (short) 0));
							// create data
							Ethernet ethOut = new Ethernet();
							ethOut.setSourceMACAddress(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff,(byte)0xff, (byte)0xff, (byte)0xff});
							ethOut.setDestinationMACAddress(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff,(byte)0xff, (byte)0xff, (byte)0xff});
							ethOut.setEtherType((short)0x07c3);
							// create packet_out
							OFPacketOut pa = new OFPacketOut();
							short l = (short)OFPacketOut.MINIMUM_LENGTH;
							pa.setBufferId(0xffffffff);
							pa.setInPort((short)-1);
							pa.setXid(getId());
							pa.setActionsLength((short)OFActionOutput.MINIMUM_LENGTH);
							l += OFActionOutput.MINIMUM_LENGTH;
							pa.setActions(actions);
							byte[] ts = MonitorUtils.longToByteArray(lp.getTimestamp());
							byte[] rttDst = MonitorUtils.longToByteArray(0);
							try{
								rttDst = MonitorUtils.longToByteArray((long) ((getSwitchContainer(sw.getId()).getStatsLatList().get(getSwitchContainer(sw.getId()).getStatsLatList().size()-1))/1000000)); // approximation
							}catch(ArrayIndexOutOfBoundsException e){}
							catch(NullPointerException e) {logger.info("Trying to get the latency of unknown switch : "+lp.toString());return Command.STOP;} // no lat_map yet for the switch
							rawData = new Data().setData(MonitorUtils.buildLatencyPayload(Long.valueOf(controllerid),(short) 1,sw.getId(),(short) 0, MonitorUtils.mergeByteArrays(ts,rttDst)));
							ethOut.setPad(true);
							ethOut.setPayload(rawData);
							byte[] data = ethOut.serialize();
							pa.setPacketData(data); 
							l += (short)data.length;
							pa.setLength(l); 
							//send everything
							try { 
								sw.write(pa, null); 
								sw.flush();
							} catch (IOException e) {
								if(logger.isErrorEnabled())
									logger.error("Failed to write packet to switch "+sw+". Cause: "+e);
							}
							return Command.STOP;
						}catch(java.lang.NullPointerException e){
							if(logger.isErrorEnabled())
								logger.error("Monitor : java.lang.NullPointerException in receive(1)");
							return Command.STOP;
						}
					}else if(lp.getOpCode()==(short) 1){ // This is a latency reply, get the link's lantecy
						try{
							double timestamp = lp.getTimestamp();
							double rttDst = lp.getRtt();
							double rttSrc = 0;
							try{
								rttSrc=(getSwitchContainer(sw.getId()).getStatsLatList().get(getSwitchContainer(sw.getId()).getStatsLatList().size()-1))/1000000;
							}catch(ArrayIndexOutOfBoundsException e){}
							double time = now - timestamp - (now - timestamp-rttSrc<0?0:rttSrc)-(now - timestamp-rttDst<0?0:rttDst); // sometime the result is < 0 , in that case put 0
							updateLatencyMap(sw,((OFPacketIn) msg).getInPort(),time/2.0); // latency ~= RTT/2
						}catch(java.lang.NullPointerException err){logger.info("Trying to get the latency of unknown switch : "+lp.toString());return Command.STOP;} // no lat_map yet for the switch
					}
					return Command.STOP;
				}else{ // regular latency packet
					try{
						double rttSrc = 0;
						double rttDst = 0;
						IOFSwitch swSrc = floodlightProvider.getSwitches().get(lp.getSwitch());
						rttSrc = (getSwitchContainer(swSrc.getId()).getStatsLatList().get(getSwitchContainer(swSrc.getId()).getStatsLatList().size()-1)/2)/1000000; // approximation 
						rttDst = (getSwitchContainer(sw.getId()).getStatsLatList().get(getSwitchContainer(sw.getId()).getStatsLatList().size()-1)/2)/1000000; // approximation 
						double time = now - lp.getTimestamp() - (now - lp.getTimestamp()-rttSrc<0?0:rttSrc)-(now - lp.getTimestamp()-rttDst<0?0:rttDst);
						updateLatencyMap(swSrc,lp.getPort(),time);
						return Command.STOP;
					}catch(NullPointerException err){return Command.STOP;} // no lat_map yet for switch //logger.error("Trying to get the latency of unknown switch "+byteArrayToLong(match.getDataLayerSource(),0,match.getDataLayerSource().length));
					catch(ArrayIndexOutOfBoundsException err){return Command.STOP;} //logger.error("ArrayIndexOutOfBoundsException in receive()")
				}
			}else{
				return Command.CONTINUE;
			}
		}
		return Command.CONTINUE;
	}
	/**
	 * Update a {@link MonitorPortContainer} statsPortsMap
	 * @param sw The switch
	 * @param port The port
	 * @param reason The reason of the message (<b>2</b>=modification,<b>1</b>=port removed (definitively),<b>0</b>=port added)
	 * @param state The state of the port (<b>0</b>=up, <b>1</b>=down)
	 */
	private void updatePortMap(IOFSwitch sw, short port, byte reason,	int state) {	
		long linkState=0; // presume the port is down
		if(reason==2 && state==0)
			linkState=1;  // port is up
		if(reason==2 && state==1)
			linkState=0;  // port is administratively down
		try{
			getSwitchContainer(sw.getId()).getPortContainer(port).setState(linkState); // set new port status
		}catch(NullPointerException e){logger.error("NullPointerException in Monitor.updatePortMap()");} // switch or port unknown
	}
	/**
	 * Update a Update a {@link MonitorPortContainer} statsLatencyMap
	 * @param sw The switch to update
	 * @param port The port to update
	 * @param latency The latency value
	 */
	private void updateLatencyMap(IOFSwitch sw, short port, double latency) {
		try{
			getSwitchContainer(sw.getId()).getPortContainer(port).setLatency((latency < 0 ? 0 : MonitorUtils.round(latency,2)));
		}catch(NullPointerException e){logger.info("NullPointerException in Monitor.updateLatencyMap()");}
	}
	/**
	 * Inform the listeners that the latency of all the links has been updated
	 */
	protected void informListeners() {
		try{
			for (IMonitorListener aMonitorAware : monitorAware) {
				aMonitorAware.latencyAlert();
			}
		}catch(java.util.ConcurrentModificationException e){logger.error("ConcurrentModificationException in informListeners()");}
	}
}