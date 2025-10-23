package org.siemens;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.sql.Timestamp;
import org.bean.CommandCompleteDAO;
import org.bean.CommandPendingDAO;
import org.bean.ManageCommandComplete;
import org.entity.CommandComplete;
import org.entity.CommandPending;
import org.util.Gateway_CreateLogFile;
import org.util.WriteToLogFile;

public class ProcessCommandReturn {
	
	public static String ManageCommandREG(CommandPending cp,String command,String box_id,DatagramSocket socket,DatagramPacket packet) {
		
		boolean delete_cp = false;
		
		try {
			
			int count = cp.getCount_send();
			
			// TYPE CONFIG
			if(cp.getType_message().equalsIgnoreCase("reg_config")){
				// Check OLD Command	
				if(cp.getTimer_to_send().before(new Timestamp(System.currentTimeMillis()))){
					
					if(count >= 6){		
						delete_cp = CommandPendingDAO.delete(cp);
						System.out.println(box_id+" Delete OLD command Pending : "+delete_cp);	
						if(delete_cp){	
						   CommandComplete cm_complete = ManageCommandComplete.addcommand_complete(cp);
						   cm_complete.setCommand_status("failed");
						   boolean complete = CommandCompleteDAO.add(cm_complete);
						   System.out.println(box_id+" ADD command complete ="+complete);
						}									
					}else{
						if(command == ""){
							command += ","+cp.getCommands_return_data()+","+cp.getSeq_id();
						}else{
							command += "#"+cp.getCommands_return_data()+","+cp.getSeq_id();
						}
							
						cp.setTime_send_device(Timestamp.valueOf(WriteToLogFile.getDateTime()));
						cp.setCount_send(count+1);
						boolean update = CommandPendingDAO.update(cp);
						System.out.println(box_id+" UPDATE = "+update);
					    String msgFile = "["+Gateway_AllFunctionDecode.findDateTimeMillisecondSV()
								+ "," +packet.getAddress()+ ":" +packet.getPort()+ "]"
								+ ","+cp.getCommands_return_data()+","+cp.getSeq_id();
						Gateway_CreateLogFile.createCommandConfig(msgFile,box_id);
					}		
				}
			// TYPE ALERT
			}else if(cp.getType_message().equalsIgnoreCase("reg_alarm")){
				// Check Alert Time	Realtime
				int time_end = Integer.valueOf(MyConfigFile.getAlertLimitTime());
				if(System.currentTimeMillis() - cp.getTime_create().getTime() < time_end * 60 * 1000){
					if(count >= 6){		
						delete_cp = CommandPendingDAO.delete(cp);
						System.out.println(box_id+" Delete OLD command Pending : "+delete_cp);	
						if(delete_cp){	
						   CommandComplete cm_complete = ManageCommandComplete.addcommand_complete(cp);
						   cm_complete.setCommand_status("failed");
						   boolean complete = CommandCompleteDAO.add(cm_complete);
						   System.out.println(box_id+" ADD command complete ="+complete);
						}									
					}else{				
							
						if(command == ""){
							command += ","+cp.getCommands_return_data()+","+cp.getSeq_id();
						}else{
							command += "#"+cp.getCommands_return_data()+","+cp.getSeq_id();
						}	
						cp.setTime_send_device(Timestamp.valueOf(WriteToLogFile.getDateTime()));
						cp.setCount_send(count+1);
						boolean update = CommandPendingDAO.update(cp);
						System.out.println(box_id+" UPDATE = "+update);
					    String msgFile = "["+Gateway_AllFunctionDecode.findDateTimeMillisecondSV()
								+ "," +packet.getAddress()+ ":" +packet.getPort()+ "]"
								+ ","+cp.getCommands_return_data()+","+cp.getSeq_id();
						Gateway_CreateLogFile.createCommandConfig(msgFile,box_id);
					}
				 // Delete Over time	
				 }else{
					 	delete_cp = CommandPendingDAO.delete(cp);
						System.out.println(box_id+" Delete OLD command Pending : "+delete_cp);	
						if(delete_cp){	
						   CommandComplete cm_complete = ManageCommandComplete.addcommand_complete(cp);
						   cm_complete.setCommand_status("overtime");
						   boolean complete = CommandCompleteDAO.add(cm_complete);
						   System.out.println(box_id+" ADD command complete ="+complete);
						}									
				 }
			 }
				
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return command;
		
	}

}
