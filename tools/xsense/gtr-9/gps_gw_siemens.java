package org.siemens;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class gps_gw_siemens {
	
	//public static String VERSION = "2008-10-27 (Change type 3 and 5 to 0 and 1 respectively)";
	//public static String VERSION = "2008-12-15 (change method find_engine_status())";
	//public static String VERSION = "2009-01-04 (Edit follow p'boon request)";
	//public static String VERSION = "2009-02-17 (Add seq, CRC_UDP and box_id into acknowledge)";
	//public static String VERSION = "2009-03-20 (Change IP and PORT to 172.30.13.14 and 40069 respectively)";
	//public static String VERSION = "2009-04-08 (Add function for decode type SystemLog)";
	//public static String VERSION = "2009-12-11 (Config sending UDP(IP,Port) in ../gps-gw-siemens/conf/wrapper.conf)";
//	public static String VERSION = "2010-06-29 ( Update sim_emei, firmwareVersion in \"BOXMASTER\" )";
	public static String VERSION = "2011-03-07 ( Edit Function Create File System.log )";
	//public static int port_server = 0;
	
	public static String ip_udp = "127.0.0.1";
	public static String port_udp = "40069"; 
//	public static String ip_udp = "58.64.30.165";
//	public static String port_udp = "40069"; 
	
	public static int DGRAM_BUF_LEN = 2048;
	public static String header;
	public static int PORT = 1777;
	public static String dir = "";
	
	public static Map<Integer, Date> m_last_time = new HashMap<Integer, Date>();
	
	public static void main(String[] args) throws Exception 
	{	
		if(args.length >= 3)
		{
			PORT = Integer.parseInt(args[0].trim());
			ip_udp = args[1].trim();
			port_udp = args[2].trim();
		}
		else if(args.length == 1)
		{
			PORT = Integer.parseInt(args[0].trim());
		}
//		else
//		{
//			System.out.println("*********** Remark ***********");
//			System.out.println("Must assign 3 parameters in configure file");
//			System.out.println("== Example ==");
//			System.out.println("wrapper.app.parameter.2=30130");
//			System.out.println("wrapper.app.parameter.3=58.64.30.165");
//			System.out.println("wrapper.app.parameter.4=40069");
//			System.out.println("******************************");
//			System.exit(0);
//		}
		
		int port = PORT;
		DatagramSocket socket = null;
		
		try {
			String msgFile = "Start UDP Server for siemens module"
					+"\r\n---------------------------"
					+ "\r\nPORT :: "+ port
					+ "\r\nVERSION :: "+VERSION
					+ "\r\nUDP IP :: "+ ip_udp
					+ "\r\nUDP Port :: "+ port_udp
					+ "\r\n---------------------------";
			System.out.println(header = msgFile);
			
			System.getenv("PATH");
			
			socket = new DatagramSocket(port);
			socket.setReuseAddress(true);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		while (true)
		{
			try { // UDP Server Receive Packet //
				byte[] buf = new byte[DGRAM_BUF_LEN];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				new Gateway_Thread(socket,packet).start();
			} catch(Exception e) {
			  e.printStackTrace();
			}
		 }
		
    	/*
    	//String conf="";
    	File file_name = new File("gps-siemens.conf");
		if(file_name.exists()==false){
			FileWriter fw = new FileWriter("gps-siemens.conf",true);
			BufferedWriter out = new BufferedWriter(fw);
			out.write("<port_server>your value</port_server>\r\n" +
					"<ip_udp>your value</ip_udp>\r\n" +
					"<port_udp>your value</port_udp>\r\n");
    		out.close();
    		file_name = new File("gps-siemens.conf");
			System.out.println("Please assign value in file \""+file_name.getCanonicalPath()+"\"");
			System.exit(0);
		}
		else{
			try {
				
				String port_server2 = null;
				String ip_udp2 = null;
				String port_udp2 = null;
				
				FileInputStream fin = new FileInputStream("gps-siemens.conf");
				InputStreamReader in = new InputStreamReader(fin);
				BufferedReader br = new BufferedReader(in);
				String line = br.readLine();
				
				
				 	//<port_server>30140</port_server>
					//<ip_udp>202.151.178.229</ip_udp>
					//<port_udp>40071</port_udp>
				 
				
				while(line != null){
					if((line.indexOf("<port_server>") != -1) && (line.indexOf("</port_server>") != -1))
					{
						line = line.replace("<port_server>","");
						line = line.replace("</port_server>","");
						port_server2 = line.trim();
					}
					else if((line.indexOf("<ip_udp>") != -1) && (line.indexOf("</ip_udp>") != -1))
					{
						line = line.replace("<ip_udp>","");
						line = line.replace("</ip_udp>","");
						ip_udp2 = line.trim();
					}
					else if((line.indexOf("<port_udp>") != -1) && (line.indexOf("</port_udp>") != -1))
					{
						line = line.replace("<port_udp>","");
						line = line.replace("</port_udp>","");
						port_udp2 = line.trim();
					}
					line = br.readLine();
				}
				
				if(port_server2.equals("your value") || ip_udp2.equals("your value") || port_udp2.equals("your value"))
				{
					System.out.println("Please assign value in file \"gps-siemens.conf\"");
					System.exit(0);
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		*/
	}
}