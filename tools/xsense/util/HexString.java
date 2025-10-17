/*
 * Created on 11 µ.¤. 2547
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.xsense.util;

/**
 * @author amnuay
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class HexString {

	public static String HextoString ( byte b[] )
	   {
	   StringBuffer sb = new StringBuffer(b.length );
	   for ( int i=0; i<b.length; i++ )
		 {
		  // look up high nibble char
		  sb.append( hexChar [( b[i] & 0xf0 ) >>> 4] );

		  // look up low nibble char
		  sb.append( hexChar [b[i] & 0x0f] );
		  }
	   return sb.toString();
	   }
	public static byte[] hexStringToBytes( String hexstring )
	 {
	   byte[] retval = new byte[(hexstring.length()/2)];
	   for( int a=0; a< hexstring.length()-1; a+=2 )
	   {
	     char hi = hexstring.charAt(a);//-0x30;
	    char lo = hexstring.charAt(a+1);//-0x30;
	    //int xhi= (atohl(hi)+ atohr(lo));
	     //if(hi >9 ) hi-=7;
	     //if(lo > 9 ) lo-=7;
	     //hi = hi << 16 + lo;
	     //System.out.print(hi+" "+lo+":");
	   // hi = hi  + lo;
	   // long x=hi * 16 + lo;
	   // System.out.println(atohl(hi)+ atohr(lo));
	     retval[(a/2)] = (byte)(atohl(hi)+ atohr(lo));
	   }
	   return retval;
	 }
	
	static int atohr (char a) {          // change ascii to hex (R)

	    if ((a>='a' && a<='f') || (a>='A' && a<='F'))
	        return ((a + 9) & 0xf);
	    else return (a & 0xf);
	}

	static int atohl (char a) {          // change ascii to hex (L) 
	    int x = atohr(a);
	    x = x << 4;
	    return (x);
	}

//	   table to convert a nibble to a hex char.
	static char[] hexChar = {
	   '0' , '1' , '2' , '3' ,
	   '4' , '5' , '6' , '7' ,
	   '8' , '9' , 'a' , 'b' ,
	   'c' , 'd' , 'e' , 'f'};
	   
	public static long HextoInteger(String hex){
		
	
		//int g=	
		//Integer beer = new Integer(bvid);
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
	public static String convertThaiAsciiToUnicode(String ascii){
		if(ascii==null) return null;
		char chars[] = ascii.toCharArray();
		for(int i=0;i<chars.length;i++){
			if(chars[i]>=0xa0 && chars[i]<=0xFF) 
			chars[i]+=(0x0E00-0xA0);
		}//end for
		return new String(chars);
	}
	public static void main(String s[]){
		System.out.println(HextoInteger("111fgghffgvg1114d2"));
		System.out.println(HextoInteger("d2"));
		System.out.println(HextoInteger(""));
		
		
		
	
	}

}
