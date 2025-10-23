package org.siemens;

import org.client.MainClassGateway;

//import java.net.DatagramPacket;
//import java.net.DatagramSocket;

public class class_tini_batch_online_enh_io 
{
	public String IP = gps_gw_siemens.ip_udp;
	public String Port = gps_gw_siemens.port_udp;
	
	public byte type;
	public int seq;
	public int boxId;
	public String data;
	
	public String udp_return = "0";
	
	public Gateway_AllFunctionDecode objDecode = new Gateway_AllFunctionDecode();
	public Gateway_udp2db objUdp = new Gateway_udp2db();
	
	
	
	public class_tini_batch_online_enh_io(byte type, int seq, int boxId, String data)throws Exception
	{
		this.type = type;
		this.seq = seq;
		this.boxId = boxId;
		this.data = data;
		manageData();
	}
	
	public void manageData()throws Exception
	{
		String msgFile = "";
		try
		{
			int k=0;
			byte crc16_1byte[] = new byte[(data.length()-2)/2];
			
			for(int i=0; i<data.length()-2; i+=2)
			{
				crc16_1byte[k++] =  (byte)Gateway_HexConvert.HextoInteger(data.substring(i,i+2));
				//System.out.println(msgDecode.substring(i,i+2));
			}

			String crcFromUDP = data.substring(data.length()-2,data.length());
			String crcCalculate = (Gateway_Checksum.CRC16(crc16_1byte)).toUpperCase();
			crcCalculate = crcCalculate.substring(crcCalculate.length()-2,crcCalculate.length());
			
			//System.out.println("crcfromudp "+crcFromUDP);
			//System.out.println("crcfromcalculate "+crcCalculate);
			
			if(crcFromUDP.equalsIgnoreCase(crcCalculate))
			{
				String Ntype = data.substring(0,2);
				String flag_degree = data.substring(2,4);
				String hdop = data.substring(4,6);
				String speed = data.substring(6,8);
				String datetime = data.substring(8,16);
				String lat = data.substring(16,24);
				String lon = data.substring(24,32);
				String digi16 = data.substring(32,36);
				String opt16 = data.substring(36,40);	// Hbit=etc, Lbit=nsat
				String nsat = data.substring(38,40);
				String alt = data.substring(40,44);
				String ana01 = data.substring(44,50);
				String ana23 = data.substring(50,56);
				String ana45 = data.substring(56,62);
				String crc = data.substring(62,64);
				
				String fNtype = ""+Ntype;	
				String flat = objDecode.findLat(Gateway_HexConvert.HextoInteger(lat));
				String flon = objDecode.findLon(Gateway_HexConvert.HextoInteger(lon));
				String fENA = objDecode.findFlag(Gateway_HexConvert.HextoInteger(flag_degree));	// E N A
				String fdegree = objDecode.findDegree(Gateway_HexConvert.HextoInteger(flag_degree));
				String fspeed = objDecode.findSpeed(Gateway_HexConvert.HextoInteger(speed));		// km/hr
				String fhdop = objDecode.findHdop(Gateway_HexConvert.HextoInteger(hdop));
				String ftime_gps = objDecode.findDatetime(Gateway_HexConvert.HextoInteger(datetime));	// yy/dd/mm hh:mm:ss
				String ftime_sv = Gateway_AllFunctionDecode.findDateTimeSV();
				String faltitude = objDecode.findAlt(Gateway_HexConvert.HextoInteger(alt));		// meter
				String fana0 = objDecode.findAnalog((Gateway_HexConvert.HextoInteger(ana01))/4096);
				String fana1 = objDecode.findAnalog(Gateway_HexConvert.HextoInteger(ana01));
				String fana2 = objDecode.findAnalog((Gateway_HexConvert.HextoInteger(ana23))/4096);
				String fana3 = objDecode.findAnalog(Gateway_HexConvert.HextoInteger(ana23));
				String fana4 = objDecode.findAnalog((Gateway_HexConvert.HextoInteger(ana45))/4096);
				String fana5 = objDecode.findAnalog(Gateway_HexConvert.HextoInteger(ana45));
				String fdigi16 = Gateway_HexConvert.HextoBinary(digi16);
				String fopt16 = ""+opt16;
				String fnsat = ""+Gateway_HexConvert.HextoInteger(nsat);
				String fcrc = ""+crc;
				String fgps_status = objDecode.findGps_status(Gateway_HexConvert.HextoInteger(flag_degree));
				String fengine_status = objDecode.findEngine_Status(Gateway_HexConvert.HextoInteger(digi16));
				
				//SortData(type, box_code, time_gps, time_server, latitude, longitude
				//,gps_status, speed, direction, altitude, engine_status, digital
				//,analog1, analog2, analog3, analog4, base_name, nsat, sys_temp, vin
				//,hdop, pdop, lac, ci, rssi, ber, gsm_temp, gsm_volt, length, sensor_nectec
				msgFile = objUdp.SortDataMessage("0",boxId,ftime_gps,ftime_sv,flat,flon
						,fgps_status,fspeed,fdegree,faltitude,fengine_status,fdigi16
						,fana0,fana1,fana2,fana3,"",fnsat,"",fana1
						,fhdop,"","","","","","","","0","");
				
				//String udp_return[] = objUdp.sendUDP_ReturnArray(msgFile,IP,Port);
				String udp_return[] = Gateway_udp2db.sendUDP_ReturnArrayRB(msgFile,MainClassGateway.ip_udp_manage, MainClassGateway.port_udp_manage);
//				String udp_return = objUdp.sendUDP(msgFile,IP,Port);
				//String udp_return = objUdp.sendUDP(msgFile,"172.30.13.2","40071");
				
				if(udp_return[0].equalsIgnoreCase("Socket timeout 5 second"))
				{
					System.out.println("["+udp_return[1]+"] UDP : "+msgFile);
					System.out.println("["+udp_return[2]+"] Ack : "+udp_return[0]);
					Gateway_CreateLogFile.createLogUdp(msgFile,boxId);
					Gateway_CreateLogFile.createBatchOnline("["+udp_return[1]+"] UDP : "+msgFile,boxId);
					Gateway_CreateLogFile.createBatchOnline("["+udp_return[2]+"] Ack : "+udp_return[0],boxId);
					Gateway_CreateLogFile.createAllAckTimeout("box_code="+boxId+",udp="+msgFile);
				
				}
				else
				{
					System.out.println("["+udp_return[1]+"] UDP : "+msgFile);
					System.out.println("["+udp_return[2]+"] Ack : "+udp_return[0]);
					Gateway_CreateLogFile.createLogUdp(msgFile,boxId);
					Gateway_CreateLogFile.createBatchOnline("["+udp_return[1]+"] UDP : "+msgFile,boxId);
					Gateway_CreateLogFile.createBatchOnline("["+udp_return[2]+"] Ack : "+udp_return[0],boxId);
					
				}
				this.udp_return=udp_return[0]; // Set Return ค่า
				
				msgFile = "seq \t\t\t"+seq
						+ "\r\nboxId \t\t\t"+boxId
						+ "\r\ndata \t\t\t"+data
						+ "\r\ntype \t\t\t"+Ntype+"\t\t"+fNtype
						+ "\r\nengine \t\t\t"+fengine_status
						+ "\r\nfDgree \t\t\t"+flag_degree+"\t\tENA="+fENA+"\t\t\tdegree="+fdegree
						+ "\r\nhdop \t\t\t"+hdop+"\t\t"+fhdop
						+ "\r\nspeed \t\t\t"+speed+"\t\t"+fspeed
						+ "\r\ndtime \t\t\t"+datetime+"\t"+ftime_gps
						+ "\r\nlat \t\t\t"+lat+"\t"+flat
						+ "\r\nlon \t\t\t"+lon+"\t"+flon
						+ "\r\ndigi16 \t\t\t"+digi16+"\t\t"+fdigi16
						+ "\r\nopt16 \t\t\t"+opt16+"\t\t"+fopt16
						+ "\r\nnsat \t\t\t"+nsat+"\t\t"+fnsat
						+ "\r\nalt \t\t\t"+alt+"\t\t"+faltitude
						+ "\r\nana01 \t\t\t"+ana01+"\t\tana0="+fana0+"\t\t\tana1="+fana1
						+ "\r\nana23 \t\t\t"+ana23+"\t\tana2="+fana2+"\t\t\tana3="+fana3
						+ "\r\nana45 \t\t\t"+ana45+"\t\tana4="+fana4+"\t\t\tana5="+fana5
						+ "\r\ncrc \t\t\t"+crc+"\t\t\t"+fcrc
						+ "\r\nCRC_ROM:: "+crcFromUDP+"\t\tCRC_ROM_CALCULATE:: "+crcCalculate
						+ "\r\nCRC_ROM_VALID";
				
//				System.out.println(msgFile);
				Gateway_CreateLogFile.createBatchOnline(msgFile,boxId);
				
			}
			else
			{
				msgFile = "ERROR, CRC of Message R (tini batch online enh i/o) error"
					+ "\r\nCRC_ROM = "+crcFromUDP+ "   CRC_ROM_CALCULATION = "+crcCalculate;
				System.out.println(msgFile);	
				Gateway_CreateLogFile.createBoxErrorLog("data = "+data
						+"\r\n"+msgFile,boxId);
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}