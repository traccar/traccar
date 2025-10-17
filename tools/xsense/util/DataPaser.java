/*
 * Created on 8 ¡.Â. 2547
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

import java.util.StringTokenizer;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
public class DataPaser {
	String boxid="";
	String date="";
	String time="";
	String status="";
	String lat="";
	String lon="";
	String engine="";
	String speed="";
	String variation="";
	String log="";
	String datetime="";
	String rmc="";
	StringTokenizer st;
	 java.util.Locale locale = java.util.Locale.US;
	//private  NumberFormat DATE_FORMATTER = new DecimalFormat("##'-'##'-'##");
	private   SimpleDateFormat df = new SimpleDateFormat( "yy-MM-dd HH:mm:ss" ,locale );
	private   SimpleDateFormat dfdb = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ,locale );
	private   SimpleDateFormat datef = new SimpleDateFormat( "yyyy-MM-dd" ,locale );
	private   SimpleDateFormat timef = new SimpleDateFormat( "HH:mm:ss" ,locale );

	private static final char PROTOCAL_HEADER='I';
	private static final String S_PASER =",";
	public String getBoxID(){
	   return boxid;
	}
	public String getDate(){
		return date;
	}
	public String getTime(){
		return time;
	}
	public String getStatus(){
	  return status;
	}
	public String getLat(){
	 return lat;
	}
	public String getLon(){
	 return lon;
	}
	public String getEngine(){
	 return engine;
	}
	public String getSpeed(){
		return speed;
	}
	public String getVariation(){
	 return variation;
	}
	public String getRMC(){
		return rmc;
	}
	public String getLog(){
		return log;
	}
	public String getDateTime(){
		return datetime;
	}
	public DataPaser(String data){
	  paser(data);
	}
	
	public void paser(String data){
		/*1024,090904,040317,1,13543391,100309249,1,00047,09055,,*/ 
		/*boxid,date,time,status,lat,lon,engine,speed,variation,, */ 
		try{
		    if(data.length()>0){// check size of data not null
				char c = data.charAt(0);// check Header
				if(c==PROTOCAL_HEADER){
					data=data.substring(1);// I DeCode
					st =new StringTokenizer(data,S_PASER);//Paser
					System.out.println( "Count of Tokens >:"+st.countTokens());
					boxid=st.nextToken();
					date=datePaser(st.nextToken());
					time=timePaser(st.nextToken());
					status=st.nextToken();
					lat=latPaser(st.nextToken());
					lon=lonPaser(st.nextToken());
					engine=st.nextToken();
					speed=speedPaser(st.nextToken());
					variation=variationPaser(st.nextToken());
					datetime=dateTimePaser(date,time);
					while(st.hasMoreTokens()){
						log=log+st.nextToken();
					}
					
					//rmc=rmcPaser(data);
				}
			}
		}catch (Exception ex){}
	}
	
	
	
	public String  datePaser(String date/* ddMMyy to yy-mm-dd*/){
		return date.substring(4)+"-"+date.substring(2,4)+"-"+ date.substring(0,2);
	}
	public String  timePaser(String time/* ddMMyy to yy-mm-dd*/){
			return time.substring(0,2)+":"+time.substring(2,4)+":"+ time.substring(4);
		}		
	public String dateTimePaser(String date,String time/*-*/){
		//String date1 =date.substring(4)+"-"+date.substring(2,4)+"-"+ date.substring(0,2);
		//String time1 =time.substring(0,2)+":"+time.substring(2,4)+":"+ time.substring(4);
		Date dt = new Date();
		try{
		
		dt = df.parse(date +" "+ time);
		//dt.getTime()
		//System.out.println(dt.getTime()+25200000);
		dt.setTime(dt.getTime()+25200000);// Bkk +7H =25200000ms 
		
		//System.out.println(dt.toString());
		//dt=df.parse(dt.toString());
		//df.setTimeZone( java.util.TimeZone.getDefault() ); 
		//dt.UTC(2004,5,10,23,50,11);
		}catch (Exception ex){
		 return "0";
		}     
		//System.out.println( "parse = " + dfdb.format( dt ) );       
		//String time =
		this.date =datef.format(dt);
		this.time= timef.format(dt);
		//System.out.println( date);
	 return dfdb.format( dt);
	}
    public String latPaser(String lat/* */){
		//System.out.println(lat);
		lat = lat.substring(0, 4) + "." + lat.substring(4);		
      return Double.toString(Double.parseDouble(lat.substring(0, 2)) +(Double.parseDouble(lat.substring(2)) / 60));
    }	
	public String lonPaser(String lon){
		//System.out.println(lon);
		lon = lon.substring(0, 5) + "." + lon.substring(5);
	  return Double.toString(Double.parseDouble(lon.substring(0, 3)) +(Double.parseDouble(lon.substring(3)) / 60));
	}
	public String speedPaser(String speed){
		NumberFormat sformatter = new DecimalFormat("000.00");
		return sformatter.format( (Integer.parseInt(speed) / 100) *1.943);
		//return Integer.toString(Integer.parseInt(speed)/100);
	}
	public String variationPaser(String variation){
		return Integer.toString(Integer.parseInt(variation)/100);
	}
	public String rmcPaser(String rmc){
		StringTokenizer st=new StringTokenizer(rmc,S_PASER) ;
		st.nextToken();//BoxID
		String date=st.nextToken();
		String time = st.nextToken();
		String	status = (st.nextToken().trim().equals("1"))? "A":"V";
		String	lat = st.nextToken();
		String	lon = st.nextToken();
			lat = lat.substring(0, 4) + "." + lat.substring(4);
			lon = lon.substring(0, 5) + "." + lon.substring(5);
		String	engine = st.nextToken();
		String	speed = st.nextToken();
		String	variation= Integer.toString( Integer.parseInt(st.nextToken())/100);
		//$rmc= "GPRMC,".$rmctime.",".$rmcstatus.",".$rmclat.",N,".$rmclong.",E,".$rmcspeed.",".$rmcdirection.",".$rmcdate.",".$rmcdirection.",W*3A";
		return "GPRMC,"+time+","+status+","+lat+",N,"+lon+",E,"+speed+","+variation+","+date+",,*3A";
	}
	
}
