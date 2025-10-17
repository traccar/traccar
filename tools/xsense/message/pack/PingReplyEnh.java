/*
 * Created on 1 Ê.¤. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message.pack;

import com.xsense.util.HexString;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PingReplyEnh extends PositionReport {
	private byte[] phonenumber;
	private byte[] id_sec;
	private byte[] stime;
	
	public void setPhoneNumber(byte[] phonenumber){
		this.phonenumber=phonenumber;
	}
	public void setID_SEC(byte[] id_sec){
		this.id_sec=id_sec;
	}
	public void setSTime(byte[] stime){
		this.stime=stime;
	}
    public String getPhoneNember(){
    	return new String(phonenumber);
    }	
    public long getID_SEC(){
    	 return HexString.HextoInteger(HexString.HextoString(id_sec));
    }
    public long getSTime(){
   	 return HexString.HextoInteger(HexString.HextoString(stime));
   }
}
