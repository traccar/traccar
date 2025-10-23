/*
 * Created on 22 มี.ค. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message.pack;

import java.io.ByteArrayInputStream;

import com.xsense.message.MessageObj;


/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PingReplyPack {

	public MessageObj msgObj;
	//StringBuffer mailbuf;
	PingReply  pingreply;
	StringBuffer message_buff;
	public PingReplyPack(MessageObj msgObj){
		this.msgObj=msgObj;
		message_buff=new StringBuffer();
		pack();	
	}
	 public void pack(){
			ByteArrayInputStream bufpack = new ByteArrayInputStream (msgObj.getDataMsg());
			pingreply =new PingReply();
			try{
					byte[] platlong =new byte[7];
					byte[] pspeed =new byte[1];
					byte[] pflagdegree =new byte[1];
					byte[] pdigital =new byte[1];
					byte[] analog =new byte[1];
					byte[] event =new byte[1];
					byte[] time32bit =new byte[4];
					//-----------ADD-------------//
					byte[] phonenum =new byte[9];
					byte[] id_sec =new byte[4];
					byte[] time_base_station =new byte[2];
					byte[] base_station =new byte[24];
					
					bufpack.read(platlong);
					bufpack.read(pspeed);
					bufpack.read(pflagdegree);
					bufpack.read(pdigital);
					bufpack.read(analog);
					bufpack.read(event);
					bufpack.read(time32bit);
					bufpack.read(phonenum);
					bufpack.read(id_sec);
					bufpack.read(time_base_station);
					bufpack.read(base_station);
					
					pingreply.setLatLong(platlong);
					pingreply.setSpeed(pspeed);
					pingreply.setFlagAndDegree(pflagdegree);
					pingreply.setDigital(pdigital);
					pingreply.setAnalog(analog);
					pingreply.setEvent(event);
					pingreply.setTime32Bit(time32bit);
					pingreply.setPhoneNumber(phonenum);
					pingreply.setID_SEC(id_sec);
					pingreply.setTimeBaseStation(time_base_station);
					pingreply.setBaseStation(base_station);

			}catch(Exception ex){}	
	 }
    public PingReply getPack(){
		return pingreply;
     
   
    }
}
