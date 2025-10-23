package com.xsense.message.pack;

import com.xsense.util.Binary;
import com.xsense.util.DateTime;
import com.xsense.util.HexString;

public class MessageConveter {
	
	public static String toDateTime(byte bmsg[]){
		try{
		 int	datetime =(int)HexString.HextoInteger(HexString.HextoString(bmsg));
			String tsdatetime =(Binary.BitIntToDec(datetime,4,26)+2000)+"-" // 0 -9 
            +addIntZeroFill(Binary.BitIntToDec(datetime,4,22))+"-"
            +addIntZeroFill(Binary.BitIntToDec(datetime,5,17))+" "
            +addIntZeroFill(Binary.BitIntToDec(datetime,5,12))+":"
			+addIntZeroFill(Binary.BitIntToDec(datetime,6,6))+":"
			+addIntZeroFill(Binary.BitIntToDec(datetime,6,0));
				// 25200000 = + 7 hour
			return DateTime.ConvertTimeZone(tsdatetime,25200000);

			
		}catch (Exception e) {
			return "";
		}
		
	}
	public static String toLatitude(byte bmsg[]){
		try {
				String lat =Long.toString(HexString.HextoInteger(HexString.HextoString(bmsg).substring(0,7)));
				if(lat.length()<8) lat="0"+lat;
				lat = lat.substring(0, 4) + "." + lat.substring(4);		
		        return MessageProperties.LAT_LONG_FORMAT.format(Double.parseDouble(lat.substring(0, 2)) +(Double.parseDouble(lat.substring(2)) / 60));
		}catch (Exception e) {
			return "";
		}
	}
	public static String toLongitude(byte bmsg[]){
		try{
				String lon =Long.toString(HexString.HextoInteger(HexString.HextoString(bmsg).substring(7,14)));
				if(lon.length()<9) lon="0"+lon;
				lon = lon.substring(0, 5) + "." + lon.substring(5);
				return MessageProperties.LAT_LONG_FORMAT.format(Double.parseDouble(lon.substring(0, 3)) +(Double.parseDouble(lon.substring(3)) / 60));
		}catch (Exception e) {
			return "";
		}
	
	}
	public static String toSpeed(byte bmsg[]){
		return MessageProperties.SPEED_FORMAT.format(Binary.BitByteToDec(bmsg[0],8)*1.943); //Convert mile to kilo
	}
	public static String toDigital0(byte bmsg[]){
		return Binary.toBitString(bmsg[0]);
	}
	public static String toDigital1(byte bmsg[]){
		return Binary.toBitString(bmsg[0]);
	}
	public static int toAnalog(byte bmsg[],byte bmsg1[]){
		int	datetime =(int)HexString.HextoInteger(HexString.HextoString(bmsg1));
		return (Binary.BitByteToDec(bmsg[0],8)<<2)+Binary.BitIntToDec(datetime,2,30);
	}
	public static int toGpsNS(byte bmsg[]){
		return Binary.BitByteSelect(bmsg[0],8);
	}
	public static int toGpsEW(byte bmsg[]){
		return Binary.BitByteSelect(bmsg[0],7);
	}
	public static int toGpsStatus(byte bmsg[]){
		return Binary.BitByteSelect(bmsg[0],6);
	}
	public static int toDirection(byte bmsg[]){
		return Binary.BitByteSelect(bmsg[0],5);
	}
	
	
	public static String addIntZeroFill(int x){
		if(x<10) return "0"+Integer.toString(x);
		else return Integer.toString(x);
	}
}
