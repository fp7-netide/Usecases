package net.floodlightcontroller.monitor.web.structures;

/**
 * Structure to store the processed data.<br>
 * Will be serialized into JSON.
 * 
 * @author K.Phemius<br>
 *         DT/CEA/TAI Lab<br>
 *         Copyright (c) 2014 Thales Communications & Security<br>
 *         4 av. des Louvresses - 92230 Gennevilliers - France<br>
 *         All rights reserved
 * 
 **/
public class DataContainer {
	private long sw;
	private short port;
	private double delay;
	private double loss;
	private long swR;
	private short portR;
	
	public DataContainer() {
		this.sw = 0;
		this.port = 0;
		this.delay = 0;
		this.loss = 0;
		this.swR = 0;
		this.portR = 0;
	}
	/**
	 * @return the Switch ID
	 */
	public long getSw() {
		return sw;
	}
	/**
	 * @param sw the sw to set
	 */
	public void setSw(long sw) {
		this.sw = sw;
	}
	/**
	 * @return the port number
	 */
	public short getPort() {
		return port;
	}
	/**
	 * @param port the port to set
	 */
	public void setPort(short port) {
		this.port = port;
	}
	/**
	 * @return the delay
	 */
	public double getDelay() {
		return delay;
	}
	/**
	 * @param delay the delay to set
	 */
	public void setDelay(double delay) {
		this.delay = delay;
	}
	/**
	 * @return the loss
	 */
	public double getLoss() {
		return loss;
	}
	/**
	 * @param loss the loss to set
	 */
	public void setLoss(double loss) {
		this.loss = loss;
	}
	/**
	 * @return the remote switch
	 */
	public long getSwR() {
		return swR;
	}
	/**
	 * @param swR the swR to set
	 */
	public void setSwR(long swR) {
		this.swR = swR;
	}
	/**
	 * @return the remote port
	 */
	public short getPortR() {
		return portR;
	}
	/**
	 * @param portR the portR to set
	 */
	public void setPortR(short portR) {
		this.portR = portR;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PortDataContainer [sw=" + sw + ", port=" + port + ", delay="
				+ delay + ", loss=" + loss + ", swR=" + swR + ", portR="
				+ portR + "]";
	}
	
}
