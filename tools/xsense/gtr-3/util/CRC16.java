/*
 * Created on 9 ¡.¾. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.util;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class CRC16 {
	 public static long CRC16CCITT(byte[] testBytes){
	  	short crc = (short) 0xFFFF;       // initial contents of LFBSR
	    ///byte[] testBytes = "123456789".getBytes("ASCII");
	  	
	    for (int j = 0; j < testBytes.length; j++)
	    {
	        byte c = testBytes[j];
	        for (int i = 0; i < 8; i++)
	        {
	            boolean c15 = ((crc >> 15      & 1) == 1);
	            boolean bit = ((c   >> (7 - i) & 1) == 1);
	            crc <<= 1;
	            if (c15 ^ bit) crc ^= 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)
	        }
	    }
	    String scrc =Integer.toHexString(crc);
	    if(scrc.length()>4)
	    scrc=scrc.substring(scrc.length()-4,scrc.length());
	    //System.out.println("CRC16 = " + scrc);
	    return HextoInteger(scrc);
	   // System.out.println("CRC16 = " + Integer.toHexString(crc));
	  }
	 public static long HextoInteger(String hex){
		char ch1[] = hex.toCharArray();
		int bit=0;
		long sum=0;
		int bitlength =(ch1.length-1)*4;
		for(int i=0;i<ch1.length;i++){   
			sum = sum+((Character.digit(ch1[i],16))<<bitlength-bit);
			bit=bit+4;
		}
		return sum; 
	}   
}
