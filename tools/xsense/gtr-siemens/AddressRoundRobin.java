package org.siemens;

public class AddressRoundRobin {
	public static int round = 0;
	
	public static String round_address[] = MyConfigFile.getManage_address().split(",");
	
	public static int num_round = round_address.length;
	
	public static int count_updown = 0;
	public static void setCountUp(){
		count_updown++;
		
	}
	public static void setCountDown(){
		if(count_updown>0)
		count_updown--;
		
	}
	public static String getAddress(){
		if(round < num_round-1) {
			round++;
		}else{
			round = 0;
		}
		return round_address[round];
	}
	
}
