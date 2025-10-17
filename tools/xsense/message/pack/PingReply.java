/*
 * Created on 15 ¸.¤. 2547
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
public class PingReply extends ExtendPosition{
	private byte[] phonenumber;
	private byte[] id_sec;
	private byte[] time_base_station;
	private byte[] base_station;
	
	public void setPhoneNumber(byte[] phonenumber){
		this.phonenumber=phonenumber;
	}
	public void setID_SEC(byte[] id_sec){
		this.id_sec=id_sec;
	}
	public void setTimeBaseStation(byte[] time_base_station){
		this.time_base_station=time_base_station;
	}
	public void setBaseStation(byte[] base_station){
		this.base_station=base_station;
	}
	
    public String getPhoneNember(){
    	return new String(phonenumber);
    }	

    public long getID_SEC(){
    	 return HexString.HextoInteger(HexString.HextoString(id_sec));
    }
    public long getTimeBaseStation(){
   	 return HexString.HextoInteger(HexString.HextoString(time_base_station));
   }
    public String getBaseStation(){
    	return new String(base_station);
    	
   }
}
