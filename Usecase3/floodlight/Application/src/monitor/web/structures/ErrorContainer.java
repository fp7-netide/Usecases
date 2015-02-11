package net.floodlightcontroller.monitor.web.structures;

/**
 * Structure to store an error.<br>
 * Will be serialized into JSON.
 * 
 * @author K.Phemius<br>
 *         DT/CEA/TAI Lab<br>
 *         Copyright (c) 2014 Thales Communications & Security<br>
 *         4 av. des Louvresses - 92230 Gennevilliers - France<br>
 *         All rights reserved
 * 
 **/
public class ErrorContainer{
	/*
	 * Error Codes
	 * 
	 * Fatal Errors :
	 * 		1 : Request Error
	 * 
	 * 20 :
	 * 		21 : Switch not found
	 * 
	 * 30 :
	 * 		31 : Port not found
	 * 
	 * 40 :
	 * 		41 : End point not found
	 * 
	 * 50 :
	 * 		51 : Invalid parameters
	 * 		52 : Source not found
	 * 		53 : Destination not found
	 * 		54 : Route not found
	 */
	
	private int code;
	private String value;
	private String reason;

	public ErrorContainer(int i, String s, String r) {
		this.code = i;
		this.value = s;
		this.reason = r;
	}

	/**
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(int code) {
		this.code = code;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return the reason
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * @param reason the reason to set
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ErrorContainer [code=" + code + ", value=" + value
				+ ", reason=" + reason + "]";
	}

}
