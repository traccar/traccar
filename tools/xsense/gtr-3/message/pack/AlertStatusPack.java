/*
 * Created on 20 ¡.¾. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message.pack;

import java.io.ByteArrayInputStream;

import com.xsense.message.MessageObj;
/**
 * @author amnuay
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AlertStatusPack {
	public MessageObj msgObj;
	//StringBuffer mailbuf;
	AlertStatus  alert;
	public AlertStatusPack(MessageObj msgObj){
		this.msgObj=msgObj;
		//mailbuf=new StringBuffer();
		pack();
			
	}
	
	 public void pack(){
			ByteArrayInputStream bufpack = new ByteArrayInputStream (msgObj.getDataMsg());
			alert =new AlertStatus();
			try{
				//for(int i=0;i<cpack;i++){
					byte[] platlong =new byte[7];
					byte[] pspeed =new byte[1];
					byte[] pflagdegree =new byte[1];
					byte[] pdigital =new byte[1];
					byte[] analog =new byte[1];
					byte[] event =new byte[1];
					byte[] time32bit =new byte[4];
					bufpack.read(platlong);
					bufpack.read(pspeed);
					bufpack.read(pflagdegree);
					bufpack.read(pdigital);
					bufpack.read(analog);
					bufpack.read(event);
					bufpack.read(time32bit);
					
					
					alert.setLatLong(platlong);
					alert.setSpeed(pspeed);
					alert.setFlagAndDegree(pflagdegree);
					alert.setDigital(pdigital);
					alert.setAnalog(analog);
					alert.setEvent(event);
					alert.setTime32Bit(time32bit);
					

			}catch(Exception ex){}
			
	 }
    public AlertStatus getPack(){
    	//msgObj.getDataMsg();
    	
    	
		return alert;
     
   
    }

}
