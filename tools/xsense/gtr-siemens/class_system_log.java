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
			
//			for(int i=0; i< buff.length; i++)
//			{
//				System.out.println("arg["+i+"] = " + buff[i]);
//			}
			
			if(buff.length >= 10)
			{	
				String box_firmware_version = buff[4];
				String sim_imei = buff[9].replace(">", "");
				
				System.out.println("version = " + box_firmware_version);
				System.out.println("sim_imei = " + sim_imei);
				
				if(sim_imei.length() <= 15)
					return;
				
				Param param = new Param();
				param.setBox_code(""+boxId);
				param.setBox_firmware_version(box_firmware_version);
				param.setSim_imei(sim_imei);
				param.setBox_ip(address.replace("/",""));
				
				if(param.getSim_imei().indexOf('~') != -1 || param.getSim_imei().indexOf('T') != -1){
					System.out.println("Fail Update BOXMASTER :: ["+param.getBox_code()+"]["+param.getSim_imei()+"]");
				}else{
					// update 2014-09-11 เฉพาะที่เป็น string = imei [8966181211447629997]
					QueryDB query = new QueryDB();
					boolean success = query.UpdateBoxMaster(param);
					if(success == true)
						System.out.println("Update BOXMASTER :: ["+param.getBox_code()+"]["+param.getSim_imei()+"]["+param.getBox_firmware_version()+"]" );
				}
				
			}
			
			/*
			switch(code)
			{
				case 'A':
				case 'a':
					System.out.println("Code A \t>>> \tReserve");
					System.out.println("seq \t>>> \t"+seq);
					System.out.println("boxId \t>>> \t"+boxId);
					System.out.println("data \t>>> \t"+data);
					break;
				case 'B':
				case 'b':
					System.out.println("Code B \t>>> \tFull ID, Time");
					System.out.println("seq \t>>> \t"+seq);
					System.out.println("boxId \t>>> \t"+boxId);
					System.out.println("data \t>>> \t"+data);
					break;
				case 'C':
				case 'c':
					System.out.println("Code C \t>>> \tTime Change");
					System.out.println("seq \t>>> \t"+seq);
					System.out.println("boxId \t>>> \t"+boxId);
					System.out.println("data \t>>> \t"+data);
					break;
				case 'D':
				case 'd':
					System.out.println("Code D \t>>> \tEngine Change");
					System.out.println("seq \t>>> \t"+seq);
					System.out.println("boxId \t>>> \t"+boxId);
					System.out.println("data \t>>> \t"+data);
					break;
				case 'M':
				case 'm':
					System.out.println("Code M \t>>> \tOther Message");
					System.out.println("seq \t>>> \t"+seq);
					System.out.println("boxId \t>>> \t"+boxId);
					System.out.println("data \t>>> \t"+data);
					break;
				case 'I':
				case 'i':
					System.out.println("Code M \t>>> \tOther Message");
					System.out.println("seq \t>>> \t"+seq);
					System.out.println("boxId \t>>> \t"+boxId);
					System.out.println("data \t>>> \t"+data);
					break;
				default:
					System.out.println("Error, Identification code not match.");
			}
			*/
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
