package org.siemens;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



public class Gateway_udp2db {
	
	public String SortDataMessage (String type,int box_code,String time_gps
			,String time_server,String latitude,String longitude,String gps_status
			,String speed,String direction,String altitude,String engine_status
			,String digital,String analog1,String analog2,String analog3,String analog4
			,String base_name,String nsat,String sys_temp,String vin,String hdop
			,String pdop,String lac,String ci,String rssi,String ber
			,String gsm_temp,String gsm_volt,String length,String sensor_nectec) throws Exception
	{
		String msg4log = "";
		String owner = "2";
		
		try
		{
			// owner id for siemens module ==> 2
			// type online ==> 0
			// type offline ==> 1
			msg4log = type+"#"+box_code+"#"+owner+"#"+time_gps+","+time_server+","+latitude+","+longitude
	   		+","+gps_status+","+speed+","+direction+","+altitude+","+engine_status+","+digital+","
	   		+analog1+","+analog2+","+analog3+","+analog4+","+base_name+","+nsat+","+sys_temp+","+vin+","
					+hdop+","+pdop+","+lac+","+ci+","+rssi+","+ber+","+gsm_temp+","+gsm_volt+","
					+length+","+sensor_nectec;
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return msg4log;
	}
	
	public String sendUDP(String data,String ipAddress, String port) throws Exception
	{
		String msg_return = "";
		try
		{
			ipAddress = ipAddress.replace('.',',');		//(".",",");
			String ip_byte [] = ipAddress.split(",");
			byte inet_byte[] = new byte[]{(byte)Integer.parseInt(ip_byte[0]) ,
					(byte)Integer.parseInt(ip_byte[1]),(byte)Integer.parseInt(ip_byte[2]),
					(byte)Integer.parseInt(ip_byte[3])};
			byte send[] = data.getBytes();
			
			InetAddress address = InetAddress.getByAddress(inet_byte);
			DatagramSocket socket = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(send, send.length,address, Integer.parseInt(port));
			
			socket.setSoTimeout(5000);
			socket.setReuseAddress(true);
			
			socket.send(packet);
			
			byte[] receive_msg = new byte[256];

			packet = new DatagramPacket(receive_msg,receive_msg.length);
			socket.receive(packet);
			String received = new String(receive_msg,0,packet.getLength());
			msg_return = received;
			socket.close();
		}
		catch(SocketTimeoutException ex)
		{
			msg_return = "Socket timeout 5 second";
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return msg_return;
	}
	
	public String[] sendUDP_ReturnArray(String data,String ipAddress, String port) throws Exception
	{
		String msg_return[] = new String[3];
		try
		{
			ipAddress = ipAddress.replace('.',',');		//(".",",");
			String ip_byte [] = ipAddress.split(",");
			byte inet_byte[] = new byte[]{(byte)Integer.parseInt(ip_byte[0]) ,
					(byte)Integer.parseInt(ip_byte[1]),(byte)Integer.parseInt(ip_byte[2]),
					(byte)Integer.parseInt(ip_byte[3])};
			
			byte send[] = data.getBytes();
			
			InetAddress address = InetAddress.getByAddress(inet_byte);
			
			DatagramSocket socket = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(send, send.length,address, Integer.parseInt(port));
			
			socket.setSoTimeout(5000);
			socket.setReuseAddress(true);
			
			msg_return[1] = Gateway_AllFunctionDecode.findDateTimeMillisecondSV();
			socket.send(packet);
			
			byte[] receive_msg = new byte[256];

			packet = new DatagramPacket(receive_msg,receive_msg.length);
			socket.receive(packet);
			msg_return[2] = Gateway_AllFunctionDecode.findDateTimeMillisecondSV();
			String received = new String(receive_msg,0,packet.getLength());
			msg_return[0] = received;
			socket.close();
		}
		catch(SocketTimeoutException ex)
		{
			msg_return[0] = "Socket timeout 5 second";
			msg_return[2] = Gateway_AllFunctionDecode.findDateTimeMillisecondSV();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return msg_return;
	}
	
	public static String[] sendUDP_ReturnArrayRB(String data,String ipAddress,String port) throws Exception
	{
		String msg_return[] = new String[4];
		
		try{
						
				msg_return =sendUDP_ReturnArrayRBc(data,InetAddress.getByName(ipAddress), Integer.parseInt(port));		
			
		}catch (Exception e) {
			System.out.println("Error:Send To GPS Manager");
			e.printStackTrace();
			// TODO: handle exception
		}
		return msg_return;

	}
	public static String[] sendUDP_ReturnArrayRBc(String data,InetAddress address,int port) throws Exception
	{
		String msg_return[] = new String[4];
		msg_return[3] = address+":"+String.valueOf(port);
		try
		{
			byte send[] = data.getBytes();

			DatagramSocket socket = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(send, send.length,address, port);
			
			socket.setSoTimeout(5000);
			socket.setReuseAddress(true);
			
			msg_return[1] = findDateTimeMillisecondSV();
			socket.send(packet);
			
			byte[] receive_msg = new byte[256];

			packet = new DatagramPacket(receive_msg,receive_msg.length);
			socket.receive(packet);
			msg_return[2] = findDateTimeMillisecondSV();
			String received = new String(receive_msg,0,packet.getLength());
			msg_return[0] = received;
			
			socket.close();
		}	
		catch(SocketTimeoutException ex)
		{
			msg_return[0] = "Socket timeout 5 second";
			msg_return[2] = findDateTimeMillisecondSV();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		
		return msg_return;
	}
	
	public static String findDateTimeMillisecondSV() throws Exception
    { 
    	DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S", Locale.US);
		return  formatter.format(new Date());
    }
}
