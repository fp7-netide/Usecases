package net.floodlightcontroller.monitor.internal;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * A set of utility methods to create/manipulate byte array for {@link Monitor} 
 * @author K.Phemius<br>
 * DT/CEA/TAI Lab<br>
 * Copyright (c) 2014 Thales Communications & Security<br>
 * 4 av. des Louvresses - 92230 Gennevilliers - France<br>
 * All rights reserved
 *
 **/
public class MonitorUtils {
	/**
	 * Transform a long into a byte array
	 * @param l The long value
	 * @return A byte array
	 */
	public static byte[] longToByteArray(long l) {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putLong(l);
		return buffer.array();
	}
	/**
	 * Transform a byte array (or a subset of the array) to a long
	 * @param b The byte array
	 * @param offset The starting position in the byte array
	 * @return A long from the byte array
	 */
	public static long byteArrayToLong(byte[] b, int offset) {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		byte[] a = new byte[8];
		for (int i = 0; i < 8; i++){
			a[i] = b[offset+i];
		}
		buffer.put(a);
		buffer.flip();//need flip 
		return buffer.getLong();
	}
	/**
	 * Transform a byte array (or a subset of the array) to a short
	 * @param b The byte array
	 * @param offset The starting position in the byte array
	 * @return A short from the byte array
	 */
	public static short byteArrayToShort(byte[] b, int offset){
		ByteBuffer buffer = ByteBuffer.allocate(2);
		byte[] a = new byte[2];
		for (int i = 0; i < 2; i++){
			a[i] = b[offset+i];
		}
		buffer.put(a);
		buffer.flip();//need flip 
		return buffer.getShort();
	}
	/**
	 * Prints a byte array to the console
	 * @param b the array to print
	 */
	public static void printbyte(byte[] b){
		System.out.print("[");
		for(int i=0;i<b.length-1;i++){
			System.out.print(b[i]+":");
		}
		System.out.println(b[b.length-1]+"]");
	}
	/**
	 * Adds a short to the beginning of a byte array
	 * @param p the short to add
	 * @param a the byte array
	 * @return a new byte array starting with the short
	 */
	public static byte[] addShortToArray(short p, byte[] a) {
		byte[] r = new byte[a.length+2];
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.putShort(p);
		buffer.flip();
		r[0]=buffer.get();
		r[1]=buffer.get();
		for(int i=2;i<=a.length+1;i++){
			r[i]=a[i-2];
		}
		return r;
	}
	/**
	 * Builds a the payload of a latency packet
	 * @param id the controller's ID
	 * @param opCode the opCode
	 * @param sw the switch ID
	 * @param p the port
	 * @param ts the timestamp
	 * @return the byte array representing the payload
	 */
	public static byte[] buildLatencyPayload(long id,short opCode, long sw, short p, byte[] ts) {
		return mergeByteArrays(longToByteArray(id), addShortToArray(opCode,mergeByteArrays(longToByteArray(sw),addShortToArray(p, ts))));
	}
	/**
	 * Merges two byte arrays
	 * @param array1
	 * @param array2
	 * @return the new merged array
	 */
	public static byte[] mergeByteArrays(byte[] array1, byte[] array2) {
		byte[] combined = new byte[array1.length + array2.length];
		for (int i = 0; i < combined.length; ++i){
			combined[i] = i < array1.length ? array1[i] : array2[i - array1.length];
		}
		return combined;
	}
	/**
	 * Last version of floodlight changed the way Ethernet packets are deserialized<br>
	 * If the EtherType is unknown, the <b>whole packet</b> is set as the payload.<br>
	 * This function get the <i>real</i> payload from an Ethernet packet.
	 * @param eth The Ethernet packet, complete with the header.
	 * @return The packet's payload
	 */
	public static byte[] getEthPayload(byte[] eth) {
		byte[] payload = new byte[eth.length - 14]; // 14 = ethSrc(6) + ethDst(6) + ethType(2)
		for (int i = 0; i < payload.length; ++i){
			payload[i] = eth[i+14];
		}
		return payload;
	}
	/**
	 * Round a double to <b>p</b> decimal places
	 * @param d the double to round
	 * @param p the number of decimal places
	 * @return the double with <b>p</b> decimal places
	 */
	public static double round(double d, int p) {
		String format = "0.";
		for(int i=0;i<p;i++)
			format+="0";
		DecimalFormat twoDForm = new DecimalFormat(format, new DecimalFormatSymbols(Locale.US));
		return Double.valueOf(twoDForm.format(d));
	}
}
