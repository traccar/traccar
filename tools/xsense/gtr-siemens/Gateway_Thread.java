package org.siemens;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Date;

public class Gateway_Thread extends Thread
{
	public DatagramSocket socket = null;
	public DatagramPacket packet = null;
	public Gateway_AllFunctionDecode objDecode = new Gateway_AllFunctionDecode();
	public static String header_thread = "";
	public static String hex_string = "";
	public static String data_message = "";
	public static String crc_compare = "";
	public static String type_message = "";
	private Date incoming_time;
	
	public Gateway_Thread (DatagramSocket socket,DatagramPacket packet){
		this.socket=socket;
		this.packet=packet;
		incoming_time = new Date();
	}
	
	public void run(){
		
		header_thread = "";
		hex_string = "";
		data_message = "";
		crc_compare = "";
		type_message = "";
		
		try{
			header_thread = "Date time: "+Gateway_AllFunctionDecode.findDateTimeMillisecondSV()
					+"\r\nFrom Host: " + packet.getAddress() + "\tPORT : " + packet.getPort()
					+"\r\nLength: " + packet.getLength();
			System.out.println(header_thread);

			// decode data
			int bleng=packet.getLength();
			byte[] bmsg =new byte[bleng];
			bmsg=packet.getData();
			
			decode(bmsg, bleng);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	

	private synchronized void decode(byte[] bmsg, int bleng) throws Exception
	{
		try
		{
			hex_string = Gateway_HexConvert.byte2HexString(bmsg,bleng);
			System.out.println("hex_string = " +hex_string);
			
			if(hex_string.indexOf("7E00") != -1)
			{
				findMessage(hex_string);
			}
			else
			{
				System.out.println("ERROR, can not decode message");
				System.out.println("Message = "+Gateway_HexConvert.hex2string(hex_string));
				System.out.println("-------------------------------------------------------");
				
				Gateway_CreateLogFile.createErrorLog(Gateway_Thread.header_thread);
				Gateway_CreateLogFile.createErrorLog("Message = "+Gateway_HexConvert.hex2string(hex_string));
				Gateway_CreateLogFile.createErrorLog("-------------------------------------------------------");
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private synchronized void findMessage (String hex_string) throws Exception
	{
		try{
			/* |------------------- Message from Box ------------------------------------|
			 * | preemble | sync | message | tailer | preemble | sync | message | tailer |
			 * | 7e7e...  | 7e00 | message | 7e7e.. | 7e7e...  | 7e00 | message | 7e7e.. |
			 * |-------------------------------------------------------------------------|
			 */
			
			int start=0;		// start == 7E00
			do
			{
				start = hex_string.indexOf("7E00");
				if(start != -1)
				{
					int size = 0;
					hex_string = hex_string.substring(start+4);
					if(hex_string.length() > 14)
					{
						size = Gateway_HexConvert.HextoInteger(hex_string.substring(4,6));
						if(size <= (hex_string.length()/2))
						{
							data_message = hex_string.substring(0,(size+3)*2);  
							// size + 3 mean include Type, Seq and Size into data message
							//System.out.println("data_message = "+data_message);
							seperateMessage(data_message);
							hex_string = hex_string.substring((size+3)*2);
						}
						else
						{
							System.out.println("Size is more than data length.");
							System.out.println("-------------------------------------------------------");
							Gateway_CreateLogFile.createErrorLog(Gateway_Thread.header_thread);
							Gateway_CreateLogFile.createErrorLog("data_message = "+data_message
									+"\r\nSize is more than data length.");
							Gateway_CreateLogFile.createErrorLog("-------------------------------------------------------");
						}
					}
					else
					{
						System.out.println("ERROR, length of message is impossible.");
						System.out.println("-------------------------------------------------------");
						Gateway_CreateLogFile.createErrorLog(Gateway_Thread.header_thread);
						Gateway_CreateLogFile.createErrorLog("hex_string = "+hex_string
								+"ERROR, length of message is impossible.");
						Gateway_CreateLogFile.createErrorLog("-------------------------------------------------------");
					}
				}
			}while(start != -1);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public synchronized void seperateMessage (String data_message) throws Exception
	{
		try{
			
			/* |------------------ Gateway_Decode Data Message --------------------------|
			 * | Type | Seq | Size | BoxID | Message+Data |;Extend | CRC16/CCITT |
			 * |  1   |  1  |	1   |  2    |       N      |    M   |    2        |
			 * |-----------------------------------------------------------------|
			 */
			// seperate data
			String type = data_message.substring(0,2);
			String seq = data_message.substring(2,4);
			String boxid = data_message.substring(6,10);
			String data = data_message.substring(10,data_message.length()-4);
			
			byte dtype = Byte.parseByte(""+Gateway_HexConvert.HextoInteger(type));
			int dseq = Gateway_HexConvert.HextoInteger(seq);
			int dboxId = Gateway_HexConvert.HextoInteger(boxid);
			dboxId += 1200000;
			
			int k=0;
			byte crc16[] = new byte[(data_message.length()-4)/2];
			for(int i=0; i<data_message.length()-4; i+=2)
			{
				crc16[k++] =  (byte)Gateway_HexConvert.HextoInteger(data_message.substring(i,i+2));
				//System.out.println("Debug CRC >> " + data_message.substring(i,i+2));
			}

			String crcFromUDP = data_message.substring(data_message.length()-4,data_message.length());
			String crcCalculate = Gateway_Checksum.CRC16(crc16);
			//System.out.println("Cal = " + crcCalculate);
			
			if(crcFromUDP.equalsIgnoreCase(crcCalculate))
			{
				crc_compare = "CRC_UDP = "+crcFromUDP+"   CRC_UDP_CALCULATION = "+crcCalculate.toUpperCase()
							+"\r\nCRC_UDP_VALID";
				//System.out.println(crc_compare);
				decodeMessage(dtype, dseq, dboxId, data, crcFromUDP.toUpperCase());
			}
			else
			{
				crc_compare = "ERROR, CRC not match."
						+ "\r\nCRC_UDP = "+crcFromUDP+"   CRC_UDP_CALCULATION = "+crcCalculate.toUpperCase()
						+"\r\nCRC_UDP_INVALID";
				//System.out.println(crc_compare);
				//System.out.println("-------------------------------------------------------");
				
				Gateway_CreateLogFile.createBoxErrorLog(Gateway_Thread.header_thread,dboxId);
				Gateway_CreateLogFile.createBoxErrorLog("data_message = "+data_message
						+"\r\n"+crc_compare,dboxId);
				Gateway_CreateLogFile.createBoxErrorLog("-------------------------------------------------------",dboxId);
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private synchronized void decodeMessage (byte dtype, int seq, int boxId, String data, String CRC_UDP) throws Exception
	{
		Gateway_AllFunctionDecode af = new Gateway_AllFunctionDecode();
		String Logfile = "";
		String ack ="0";
	
		if(gps_gw_siemens.m_last_time.containsKey(boxId))
		{
			Date d = gps_gw_siemens.m_last_time.get(boxId);
			long diff = incoming_time.getTime() - d.getTime();
			System.out.println("**** diff = " + diff / 1000);
			
			gps_gw_siemens.m_last_time.put(boxId, incoming_time);
		}
		else
		{
			Date d = new Date();
			gps_gw_siemens.m_last_time.put(boxId, d);
		}
		try {
			String box_ip = packet.getAddress().toString().replace("/","");
			System.out.println(boxId+"#"+box_ip);
		} catch (Exception e) {
			// TODO: handle exception
		}
		try{
			switch(dtype)
			{
				case Gateway_DefineMessageType.system_log:
					type_message = "--- System Log ---";
					System.out.println(type_message);
					Logfile = Gateway_Thread.header_thread+"\r\n"+Gateway_Thread.hex_string
						+"\r\n"+Gateway_Thread.data_message+"\r\n"+Gateway_Thread.crc_compare;
					Gateway_CreateLogFile.createSystemLog(Logfile,boxId);
					Gateway_CreateLogFile.createSystemLog(type_message,boxId);
					Gateway_CreateLogFile.createAllSystemLog(Logfile); // All system log
					Gateway_CreateLogFile.createAllSystemLog(type_message);	// all system log
					Gateway_CreateLogFile.createAllSystemLog_2(Logfile);
					Gateway_CreateLogFile.createAllSystemLog_2(type_message);
					new class_system_log(dtype,seq,boxId,data,packet.getAddress().toString());
					
//					socket.send(packet);
					System.out.println("-------------------------------------------------------");
					Gateway_CreateLogFile.createSystemLog("-------------------------------------------------------",boxId);
					Gateway_CreateLogFile.createAllSystemLog("-------------------------------------------------------");	// all system log
					Gateway_CreateLogFile.createAllSystemLog_2("-------------------------------------------------------");
					break;
				case Gateway_DefineMessageType.update_time_result:
					type_message = "--- Update time result ---";
					System.out.println(type_message);
					new class_update_time_result(dtype,seq,boxId,data);
					break;
				case Gateway_DefineMessageType.ping_reply:
					type_message = "--- Ping reply ---";
					System.out.println(type_message);
					new class_ping_reply(dtype,seq,boxId,data);
					break;
				case Gateway_DefineMessageType.ping_reply_enh_io:
					type_message = "--- Ping reply Enh I/O ---";
					System.out.println(type_message);
					Logfile = Gateway_Thread.header_thread+"\r\n"+Gateway_Thread.hex_string
						+"\r\n"+Gateway_Thread.data_message+"\r\n"+Gateway_Thread.crc_compare;
					Gateway_CreateLogFile.createPingReplyEnh(Logfile,boxId);
					Gateway_CreateLogFile.createPingReplyEnh(type_message,boxId);
					class_ping_reply_enh_io pl = new class_ping_reply_enh_io(dtype,seq,boxId,data);
					ack = pl.udp_return;
//					socket.send(packet);
					System.out.println("-------------------------------------------------------");
					Gateway_CreateLogFile.createPingReplyEnh("-------------------------------------------------------",boxId);

					break;
				case Gateway_DefineMessageType.new_position_gps32_report:
					type_message = "--- New Position GPS32 Report ---";
					System.out.println(type_message);
					new class_new_position_gps32_report(dtype,seq,boxId,data);
					break;
				case Gateway_DefineMessageType.tini_batch_online_enh_io:
					type_message = "--- Tini Batch Online Enh I/O ---";
					System.out.println(type_message);
					Logfile = Gateway_Thread.header_thread+"\r\n"+Gateway_Thread.hex_string
						+"\r\n"+Gateway_Thread.data_message+"\r\n"+Gateway_Thread.crc_compare;
					Gateway_CreateLogFile.createBatchOnline(Logfile,boxId);
					Gateway_CreateLogFile.createBatchOnline(type_message,boxId);
					class_tini_batch_online_enh_io pon = new class_tini_batch_online_enh_io(dtype,seq,boxId,data);
					ack = pon.udp_return;
//					socket.send(packet);
					System.out.println("-------------------------------------------------------");
					Gateway_CreateLogFile.createBatchOnline("-------------------------------------------------------",boxId);
					
					break;
				case Gateway_DefineMessageType.tini_batch_offline_enh_io:
					type_message = "--- Tini Batch Offline Enh I/O ---";
					System.out.println(type_message);
					Logfile = Gateway_Thread.header_thread+"\r\n"+Gateway_Thread.hex_string
						+"\r\n"+Gateway_Thread.data_message+"\r\n"+Gateway_Thread.crc_compare;
					Gateway_CreateLogFile.createBatchOffline(Logfile,boxId);
					Gateway_CreateLogFile.createBatchOffline(type_message,boxId);
					class_tini_batch_offline_enh_io poff = new class_tini_batch_offline_enh_io(dtype,seq,boxId,data);
					ack = poff.udp_return;
//					socket.send(packet);
					System.out.println("-------------------------------------------------------");
					Gateway_CreateLogFile.createBatchOffline("-------------------------------------------------------",boxId);

					break;
				case Gateway_DefineMessageType.location_base_report:
					type_message = "--- Location Base Report ---";
					System.out.println(type_message);
					new class_location_base_report(dtype,seq,boxId,data);
					break;
				case Gateway_DefineMessageType.gps_report:
					type_message = "--- GPS Report ---";
					System.out.println(type_message);
					new class_gps_report(dtype,seq,boxId,data);
					break;
				default:
					type_message = "--- ERROR MESSAGE ---"
							+ "\r\nIdentification message type not match.";
					System.out.println(type_message);
					System.out.println("-------------------------------------------------------");
					
					Gateway_CreateLogFile.createBoxErrorLog(Gateway_Thread.header_thread,boxId);
					Gateway_CreateLogFile.createBoxErrorLog("type_message = "+type_message
							+"\r\nData = "+data,boxId);
					Gateway_CreateLogFile.createBoxErrorLog("-------------------------------------------------------",boxId);
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally{
			// return acknowledge to client
			String re_ack = ">OK";
			if(ack.equals("0")){
				byte[] buf = (re_ack+","+CRC_UDP+","+af.findSeq(seq)+","+af.findBoxId(boxId)+",*\r\n"+re_ack+"\r\n").getBytes();
				InetAddress clientAddr = packet.getAddress();
				int port = packet.getPort();
				packet = new DatagramPacket(buf, buf.length, clientAddr, port);
				socket.send(packet);
			}else{
				re_ack = ">Network Error";
				System.out.println(re_ack+","+CRC_UDP+","+af.findSeq(seq)+","+af.findBoxId(boxId)+",*\r\n"+re_ack+"\r\n");
			}
				
			
		}
	}
}
