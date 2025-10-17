/*
 * Created on 30 ¾.Â. 2547
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
/*
 * |---------------------------------------------------------------------------|
 * | Type | Size | Ver | TID  | Seq NO. |  Message+Data |;Extend | CRC16/CCITT |
 * |  1	  |  2   |	1  |  3   |  1      |		N       |  M     |     2	   |
 * |---------------------------------------------------------------------------|
 * 
 * */
public class MessageType {
	public  static final String S_SYSTEM_LOG="System Log";
	//public static final String S_CURREN_POSITION="Current Position Report";
	public static final String S_ALERT=" Alert Status (PowerDown,Driver,Customer)";
	public static final String S_UPDATE_INTERVAL_TIME_RESULT="Update Interval Time Result";
	public static final String S_ENGINE_CONTROL_RESULT="Engine Control Result(Status ON/OFF)";
	public static final String S_PING_REPLY="Ping Reply";
	public static final String S_EXTEND_POSITION_REPORT="Extend Position Report";
	public static final String S_BATCH_POSITION_REPORT="Batch Online Position Report";
	public static final String S_BATCH_OFFLINE_POSITION_REPORT="Batch Offline Position Report";
//	 Version 3.0//
	public static final String S_POSITION_REPORT_ENHIO="Position Report Enh I/O";
	public static final String S_BATCH_ONLINE_POSITION_REPORT_ENHIO="Batch Online Position Report Enh I/O";
	public static final String S_BATCH_OFFLINE_POSITION_REPORT_ENHIO="Batch Offline Position Report Enh I/O";
	public static final String S_PING_REPLY_ENHIO="Ping Reply Enh I/O";
//	 Version 3.1//
	public static final String S_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO="Tini Batch Online Position Report Enh I/O";
	public static final String S_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO="Tini Batch Offline Position Report Enh I/O";
	
	
	public  static final byte M_SYSTEM_LOG=97;
	//public static final byte M_CURREN_POSITION=98;
	public static final byte M_ALERT=99;
	public static final byte M_UPDATE_INTERVAL_TIME_RESULT=100;
	public static final byte M_ENGINE_CONTROL_RESULT=101;
	public static final byte M_PING_REPLY=102;
	public static final byte M_EXTEND_POSITION_REPORT=103;
	public static final byte M_BATCH_POSITION_REPORT=104;
	public static final byte M_BATCH_OFFLINE_POSITION_REPORT=105;
	// Version 3.0//
	public static final byte M_POSITION_REPORT_ENHIO=106;
	public static final byte M_BATCH_ONLINE_POSITION_REPORT_ENHIO=107;
	public static final byte M_BATCH_OFFLINE_POSITION_REPORT_ENHIO=108;
	public static final byte M_PING_REPLY_ENHIO=109;
	// Version 3.1//
	public static final byte M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO=114;
	public static final byte M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO=115;
	
	
	
	public  static final byte K_SYSTEM_LOG=(byte)0x39;
	public static final byte K_ALERT=(byte)0x25;
	public static final byte K_UPDATE_INTERVAL_TIME_RESULT=(byte)0x56;
	public static final byte K_ENGINE_CONTROL_RESULT=(byte)0x72;
	public static final byte K_PING_REPLY=(byte)0x29;
	public static final byte K_EXTEND_POSITION_REPORT=(byte)0x33;
	public static final byte K_BATCH_POSITION_REPORT=(byte)0x73;
	public static final byte K_BATCH_OFFLINE_POSITION_REPORT=(byte)0xe7;
//	 Version 3.0//
	public static final byte K_POSITION_REPORT_ENHIO=(byte)0x66;
	public static final byte K_BATCH_ONLINE_POSITION_REPORT_ENHIO=(byte)0x7a;
	public static final byte K_BATCH_OFFLINE_POSITION_REPORT_ENHIO=(byte)0xdc;
	public static final byte K_PING_REPLY_ENHIO=(byte)0xb9;
	//	 Version 3.1//
	public static final byte K_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO=(byte)0xad;
	public static final byte K_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO=(byte)0xd7;
	
	
}
