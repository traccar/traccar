/*
 * Created on 11 µ.¤. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message.pack;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class test {

	public static void main(String[] args) {
		//String s =new String("ffdfdf''gg\"'");
		String s = "1#1101247#1#2007-11-20 12:51:04,2007-11-20 13:54:46,13.549913,100.275000,1,005.83,17,0,1,1111111111111111,140,0,0,0,@#2007-11-20 12:50:44,2007-11-20 13:54:46,13.550168,100.276465,1,036.92,17,0,1,1111111111111111,139,0,0,0,@#2007-11-20 12:50:24,2007-11-20 13:54:46,13.550563,100.278398,1,044.69,17,0,1,1111111111111111,139,0,0,0,@#2007-11-20 12:50:03,2007-11-20 13:54:46,13.550945,100.280528,1,023.32,17,0,1,1111111111111111,138,0,0,0,@#2007-11-20 12:49:43,2007-11-20 13:54:46,13.551062,100.281010,1,000.00,16,0,1,1111111111111111,133,0,0,0,@#2007-11-20 12:49:23,2007-11-20 13:54:46,13.551080,100.280983,1,000.00,16,0,1,1111111111111111,133,0,0,0,@";
		//s = s.replaceAll("\'","");
		
		//char data[] = {'\'', '\"', '\''};
		//char datar[] = {' ', ' ', ' '};
		//s=s.replace(data,datar);
		s = s.replace("@","xx");
		System.out.println(s);
	}
}
