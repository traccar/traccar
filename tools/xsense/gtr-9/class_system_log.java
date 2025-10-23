package org.siemens;

import org.entity.Param;
import org.entity.QueryDB;

public class class_system_log {

	public byte type;
	public int seq;
	public int boxId;
	public String data;
	public String address;
	
	public class_system_log(byte type, int seq, int boxId, String data,String address)throws Exception
	{
		this.type = type;
		this.seq = seq;
		this.boxId = boxId;
		this.data = data;
		this.address = address;
		manageData();
	}
	
	public void manageData()throws Exception
	{
		try
		{
			//char code = (char)Gateway_HexConvert.HextoInteger(data.substring(0,2));
			data = Gateway_HexConvert.hex2string_systemlog(data.substring(0,data.length()));
			data = data.trim();
			
			String msgFile = "seq \t\t\t"+seq
							+"\r\nboxId \t\t\t"+boxId
							+"\r\ndata \t\t\t"+data;

			System.out.println(msgFile);
			
			Gateway_CreateLogFile.createSystemLog(msgFile,boxId);
			Gateway_CreateLogFile.createAllSystemLog(msgFile);
			Gateway_CreateLogFile.createAllSystemLog_2(data);
			
			// sim_imei
			// box_firmware_version
			
			/*
			Date time: 2010-06-29 11:15:21
			From Host: /202.149.24.41	PORT : 61142
			Length: 104
			7E7E7E7E0061EC5E0FFE234C2C3135313230343039342C3032302C32393030302C56332E396331352C4930303132332C30303035382C302C30343039342C3E383936363031303931303032333433393336392C403335373234383031303230383439382C32D47E7E
			61EC5E0FFE234C2C3135313230343039342C3032302C32393030302C56332E396331352C4930303132332C30303035382C302C30343039342C3E383936363031303931303032333433393336392C403335373234383031303230383439382C32D4
			CRC_UDP = 32D4   CRC_UDP_CALCULATION = 32D4
			CRC_UDP_VALID
			--- Tini Batch Online Enh I/O ---
			seq 			236
			boxId 			1204094
			data 			#L,151204094,020,29000,V3.9c15,I00123,00058,0,04094,>8966010910023439369,@357248010208498,
			-------------------------------------------------------
			*/
			
			String buff[] = data.split(",");
			String buff_dv[] = data.split("\\[");
			
//			for(int i=0; i< buff.length; i++)
//			{
//				System.out.println("arg["+i+"] = " + buff[i]);
//			}
			
			if(buff.length >= 10)
			{	
				String speed_limit = buff[3];
				String box_firmware_version = buff[4];
				// UPDATE Firmware
				if(buff[9].startsWith(">") && buff[9].length() < 30){
						
					String sim_imei = buff[9].replace(">", "");
					
					System.out.println("version = " + box_firmware_version);
					System.out.println("sim_imei = " + sim_imei);
					
					if(sim_imei.length() <= 15)
						return;
					
					Param param = new Param();
					param.setBox_code(""+boxId);
					param.setBox_firmware_version(box_firmware_version);
					param.setSim_imei(sim_imei);
					param.setSpeed(speed_limit);
					param.setBox_ip(address.replace("/",""));
					if(buff_dv.length == 2){	
						if(CheckMsfDigit(buff_dv[1])){
							param.setDriver(buff_dv[1].replace("]", "").trim());
						}else{
							param.setDriver(null);
						}
					}else{
						param.setDriver(null);
					}
					if(param.getSim_imei().indexOf('~') != -1 || param.getSim_imei().indexOf('T') != -1 || param.getSim_imei().indexOf('U') != -1 ){
						System.out.println("Fail Update BOXMASTER :: ["+param.getBox_code()+"]["+param.getSim_imei()+"]");
					}else{
						// update 2014-09-11 ੾�з���� string = imei [8966181211447629997]
						QueryDB query = new QueryDB();
						boolean success = query.UpdateBoxMaster(param);
						if(success == true)
							System.out.println("Update BOXMASTER :: ["+param.getBox_code()+"]["+param.getSim_imei()+"]["+param.getBox_firmware_version()+"]" );
					}
				 }else if(buff[9].indexOf('+') != -1){
					 
					 String firmware_imei = buff[9];
					 System.out.println("version = " + box_firmware_version);
					 System.out.println("sim_imei = " + firmware_imei);
					 
					 if(firmware_imei.length() <= 15)
							return;
					 
					 	Param param = new Param();
						param.setBox_code(""+boxId);
						param.setBox_firmware_version(box_firmware_version);
						param.setBox_firmware_gsm(firmware_imei);
						param.setSpeed(speed_limit);
						param.setBox_ip(address.replace("/",""));
						if(buff_dv.length == 2){
							if(CheckMsfDigit(buff_dv[1])){
								param.setDriver(buff_dv[1].replace("]", "").trim());
							}else{
								param.setDriver(null);
							}
						}else{
							param.setDriver(null);
						}
						
						if(param.getBox_firmware_gsm().startsWith("+G")) {
							System.out.println("Fail Update BOXMASTER :: ["+param.getBox_code()+"]["+param.getBox_firmware_gsm()+"]");
						}else{
							// Update 2016-09-02 Update New Firmware Imei
							QueryDB query = new QueryDB();
							boolean success = query.UpdateBoxMasterFirmwareImei(param);
							if(success == true)
								System.out.println("Update BOXMASTER :: ["+param.getBox_code()+"]["+param.getBox_firmware_gsm()+"]["+param.getBox_firmware_version()+"]" );
						}
				 }
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private static boolean CheckMsfDigit(String msg){
		boolean check =  false;
		String regex = "\\d+";
		try {
			String digit = msg.substring(0,2);
			if(digit.matches(regex)){
				check = true;
			}
		
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return check;	
	}
}
