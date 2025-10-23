/*
 * Created on 5 ๏ฟฝ.๏ฟฝ. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.boxmanager;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
//import java.util.Iterator;
//import java.util.Set;
//import java.nio.ByteBuffer;
import com.xsense.message.MessageProtocalManager;
//import com.xsense.util.GenAckCode;
//import com.xsense.util.HexString;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DataServer extends Thread {
	DatagramSocket socket;
	DatagramPacket packet;
	
	public DataServer(DatagramSocket socket,DatagramPacket packet){
		this.socket=socket;
		this.packet=packet;
	}
	public void run()
	{
		try
		{
			/*String header = 
			"\nFrom Host: " + packet.getAddress() +
			"\nLength: " + packet.getLength() +
			"\nContent: ";
			String msg = new String(packet.getData()).trim();
			*/
			//System.out.println(WriteToLogFile.getDateTime()+ header + msg);
			int bleng=packet.getLength();
			byte[] bmsg =new byte[bleng];
			bmsg=packet.getData();	
			InetAddress clientAddr = packet.getAddress();
			int port = packet.getPort();
			
			byte[] buf = "\r\n>OK\r\n>OK\r\n>OK".getBytes();
			// prepare packet for return to client
			packet = new DatagramPacket(buf, buf.length, clientAddr, port);
			socket.send(packet);
			
			// For Mirror Server 30050
			//mirror(bmsg,bleng);

			MessageProtocalManager msgProtocal = new MessageProtocalManager(bmsg,bleng);
			com.xsense.message.MessageObj msgObj=msgProtocal.getMessageObj();
			if(msgObj.IsOK())
			{
				/*
				 * |---------------------------------------------------------------------------|
				 * | Type | Size | Ver | TID  | Seq NO. |  Message+Data |;Extend | CRC16/CCITT |
				 * |  1	  |  2   |	1  |  3   |  1      |		N       |  M     |     2	   |
				 * |---------------------------------------------------------------------------|
				 * 
				 * */
			
				//-------- Pack Byte Array Send to Data Manager ----------- //
				ProtoManager mu =	new ProtoManager(msgObj);
				mu.update();
				
				/* เอาไว้ส่ง Acknowledge กลับไปยังกล่องเพื่อเปลี่ยนค่า ip และ port ใหม่
				String box_code=Long.toString(HexString.HextoInteger(HexString.HextoString(msgObj.getTID())));
				Set<String> s = UDPServer.s;
				Iterator<String> it = s.iterator();
				boolean have_box = false;
				String[] box_split = null;
				
				while(it.hasNext())
				{
					String box_buf = it.next();
					if(box_buf.indexOf(box_code) != -1)
					{
						box_split = box_buf.split(",");
						if(box_split.length == 3)
						{
							have_box = true;
							break;
						}
					}
				}
				
				// Send Generate Code for change IP and Port of setting in box GTR.
				if(have_box == true)
				{
					String ip_box = box_split[1];
					int port_box = Integer.parseInt(box_split[2]);
					byte[] buf;
					
					GenAckCode gen = new GenAckCode();
					String gencode = gen.genCode(Integer.parseInt(box_code), ip_box, port_box);
					
					// Generate code error.
					if(gencode.indexOf("error") != -1)
					{
						buf = "\r\n>OK\r\n>OK\r\n>OK".getBytes();
						gen.createLogGenAckError(box_code,ip_box,port_box,gencode);
						System.out.println("box_code="+box_code+" ip="+ip_box+" port="+port_box);
						System.out.println("gencode="+gencode);
					}
					else
					{
						buf = gencode.getBytes();
						gen.createLogGenAckComplete(box_code,ip_box,port_box,gencode);
						System.out.println("box_code="+box_code+" ip="+ip_box+" port="+port_box);
						System.out.println("gencode="+gencode);
					}
					
					packet = new DatagramPacket(buf, buf.length, clientAddr, port);
					socket.send(packet);
				}
				else
				{
					byte[] buf = "\r\n>OK\r\n>OK\r\n>OK".getBytes();
					packet = new DatagramPacket(buf, buf.length, clientAddr, port);
					socket.send(packet);
					System.out.println("box_code="+box_code);
				}
				*/
			}
			else
			{
				//WriteToLogFile.writeOutput("/usr/log/xBox/datadump"+port+".log",WriteToLogFile.getDateTime()+"\n"+ header +"\n"+ msg+"\n\n");
			}
			//--------Restore MEM-------------//
			msgProtocal=null;
			msgObj=null;
		}
		catch(Exception ex)
		{
			//WriteToLogFile.writeOutput("/usr/log/xBox/datadump"+port+".log",WriteToLogFile.getDateTime()+"\n"+ header +"\n"+ msg+"\n\n");
			System.out.println(" !!Fail :>");
			ex.printStackTrace();
		}
		System.out.println("--------------------------------------------------------------");
	}
}
