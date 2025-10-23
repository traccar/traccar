package org.siemens;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Gateway_AllFunctionDecode {

	public String findLat(int lat) throws Exception
	{
		int mm_mmmmm = (lat%10000000);
		int dd = lat/10000000;
		double arg = (double)dd+(double)mm_mmmmm/6000000;
		return arg + "";
//		return dd+(double)mm_mmmmm/6000000 + "";
	}
	
	public String findLon(int lon) throws Exception
	{
		int mm_mmmmm = (lon%10000000);
		int ddd = lon/10000000;
		double arg = (double)ddd+(double)mm_mmmmm/6000000;
		return arg + "";
//		return ddd+(double)mm_mmmmm/6000000+"";
	}
	
	public  String findDatetime(int datetime) throws Exception
	{
		int sec= (datetime%32)*2;
		int min = (datetime/32)%64;
		int hour = (datetime/32/64)%32;
		int day = (datetime/32/64/32)%32;
		int month = (datetime/32/64/32/32)%16;
		int year = (datetime/32/64/32/32/16)%128;
		
		String date = 2000+year+"-"+(month<10?"0"+month:month)
		+"-"+(day<10?"0"+day:day)+" "+(hour<10?"0"+hour:hour)+":"
		+(min<10?"0"+min:min)+":"+(sec<10?"0"+sec:sec);
		
		if(!(year == 0) || !(month == 0) || !(day == 0))
		{
			date = 2000+year+"-"+(month<10?"0"+month:month)
			+"-"+(day<10?"0"+day:day)+" "+(hour<10?"0"+hour:hour)+":"
			+(min<10?"0"+min:min)+":"+(sec<10?"0"+sec:sec);
			date = ConvertTimeZone(date,25200000);
		}
		
		return date;
	}

	public static String ConvertTimeZone(String s,int c){
//		String time1 =time.substring(0,2)+":"+time.substring(2,4)+":"+ time.substring(4);
		Date dt;// = new Date();
		SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ,java.util.Locale.US);
		
		try{
			DATE_FORMAT.setLenient(false);
			dt = DATE_FORMAT.parse(s.trim());
			dt.setTime(dt.getTime()+c);// Bkk +7H =25200000ms 
			
		}catch (Exception ex){
			System.out.println("String Date:"+s);
			ex.printStackTrace();
			return "";
		}     
	   return DATE_FORMAT.format( dt);
	}
	
    @SuppressWarnings("deprecation")
	public static String findDateTimeSV() throws Exception
    { 
    	Date datetime = new Date();
        long longdate = datetime.getTime();
        Date datetime2 = new Date(longdate);
    	DateFormat myformat = new SimpleDateFormat("-MM-dd HH:mm:ss");
    	return (datetime2.getYear()+1900)+myformat.format(datetime2);
    }
    
    @SuppressWarnings("deprecation")
	public static String findDateTimeMillisecondSV() throws Exception
    { 
    	Date datetime = new Date();
        long longdate = datetime.getTime();
        Date datetime2 = new Date(longdate);
    	DateFormat myformat = new SimpleDateFormat("-MM-dd HH:mm:ss.S");
    	return (datetime2.getYear()+1900)+myformat.format(datetime2);
    }

	public String findAnalog(int ana) throws Exception
	{
		return ana%4096+"";
	}
	
	public String findFlag(int flag_degree) throws Exception
	{
		// flag_degree = flag 3 bit + degree 5 bit
		int flag = flag_degree/32;
		int A = flag%2;
		int N = (flag/2)%2;
		int E = (flag/2/2);
		return E+""+N+""+A;
	}
	
	public String findDegree(int flag_degree) throws Exception
	{
		int degree = flag_degree%32;
		return ""+degree;
	}
	
	public String findHdop(int hdop) throws Exception
	{
		return ""+hdop;
	}
	
	public String findSpeed(int speed) throws Exception
	{
		return ""+(float)speed*1.852;
	}
	
	public String findAlt(int alt) throws Exception
	{
		double altitude = (alt-10000)*0.3048;
		return ""+(float)altitude;
	}

	public String findGps_status(int flag_degree) throws Exception
	{
		return ""+(flag_degree/32)%2;
	}
	
	public String findEngine_Status(int digi16) throws Exception
	{
		return ""+((digi16/256)%2);  // (digi16/(2^8))%2
		// if((digi16 & 0x100) != 0)
		//	return 1;
		// else
		//	return 0;
	}

	public String findLCA(int cell_id) throws Exception
	{
		return ""+(cell_id/65536);
	}
	
	public String findCi(int cell_id) throws Exception
	{
		return ""+(cell_id%65536);
	}
	
	public String findBoxId(int box_id) throws Exception
	{
		String box_id2 = ""+box_id%100000;
		for(int i=0; i< (5-box_id2.length()); i++)
		{
			box_id2 = "0"+box_id2;
		}
		return box_id2;
	}
	
	public String findSeq(int seq) throws Exception
	{
		String seq2 = ""+seq;
		for(int i=0; i< (3-seq2.length()); i++)
		{
			seq2 = "0"+seq2;
		}
		return seq2;
	}
}
