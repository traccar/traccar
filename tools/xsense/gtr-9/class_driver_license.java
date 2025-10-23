package org.siemens;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.client.MainClassGateway;
import org.util.SendUDP;

public class class_driver_license {

	public String IP = gps_gw_siemens.ip_udp;
	public String Port = gps_gw_siemens.port_udp;
	public byte type;
	public int seq;
	public int boxId;
	public String data;
	public String udp_return = "0";
	public Gateway_AllFunctionDecode objDecode = new Gateway_AllFunctionDecode();
	public Gateway_udp2db objUdp = new Gateway_udp2db();
	
	public class_driver_license(byte type, int seq, int boxId, String data)throws Exception
	{
		this.type = type;
		this.seq = seq;
		this.boxId = boxId;
		this.data = data;
		manageDataLicense();
	}
	
	public void manageDataLicense()throws Exception{
		
		String msgFile = "";
		try {
			
				String Ntype = data.substring(0,2);
				String flag_degree = data.substring(2,4);
				String hdop = data.substring(4,6);
				String speed = data.substring(6,8);
				String datetime = data.substring(8,16);
				String lat = data.substring(16,24);
				String lon = data.substring(24,32);
				String license = data.substring(38);
				       license = Gateway_HexConvert.hex2string(license);
				
				
				String flat = objDecode.findLat(Gateway_HexConvert.HextoInteger(lat));
				String flon = objDecode.findLon(Gateway_HexConvert.HextoInteger(lon));
				String ftime_gps = objDecode.findDatetime(Gateway_HexConvert.HextoInteger(datetime),boxId);	// yy/dd/mm hh:mm:ss
				Timestamp time_diff = Timestamp.valueOf(ftime_gps);
				long diff = time_diff.getTime()- 25200000;
				time_diff = new Timestamp(diff);
				ftime_gps = String.valueOf(time_diff);
				Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(ftime_gps);
				String format_datetime = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss").format(d); // 9:00
				
				if(license.startsWith("[#?")){
					
					license = license.replace("[#?","");
					license = license.replace("]","").trim();
					license = "[%^null$null$null^^?;0000000000000000000=000000000000=?_"+license+"?]";
				}
					
				//>>MFB(GDLC)1309994,-1,1,2014-02-16/19:44:50,99.17792,19.86238[%^$$^^?;0000000000000000000=000000000000=?_241000015750200?]
				msgFile = ">>MFB(GDLC)"+boxId
						   +",-1,1,"+format_datetime
						   +","+flon
						   +","+flat
						   +license;
							
				
				System.out.println("msg rfid: "+ msgFile);
				String udp_return[] = SendUDP.sendUDP_ReturnArray(msgFile, MainClassGateway.address_serGDLC, MainClassGateway.port_serGDLC);
				if(udp_return[0].equalsIgnoreCase("Socket timeout 5 second")){
					System.out.println("["+udp_return[1]+"] UDP : "+msgFile);
					System.out.println("["+udp_return[2]+"] ACK : "+udp_return[0]);
					Gateway_CreateLogFile.createLogUdp(msgFile,boxId);
					Gateway_CreateLogFile.createDriverLicense("["+udp_return[1]+"] UDP : "+msgFile,boxId);
					Gateway_CreateLogFile.createDriverLicense("["+udp_return[2]+"] Ack : "+udp_return[0],boxId);
					Gateway_CreateLogFile.createAllAckTimeout("box_code="+boxId+",udp="+msgFile);
				}else{
					System.out.println("["+udp_return[1]+"] UDP : "+msgFile);
					System.out.println("["+udp_return[2]+"] ACK : "+udp_return[0]);
					Gateway_CreateLogFile.createLogUdp(msgFile,boxId);
					Gateway_CreateLogFile.createDriverLicense("["+udp_return[1]+"] UDP : "+msgFile,boxId);
					Gateway_CreateLogFile.createDriverLicense("["+udp_return[2]+"] Ack : "+udp_return[0],boxId);
				}
			
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unused")
	private static String formatDateTime(Date date) {
		SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss",Locale.US);
		if (date == null) {
			return "";
		} else {
			//date.setTime(date.getTime()+25200000);// Bkk +7H =25200000ms 
			return DATE_TIME_FORMAT.format(date);
		}
	 }
	
}
