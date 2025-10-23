package org.siemens;

public class class_update_time_result {

	public byte type;
	public int seq;
	public int boxId;
	public String data;
	
	public class_update_time_result(byte type, int seq, int boxId, String data)throws Exception
	{
		this.type = type;
		this.seq = seq;
		this.boxId = boxId;
		this.data = data;
	}
	
}
