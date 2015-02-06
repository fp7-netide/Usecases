package net.floodlightcontroller.netide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.firewall.IFirewallService;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;

public class ElementsConfigurator implements IFloodlightModule, IOFSwitchListener, IFloodlightService {
	protected static Logger log = LoggerFactory.getLogger(ElementsConfigurator.class);
	protected IFloodlightProviderService floodlightProvider;
	protected IFirewallService firewall;
	
	String ipp1 = "192.168.0.0/24";
	String ipp2 = "10.0.0.0/28";
	String ipp3 = "10.0.200.0/28";

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = 
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IElementsConfiguratorService.class);
        return l;
	}

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        Map<Class<? extends IFloodlightService>,
            IFloodlightService> m = 
                new HashMap<Class<? extends IFloodlightService>,
                    IFloodlightService>();
        m.put(IElementsConfiguratorService.class, this);
        return m;
    }

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = 
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider =
	            context.getServiceImpl(IFloodlightProviderService.class);
		firewall = context.getServiceImpl(IFirewallService.class);
		
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
        floodlightProvider.addOFSwitchListener(this);	
        //firewall.enableFirewall(true);
	}
	
    public IFloodlightProviderService getFloodlightProvider() {
        return floodlightProvider;
    }

    public void setFloodlightProvider(IFloodlightProviderService floodlightProvider) {
        this.floodlightProvider = floodlightProvider;
    }

	@Override
	public void addedSwitch(IOFSwitch sw) {
		if (sw.getStringId().equalsIgnoreCase("00:00:00:00:00:00:00:01")) {
			configureRouter(sw);		
		}	
		else if (sw.getStringId().equalsIgnoreCase("00:00:00:00:00:00:00:04")) {
			configureFW1(sw);			
		}
		else if (sw.getStringId().equalsIgnoreCase("00:00:00:00:00:00:00:05")){
			configureFW2(sw);
		}
		else {
			addDefaultAllow(sw);
		}
	}

	@Override
	public void removedSwitch(IOFSwitch sw) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void switchPortChanged(Long switchId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		return "ElementsConfigurator";
	}
	
	private void configureRouter(IOFSwitch sw) {
		addDefaultDrop(sw);
	}
	
	private void configureFW1(IOFSwitch sw) {
		addDefaultDrop(sw);
		addARPAllow(sw);
		addIPAllow(sw, ipp2, (short)1);
		addIPAllow(sw, ipp3, (short)1);
		addPortAllow(sw, ipp1, IPv4.PROTOCOL_TCP, (short)80, (short)2);
		addPortAllow(sw, ipp1, IPv4.PROTOCOL_UDP, (short)53, (short)2);
	}
	
	private void configureFW2(IOFSwitch sw) {
		addDefaultDrop(sw);
		addARPAllow(sw);
		addIPAllow(sw, ipp2, (short)2);
		addIPAllow(sw, ipp3, (short)1);
	}
	
    private int[] IPCIDRToPrefixBits(String cidr) {
        int ret[] = new int[2];

        // as IP can also be a prefix rather than an absolute address
        // split it over "/" to get the bit range
        String[] parts = cidr.split("/");
        String cidr_prefix = parts[0].trim();
        int cidr_bits = 0;
        if (parts.length == 2) {
            try {
                cidr_bits = Integer.parseInt(parts[1].trim());
            } catch (Exception exp) {
                cidr_bits = 32;
            }
        }
        ret[0] = IPv4.toIPv4Address(cidr_prefix);
        ret[1] = cidr_bits;

        return ret;
    }
	
    private void addDefaultDrop(IOFSwitch sw){
    	OFFlowMod fm = new OFFlowMod();
    	OFMatch match = new OFMatch();
    	fm.setMatch(match);
    	fm.setPriority((short) 350);
    	fm.setHardTimeout((short)0);
    	fm.setOutPort(OFPort.OFPP_NONE);
    	fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);
    	List<OFAction> acts = new ArrayList<OFAction>();
    	fm.setActions(acts);
    	fm.setLengthU(OFFlowMod.MINIMUM_LENGTH);
    	sendMsg(sw, fm);
    }
    
    private void addARPAllow(IOFSwitch sw){
    	
    	OFFlowMod fm = new OFFlowMod();
    	OFMatch match = new OFMatch();
    	match.setDataLayerType(Ethernet.TYPE_ARP);
    	match.setWildcards(OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_DL_TYPE);
    	fm.setMatch(match);
    	fm.setPriority((short) 400);
    	fm.setHardTimeout((short)0);
    	fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);
    	OFActionOutput act = new OFActionOutput();
    	act.setPort(OFPort.OFPP_FLOOD.getValue());
    	List<OFAction> acts = new LinkedList<OFAction>();
    	acts.add(act);
    	fm.setActions(acts);
    	fm.setLengthU(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH);
    	sendMsg(sw, fm);
    }
    
    private void addIPAllow(IOFSwitch sw, String ip, Short outPort){
    	OFFlowMod fm = new OFFlowMod();
    	OFMatch match = new OFMatch();
    	match.setDataLayerType(Ethernet.TYPE_IPv4);
    	match.setNetworkSource(IPCIDRToPrefixBits(ip)[0]);
    	match.setWildcards(OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_DL_TYPE & 
    			~(OFMatch.OFPFW_NW_SRC_MASK & ((31+IPCIDRToPrefixBits(ip)[1]) << 8)));
    	fm.setMatch(match);
    	fm.setPriority((short) 400);
    	fm.setHardTimeout((short)0);
    	fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);
    	OFActionOutput act = new OFActionOutput();
    	act.setPort(outPort);
    	List<OFAction> acts = new LinkedList<OFAction>();
    	acts.add(act);
    	fm.setActions(acts);
    	fm.setLengthU(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH);
    	sendMsg(sw, fm);
    }
    
    private void addPortAllow(IOFSwitch sw, String ip, byte networkProtocol, Short tcpPort, Short outPort){
    	OFFlowMod fm = new OFFlowMod();
    	OFMatch match = new OFMatch();
    	match.setDataLayerType(Ethernet.TYPE_IPv4);
    	match.setNetworkSource(IPCIDRToPrefixBits(ip)[0]);
    	match.setNetworkProtocol(networkProtocol);
    	match.setTransportDestination(tcpPort);
    	match.setWildcards(OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_DL_TYPE & 
    			~(OFMatch.OFPFW_NW_SRC_MASK & ((31+IPCIDRToPrefixBits(ip)[1]) << 8))
    			& ~OFMatch.OFPFW_NW_PROTO & ~OFMatch.OFPFW_TP_DST);
    	fm.setMatch(match);
    	fm.setPriority((short) 500);
    	fm.setHardTimeout((short)0);
    	fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);
    	OFActionOutput act = new OFActionOutput();
    	act.setPort(outPort);
    	List<OFAction> acts = new LinkedList<OFAction>();
    	acts.add(act);
    	fm.setActions(acts);
    	fm.setLengthU(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH);
    	sendMsg(sw, fm);
    }
    
    private void addDefaultAllow(IOFSwitch sw){
        OFFlowMod fm = new OFFlowMod();
        OFMatch match = new OFMatch();
        match.setWildcards(OFMatch.OFPFW_ALL );
        fm.setMatch(match);
        fm.setPriority((short)350);
        fm.setHardTimeout((short)0);
        fm.setBufferId(OFPacketOut.BUFFER_ID_NONE);
        OFActionOutput act = new OFActionOutput();
        act.setPort(OFPort.OFPP_NORMAL.getValue());
        List<OFAction> acts = new LinkedList<OFAction>();
        acts.add(act);
        fm.setActions(acts);
        fm.setLengthU(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH);
        sendMsg(sw, fm);
      }



    private void clearFlowMods(IOFSwitch sw){
        // Delete pre-existing flows with the same match, and output action port
        // or outPort
    	OFFlowMod fm = new OFFlowMod();
    	OFMatch match = new OFMatch();
    	match.setInputPort((short) OFPort.OFPP_CONTROLLER.getValue());
        match.setWildcards(OFMatch.OFPFW_ALL & ~OFMatch.OFPFW_IN_PORT);
        fm.setMatch(match).setCommand(OFFlowMod.OFPFC_MODIFY).setPriority((short)0);
        fm.setLengthU(OFFlowMod.MINIMUM_LENGTH);
        sendMsg(sw, fm);
    }

    private void sendMsg(IOFSwitch sw, OFMessage msg) {
		List<OFMessage> msglist = new ArrayList<OFMessage>();
		msglist.add(msg);
		try {
			sw.write(msglist,  null);
		} catch (IOException e) {
            log.error("Tried to write to switch {} but got {}", sw.getStringId(), e.getMessage());
        }
	}

}
