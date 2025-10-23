/*
 * Created on 30 �.�. 2547
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.boxmanager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;

import com.xsense.util.MyConfigFile;
import com.xsense.util.ReadBoxConf;
import com.xsense.util.WriteToLogFile;

import entity.Param;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class UDPServer {
	
	public static String conFigureFile = "../ConfBoxChangeGW.txt";
	public static String GenAckErrorFile = "../GenAckError.txt";
	public static String GenAckCompleteFile = "../GenAckComplete.txt";
	public static Set<String> s = new HashSet<String>();
	// 2019-01-03 PositionReport.getDateTime 2010 2020//
	// 2019-02-13 send en-mange domain//
	
	public static void main(String[] args)
	{	
		int port=(args.length>0)? Integer.parseInt( args[0]):BoxManagerType.DEF_PORT;
		DatagramSocket socket = null;
		//DatagramSocket socketmanager = null;
		System.out.println("Start gateway server...[port:"+port+"]");
		System.out.println("-----------"+port+"--------------");
		System.out.println("Version :: " + BoxManagerType.VERSION);	
		System.out.println("Function :: " + BoxManagerType.FUNCTION );
		System.out.println("---------------------------");
		
		/*
		try {
			ReadBoxConf obj = new ReadBoxConf();
			s = obj.ReadFirstConfigureFile();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		Timer tm = new Timer();
		tm.schedule(new ReadBoxConf(), (5*60*1000), (5*60*1000));	// Read Configure File every 5 Minute.
		*/
		
		/*
		 * Load Server Plugin Manager
		 * 
		 */
		//loadPlugin_DB();
		
		//---------------------------------------------//
		
		WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+WriteToLogFile.getDate()+"boot.log", "-----------"+port+"--------------");
		WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+WriteToLogFile.getDate()+"boot.log", BoxManagerType.VERSION);	
		WriteToLogFile.writeOutput2(MyConfigFile.getSystem_log()+WriteToLogFile.getDate()+"boot.log",BoxManagerType.FUNCTION );
		WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+WriteToLogFile.getDate()+"boot.log", "---------------------------");
		try {
			socket = new DatagramSocket(port);
			socket.setReuseAddress(true);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(2);
		}
		while (true) {
			//----------------Box--------------------------//
			try { // UDP Server Receive Packet //
				byte[] buf = new byte[BoxManagerType.DGRAM_BUF_LEN];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet); 
				new DataServer(socket,packet).start();
			} catch(IOException e) {
			  e.printStackTrace();
			  WriteToLogFile.writeOutput(e.getMessage());
			  System.exit(1);
			}
		 }// While 
	}
	
	/*@SuppressWarnings("unchecked")
	private static void loadPlugin_DB(){
		for(String h_db:MyConfigFile.getProtocol_db().split(",")){
			try{
					String[] h_ser=h_db.split(":");
					HostProtoAddress hp_db =new HostProtoAddress();
					hp_db.setHostname(h_ser[0]);
					hp_db.setPort(h_ser[1]);
					BoxManagerType.H_DB.add(hp_db);
			}catch (Exception e) {
				// TODO: handle exception
			}
		}
		System.out.println("----------------DB Server------------------------");
		for(int i=0;i<BoxManagerType.H_DB.size();i++){
			HostProtoAddress hpdb =(HostProtoAddress)BoxManagerType.H_DB.elementAt(i);
			System.out.println( hpdb.getHostname()+":"+hpdb.getPort());
		}
		}
	*/
		
	
	
	
	
	
}
