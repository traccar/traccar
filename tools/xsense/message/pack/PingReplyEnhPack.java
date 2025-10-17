/*
 * Created on 1 Ê.¤. 2548
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
public class PingReplyEnhPack {
	public MessageObj msgObj;
	PingReplyEnh  pingreply;
	public PingReplyEnhPack(MessageObj msgObj){
		this.msgObj=msgObj;
		pack();	
	}
	 public void pack(){
	 	//String boxid =Long.toString(HexString.HextoInteger(HexString.HextoString(msgObj.getTID()))); 
			ByteArrayInputStream bufpack = new ByteArrayInputStream (msgObj.getDataMsg());
			pingreply =new PingReplyEnh();
			try{
				//-----------ADD-------------//
					byte[] platlong =new byte[7];
					byte[] pspeed =new byte[1];
					byte[] pflagdegree =new byte[1];
					byte[] pdigital =new byte[1];
					byte[] analog =new byte[1];
					byte[] enh =new byte[1];
					byte[] time32bit =new byte[4];
					byte[] phonenum =new byte[14];
					byte[] id_sec =new byte[4];
					byte[] stime =new byte[2];
					byte[] reserv =new byte[4];
					byte[] ltcell =new byte[2];
					//byte[] cell_id =new byte[4];
					byte[] lac =new byte[2];
					byte[] ci =new byte[2];
					byte[] ta =new byte[1];
					byte[] tc =new byte[1];
					byte[] ltbs =new byte[2];
					byte[] base_station =new byte[32];
					// ----------Read ---------------//
					bufpack.read(platlong);
					bufpack.read(pspeed);
					bufpack.read(pflagdegree);
					bufpack.read(pdigital);
					bufpack.read(analog);
					bufpack.read(enh);
					bufpack.read(time32bit);
					bufpack.read(phonenum);
					bufpack.read(id_sec);
					bufpack.read(stime);
					bufpack.read(reserv);
					bufpack.read(ltcell);
					bufpack.read(lac);
					bufpack.read(ci);
					bufpack.read(ta);
					bufpack.read(tc);
					bufpack.read(ltbs);
					bufpack.read(base_station);
					//-----------SET-------------//
					pingreply.setLatLong(platlong);
					pingreply.setSpeed(pspeed);
					pingreply.setFlagAndDegree(pflagdegree);
					pingreply.setDigital(pdigital);
					pingreply.setAnalog(analog);
					pingreply.setEnh(enh);
					pingreply.setTime32Bit(time32bit);
					pingreply.setPhoneNumber(phonenum);
					pingreply.setID_SEC(id_sec);
					pingreply.setSTime(stime);
					pingreply.setLTCell(ltcell);
					pingreply.setLAC(lac);
					pingreply.setCI(ci);
					pingreply.setTa(ta);
					pingreply.setTc(tc);
					pingreply.setLTbs(ltbs);
					pingreply.setBase_Station(base_station);
   
			}catch(Exception ex){}
			
	 }
    public PingReplyEnh getPack(){
		return pingreply;
    }

}
