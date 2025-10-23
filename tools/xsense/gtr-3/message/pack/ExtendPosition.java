/*
 * Created on 14 ¸.¤. 2547
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message.pack;


import com.xsense.util.Binary;
import com.xsense.util.DateTime;
import com.xsense.util.HexString;
import java.text.*;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ExtendPosition {
	private byte[] latlong;
	private byte[] speed;
	private byte[] flag_degree;
	private byte[] digital;
	private byte[] event;
	private byte[] analog;
	//private byte[] time32bit;
	
	private String sdatetime =null;
	private String slat=null;
	private String slon =null;
	int datetime;
	NumberFormat nlatlongformatter;// = new DecimalFormat("#.000000");
	//NumberFormat ndigitalevent;
	NumberFormat nspeed;

	public ExtendPosition(){
		nlatlongformatter = new DecimalFormat("#.000000");
		nspeed =new DecimalFormat("000.00");
		nspeed.setMaximumFractionDigits(2); 
		nspeed.setMaximumIntegerDigits(3); 
	}
	
	
	public void setLatLong(byte[] latlong){
		this.latlong=latlong;
	}
	public void setSpeed(byte[] speed){
		this.speed=speed;
	}
	public void setFlagAndDegree(byte[]	flag_degree){
		this.flag_degree=flag_degree;
	}
	public void setDigital(byte[] digital){
		this.digital=digital;
	}
	public void setEvent(byte[] event){
		this.event=event;
	}
	public void setAnalog(byte[] analog){
		this.analog=analog;	
	}
	public void setTime32Bit(byte[] time32bit){
		//this.time32bit=time32bit;
		datetime =(int)HexString.HextoInteger(HexString.HextoString(time32bit));
		
	}
	public String getHexStringLatLong(){
		return HexString.HextoString(latlong);
	}
	//------------------7 byte----------------------------//
	public String getLatitude(){
		
		try{
			if(slat!=null) return slat;
			else {
				slat= latPaser(Long.toString(HexString.HextoInteger(getHexStringLatLong().substring(0,7))));
				return slat;
			}
		}catch(Exception ex){
			return "0.00";
		}
	}
	public String getLongitude(){
		
		try{
			if(slon!=null) return slon;
			else{
				slon=lonPaser(Long.toString(HexString.HextoInteger(getHexStringLatLong().substring(7,14))));
				return slon;
			}
		}catch(Exception ex){
			return "0.00";
		}
	}
	//-----------------1 byte--------------------------//
	public String  getSpeed(){
//		 set how many places you want to the right of the decimal. 
		return nspeed.format(Binary.BitByteToDec(speed[0],8)*1.943); //Convert mile to kilo
		
	}
	public int getGPSE(){
		return Binary.BitByteSelect(flag_degree[0],8);
	}
	public int getGPSN(){
		return Binary.BitByteSelect(flag_degree[0],7);
	}
	public int getGPSStatus(){
		return Binary.BitByteSelect(flag_degree[0],6);
	}
	public int getGPSDegree(){
		return Binary.BitByteToDec(flag_degree[0],5);
	}
	public String getFlag(){
		return Binary.toBitString(flag_degree[0]);
	}
	public int getGPSFlagDegree(){
		 return flag_degree[0] ^ (byte)0xFF;
		}
	
//-----------------1 byte--------------------------//
	public String getDigital8Bit(){
		return Binary.toBitString(digital[0]);
	}
	
	public int getEngineStatus(){
		return Binary.BitByteSelect(digital[0],1);
	}
	
//-----------------1 byte--------------------------//	
	public int getAnalog(){	
		return (Binary.BitByteToDec(analog[0],8)<<2)+Binary.BitIntToDec(datetime,2,30);
	}
//	-----------------1 byte--------------------------//
	public String getEvent(){
		return Binary.toBitString(event[0]);
	}
//	-----------------1 byte--------------------------//	
	public String getDateTime(){
		if(sdatetime!=null){
			return sdatetime;
		}else{
					String tsdatetime =(Binary.BitIntToDec(datetime,4,26)+2000)+"-" // 0 -9 
					                   +addIntZeroFill(Binary.BitIntToDec(datetime,4,22))+"-"
					                   +addIntZeroFill(Binary.BitIntToDec(datetime,5,17))+" "
					                   +addIntZeroFill(Binary.BitIntToDec(datetime,5,12))+":"
									   +addIntZeroFill(Binary.BitIntToDec(datetime,6,6))+":"
									   +addIntZeroFill(Binary.BitIntToDec(datetime,6,0));
										// 25200000 = + 7 hour
					sdatetime =DateTime.ConvertTimeZone(tsdatetime,25200000);
					return sdatetime;
		}
		
	}
	
	public int getDateTimeInt(){
		return Binary.BitIntToDec(datetime,30,0);
	}
	
	public String addIntZeroFill(int x){
		if(x<10) return "0"+Integer.toString(x);
		else return Integer.toString(x);
	}
	
	 public String latPaser(String lat/* */){
		//System.out.println(lat);
	 	if(lat.length()<8) lat="0"+lat;
		lat = lat.substring(0, 4) + "." + lat.substring(4);		
		nlatlongformatter.setGroupingUsed(true ); 
//		 set how many places you want to the right of the decimal. 
		nlatlongformatter.setMinimumFractionDigits(6); 
		nlatlongformatter.setMaximumFractionDigits(6); 
//		set how many places you want to the left of the decimal. 
		
		nlatlongformatter.setMinimumIntegerDigits(1); 
		nlatlongformatter.setMaximumIntegerDigits(2 ); 

      return nlatlongformatter.format(Double.parseDouble(lat.substring(0, 2)) +(Double.parseDouble(lat.substring(2)) / 60));
    }	
	public String lonPaser(String lon){
		//System.out.println(lon);
		if(lon.length()<9) lon="0"+lon;
		lon = lon.substring(0, 5) + "." + lon.substring(5);
		nlatlongformatter.setGroupingUsed(true ); 
//		 set how many places you want to the right of the decimal. 
		nlatlongformatter.setMinimumFractionDigits(6); 
		nlatlongformatter.setMaximumFractionDigits(6); 
//		 set how many places you want to the left of the decimal. 
		nlatlongformatter.setMinimumIntegerDigits(1); 
		nlatlongformatter.setMaximumIntegerDigits(3 ); 
	 // return Double.toString(Double.parseDouble(lon.substring(0, 3)) +(Double.parseDouble(lon.substring(3)) / 60));
		return nlatlongformatter.format(Double.parseDouble(lon.substring(0, 3)) +(Double.parseDouble(lon.substring(3)) / 60));
	}
	
}
