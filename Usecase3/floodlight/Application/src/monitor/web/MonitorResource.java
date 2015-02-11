package net.floodlightcontroller.monitor.web;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;

import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.monitor.IMonitorService;
import net.floodlightcontroller.monitor.stuctures.MonitorPortContainer;
import net.floodlightcontroller.monitor.stuctures.MonitorSwitchContainer;
import net.floodlightcontroller.monitor.web.structures.DataContainer;
import net.floodlightcontroller.monitor.web.structures.ErrorContainer;
import net.floodlightcontroller.monitor.web.structures.PathContainer;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.topology.NodePortTuple;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * @author K.Phemius<br>
 * DT/CEA/TAI Lab<br>
 * Copyright (c) 2014 Thales Communications & Security<br>
 * 4 av. des Louvresses - 92230 Gennevilliers - France<br>
 * All rights reserved
 *
 **/
public class MonitorResource extends ServerResource {

	/**
	 * @return the queried data
	 */
	@Get("json")
	public Object retrieve() {
		IMonitorService m = (IMonitorService)getContext().getAttributes().get(IMonitorService.class.getCanonicalName());
		ILinkDiscoveryService l = (ILinkDiscoveryService)getContext().getAttributes().get(ILinkDiscoveryService.class.getCanonicalName());
		IRoutingService r = (IRoutingService)getContext().getAttributes().get(IRoutingService.class.getCanonicalName());
		MonitorSwitchContainer msc;
		MonitorPortContainer mpc;
		try{
			switch (QueryType.valueOf((String) getRequestAttributes().get("query"))) {
			case getRawData:
				return m.getSwList();
			case getRawSwitchData:
				/** /wm/monitor/getRawSwitchData?id=X */
				try{
					return m.getSwitchContainer(Long.valueOf(getQuery().getValues("id")));
				}catch(NullPointerException e){
					return new ErrorContainer(21, "UnknownSwitchError", "The server did not found a switch with the provided ID ("+Long.valueOf(getQuery().getValues("id"))+").");
				}
			case getRawPortData:
				/** /wm/monitor/getRawPortData?id=X&port=Y */
				msc = m.getSwitchContainer(Long.valueOf(getQuery().getValues("id")));
				if(msc==null)
					return new ErrorContainer(21, "UnknownSwitchError", "The server did not find the switch ("+Long.valueOf(getQuery().getValues("id"))+").");
				mpc = msc.getPortContainer(Short.valueOf(getQuery().getValues("port")));
				if(mpc!=null)
					return mpc;
				return new ErrorContainer(31, "UnknownPortError", "The server did not found the port ("+Short.valueOf(getQuery().getValues("port"))+").");
			case getAllData:
				/** /wm/monitor/getData */
				ArrayList<DataContainer> list = new ArrayList<DataContainer>();
				for(MonitorSwitchContainer sw : m.getSwList()) {
					for(MonitorPortContainer port : sw.getPorts()) {
						DataContainer p = new DataContainer();
						p.setSw(sw.getSw());
						p.setPort(port.getPort());
						p.setDelay(port.getLatency());
						try {
							p.setLoss(port.getStatsInstantaneousPortList().get(4));
						}catch(IndexOutOfBoundsException e) {
							// the structure wasn't instantiated properly
							// this shouldn't happend
						}
						SimpleEntry<Long, Short> ep = findEP(l,p.getSw(),p.getPort());
						if(ep==null)
							continue; // not internal link or link down.
						p.setSwR(ep.getKey());
						p.setPortR(ep.getValue());
						list.add(p);
					}
				}
				return list;
			case getSwitchData:
				/** /wm/monitor/getSwitchData?id=X */
				try{
					msc = m.getSwitchContainer(Long.valueOf(getQuery().getValues("id")));
					ArrayList<DataContainer> listP = new ArrayList<DataContainer>(msc.getPorts().size());
					for(MonitorPortContainer port : msc.getPorts()) {
						DataContainer p = new DataContainer();
						p.setSw(msc.getSw());
						p.setPort(port.getPort());
						p.setDelay(port.getLatency());
						try {
							p.setLoss(port.getStatsInstantaneousPortList().get(4));
						}catch(IndexOutOfBoundsException e) {
							// the structure wasn't instantiated properly
							// this shouldn't happend
						}
						SimpleEntry<Long, Short> ep = findEP(l,p.getSw(),p.getPort());
						if(ep==null)
							continue; // not internal link or link down.
						p.setSwR(ep.getKey());
						p.setPortR(ep.getValue());
						listP.add(p);
					}
					return listP;
				}catch(NullPointerException e){
					return new ErrorContainer(21, "UnknownSwitchError", "The server did not found a switch with the provided ID ("+Long.valueOf(getQuery().getValues("id"))+").");
				}
			case getPortData:
				/** /wm/monitor/getPortData?id=X&port=Y */
				msc = m.getSwitchContainer(Long.valueOf(getQuery().getValues("id")));
				if(msc==null)
					return new ErrorContainer(21, "UnknownSwitchError", "The server did not found a switch with the provided ID ("+Long.valueOf(getQuery().getValues("id"))+").");
				mpc = msc.getPortContainer(Short.valueOf(getQuery().getValues("port")));
				if(mpc!=null) {
					DataContainer port = new DataContainer();
					port.setSw(msc.getSw());
					port.setPort(mpc.getPort());
					port.setDelay(mpc.getLatency());
					try {
						port.setLoss(mpc.getStatsInstantaneousPortList().get(4));
					}catch(IndexOutOfBoundsException e) {
						// the structure wasn't instantiated properly
						// this shouldn't happend
					}
					SimpleEntry<Long, Short> ep = findEP(l,port.getSw(),port.getPort());
					if(ep==null)
						return new ErrorContainer(41, "EndPointError", "The server did not found the end point attached to the provided source. ["+port.getSw()+":"+port.getPort()+"] is not an Internal link or is down.");
					port.setSwR(ep.getKey());
					port.setPortR(ep.getValue());
					return port;
				}
				return new ErrorContainer(31, "UnknownPortError", "The server did not found the port ("+Short.valueOf(getQuery().getValues("port"))+").");
			case getRouteData:
				/** /wm/monitor/getRouteData?id1=X&id2=Z[&multiple=true] */
				String s1 = getQuery().getValues("id1");
				String s2 = getQuery().getValues("id2");

				if(s1==null || s2==null)
					return new ErrorContainer(51, "RouteError", "Must provide valid switch IDs.");

				if(m.getSwitchContainer(Long.valueOf(s1))==null)
					return new ErrorContainer(52, "RouteError", "Switch not found ("+Long.valueOf(s1)+").");
				if(m.getSwitchContainer(Long.valueOf(s2))==null)
					return new ErrorContainer(53, "RouteError", "Switch not found ("+Long.valueOf(s2)+").");

				// useless for now because routing doesn't implement this yet;
				boolean multiple=Boolean.valueOf(getQuery().getValues("multiple"));

				ArrayList<Route> routes = new ArrayList<Route>();
				if(multiple) {
					// not implemented yet
					routes = r.getRoutes(Long.valueOf(s1), Long.valueOf(s2), true);
				} else{
					routes.add(r.getRoute(Long.valueOf(s1), Long.valueOf(s2),0));
				}
				if(routes.isEmpty())
					return new ErrorContainer(54, "RouteError", "No route found.");
				ArrayList<PathContainer> listP = new ArrayList<PathContainer>();
				for(Route route : routes) {
					if(route==null || route.getPath().isEmpty())
						continue;
					PathContainer p = new PathContainer();
					p.setSw(route.getPath().get(0).getNodeId());
					p.setPort(route.getPath().get(0).getPortId());
					p.setSwR(route.getPath().get(route.getPath().size()-1).getNodeId());
					p.setPortR(route.getPath().get(route.getPath().size()-1).getPortId());
					ArrayList<String> listNpt = new ArrayList<String>();
					for(NodePortTuple npt : route.getPath()) {
						listNpt.add(npt.getNodeId()+":"+npt.getPortId());
					}
					p.setPath(listNpt);
					for(int i=0;i<route.getPath().size();i++) {
						NodePortTuple npt = route.getPath().get(i);
						if(i%2==0) {
							p.setRtt(p.getRtt()+m.getSwitchContainer(npt.getNodeId()).getPortContainer(npt.getPortId()).getLatency());
						}else {
							p.setRtt(p.getRtt()+m.getSwitchContainer(npt.getNodeId()).getPortContainer(npt.getPortId()).getLatency());
							p.setDelay(p.getDelay()+m.getSwitchContainer(npt.getNodeId()).getPortContainer(npt.getPortId()).getLatency());
							try {
								p.setLoss(p.getLoss()+m.getSwitchContainer(npt.getNodeId()).getPortContainer(npt.getPortId()).getStatsInstantaneousPortList().get(4));
							}catch(IndexOutOfBoundsException e) {
								// the structure wasn't instantiated properly
								// this shouldn't happend
								continue;
							}
						}
					}
					listP.add(p);
				}
				if(listP.isEmpty())
					return new ErrorContainer(54, "RouteError", "No route found.");
				return listP;
			default:
				return new ErrorContainer(1, "RequestError", "The server did not recognize the request. Check help.");
			}
		}catch(java.lang.IllegalArgumentException e){
			return new ErrorContainer(1, "RequestError", "The server did not recognize the request. Check help.");
		}
	}
	/**
	 * Find the other end of a link
	 * @param l {@link ILinkDiscoveryService} instance
	 * @param sw the switch
	 * @param p the port
	 * @return a tuple {switch,port} or <b>null</b> if no end point was found
	 */
	private SimpleEntry<Long, Short> findEP(ILinkDiscoveryService l, long sw, short p) {
		for(Link link : l.getSwitchLinks().get(sw)) {
			if(link.getSrc()==sw && link.getSrcPort()==p)
				return new AbstractMap.SimpleEntry<Long, Short>(link.getDst(),link.getDstPort());
		}
		return null;
	}
	private enum QueryType{
		// raw data
		getRawData, getRawSwitchData, getRawPortData,
		// procesed data
		getAllData, getSwitchData, getPortData, getRouteData,
	}
}