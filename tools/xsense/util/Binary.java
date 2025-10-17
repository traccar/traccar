/*
 * Created on 15 ¸.¤. 2547
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
public class Binary {
	
	public static int BitByteToDec(byte x,int n){
		 int mask =1 <<n-1;
		 double sum=0;
		for(int i=0;i<n;i++){
			sum=sum+(((x&mask)==0? 0:1)*(Math.pow((double)2,(double)((n-1)-i))));
		    x<<=1;
		}
		return (int)sum;
	}
	public static int BitIntToDec(int x,int n){
		 int mask =1 <<n-1;
		 double sum=0;
		for(int i=0;i<n;i++){
			sum=sum+(((x&mask)==0? 0:1)*(Math.pow((double)2,(double)((n-1)-i))));
		    x<<=1;
		}
		return (int)sum;
	}
	
	public static int BitIntToDec(int x,int n,int y){
		 int mask =1 << n-1;
		  double sum=0;
		  x>>>=y;// Shift fill 0
		 // print(x,32);
		for(int i=0;i<n;i++){
			sum=sum+(((x&mask)==0? 0:1)*(Math.pow((double)2,(double)((n-1)-i))));
		    x<<=1;
		}
		return (int)sum;
	}
	public static int BitByteSelect(byte x,int n){
		
		int mask = 1 << 7;
		x<<=8-n;
		return (x& mask)== 0 ? 0:1;
		
	}
	
	public static void print(int x,int n){
		int mask = 1 << n-1;
		 
		for(int i=0;i<n;i++){
			System.out.print( (x& mask)== 0 ? 0:1);
			x<<=1;
		}
		System.out.println();
	} 
	public static String toBitString(byte x){
		int mask = 1 << 7;
		 String bit="";
		for(int i=0;i<8;i++){
			bit =bit +((x& mask)== 0 ? "0":"1");
			x<<=1;
		}
		return bit;
	} 
	public static String toBitString(int x,int n){
		int mask = 1 << n-1;
		 StringBuffer bf =new StringBuffer();
		for(int i=0;i<n;i++){
			bf.append( (x& mask)== 0 ? 0:1);
			x<<=1;
		}
		return bf.toString();
	} 
	
	/*public static byte[] hexStringToBytes(String val) {
	    byte[] buf = new byte[val.length() / 2];
	    final char[] hexBytes = {
	        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
	        'E', 'F'
	    };
	    byte[] hexMap = new byte[256];
	    for (int i = 0; i < hexBytes.length; i++) {
	      hexMap[hexBytes[i]] = (byte) i;
	    }
	    int pos = 0;
	    for (int i = 0; i < buf.length; i++) {
	      buf[i] = (byte) (hexMap[val.charAt(pos++)] << 4);
	      buf[i] += (byte)hexMap[val.charAt(pos++)];
	    }
	    return buf;
	  }*/
	public static byte[] mhexStringToBytes( String hexstring )
	 {
	   byte[] retval = new byte[(hexstring.length()/2)];
	   for( int a=0; a< hexstring.length()-1; a+=2 )
	   {
	     int hi = hexstring.charAt(a) - 0x30;
	     int lo = hexstring.charAt(a+1) - 0x30;
	     if(hi >9) hi-=7;
	     if(lo >9) lo-=7;
	     hi = hi*16 + lo;
	     retval[(a/2)] = (byte)hi;
	   }
	   return retval;
	 }

	
	public static String bytestohexstring( byte[] bytes )
	 {
	   String retstring = new String();
	   for( int a=0; a< bytes.length; a++)
	   {
	     int lo = 0x30 + (bytes[a]&0x0f); if(lo>0x39) lo+=7;
	     int hi = 0x30 + ((bytes[a]&0xf0)>>4);
	     if(hi>0x39) hi+=7;
	     retstring = retstring + (char)hi + (char)lo;
	   }
	   return retstring;
	 }
	 
	 
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

//	   table to convert a nibble to a hex char.
	static char[] hexChar = {
	   '0' , '1' , '2' , '3' ,
	   '4' , '5' , '6' , '7' ,
	   '8' , '9' , 'a' , 'b' ,
	   'c' , 'd' , 'e' , 'f'};

	/*public static void main(String as[]){
		System.out.println(BitByteToDec((byte)0xF0,8));
		System.out.println(BitByteToDec((byte)0xF0,7));
		System.out.println(BitByteToDec((byte)0xF0,6));
		System.out.println(BitByteToDec((byte)0xF0,5));
		System.out.println(BitByteToDec((byte)0xF0,4));
	}*/

}
