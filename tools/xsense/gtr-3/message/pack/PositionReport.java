/*
 * Created on 15 ธ.ค. 2547
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message.pack;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.xsense.util.Binary;
import com.xsense.util.DateTime;
import com.xsense.util.HexString;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PositionReport extends BaseStation{
	private byte[] latlong;
	private byte[] speed;
	private byte[] flag_degree;
	private byte[] digital;
	private byte[] enh;
	private byte[] analog;
	private byte[] time32bit;
	
	private String sdatetime =null;
	private String slat=null;
	private String slon =null;
	int datetime;
	NumberFormat nlatlongformatter;// = new DecimalFormat("#.000000");
	NumberFormat ndigitalevent;
	NumberFormat nspeed;
	 SimpleDateFormat DATE_FORMAT;

	public PositionReport(){
		nlatlongformatter = new DecimalFormat("#.000000");
		DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ,java.util.Locale.US);
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
	public void setEnh(byte[] enh){
		this.enh=enh;
	}
	public void setAnalog(byte[] analog){
		this.analog=analog;	
	}
	public void setTime32Bit(byte[] time32bit){
		this.time32bit=time32bit;
		
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
		return nspeed.format(Binary.BitByteToDec(speed[0],8)*1.943);
		
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
	public String getEnh(){
		return Binary.toBitString(enh[0]);
	}
//	-----------------1 byte--------------------------//	
	public String getDateTime(){
		if(sdatetime!=null){
			return sdatetime;
		}else{
				if(getGPSStatus()==1){ //GPS Valid
					
					/*  อันเดิมพี่นวย
					datetime =(int)HexString.HextoInteger(HexString.HextoString(time32bit));
					String sdatetime =(Binary.BitIntToDec(datetime,4,26)+2000)+"-" // 0 -9 
					                   +addIntZeroFill(Binary.BitIntToDec(datetime,4,22))+"-"
					                   +addIntZeroFill(Binary.BitIntToDec(datetime,5,17))+" "
					                   +addIntZeroFill(Binary.BitIntToDec(datetime,5,12))+":"
									   +addIntZeroFill(Binary.BitIntToDec(datetime,6,6))+":"
									   +addIntZeroFill(Binary.BitIntToDec(datetime,6,0));
										// 25200000 = + 7 hour
					sdatetime =ConvertTimeZone(sdatetime,25200000);
					*/
					
					// louis jame2019 แก้
					datetime =(int)HexString.HextoInteger(HexString.HextoString(time32bit));
					int year = (Binary.BitIntToDec(datetime,4,26)); // 0 -9 
					if(year == 9){ year = year + 2010;}	// ปี 2000 ++
					else{year = year + 2020;}	// ปี 2010 ++
					String sdatetime = year+"-" 
	                   +addIntZeroFill(Binary.BitIntToDec(datetime,4,22))+"-"
	                   +addIntZeroFill(Binary.BitIntToDec(datetime,5,17))+" "
	                   +addIntZeroFill(Binary.BitIntToDec(datetime,5,12))+":"
					   +addIntZeroFill(Binary.BitIntToDec(datetime,6,6))+":"
					   +addIntZeroFill(Binary.BitIntToDec(datetime,6,0));
						// 25200000 = + 7 hour
					sdatetime =ConvertTimeZone(sdatetime,25200000);
					//System.out.println("Debug >> year = " + sdatetime);
					// end louis jame2019แก้
					
					return sdatetime;
				}else{ //GPS Invalid
					sdatetime=DateTime.getDateTimeInvalid();
					return sdatetime;
				}
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
		//latlongformatter
		nlatlongformatter.setGroupingUsed(true ); 
//		 set how many places you want to the right of the decimal. 
		nlatlongformatter.setMinimumFractionDigits(6); 
		nlatlongformatter.setMaximumFractionDigits(6); 
//		 set how many places you want to the left of the decimal. 
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
		return nlatlongformatter.format(Double.parseDouble(lon.substring(0, 3)) +(Double.parseDouble(lon.substring(3)) / 60));
	}
	
	private String ConvertTimeZone(String s,int c){
//		String time1 =time.substring(0,2)+":"+time.substring(2,4)+":"+ time.substring(4);
		Date dt;// = new Date();
		try{
			DATE_FORMAT.setLenient(false);
			dt = DATE_FORMAT.parse(s.trim());
			dt.setTime(dt.getTime()+c);// Bkk +7H =25200000ms 
			
		}catch (Exception ex){
			System.out.println("String Date:"+s);
			//ex.printStackTrace();
			return DateTime.Default;
		}     
	 return DATE_FORMAT.format( dt);
		
	}

}
