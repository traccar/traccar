/*
 * Created on 11 µ.¤. 2547
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.xsense.util;

/**
 * @author amnuay
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
import java.util.Date;

import java.text.SimpleDateFormat;

public class DateTime {
	//private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	private  static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ,java.util.Locale.US);
	public static String Default ="1970-01-01 00:00:01";
	//private   SimpleDateFormat dfdb = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ,locale );
	public static String SecToTime(long sec){	
		long unixTime = (long)((sec) * 1000.0); // convert sec to millisec
		if(unixTime ==0 ||unixTime > System.currentTimeMillis()+25200000){
			return Default;
	   }else
		return DATE_FORMAT.format(new java.util.Date(unixTime));
	    
	}
	/*public static String getDateTime(){
		return Default;
	}*/
	
	public static String getDateTime(){
		return  DATE_FORMAT.format(new Date());
	}

	public static String getDateTimeInvalid(){
		return Default;
	}
	
	public static String ConvertTimeZone(String s,int c){
//		String time1 =time.substring(0,2)+":"+time.substring(2,4)+":"+ time.substring(4);
		Date dt;// = new Date();
		try{
			DATE_FORMAT.setLenient(false);
			dt = DATE_FORMAT.parse(s.trim());
			dt.setTime(dt.getTime()+c);// Bkk +7H =25200000ms 
			
		}catch (Exception ex){
			System.out.println("String Date:"+s);
			ex.printStackTrace();
			return Default;
		}     
	 return DATE_FORMAT.format( dt);
		
	}
	
	public static long ConvertTimeZoneLong(String s){
//		String time1 =time.substring(0,2)+":"+time.substring(2,4)+":"+ time.substring(4);
		Date dt = new Date();
		try{
			dt = DATE_FORMAT.parse(s);
			//dt.parse(DATE_FORMAT.parse(s));
		}catch (Exception ex){
			return 0;
		}     
	 return dt.getTime();
		
	}
	
	
}
