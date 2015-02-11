package net.floodlightcontroller.monitor.stuctures;

import net.floodlightcontroller.monitor.internal.MonitorUtils;

/**
 * Java object representing a latency packet
 * @author K.Phemius<br>
 * DT/CEA/TAI Lab<br>
 * Copyright (c) 2014 Thales Communications & Security<br>
 * 4 av. des Louvresses - 92230 Gennevilliers - France<br>
 * All rights reserved
 *
 **/
public class LatencyPacket{ 
	/*
	This is the format of the latency packet (full ethernet frame)
	The source is the source port's MAC address
	The destination is the broadcast address
	The etherType is 0x07C3
	The payload contains :
		The controller ID
		The operation code
			0 : latency request
			1 : latency reply
		The incoming switch ID
		The port
		The timestamp
		optionnal : The switch RTT (if the packet is a reply)
	 _________________________________________________________________________________________
	| Eth src  | Eth dst | Eth type |                        Payload                          | 
	|__________|_________|__________|____________________________________________ _ _ _ _ _ _ |
	| port MAC | 0xFF... |  0x07C3  | CtrlID | opCode | switch| port | timestamp | [RTT|null] |
	|__________|_________|__________|________|________|_______|______|___________|_ _ _ _ _ _ |
	*/
	long id;
	short opCode;
	long sw;
	short port;
	long timestamp;
	long rtt;
	
	public LatencyPacket() {}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public short getOpCode() {
		return opCode;
	}

	public void setOpCode(short opCode) {
		this.opCode = opCode;
	}
	public long getSwitch() {
		return sw;
	}
	
	public void setSwitch(long sw) {
		this.sw=sw;
	}

	public short getPort() {
		return port;
	}

	public void setPort(short port) {
		this.port = port;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getRtt() {
		return rtt;
	}

	public void setRtt(long rtt) {
		this.rtt = rtt;
	}

	/**
	 * Deserialize a byte array into this LatencyPacket object
	 * @param payload
	 */
	public void deserializeLatencyPacket(byte[] payload){
		int offset = 0;
		setId(MonitorUtils.byteArrayToLong(payload,offset));
		offset+=8;
		setOpCode(MonitorUtils.byteArrayToShort(payload, offset));
		offset+=2;
		setSwitch(MonitorUtils.byteArrayToLong(payload,offset));
		offset+=8;
		setPort(MonitorUtils.byteArrayToShort(payload, offset));
		offset+=2;
		setTimestamp(MonitorUtils.byteArrayToLong(payload,offset));
		offset+=8;
		if(opCode!=(short)0)
			setRtt(MonitorUtils.byteArrayToLong(payload,offset));
	}

	@Override
	public String toString() {
		return "LatencyPacket [id=" + id + ", opCode=" + opCode + ", sw=" + sw
				+ ", port=" + port + ", timestamp=" + timestamp + ", rtt="
				+ rtt + "]";
	}
	
}