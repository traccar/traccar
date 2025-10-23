package org.siemens;

import org.client.MainClassGateway;

public class class_ping_reply_enh_io 
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

	String vtime = "",opt1 = "",opt2 = "",opt3 = "",opt4 = "",sync = "";
	String fvtime = "",fopt1 = "",fopt2 = "",fopt3 = "",fopt4 = "",fsync = "";

	public class_ping_reply_enh_io(byte type, int seq, int boxId, String data)throws Exception
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
			byte crc16_1byte[] = new byte[31];
			for(int i=0; i<62; i+=2)
			{
				//check CRC message N
				crc16_1byte[k++] =  (byte)Gateway_HexConvert.HextoInteger(data.substring(i,i+2));
				//System.out.println(msgDecode.substring(i,i+2));
			}

			String crcFromUDP = data.substring(62,64);
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
				String offline_ptr = data.substring(64,68);
				String id_sec = data.substring(68,72);
				String stime = data.substring(72,76);
				String mcc = data.substring(76,80);
				String mnc = data.substring(80,84);
				String ltcell = data.substring(84,88);
				String cell_id = data.substring(88,96);
				String ta = data.substring(96,98);
				String tc = data.substring(98,100);
				String ltbs = data.substring(100,104);
				String base_station = data.substring(104,168);
				String rssi = data.substring(168,170);
				String ltc1_3 = data.substring(170,174);
				String mcc1 = data.substring(174,178);
				String mnc1 = data.substring(178,182);
				String cell_id1 = data.substring(182,190);
				String ta1 = data.substring(190,192);
				String mcc2 = data.substring(192,196);
				String mnc2 = data.substring(196,200);
				String cell_id2 = data.substring(200,208);
				String ta2 = data.substring(208,210);
				String mcc3 = data.substring(210,214);
				String mnc3 = data.substring(214,218);
				String cell_id3 = data.substring(218,226);
				String ta3 = data.substring(226,228);
				String opt = data.substring(228,232);
				vtime = data.substring(232,236);
				opt1 = data.substring(236,240);
				opt2 = data.substring(240,244);
				opt3 = data.substring(244,248);
				opt4 = data.substring(248,252);
				sync = data.substring(252,256);

				String fNtype = ""+Ntype;		
				String flat = objDecode.findLat(Gateway_HexConvert.HextoInteger(lat));	// ddmm.mmmmm
				String flon = objDecode.findLon(Gateway_HexConvert.HextoInteger(lon));	// dddmm.mmmmm
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
				
				String foffline_ptr = ""+Gateway_HexConvert.HextoInteger(offline_ptr);
				String fid_sec = ""+Gateway_HexConvert.HextoInteger(id_sec);
				String fstime = ""+Gateway_HexConvert.HextoInteger(stime);
				String fmcc = mcc;
				String fmnc = mnc;
				String fltcell = ""+Gateway_HexConvert.HextoInteger(ltcell);
				String fcell_id = cell_id;
				String fta = ""+Gateway_HexConvert.HextoInteger(ta);
				String ftc = ""+Gateway_HexConvert.HextoInteger(tc);
				String fltbs =""+Gateway_HexConvert.HextoInteger(ltbs);
				String fbase_station = (Gateway_HexConvert.hex2string_base_station(base_station)).trim();
				String frssi =""+Gateway_HexConvert.HextoInteger(rssi);
				String fltc1_3 = ""+Gateway_HexConvert.HextoInteger(ltc1_3);
				String fmcc1 = mcc1;
				String fmnc1 = mnc1;
				String fcell_id1 = cell_id1;
				String fta1 = ""+Gateway_HexConvert.HextoInteger(ta1);
				String fmcc2 = mcc2;
				String fmnc2 = mnc2;
				String fcell_id2 = cell_id2;
				String fta2 = ""+Gateway_HexConvert.HextoInteger(ta2);
				String fmcc3 = mcc3;
				String fmnc3 = mnc3;
				String fcell_id3 = cell_id3;
				String fta3 = ""+Gateway_HexConvert.HextoInteger(ta3);
				String fopt = opt;
				fvtime = ""+Gateway_HexConvert.HextoInteger(vtime);
				fopt1 = ""+Gateway_HexConvert.HextoInteger(opt1);
				fopt2 = ""+Gateway_HexConvert.HextoInteger(opt2);
				fopt3 = ""+Gateway_HexConvert.HextoInteger(opt3);
				fopt4 = ""+Gateway_HexConvert.HextoInteger(opt4);
				fsync = ""+Gateway_HexConvert.HextoInteger(sync);

				// -- for Sort udp -- //
				String fgps_status = objDecode.findGps_status(Gateway_HexConvert.HextoInteger(flag_degree));
				String fengine_status = objDecode.findEngine_Status(Gateway_HexConvert.HextoInteger(digi16));
				String flca = objDecode.findLCA(Gateway_HexConvert.HextoInteger(fcell_id));
				String fci = objDecode.findCi(Gateway_HexConvert.HextoInteger(fcell_id));
				
				
				//SortData(type, box_code, time_gps, time_server, latitude, longitude
				//,gps_status, speed, direction, altitude, engine_status, digital
				//,analog1, analog2, analog3, analog4, base_name, nsat, sys_temp, vin
				//,hdop, pdop, lac, ci, rssi, ber, gsm_temp, gsm_volt, length, sensor_nectec
				msgFile = objUdp.SortDataMessage("0",boxId,ftime_gps,ftime_sv,flat,flon
						,fgps_status,fspeed,fdegree,faltitude,fengine_status,fdigi16
						,fana0,fana1,fana2,fana3,fbase_station,fnsat,"",fana1
						,fhdop,"",flca,fci,frssi,"","","",""+fopt.length(),fopt);
				
				//String udp_return[] = objUdp.sendUDP_ReturnArray(msgFile,IP,Port);
				String udp_return[] = Gateway_udp2db.sendUDP_ReturnArrayRB(msgFile,MainClassGateway.ip_udp_manage, MainClassGateway.port_udp_manage);
				
				//String udp_return = objUdp.sendUDP(msgFile,"172.30.13.2","40071");
				
				if(udp_return[0].equalsIgnoreCase("Socket timeout 5 second"))
				{
					System.out.println("["+udp_return[1]+"] UDP : "+msgFile);
					System.out.println("["+udp_return[2]+"] Ack : "+udp_return[0]);
					Gateway_CreateLogFile.createLogUdp(msgFile,boxId);
					Gateway_CreateLogFile.createPingReplyEnh("["+udp_return[1]+"] UDP : "+msgFile,boxId);
					Gateway_CreateLogFile.createPingReplyEnh("["+udp_return[2]+"] Ack : "+udp_return[0],boxId);
					Gateway_CreateLogFile.createAllAckTimeout("box_code="+boxId+",udp="+msgFile);
				}
				else
				{
					System.out.println("["+udp_return[1]+"] UDP : "+msgFile);
					System.out.println("["+udp_return[2]+"] Ack : "+udp_return[0]);
					Gateway_CreateLogFile.createLogUdp(msgFile,boxId);
					Gateway_CreateLogFile.createPingReplyEnh("["+udp_return[1]+"] UDP : "+msgFile,boxId);
					Gateway_CreateLogFile.createPingReplyEnh("["+udp_return[2]+"] Ack : "+udp_return[0],boxId);
				}
				this.udp_return=udp_return[0]; // Set Return ค่า
				
				msgFile = "seq \t\t\t"+seq
						+ "\r\nboxId \t\t\t"+boxId
						+ "\r\ndata \t\t\t"+data
						+ "\r\ntype \t\t\t"+Ntype+"\t\t\t"+fNtype
						+ "\r\nengine \t\t\t"+fengine_status
						+ "\r\nfDgree \t\t\t"+flag_degree+"\t\t\tENA="+fENA+"\t\t\tdegree="+fdegree
						+ "\r\nhdop \t\t\t"+hdop+"\t\t\t"+fhdop
						+ "\r\nspeed \t\t\t"+speed+"\t\t\t"+fspeed
						+ "\r\ndtime \t\t\t"+datetime+"\t\t"+ftime_gps
						+ "\r\nlat \t\t\t"+lat+"\t\t"+flat
						+ "\r\nlon \t\t\t"+lon+"\t\t"+flon
						+ "\r\ndigi16 \t\t\t"+digi16+"\t\t\t"+fdigi16
						+ "\r\nopt16 \t\t\t"+opt16+"\t\t\t"+fopt16
						+ "\r\nnsat \t\t\t"+nsat+"\t\t"+fnsat
						+ "\r\nalt \t\t\t"+alt+"\t\t\t"+faltitude
						+ "\r\nana01 \t\t\t"+ana01+"\t\t\tana0="+fana0+"\t\t\tana1="+fana1
						+ "\r\nana23 \t\t\t"+ana23+"\t\t\tana2="+fana2+"\t\t\tana3="+fana3
						+ "\r\nana45 \t\t\t"+ana45+"\t\t\tana4="+fana4+"\t\t\tana5="+fana5
						+ "\r\ncrc \t\t\t"+crc+"\t\t\t"+fcrc
						+ "\r\nCRC_ROM:: "+crcFromUDP+"\t\tCRC_ROM_CALCULATE:: "+crcCalculate
						+ "\r\nCRC_ROM_VALID"
						+ "\r\noffline_ptr \t\t"+offline_ptr+"\t\t\t"+foffline_ptr
						+ "\r\nid_sec \t\t\t"+id_sec+"\t\t\t"+fid_sec
						+ "\r\nstime \t\t\t"+stime+"\t\t\t"+fstime
						+ "\r\nmcc \t\t\t"+mcc+"\t\t\t"+fmcc
						+ "\r\nmnc \t\t\t"+mnc+"\t\t\t"+fmnc
						+ "\r\nltcell \t\t\t"+ltcell+"\t\t\t"+fltcell
						+"\r\ncell_id \t\t"+cell_id+"\t\t"+fcell_id
						+ "\r\nta \t\t\t"+ta+"\t\t\t"+fta
						+ "\r\ntc \t\t\t"+tc+"\t\t\t"+ftc
						+ "\r\nltbs \t\t\t"+ltbs+"\t\t\t"+fltbs
						+ "\r\nbase_station \t\t"+base_station
						+"\r\nbase_station_name \t"+fbase_station
						+ "\r\nrssi \t\t\t"+rssi+"\t\t\t"+frssi
						+ "\r\nltc1_3 \t\t"+ltc1_3+"\t\t\t"+fltc1_3
						+ "\r\nmcc1 \t\t\t"+mcc1+"\t\t\t"+fmcc1
						+ "\r\nmnc1 \t\t\t"+mnc1+"\t\t\t"+fmnc1
						+"\r\ncell_id1 \t\t"+cell_id1+"\t\t\t"+fcell_id1
						+ "\r\nta1 \t\t\t"+ta1+"\t\t\t"+fta1
						+ "\r\nmcc2 \t\t\t"+mcc2+"\t\t\t"+fmcc2
						+ "\r\nmnc2 \t\t\t"+mnc2+"\t\t\t"+fmnc2
						+ "\r\ncell_id2 \t\t"+cell_id2+"\t\t\t"+fcell_id2
						+ "\r\nta2 \t\t\t"+ta2+"\t\t\t"+fta2
						+ "\r\nmcc3 \t\t\t"+mcc3+"\t\t\t"+fmcc3
						+ "\r\nmnc3 \t\t\t"+mnc3+"\t\t\t"+fmnc3
						+ "\r\ncell_id3 \t\t"+cell_id3+"\t\t\t"+fcell_id3
						+ "\r\nta3 \t\t\t"+ta3+"\t\t\t"+fta3
						+ "\r\nopt \t\t\t"+opt+"\t\t\t"+fopt
						+ "\r\nvtime \t\t\t"+vtime+"\t\t\t"+fvtime
						+ "\r\nopt1 \t\t\t"+opt1+"\t\t\t"+fopt1
						+ "\r\nopt2 \t\t\t"+opt2+"\t\t\t"+fopt2
						+ "\r\nopt3 \t\t\t"+opt3+"\t\t\t"+fopt3
						+ "\r\nopt4 \t\t\t"+opt4+"\t\t\t"+fopt4
						+ "\r\nsync \t\t\t"+sync+"\t\t\t"+fsync;
//				System.out.println(msgFile);
				Gateway_CreateLogFile.createPingReplyEnh(msgFile,boxId);

			}
			else
			{
				msgFile = "ERROR, CRC of Message M (Ping Reply Enhance I/O) error"
						+ "\r\nCRC_UDP = "+crcFromUDP+ "   CRC_CALCULATION = "+crcCalculate;
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