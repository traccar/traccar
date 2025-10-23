package org.siemens;

public class Gateway_Decode {
	private byte[] bmsg;
	private int bleng;
	public String msgFile = "";
	
	public Gateway_Decode(byte[] bmsg,int bleng)  throws Exception
	{
		this.bmsg = bmsg;
		this.bleng = bleng;
		decode();
	}
	
	private void decode() throws Exception
	{
		try
		{
			String hexString = Gateway_HexConvert.byte2HexString(bmsg,bleng);
			msgFile += "\r\nHex String = "+hexString;
			
			System.out.print(msgFile);
			//Gateway_CreateLogFile.creatLogfile(msgFile);
			msgFile = "";
			
			findMessage(hexString);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private void findMessage (String hexString) throws Exception
	{
		try{
			/* |------------------- Message from Box ------------------------------------|
			 * | preemble | sync | message | tailer | preemble | sync | message | tailer |
			 * | 7e7e...  | 7e00 | message | 7e7e.. | 7e7e...  | 7e00 | message | 7e7e.. |
			 * |-------------------------------------------------------------------------|
			 */
			int start=0;		// start == 7E00
			int count=0;
			do
			{
				start = hexString.indexOf("7E00");
				if(start != -1)
				{
					int size = 0;
					count++;
					msgFile += "\r\n\r\n<<Message "+count+">>";
					hexString = hexString.substring(start+4);
					if(hexString.length() > 14)
					{
						size = Gateway_HexConvert.HextoInteger(hexString.substring(4,6));
						if(size <= (hexString.length()/2))
						{
							String dataMsg = hexString.substring(0,(size+3)*2);  // size + 3 mean include Type, Seq and Size into data message
							msgFile += "\r\nMessage "+count+" = "+dataMsg;
							seperateMessage(dataMsg);
							hexString = hexString.substring((size+3)*2);
							System.out.print(msgFile);
							//Gateway_CreateLogFile.creatLogfile(msgFile);
							msgFile = "";
						}
						else
						{
							msgFile += "\r\nSize more than data length.";
							System.out.print(msgFile);
							//Gateway_CreateLogFile.creatLogfile(msgFile);
							msgFile = "";
						}
					}
					else
					{
						msgFile += "\r\n>>> ERROR, length of message is impossible.";
						System.out.print(msgFile);
						//Gateway_CreateLogFile.creatLogfile(msgFile);
						msgFile = "";
					}
				}
			}while(start != -1);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	public void seperateMessage (String msgDecode) throws Exception
	{
		try{

			/* |------------------ Gateway_Decode Data Message --------------------------|
			 * | Type | Seq | Size | BoxID | Message+Data |;Extend | CRC16/CCITT |
			 * |  1   |  1  |	1   |  2    |       N      |    M   |    2        |
			 * |-----------------------------------------------------------------|
			 */
			
			//724B2416B311EE080814AD4024086AD3693BD06A9C7FFF000B272800400400496400000030741D
			
			int k=0;
			byte crc16[] = new byte[(msgDecode.length()-4)/2];
			for(int i=0; i<msgDecode.length()-4; i+=2)
			{
				crc16[k++] =  (byte)Gateway_HexConvert.HextoInteger(msgDecode.substring(i,i+2));
				//System.out.println(msgDecode.substring(i,i+2));
			}

			String crcFromUDP = msgDecode.substring(msgDecode.length()-4,msgDecode.length());
			String crcCalculate = Gateway_Checksum.CRC16(crc16);

			//System.out.println(crcFromUDP);
			//System.out.println(crcCalculate);
			
			if(crcFromUDP.equalsIgnoreCase(crcCalculate))
			{
				msgFile += "\r\nCRC from UDP = "+crcFromUDP+"   CRC from calculation = "+crcCalculate;
				msgFile += "\r\nCRC OK";
				
				System.out.print(msgFile);
				//Gateway_CreateLogFile.createFile(msgFile);
				//Gateway_CreateLogFile.creatLogfile(msgFile);
				msgFile = "";
				
				String type = msgDecode.substring(0,2);
				String seq = msgDecode.substring(2,4);
				String boxid = msgDecode.substring(6,10);
				String data = msgDecode.substring(10,msgDecode.length()-4);
				
				byte dtype = Byte.parseByte(""+Gateway_HexConvert.HextoInteger(type));
				int dseq = Gateway_HexConvert.HextoInteger(seq);
				int dboxId = Gateway_HexConvert.HextoInteger(boxid);
				dboxId += 1200000;
				
				decodeMessage(dtype, dseq, dboxId, data);
			}
			else
			{
				msgFile += "\r\n>>> ERROR, CRC not match. !!!!!";
				msgFile += "\r\nCRC from UDP = "+crcFromUDP+"   CRC from calculation = "+crcCalculate;
				
				System.out.print(msgFile);
				//Gateway_CreateLogFile.createFile(msgFile);
				//Gateway_CreateLogFile.creatLogfile(msgFile);
				msgFile = "";
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private void decodeMessage (byte dtype, int seq, int boxId, String data) throws Exception
	{
		try{
			System.out.println();
			switch(dtype)
			{
				case Gateway_DefineMessageType.system_log:
					msgFile += "\r\n--- System Log ---";
					System.out.println(msgFile);
					//Gateway_CreateLogFile.createFile(msgFile);
					//Gateway_CreateLogFile.creatLogfile(msgFile);
					msgFile = "";
					//new class_system_log(dtype,seq,boxId,data);
					break;
				case Gateway_DefineMessageType.driver_license:
					msgFile += "\r\n--- Update time result ---";
					System.out.println(msgFile);
					//Gateway_CreateLogFile.createFile(msgFile);
					//Gateway_CreateLogFile.creatLogfile(msgFile);
					msgFile = "";
					new class_driver_license(dtype,seq,boxId,data);
					break;
				case Gateway_DefineMessageType.ping_reply:
					msgFile += "\r\n--- Ping reply ---";
					System.out.println(msgFile);
					//Gateway_CreateLogFile.createFile(msgFile);
					//Gateway_CreateLogFile.creatLogfile(msgFile);
					msgFile = "";
					new class_ping_reply(dtype,seq,boxId,data);
					break;
				case Gateway_DefineMessageType.ping_reply_enh_io:
					msgFile += "\r\n--- Ping reply Enh I/O ---";
					System.out.println(msgFile);
					//Gateway_CreateLogFile.createFile(msgFile);
					//Gateway_CreateLogFile.creatLogfile(msgFile);
					msgFile = "";
					new class_ping_reply_enh_io(dtype,seq,boxId,data);
					break;
				case Gateway_DefineMessageType.new_position_gps32_report:
					msgFile += "\r\n--- New Position GPS32 Report ---";
					System.out.println(msgFile);
					new class_new_position_gps32_report(dtype,seq,boxId,data);
					break;
				case Gateway_DefineMessageType.tini_batch_online_enh_io:
					msgFile += "\r\n--- Tini Batch Online Enh I/O ---";
					System.out.println(msgFile);
					//Gateway_CreateLogFile.createFile(msgFile);
					//Gateway_CreateLogFile.creatLogfile(msgFile);
					msgFile = "";
					new class_tini_batch_online_enh_io(dtype,seq,boxId,data);
					break;
				case Gateway_DefineMessageType.tini_batch_offline_enh_io:
					msgFile += "\r\n--- Tini Batch Offline Enh I/O ---";
					System.out.println(msgFile);
					//Gateway_CreateLogFile.createFile(msgFile);
					//Gateway_CreateLogFile.creatLogfile(msgFile);
					msgFile = "";
					new class_tini_batch_offline_enh_io(dtype,seq,boxId,data);
					break;
				case Gateway_DefineMessageType.location_base_report:
					msgFile += "\r\n--- Location Base Report ---";
					System.out.println(msgFile);
					//Gateway_CreateLogFile.createFile(msgFile);
					//Gateway_CreateLogFile.creatLogfile(msgFile);
					msgFile = "";
					new class_location_base_report(dtype,seq,boxId,data);
					break;
				case Gateway_DefineMessageType.gps_report:
					msgFile += "\r\n--- GPS Report ---";
					System.out.println(msgFile);
					//Gateway_CreateLogFile.createFile(msgFile);
					//Gateway_CreateLogFile.creatLogfile(msgFile);
					msgFile = "";
					new class_gps_report(dtype,seq,boxId,data);
					break;
				default:
					msgFile += "\r\n--- ERROR MESSAGE ---";
					msgFile += "\r\nIdentification message type not match.";
					System.out.println(msgFile);
					//Gateway_CreateLogFile.createFile(msgFile);
					//Gateway_CreateLogFile.creatLogfile(msgFile);
					msgFile = "";
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}