/*
 * Created on 20 ?.?. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message.pack;

import java.io.ByteArrayInputStream;
import com.xsense.util.*;
import com.xsense.message.MessageObj;

/**
 * @author amnuay
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ExtendPositionPack {
	public MessageObj msgObj;
	ExtendPosition mx[];
	StringBuffer sp =new StringBuffer();
	public ExtendPositionPack(MessageObj msgObj){
		this.msgObj=msgObj;
		pack();
	}
	
	
	public String getSp(){
		
		 return sp.toString();
	}
    public void pack(){
//    	msgObj.getDataMsg();
    	
    	//String boxid =Long.toString(HexString.HextoInteger(HexString.HextoString(msgObj.getTID())));
    
    	int cpack =(msgObj.getDataMsg().length/16);
		
		ByteArrayInputStream bufpack = new ByteArrayInputStream (msgObj.getDataMsg());
	        mx =new ExtendPosition[cpack];
		try{
			for(int i=0;i<cpack;i++){
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
				
				mx[i] =new ExtendPosition();
				mx[i].setLatLong(platlong);
				mx[i].setSpeed(pspeed);
				mx[i].setFlagAndDegree(pflagdegree);
				mx[i].setDigital(pdigital);
				mx[i].setAnalog(analog);
				mx[i].setEvent(event);
				mx[i].setTime32Bit(time32bit);
				
				
				/*System.out.println("Message		:"+i);
				System.out.println("Lat  		:"+mx[i].getLatitude());
				System.out.println("Long  		:"+mx[i].getLongitude());
				System.out.println("Status  	:"+mx[i].getFlag()+":"+mx[i].getGPSStatus());
				System.out.println("Speed  		:"+mx[i].getSpeed());
				System.out.println("Digital8Bit  	:"+mx[i].getDigital8Bit());
				System.out.println("Event  	        :"+mx[i].getEvent());
				System.out.println("Analog  	:"+mx[i].getAnalog());
				System.out.println("N  		 :"+mx[i].getGPSN());
				System.out.println("E  		 :"+mx[i].getGPSE());
				System.out.println("Degree  	:"+mx[i].getFlag()+":"+mx[i].getGPSDegree());
				System.out.println("Engine  	:"+mx[i].getDigital8Bit()+":"+mx[i].getEngineStatus());
				System.out.println("DateTime  	:"+mx[i].getDateTime());
				System.out.println("----------------------------------");
				*/
				
				/*
				if(mx[i].getDateTime()==DateTime.Default){
					//WriteToLogFile.writeOutput(MyConfigFile.getLogDir()+"time_decode_error"+WriteToLogFile.getDate()+".log",HexString.HextoString(msgObj.getFullMsg())+"\n\n");
					WriteToLogFile.writeOutput(MyConfigFile.getLogDir()+"time_decode_error"+WriteToLogFile.getDate()+".log",
							HexString.HextoString(msgObj.getFullMsg())+"\n"+
							"Hex Time		:"+HexString.HextoString(time32bit)+"\n"+
							"Hex Analog		:"+HexString.HextoString(analog)+"\n"+
							"Lat  			:"+mx[i].getLatitude()+"\n"+
							"Long  			:"+mx[i].getLongitude()+"\n"+
							"Status  		:"+mx[i].getFlag()+":"+mx[i].getGPSStatus()+"\n"+
							"Speed  		:"+mx[i].getSpeed()+"\n"+
							"Digital8Bit  	:"+mx[i].getDigital8Bit()+"\n"+
							"Event  	    :"+mx[i].getEvent()+"\n"+
							"Analog  		:"+mx[i].getAnalog()+"\n"+
							"N  		 	:"+mx[i].getGPSN()+"\n"+
							"E  		 	:"+mx[i].getGPSE()+"\n"+
							"Degree  		:"+mx[i].getFlag()+":"+mx[i].getGPSDegree()+"\n"+
							"DateTime  		:"+mx[i].getDateTime()+"\n"+
						"\n\n");
				}*/
			}
			 for(ExtendPosition mxi:getPack()){
				 	sp.append("#");
					sp.append(mxi.getDateTime()+",");
					sp.append(DateTime.getDateTime()+",");
					sp.append(mxi.getLatitude()+",");
					sp.append(mxi.getLongitude()+",");
					sp.append(mxi.getGPSStatus()+",");
					sp.append(mxi.getSpeed()+",");
					sp.append(mxi.getGPSDegree()+",");
					sp.append(0+",");
					sp.append(mxi.getEngineStatus()+",");
					sp.append(mxi.getDigital8Bit()+mxi.getEvent()+",");
					sp.append(mxi.getAnalog()+",");
					sp.append("0"+",");
					sp.append("0"+",");
					sp.append("0"+",");
					sp.append("-");
					}
			
		}catch(Exception ex){}
	
    }
    public ExtendPosition[]  getPack(){
  
		//return this.mx;
    	return getPackSort();
    }
    
    public ExtendPosition[]  getPackSort(){
    	 System.out.println("Bi-dir-Bubble Sort Algorithm");
    	     BidirBubbleSortAlgorithm bisort =new BidirBubbleSortAlgorithm();
    	      try {	
    	      	 bisort.sortExt(mx);
    	      }catch (Exception ex){}
    			return this.mx;
    	    }
   
}
