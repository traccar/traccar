package org.siemens;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Gateway_CreateLogFile {
	
//	public static String dir = "../logs/";
	public static String dir = "../../systemlog_gtr_ix/";
	
	/*public static void writeLog (String fpath, String filename, String message)
	{
		try{
			String path = findPath(fpath);
			String dirname = getDate() +"_"+filename+".log";
			
    		File ff = new File(path);
    		if(ff.exists()==false){
    			ff.mkdir();
    		}
    		
    		FileWriter fw = new FileWriter(path + dirname,true);
    		BufferedWriter out = new BufferedWriter(fw);
    		out.write(message + "\r\n");
    		out.flush();
    		out.close();
    		
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
	}
	
	private static String findPath(String fpath){
		try {
			String pathLinux = "../logs/" + fpath + "/";
			String pathWindow = "logs\\" + fpath + "\\";
			
			if(System.getProperty("os.name").startsWith("Windows"))
				return pathWindow;
			else
				return pathLinux;
		} catch (Exception e) {
			return "../logs/";
			// TODO: handle exception
		}
		
	}*/
	
	public static String getDate(){
		  DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		  return  formatter.format(new Date());

	}
	
	public static void createAllAckTimeout(String msgFile) throws Exception
    {
    	try{
    		File ff = new File(dir);
    		if(ff.exists()==false){
    			ff.mkdir();
    		}
    		
    		File file = new File(dir+findYYMMDD()+"-AllAckTimeout.txt");
    		if(file.exists() == false)
    		{
    			FileWriter fw = new FileWriter(dir+findYYMMDD()+"-AllAckTimeout.txt",true);
    			BufferedWriter out = new BufferedWriter(fw);
    			out.write("Siemens version = "+ gps_gw_siemens.VERSION+"\r\n");
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    		else{
        		FileWriter fw = new FileWriter(dir+findYYMMDD()+"-AllAckTimeout.txt",true);
        		BufferedWriter out = new BufferedWriter(fw);
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
	
    public static void createErrorLog(String msgFile) throws Exception
    {
    	try{
    		File ff = new File(dir);
    		if(ff.exists()==false){
    			ff.mkdir();
    		}
    		
    		File fff = new File(dir+findYYMMDD());
    		if(fff.exists()==false){
    			fff.mkdir();
    		}
    		
    		File file = new File(dir+findYYMMDD()+"/"+findYYMMDD()+"-ErrorLog.txt");
    		if(file.exists() == false)
    		{
    			FileWriter fw = new FileWriter(dir+findYYMMDD()+"/"+findYYMMDD()+"-ErrorLog.txt",true);
    			BufferedWriter out = new BufferedWriter(fw);
    			out.write(gps_gw_siemens.header+"\r\n");
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    		else{
        		FileWriter fw = new FileWriter(dir+findYYMMDD()+"/"+findYYMMDD()+"-ErrorLog.txt",true);
        		BufferedWriter out = new BufferedWriter(fw);
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
    

    public static void createAllSystemLog(String msgFile) throws Exception
    {
    	try{
    		File ff = new File(dir);
    		if(ff.exists()==false){
    			ff.mkdir();
    		}
    		
    		File fff = new File(dir+findYYMMDD());
    		if(fff.exists()==false){
    			fff.mkdir();
    		}
    		
    		File file = new File(dir
    				+findYYMMDD()+"/"+findYYMMDD()+"-AllSystemLog.txt");
    		if(file.exists() == false)
    		{
    			FileWriter fw = new FileWriter(dir
    					+findYYMMDD()+"/"+findYYMMDD()+"-AllSystemLog.txt",true);
    			BufferedWriter out = new BufferedWriter(fw);
    			out.write(gps_gw_siemens.header+"\r\n");
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    		else{
        		FileWriter fw = new FileWriter(dir
        				+findYYMMDD()+"/"+findYYMMDD()+"-AllSystemLog.txt",true);
        		BufferedWriter out = new BufferedWriter(fw);
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
    
    public static void createAllSystemLog_2(String msgFile) throws Exception
    {
    	try{
    		String dir_log = dir;
    		
    		File ff = new File(dir_log);
    		if(ff.exists()==false){
    			ff.mkdirs();
    		}
    		
    		File file = new File(dir_log+findYYMMDD()+"-AllSystemLog.txt");
    		if(file.exists() == false)
    		{
    			FileWriter fw = new FileWriter(dir_log+findYYMMDD()+"-AllSystemLog.txt",true);
    			BufferedWriter out = new BufferedWriter(fw);
    			out.write(gps_gw_siemens.header+"\r\n");
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    		else{
        		FileWriter fw = new FileWriter(dir_log+findYYMMDD()+"-AllSystemLog.txt",true);
        		BufferedWriter out = new BufferedWriter(fw);
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
    
    public static void createBoxErrorLog(String msgFile, int box_code) throws Exception
    {
    	try{
    		File ff = new File(dir);
    		if(ff.exists()==false){
    			ff.mkdir();
    		}
    		
    		File fff = new File(dir+findYYMMDD());
    		if(fff.exists()==false){
    			fff.mkdir();
    		}
    		
    		File ffff = new File(dir+findYYMMDD()+"/"+box_code);
    		if(ffff.exists()==false){
    			ffff.mkdir();
    		}
    		
    		File file = new File(dir
    				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-BoxErrorLog.txt");
    		if(file.exists() == false)
    		{
    			FileWriter fw = new FileWriter(dir
    					+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-BoxErrorLog.txt",true);
    			BufferedWriter out = new BufferedWriter(fw);
    			out.write(gps_gw_siemens.header+"\r\n");
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    		else{
        		FileWriter fw = new FileWriter(dir
        				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-BoxErrorLog.txt",true);
        		BufferedWriter out = new BufferedWriter(fw);
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
    
    public static void createSystemLog(String msgFile, int box_code) throws Exception
    {
    	try{
    		File ff = new File(dir);
    		if(ff.exists()==false){
    			ff.mkdir();
    		}
    		
    		File fff = new File(dir+findYYMMDD());
    		if(fff.exists()==false){
    			fff.mkdir();
    		}
    		
    		File ffff = new File(dir+findYYMMDD()+"/"+box_code);
    		if(ffff.exists()==false){
    			ffff.mkdir();
    		}
    		
    		File file = new File(dir
    				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-SystemLog.txt");
    		if(file.exists() == false)
    		{
    			FileWriter fw = new FileWriter(dir
    					+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-SystemLog.txt",true);
    			BufferedWriter out = new BufferedWriter(fw);
    			out.write(gps_gw_siemens.header+"\r\n");
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    		else{
        		FileWriter fw = new FileWriter(dir
        				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-SystemLog.txt",true);
        		BufferedWriter out = new BufferedWriter(fw);
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
    
    public static void createPingReplyEnh(String msgFile, int box_code) throws Exception
    {
    	try{
    		File ff = new File(dir);
    		if(ff.exists()==false){
    			ff.mkdir();
    		}
    		
    		File fff = new File(dir+findYYMMDD());
    		if(fff.exists()==false){
    			fff.mkdir();
    		}
    		
    		File ffff = new File(dir+findYYMMDD()+"/"+box_code);
    		if(ffff.exists()==false){
    			ffff.mkdir();
    		}
    		
    		File file = new File(dir
    				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-PingReplyEnh.txt");
    		if(file.exists() == false)
    		{
    			FileWriter fw = new FileWriter(dir
    					+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-PingReplyEnh.txt",true);
    			BufferedWriter out = new BufferedWriter(fw);
    			out.write(gps_gw_siemens.header+"\r\n");
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    		else{
        		FileWriter fw = new FileWriter(dir
        				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-PingReplyEnh.txt",true);
        		BufferedWriter out = new BufferedWriter(fw);
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
    
    public static void createBatchOnline(String msgFile, int box_code) throws Exception
    {
    	try{
    		File ff = new File(dir);
    		if(ff.exists()==false){
    			ff.mkdir();
    		}
    		
    		File fff = new File(dir+findYYMMDD());
    		if(fff.exists()==false){
    			fff.mkdir();
    		}
    		
    		File ffff = new File(dir+findYYMMDD()+"/"+box_code);
    		if(ffff.exists()==false){
    			ffff.mkdir();
    		}
    		
    		File file = new File(dir
    				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-TiniBatchOnline.txt");
    		if(file.exists() == false)
    		{
    			FileWriter fw = new FileWriter(dir
    					+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-TiniBatchOnline.txt",true);
    			BufferedWriter out = new BufferedWriter(fw);
    			out.write(gps_gw_siemens.header+"\r\n");
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    		else{
        		FileWriter fw = new FileWriter(dir
        				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-TiniBatchOnline.txt",true);
        		BufferedWriter out = new BufferedWriter(fw);
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
    
    public static void createDriverLicense(String msgFile, int box_code) throws Exception
    {
    	try{
    		File ff = new File(dir);
    		if(ff.exists()==false){
    			ff.mkdir();
    		}
    		
    		File fff = new File(dir+findYYMMDD());
    		if(fff.exists()==false){
    			fff.mkdir();
    		}
    		
    		File ffff = new File(dir+findYYMMDD()+"/"+box_code);
    		if(ffff.exists()==false){
    			ffff.mkdir();
    		}
    		
    		File file = new File(dir
    				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-DriverLicense.txt");
    		if(file.exists() == false)
    		{
    			FileWriter fw = new FileWriter(dir
    					+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-DriverLicense.txt",true);
    			BufferedWriter out = new BufferedWriter(fw);
    			out.write(gps_gw_siemens.header+"\r\n");
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    		else{
        		FileWriter fw = new FileWriter(dir
        				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-DriverLicense.txt",true);
        		BufferedWriter out = new BufferedWriter(fw);
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }

    public static void createBatchOffline(String msgFile, int box_code) throws Exception
    {
    	try{
    		File ff = new File(dir);
    		if(ff.exists()==false){
    			ff.mkdir();
    		}
    		
    		File fff = new File(dir+findYYMMDD());
    		if(fff.exists()==false){
    			fff.mkdir();
    		}
    		
    		File ffff = new File(dir+findYYMMDD()+"/"+box_code);
    		if(ffff.exists()==false){
    			ffff.mkdir();
    		}
    		
    		File file = new File(dir
    				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-TiniBatchOffline.txt");
    		if(file.exists() == false)
    		{
    			FileWriter fw = new FileWriter(dir
    					+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-TiniBatchOffline.txt",true);
    			BufferedWriter out = new BufferedWriter(fw);
    			out.write(gps_gw_siemens.header+"\r\n");
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    		else{
        		FileWriter fw = new FileWriter(dir
        				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-TiniBatchOffline.txt",true);
        		BufferedWriter out = new BufferedWriter(fw);
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
    
    public static void createLogUdp(String msgFile, int box_code) throws Exception
    {
    	try{
    		File ff = new File(dir);
    		if(ff.exists()==false){
    			ff.mkdir();
    		}
    		
    		File fff = new File(dir+findYYMMDD());
    		if(fff.exists()==false){
    			fff.mkdir();
    		}
    		
    		File ffff = new File(dir+findYYMMDD()+"/"+box_code);
    		if(ffff.exists()==false){
    			ffff.mkdir();
    		}
    		
    		File file = new File(dir
    				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-LogUDP.txt");
    		if(file.exists() == false)
    		{
    			FileWriter fw = new FileWriter(dir
    					+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-LogUDP.txt",true);
    			BufferedWriter out = new BufferedWriter(fw);
    			out.write(gps_gw_siemens.header+"\r\n");
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    		else{
        		FileWriter fw = new FileWriter(dir
        				+findYYMMDD()+"/"+box_code+"/"+findYYMMDD()+"_"+box_code+"-LogUDP.txt",true);
        		BufferedWriter out = new BufferedWriter(fw);
        		out.write(msgFile+"\r\n");
        		out.close();
    		}
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
/*    
    public static void createFile(String msgFile) throws Exception
    {
    	try{
    		msgFile = msgFile.replace("\n","");
    		FileWriter fw = new FileWriter("/home/louis/project_Siemens/log.txt",true);
        	BufferedWriter out = new BufferedWriter(fw);
        	out.write(msgFile+"\r");
        	out.close();
    	}catch(Exception ex){
    		ex.printStackTrace();
    		System.out.println(ex);
    	}
    }
*/    
	@SuppressWarnings("deprecation")
	public static String findYYMMDD() throws Exception
    { 
    	Date datetime = new Date();
        long longdate = datetime.getTime();
        Date datetime2 = new Date(longdate);
    	DateFormat myformat = new SimpleDateFormat("-MM-dd");
    	return (datetime2.getYear()+1900)+myformat.format(datetime2);
    }
	
	@SuppressWarnings("deprecation")
	public static String findYYMM() throws Exception
    { 
    	Date datetime = new Date();
        long longdate = datetime.getTime();
        Date datetime2 = new Date(longdate);
    	DateFormat myformat = new SimpleDateFormat("-MM");
    	return (datetime2.getYear()+1900)+myformat.format(datetime2);
    }
}
