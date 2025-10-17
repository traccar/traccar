/*
 * Created on 1 ?.?. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message.pack;

import java.io.ByteArrayInputStream;

import com.xsense.message.MessageObj;
import com.xsense.util.BidirBubbleSortAlgorithm;
import com.xsense.util.DateTime;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PositionReportPack {
	public MessageObj msgObj;
	//StringBuffer mailbuf;
	PositionReport mx[];
	BaseStation bs;
	StringBuffer sp =new StringBuffer();
	StringBuffer spcell =new StringBuffer();
	boolean isOnline =true;
	public PositionReportPack(MessageObj msgObj,boolean isOnline){
		this.msgObj=msgObj;
		this.isOnline =isOnline;
		pack();
			
	}
    public void pack(){
//    	msgObj.getDataMsg();
    	
    	//String boxid =Long.toString(HexString.HextoInteger(HexString.HextoString(msgObj.getTID())));
    
    	int cpack =10;
		
		ByteArrayInputStream bufpack = new ByteArrayInputStream (msgObj.getDataMsg());
	        mx =new PositionReport[cpack];
		try{
			for(int i=0;i<cpack;i++){
				byte[] platlong =new byte[7];
				byte[] pspeed =new byte[1];
				byte[] pflagdegree =new byte[1];
				byte[] pdigital =new byte[1];
				byte[] analog =new byte[1];
				byte[] enh =new byte[1];
				byte[] time32bit =new byte[4];
				bufpack.read(platlong);
				bufpack.read(pspeed);
				bufpack.read(pflagdegree);
				bufpack.read(pdigital);
				bufpack.read(analog);
				bufpack.read(enh);
				bufpack.read(time32bit);
				
				mx[i] =new PositionReport();
				mx[i].setLatLong(platlong);
				mx[i].setSpeed(pspeed);
				mx[i].setFlagAndDegree(pflagdegree);
				mx[i].setDigital(pdigital);
				mx[i].setAnalog(analog);
				mx[i].setEnh(enh);
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
				/*if(mx[i].getDateTime()==DateTime.Default){
					WriteToLogFile.writeOutput(MyConfigFile.getLogDir()+"time_decode_error"+WriteToLogFile.getDate()+".log",
							HexString.HextoString(msgObj.getFullMsg())+"\n"+
							"Hex Time		:"+HexString.HextoString(time32bit)+"\n"+
							"Hex Analog		:"+HexString.HextoString(analog)+"\n"+
							"Lat  		:"+mx[i].getLatitude()+"\n"+
							"Long  		:"+mx[i].getLongitude()+"\n"+
							"Status  	:"+mx[i].getFlag()+":"+mx[i].getGPSStatus()+"\n"+
							"Speed  		:"+mx[i].getSpeed()+"\n"+
							"Digital8Bit  	:"+mx[i].getDigital8Bit()+"\n"+
							"Event  	        :"+mx[i].getEnh()+"\n"+
							"Analog  	:"+mx[i].getAnalog()+"\n"+
							"N  		 :"+mx[i].getGPSN()+"\n"+
							"E  		 :"+mx[i].getGPSE()+"\n"+
							"Degree  	:"+mx[i].getFlag()+":"+mx[i].getGPSDegree()+"\n"+
							"DateTime  	:"+mx[i].getDateTime()+"\n"+
						"\n\n");
				}*/
			}
			for(PositionReport mx:getPack()){
		 		        sp.append("#");
						sp.append(mx.getDateTime()+",");
						sp.append(DateTime.getDateTime()+",");
						sp.append(mx.getLatitude()+",");
						sp.append(mx.getLongitude()+",");
						sp.append(mx.getGPSStatus()+",");
						sp.append(mx.getSpeed()+",");
						sp.append(mx.getGPSDegree()+",");
						sp.append(0+",");
						sp.append(mx.getEngineStatus()+",");
						sp.append(mx.getDigital8Bit()+mx.getEnh()+",");
						sp.append(mx.getAnalog()+",");
						sp.append("0"+",");
						sp.append("0"+",");
						sp.append("0"+",");
						sp.append("@,");
						sp.append("tc");
						
						
		 	 }
			//---------------Base Station--------------------//
			if(isOnline){
				byte[] ltcell =new byte[2];
				//byte[] cell_id =new byte[4];
				byte[] lac =new byte[2];
				byte[] ci =new byte[2];
				byte[] ta =new byte[1];
				byte[] tc =new byte[1];
				byte[] ltbs =new byte[2];
				byte[] base_station =new byte[32];
			
				bufpack.read(ltcell);
				bufpack.read(lac);
				bufpack.read(ci);
				bufpack.read(ta);
				bufpack.read(tc);
				bufpack.read(ltbs);
				bufpack.read(base_station);
				bs =new BaseStation();
				bs.setLTCell(ltcell);
				bs.setLAC(lac);
				bs.setCI(ci);
				bs.setTa(ta);
				bs.setTc(tc);
				bs.setLTbs(ltbs);
				bs.setBase_Station(base_station);
				
				spcell.append("#");
				spcell.append(bs.getLTCell()+",");
				spcell.append(bs.getLAC()+",");
				spcell.append(bs.getCI()+",");
				spcell.append(bs.getTa()+",");
				spcell.append(bs.getTc()+",");
				spcell.append(bs.getLTbs()+",");
				spcell.append(bs.getBase_Station());
			}
		}catch(Exception ex){}
	
    }
    public PositionReport[]  getPack(){
    	return getPackSort();
    }
    public BaseStation  getBaseStation(){
    	return bs;
    }
    public String getSp(){
		 return sp.toString();
	}
    public String getBS(){
    	
    	if(bs !=null){
        	String doubleQuote = "\"";
    		String singleQuote = "\'";
    		String bss = bs.getBase_Station().replace(doubleQuote, "");
    		bss =bss.replace(singleQuote, "");
        	return bss;
        	}else return "@";
        	
    } 
    public String getSPCell(){
    	return spcell.toString();
    	
    }
    public PositionReport[]  getPackSort(){
    	 System.out.println("Bi-dir-Bubble Sort Algorithm");
    	     BidirBubbleSortAlgorithm bisort =new BidirBubbleSortAlgorithm();
    	      try {	
    	      	 bisort.sortExt(mx);
    	      }catch (Exception ex){}
    			return this.mx;
    	    }
   
}
