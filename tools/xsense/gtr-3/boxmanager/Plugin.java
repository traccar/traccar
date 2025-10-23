package com.xsense.boxmanager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import com.xsense.util.MyConfigFile;
import com.xsense.util.WriteToLogFile;

public class Plugin {
	private static DatagramPacket dp ;
	private static DatagramSocket ds;
	/*public static void sendToDB(String pak){
		for(int i=0;i<BoxManagerType.H_DB.size();i++){
			try{
				HostProtoAddress hpdb =(HostProtoAddress)BoxManagerType.H_DB.elementAt(i);
				sender(pak.getBytes(),pak.length(),InetAddress.getByName(hpdb.getHostname()),Integer.parseInt( hpdb.getPort()));
			}catch (Exception e) {
				System.out.println("Send To DB Error");
				// TODO: handle exception
			}
			
		}
	}*/
	public static void sendToGPSManage(String pak){
		try{
			//sender(pak,InetAddress.getByName(MyConfigFile.getGps_addr()), MyConfigFile.getGps_port());
			//Edit Round Robin
			boolean serv_status = true;
			while(serv_status){
//				String inetaddress = AddressRoundRobin.getAddress(); 
//				String ip_port[] = inetaddress.split(":");
				InetAddress address = InetAddress.getByName("gpsmanage.xsense.co.th");
				int port = 40069;
//				if(sender(pak,InetAddress.getByName(ip_port[0]), Integer.parseInt(ip_port[1]))){
				if(sender(pak,address, port)){
					serv_status = false;
					AddressRoundRobin.setCountDown();
				}
				else{
					serv_status = true;
//					System.out.println("Server timeout:"+ip_port[0]+","+ip_port[1]+" count:"+AddressRoundRobin.count_updown);
					System.out.println("Server timeout:"+address+","+port+" count:"+AddressRoundRobin.count_updown);
					AddressRoundRobin.setCountUp();
					if(AddressRoundRobin.count_updown>10000)
						System.exit(1);
					//System.out.println("Server down data:"+pak);
					}
			}
			
			
		}catch (Exception e) {
			System.out.println("Error:Send To GPS Manager");
			// TODO: handle exception
		}
		
	}
	public static void SaveLog(String box_code,String pak){
		
		try{
			WriteToLogFile.writeOutput2(MyConfigFile.getData_log()+box_code+"-"+WriteToLogFile.getDate(), pak);
		}catch (Exception e) {
			System.out.println("Error:Save To Log");
			// TODO: handle exception
		}
	}
	
	/*public static void sendToFLEET(String pak){
		for(int i=0;i<BoxManagerType.H_FLEET.size();i++){
			try{
				HostProtoAddress hpdb =(HostProtoAddress)BoxManagerType.H_FLEET.elementAt(i);
				sender(pak.getBytes(),pak.length(),InetAddress.getByName(hpdb.getHostname()),Integer.parseInt( hpdb.getPort()));
			}catch (Exception e) {
				System.out.println("Send To DB Error");
				// TODO: handle exception
			}
			
		}
	}*/
	public static  boolean sender(String packet,InetAddress host,int port){
 		try{
 			
 			int leng =packet.length();
 			byte[] pak =packet.getBytes();
 			//System.out.println("Leng :"+leng);
 			//System.out.println("Msg :"+packet);
 			//System.out.println("Send:"+host.getHostAddress()+","+port);
 			dp = new DatagramPacket(pak, leng,host,port);
 			ds = new DatagramSocket();
 			ds.setSoTimeout(5000);
 			//ds.setReuseAddress(true);
 			ds.send(dp);
 			System.out.println("Data:"+packet);
 			byte[] buf = new byte[1];
            dp = new DatagramPacket(buf, buf.length);
 			ds.receive(dp);
            String received = new String(dp.getData());
            System.out.println("Send:"+host.getHostAddress()+","+port+" Receive:"+received);
 			ds.close();
 			if(received.trim().equals("0"))
 				return true;
 			else return false;
 		}catch (SocketTimeoutException e) {
			// TODO: handle excepion
 			WriteToLogFile.writeOutput2(MyConfigFile.getGps_buffer(),packet);
 			return false;
 		} catch(IOException e) {
 			e.printStackTrace();
 			return false;
 		}
 		
      }
}
