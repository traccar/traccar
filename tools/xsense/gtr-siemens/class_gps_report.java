package org.siemens;

public class class_gps_report {
	
	public byte type;
	public int seq;
	public int boxId;
	public String data;
	
	public class_gps_report(byte type, int seq, int boxId, String data)throws Exception
	{
		this.type = type;
		this.seq = seq;
		this.boxId = boxId;
		this.data = data;
	}
	
}
