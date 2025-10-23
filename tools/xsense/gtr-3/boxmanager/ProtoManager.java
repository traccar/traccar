/*
 * Created on 8 ?.?. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.boxmanager;

//import java.util.StringTokenizer;


import java.net.DatagramPacket;
import java.net.DatagramSocket;

import com.xsense.message.MessageObj;
import com.xsense.message.MessageType;
import com.xsense.message.pack.*;
import com.xsense.util.HexString;
import com.xsense.util.MyConfigFile;
import com.xsense.util.WriteToLogFile;

import entity.Param;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ProtoManager {
	
	public com.xsense.message.MessageObj msgObj;
	//String mtype="";
	public int itype;
	String box_code;
	
	DatagramPacket dp ;
	DatagramSocket ds;
	//byte ad[] =new byte[]{(byte)10,(byte)8,(byte)0,(byte)13};
	
	public ProtoManager(com.xsense.message.MessageObj msgObj){
		this.msgObj=msgObj;
		box_code=Long.toString(HexString.HextoInteger(HexString.HextoString(msgObj.getTID())));	 
	}
	
	 public void update(){
		try{
// 			System.out.println("Type 		:"+HexString.HextoString(msgObj.getType()));
// 			System.out.println("Size 		:"+HexString.HextoString(msgObj.getDataSize()));
// 			System.out.println("Ver 		:"+HexString.HextoString(msgObj.getVersion()));
// 			System.out.println("TID 		:"+HexString.HextoString(msgObj.getTID()));
// 			System.out.println("Seq No 		:"+HexString.HextoString(msgObj.getSeqNO()));
// 			System.out.println("Message+Ext	:"+HexString.HextoString(msgObj.getDataMsg()));
// 			System.out.println("CRC16 		:"+HexString.HextoString(msgObj.getCRC16CCITT()));
 			checktype(msgObj);
		}catch (Exception ex){ex.printStackTrace();}	
	}  
     private void checktype(MessageObj msgObj){
     	itype=msgObj.getType()[0];
		System.out.print("Type Decode >"+itype+" = ");
		switch(itype){
		 case MessageType.M_SYSTEM_LOG :
		 		System.out.println(MessageType.S_SYSTEM_LOG);
		 		//mtype=MessageType.S_SYSTEM_LOG;
		 		String log =new String(msgObj.getDataMsg());
		 		WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+box_code+"_"+WriteToLogFile.getDate()+"_system.log",log);
		 		UpdateSim(log);
		 		break; 
		 case MessageType.M_ALERT:
		 		System.out.println(MessageType.S_ALERT);
		 		//mtype=MessageType.S_ALERT;
		 	  break;	
		 	  
		 case MessageType.M_UPDATE_INTERVAL_TIME_RESULT :
		 	  	System.out.println(MessageType.S_UPDATE_INTERVAL_TIME_RESULT);
		 		//mtype=MessageType.S_UPDATE_INTERVAL_TIME_RESULT;
		 	  break;	
		 case MessageType.M_ENGINE_CONTROL_RESULT :
		 	  	System.out.println(MessageType.S_ENGINE_CONTROL_RESULT);
		 		//mtype=MessageType.S_ENGINE_CONTROL_RESULT;
		 	  break;	
		 case MessageType.M_PING_REPLY :
		 	  	System.out.println(MessageType.S_PING_REPLY);
		 		//mtype=MessageType.S_PING_REPLY;
		 		PingReplyPack pingpack = new PingReplyPack(msgObj);
		 		PingReply pr =pingpack.getPack();
		 		WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+box_code+"_"+WriteToLogFile.getDate()
		 				+"pingreply.log","Phone_Number:"+pr.getPhoneNember()+
						"\nID_SEC:"+pr.getID_SEC()+
						"\nTime_Base_Station(sec):"+pr.getTimeBaseStation()+
		 				"\nBase Station:"+pr.getBaseStation()+"\n");
		 	  break;
		 case MessageType.M_EXTEND_POSITION_REPORT :
		 	  	System.out.println(MessageType.S_EXTEND_POSITION_REPORT);
		 		//mtype=MessageType.S_EXTEND_POSITION_REPORT;
		 		ExtendPositionPack epackext =new ExtendPositionPack(msgObj );
		 		//sizep=epackext.getSp().getBytes().length;
			 	sendTo(itype,box_code,0+"#"+box_code+"#"+BoxManagerType.OWNER_ID+epackext.getSp());
		 	  break;
		 case MessageType.M_BATCH_POSITION_REPORT:
		 	  	System.out.println(MessageType.S_BATCH_POSITION_REPORT);
		 		//mtype=MessageType.S_BATCH_POSITION_REPORT;
		    	ExtendPositionPack epackon =new ExtendPositionPack(msgObj );
		    	// sizep=epackon.getSp().getBytes().length;
		 		sendTo(itype,box_code,0+"#"+box_code+"#"+BoxManagerType.OWNER_ID+epackon.getSp());
		 	  break;	
		 case MessageType.M_BATCH_OFFLINE_POSITION_REPORT:
	 	  		System.out.println(MessageType.S_BATCH_OFFLINE_POSITION_REPORT);
		 		//mtype=MessageType.S_BATCH_OFFLINE_POSITION_REPORT;
		 		ExtendPositionPack epackoff =new ExtendPositionPack(msgObj );
		 		//sizep=epackoff.getSp().getBytes().length;
		 		sendTo(itype,box_code,1+"#"+box_code+"#"+BoxManagerType.OWNER_ID+epackoff.getSp());
		 		break;
		 case MessageType.M_PING_REPLY_ENHIO:
	 	  	System.out.println(MessageType.S_PING_REPLY);
	 		//mtype=MessageType.S_PING_REPLY;
	 		PingReplyEnhPack pingpackenh = new PingReplyEnhPack(msgObj);
	 		PingReplyEnh prenh =pingpackenh.getPack();
	 		
	 		WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+box_code+"_"+WriteToLogFile.getDate()
	 				+"pingreply.log",
	 				"BoxID:"+box_code+
	 				"\nDT:"+prenh.getDateTime()+
	 				"\nLatitude:"+prenh.getLatitude()+
	 				"\nLongitude:"+prenh.getLongitude()+
	 				"\nGPS Status:"+prenh.getGPSStatus()+
	 				"\nSpeed:"+prenh.getSpeed()+
	 				"\nDirection:"+prenh.getGPSDegree()+
	 				"\nDigital:"+prenh.getDigital8Bit()+
	 				"\nAnalog:"+prenh.getAnalog()+
	 				"\nEnhance IO:"+prenh.getEnh()+
	 				"\nPhone Number:"+prenh.getPhoneNember()+
					"\nID SEC:"+prenh.getID_SEC()+
					"\nSTime(sec):"+prenh.getSTime()+
					"\nLTCell:"+prenh.getLTCell()+
					"\nLAC:"+prenh.getLAC()+
					"\nCI:"+prenh.getCI()+
					"\nTa:"+prenh.getTa()+
					"\nTc:"+prenh.getTc()+
					"\nLTbs:"+prenh.getLTbs()+
	 				"\nBase Station:"+prenh.getBase_Station()+"\n");
	 	  break;
		 case MessageType.M_BATCH_ONLINE_POSITION_REPORT_ENHIO:
	 	  	System.out.println(MessageType.S_BATCH_ONLINE_POSITION_REPORT_ENHIO);
	 		//mtype=MessageType.S_PING_REPLY;
	 		PositionReportPack bpkenh = new PositionReportPack(msgObj,true);
	 		//sizep=bpkenh.getSp().getBytes().length;
	 		//System.out.println(bpkenh.getBS());
	 		String bpkenh_org = bpkenh.getSp().trim();
	 		String bpkenh_rep  = bpkenh_org.replace("@", bpkenh.getBS().trim());
	 		BaseStation bsp =  bpkenh.getBaseStation();
	 		bpkenh_rep = bpkenh_rep.replace("tc",Long.toString(bsp.getTc())); 
	 		
	 		//System.out.println(bpkenh_rep);
	 		sendTo(itype,box_code,0+"#"+box_code+"#"+BoxManagerType.OWNER_ID+bpkenh_rep);
	 	  break;
		 case MessageType.M_BATCH_OFFLINE_POSITION_REPORT_ENHIO:
	 	  	System.out.println(MessageType.S_BATCH_OFFLINE_POSITION_REPORT_ENHIO);
	 		PositionReportPack bpoffkenh = new PositionReportPack(msgObj,false);
	 		//sizep=bpoffkenh.getSp().getBytes().length;
	 		sendTo(itype,box_code,1+"#"+box_code+"#"+BoxManagerType.OWNER_ID+bpoffkenh.getSp());
	 	  break;
	 	  
		 case MessageType.M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO:
	 	  	System.out.println(MessageType.S_BATCH_ONLINE_POSITION_REPORT_ENHIO);
	 		TiniPositionReportPack tbpkenh = new TiniPositionReportPack(msgObj,true);
	 		//System.out.println(tbpkenh.getBS());
	 		String tbpkenh_org = tbpkenh.getSp().trim();
	 		String tbpkenh_rep = tbpkenh_org.replace("@", tbpkenh.getBS().trim());
	 		BaseStation bs =  tbpkenh.getBaseStation();
	 		tbpkenh_rep = tbpkenh_rep.replace("tc",Long.toString(bs.getTc())); 
	 		//System.out.println(tbpkenh_rep);
	 		sendTo(itype,box_code,0+"#"+box_code+"#"+BoxManagerType.OWNER_ID+tbpkenh_rep);
	 	  break;
		 case MessageType.M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO:
	 	  	System.out.println(MessageType.S_BATCH_OFFLINE_POSITION_REPORT_ENHIO);
	 		TiniPositionReportPack tbpoffkenh = new TiniPositionReportPack(msgObj,false);
	 		sendTo(itype,box_code,1+"#"+box_code+"#"+BoxManagerType.OWNER_ID+tbpoffkenh.getSp());
	 	  break;
		 default :
		 	System.out.println("Type can not decode !!");
		    WriteToLogFile.writeOutput(MyConfigFile.getSystem_log()+"nodecode.log",new String(msgObj.getDataMsg()));
		 	break;
		}
	}
    
     public void UpdateSim(String log)
     {
    	 try
    	 {
    		 String buff[] = log.split(",");
    		 //#I,141103646,120,0,[4]V3.5J5~0009,I02018,01987,0,1103646,[9]>8966010804003447072,0000		
    		 // length = 11
    		 
    		 // #V,141102269,120,29000,0,[5]V3.5J89,I03216,03185,0,1102269,[10]>8966010940106605693,0000	
    		 // length = 12
    		 
// 			for(int i=0; i< buff.length; i++)
// 			{
// 				System.out.println("arg["+i+"] = " + buff[i]);
// 			}
 			
 			if(buff.length >= 11)
 			{	
 				int index_fw_version = buff.length - 7;
 				int index_imei = buff.length - 2;
 				
 				String box_firmware_version = buff[index_fw_version];
 				String sim_imei = buff[index_imei].replace(">", "");
 				
 				if(sim_imei.length() < 19)
 					return;
 				
 				Param param = new Param();
 				param.setBox_code(""+box_code);
 				param.setBox_firmware_version(box_firmware_version);
 				param.setSim_imei(sim_imei);
 				
 				//QueryDB query = new QueryDB();
 				//boolean success = query.UpdateBoxMaster(param);
 				//if(success == true)
 				//	System.out.println("update boxmaster :: ["+param.getBox_code()+"]["+param.getSim_imei()+"]["+param.getBox_firmware_version()+"]" );
 			}
    	 }
    	 catch(Exception ex)
    	 {
    		 ex.printStackTrace();
    	 }
     }
     
     public void sendTo(int type,String box_code,String pak){
    	 	  Plugin.sendToGPSManage(pak);
    	 	  Plugin.SaveLog(box_code, pak);
			  /*switch(type){
			    case MessageType.M_SYSTEM_LOG: break;
			    case MessageType.M_UPDATE_INTERVAL_TIME_RESULT :break;
			    case MessageType.M_ENGINE_CONTROL_RESULT :break;
			    case MessageType.M_PING_REPLY :break;
			    case MessageType.M_EXTEND_POSITION_REPORT :
			    	Plugin.sendToGPSManage(pak);
					break;
			    case MessageType.M_BATCH_POSITION_REPORT:
			    	Plugin.sendToGPSManage(pak);
					break;
				case MessageType.M_BATCH_OFFLINE_POSITION_REPORT:
					Plugin.sendToGPSManage(pak);
					break;
				case MessageType.M_PING_REPLY_ENHIO: break;
				case MessageType.M_BATCH_ONLINE_POSITION_REPORT_ENHIO:
					Plugin.sendToGPSManage(pak);
					break;
				case MessageType.M_BATCH_OFFLINE_POSITION_REPORT_ENHIO:
					Plugin.sendToGPSManage(pak);
					break;
				case MessageType.M_TINI_BATCH_ONLINE_POSITION_REPORT_ENHIO: 
					Plugin.sendToGPSManage(pak);
					break;
				case MessageType.M_TINI_BATCH_OFFLINE_POSITION_REPORT_ENHIO:
					Plugin.sendToGPSManage(pak);
					break;
				default :
				 	System.out.println("Type can not decode !!");
				    
			 	break;
			  }*/

	}  
}
