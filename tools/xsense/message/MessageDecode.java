/*
 * Created on 30 ¾.Â. 2547
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message;

import java.io.ByteArrayInputStream;
import com.xsense.util.CRC16CheckSum;
import com.xsense.util.HexString;
import com.xsense.util.MyConfigFile;
import com.xsense.util.WriteToLogFile;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MessageDecode {
	private MessageObj msgObj;
	private byte msg[];
	private int msgsize;
	private byte msgtype; 
	
	public MessageDecode(byte[] msg,int msgsize){
	//---------Int Value------------------//
		this.msg=msg;
		this.msgsize=msgsize;
		this.msgtype=msg[0];
	//------------Decode Message Type-----//
		checktype();
	}
	private void checktype(){
		System.out.print("Type Decode >"+msgtype+" = ");
		switch(msgtype){
		 case MessageType.M_SYSTEM_LOG :
		 	  	System.out.println(MessageType.S_SYSTEM_LOG);
		        decode(msg,MessageType.K_SYSTEM_LOG);
		        WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+WriteToLogFile.getDate()+"-system.log",new String(msgObj.getDataMsg())+"\n");
		 	  break;	
		 case MessageType.M_ALERT:
		 	  	System.out.println(MessageType.S_ALERT);
		 		decode(msg,MessageType.K_ALERT);
		 		WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+WriteToLogFile.getDate()+"-alert.log",new String(msgObj.getDataMsg())+"\n");
		 	  break;	
		 case MessageType.M_UPDATE_INTERVAL_TIME_RESULT :
		 	  	System.out.println(MessageType.S_UPDATE_INTERVAL_TIME_RESULT);
		 	  	decode(msg,MessageType.K_UPDATE_INTERVAL_TIME_RESULT);
		 	  	WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+WriteToLogFile.getDate()+"-updatetime.log",new String(msgObj.getDataMsg())+"\n");
		 	  break;	
		 case MessageType.M_ENGINE_CONTROL_RESULT :
		 	  	System.out.println(MessageType.S_ENGINE_CONTROL_RESULT);
		 		decode(msg,MessageType.K_ENGINE_CONTROL_RESULT);
		 		WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+WriteToLogFile.getDate()+"-enginectl.log",new String(msgObj.getDataMsg())+"\n");
		 	  break;	
		 case MessageType.M_PING_REPLY :
		 	  	System.out.println(MessageType.S_PING_REPLY);
		 		decode(msg,MessageType.K_PING_REPLY);
		 		WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+WriteToLogFile.getDate()+"-ping.log",new String(msgObj.getDataMsg())+"\n");
		 	  break;
		 case MessageType.M_EXTEND_POSITION_REPORT :
		 	  	System.out.println(MessageType.S_EXTEND_POSITION_REPORT);
		        decode(msg,MessageType.K_EXTEND_POSITION_REPORT);	
		 	  break;
		 case MessageType.M_BATCH_POSITION_REPORT:
		 	  	System.out.println(MessageType.S_BATCH_POSITION_REPORT);
		 		decode(msg,MessageType.K_BATCH_POSITION_REPORT);	
		 	  break;	
		 case MessageType.M_BATCH_OFFLINE_POSITION_REPORT:
	 	  		System.out.println(MessageType.S_BATCH_OFFLINE_POSITION_REPORT);
		 		decode(msg,MessageType.K_BATCH_OFFLINE_POSITION_REPORT);	
		 	  break;
	 	  //-------------- New Ver 3.0 -----------------//
		 	  //--------------------PING---------------------------//
		 case MessageType.M_PING_REPLY_ENHIO:
 	  			System.out.println(MessageType.S_PING_REPLY_ENHIO);
	 			decode(msg,MessageType.K_PING_REPLY_ENHIO);
	 			WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+WriteToLogFile.getDate()+"-ping.log",new String(msgObj.getDataMsg())+"\n");
	 		  break;
	 		  //--------------------ONLINE---------------------------//	  
		case MessageType.M_BATCH_ONLINE_POSITION_REPORT_ENHIO:
 	  			System.out.println(MessageType.S_BATCH_ONLINE_POSITION_REPORT_ENHIO);
	 			decode(msg,MessageType.K_BATCH_ONLINE_POSITION_REPORT_ENHIO);
	 		 break;
	 		 //--------------------OFFLINE---------------------------//			 
		case MessageType.M_BATCH_OFFLINE_POSITION_REPORT_ENHIO:
 	  			System.out.println(MessageType.S_BATCH_OFFLINE_POSITION_REPORT_ENHIO);
	 			decode(msg,MessageType.K_BATCH_OFFLINE_POSITION_REPORT_ENHIO);
	 		 break;
	 		 
	 	//--------------------New Ver 3.1-------------------------------//
	 		 //--------------------ONLINE---------------------------//	  
		case MessageType.M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO:
 	  			System.out.println(MessageType.S_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO);
	 			decode(msg,MessageType.K_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO);
	 		 break;
	 		 //--------------------OFFLINE---------------------------//			 
		case MessageType.M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO:
 	  			System.out.println(MessageType.S_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO);
	 			decode(msg,MessageType.K_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO);
	 		 break;
	 		 
		 default :
		 	System.out.println("Type can not decode !!");
		    WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+WriteToLogFile.getDate()+"-nodecode.log",new String(msg)+"\n\n");
		 	break;
		}
		
	}
	
	private boolean decode(byte[] dmsg,byte bdecode){
		/*
		 * |---------------------------------------------------------------------------|
		 * | Type | Size | Ver | TID  | Seq NO. |  Message+Data | Extend | CRC16/CCITT |
		 * |  1	  |  2   |	1  |  3   |  1      |		N       |  M     |     2	   |
		 * |---------------------------------------------------------------------------|
		 * 
		 * */
		 //-----Decode---//
		//System.out.println("Full MSG (String) >"+ new String(dmsg));
		//System.out.println("Full MSG (Hex) >"+ HexString.HextoString(dmsg));
		System.out.println("---------Decode--------------");
		for(int i=1;i<msgsize;i++){
			dmsg[i]= (byte)(dmsg[i] ^bdecode);
		}
		// ---------------------Set new msg Decode -----------------------------------//
		
		//System.out.println("Full MSG (Hex) >"+ HexString.HextoString(dmsg));
		ByteArrayInputStream buf = new ByteArrayInputStream (dmsg);
		//----------------------Look up Table------------------------------//
		byte crc16[] =new byte[2];
		crc16[0]=dmsg[msgsize-2];
		crc16[1]=dmsg[msgsize-1];
		//System.out.println(crc16.toString());
		//try{
		//	buf.read(crc16,msgsize-2,2);
		//}catch (Exception ex){/*ex.printStackTrace();*/}
		//System.out.println("CRC16CCITT >"+ HexString.HextoInteger(HexString.HextoString(crc16)));
		byte csum[] =new byte[msgsize-2];
		try{
			buf.read(csum);
		}catch (Exception ex){ex.printStackTrace();}
		//System.out.println("Msg >"+ HexString.HextoString(csum));
		//System.out.println("Msg >"+ new String(csum));
		System.out.println("CRC > "+  CRC16CheckSum.CRC16(csum));
		boolean v;
		//----------------Packet Type -----------------//
		msgObj = new MessageObj();
		if(v = checksum(HexString.HextoInteger(HexString.HextoString(crc16)),CRC16CheckSum.CRC16(csum))){
			System.out.println("Ckecksum  OK");
			msgObj.setCHK(v);
			ByteArrayInputStream bufpack = new ByteArrayInputStream (csum);
			byte[] ptype =new byte[1];
			byte[] psize =new byte[2];
			byte[] pver =new byte[1];
			byte[] ptid =new byte[3];
			byte[] pseq =new byte[1];
			byte[] pmsg =new byte[msgsize-10];
			try{
				
				bufpack.read(ptype);
				bufpack.read(psize);
				bufpack.read(pver);
				bufpack.read(ptid);
				bufpack.read(pseq);
				bufpack.read(pmsg);
	
				msgObj.setType(ptype);
				msgObj.setDataSize(psize);
				msgObj.setVersion(pver);
				msgObj.setTID(ptid);
				msgObj.setSeqNO(pseq);
				msgObj.setDataMsg(pmsg);
				msgObj.setCRC16CCITT(crc16);
				msgObj.setFullMsgSize(msgsize);
				msgObj.setFullMsg(dmsg);
				
			}catch(Exception ex){ex.printStackTrace();}
		 return true;
		}
		System.out.println("Ckecksum  false");
		return false;
	}
	private boolean checksum(long x1,long x2){
	   if(x1==x2) return true;
	   	else return false;
	}
	public MessageObj getMessageObject(){
		 return msgObj;
	}
	
}
