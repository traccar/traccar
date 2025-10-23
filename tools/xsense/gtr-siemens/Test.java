package org.siemens;

import org.entity.*;

public class Test {

	public static void main(String[] args) 
	{
		try {
			
			String str = Gateway_AllFunctionDecode.findDateTimeMillisecondSV();
			System.out.println(str);
			
//			for(int i =0;i< 20;i++){
//				//System.out.println(AddressRoundRobin.round);
//				//System.out.println(AddressRoundRobin.getAddress());
//				new Gateway_udp2db().sendUDP_ReturnArrayRB("bbb");
//				
//			}
			
//			String base_station = "54656577616E6728332900000000000000000000000000000030000000000000";
//			String fbase_station = (Gateway_HexConvert.hex2string_base_station(base_station)).trim();
//			System.out.println(fbase_station);
			
//			String box_code = "1204094";
//			String data = "#L,151204094,020,29000,V3.9c15,I00123,00058,0,04094,>8966010910023439369,@357248010208498,";
//			String buff[] = data.split(",");
//			
//			for(int i=0; i< buff.length; i++)
//			{
//				System.out.println("arg["+i+"] = " + buff[i]);
//			}
//			
//			if(buff.length >= 10)
//			{	
//				String box_firmware_version = buff[4];
//				String sim_imei = buff[9].replace(">", "");
//				
//				System.out.println("version = " + box_firmware_version);
//				System.out.println("sim_imei = " + sim_imei);
//				
//				Param param = new Param();
//				param.setBox_code(box_code);
//				param.setBox_firmware_version(box_firmware_version);
//				param.setSim_imei(sim_imei);
//				
//				QueryDB query = new QueryDB();
//				boolean success = query.UpdateBoxMaster(param);
//				System.out.println("Update BOXMASTER :: " + success );
//			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

}
