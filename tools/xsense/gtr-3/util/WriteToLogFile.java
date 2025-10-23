/*
 * Created on 21 ¡.Â. 2547
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.xsense.util;
import java.io.*;
/**
 * @author amnuay
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
//import javax.swing.text.DateFormatter;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
//import java.util.Date;

public class WriteToLogFile {
	public static void writeOutput(String output)
			{
		
				 try 
				 {
					  File logFile = new File("data.log");
					  BufferedWriter out = new BufferedWriter(new FileWriter(logFile, true));
					out.write(getDateTime()+"\n");
					  out.write(output);
					  out.write("\r\n");
					  out.close();
				 } 
     
				 catch (IOException e) 
				 {
				 }
			}
	public static void writeOutput(String filename,String output)
	{
		 try 
		 {
			  File logFile = new File(filename);
			  BufferedWriter out = new BufferedWriter(new FileWriter(logFile, true));
			out.write(getDateTime()+"\n");
			  out.write(output+"\n");
			  //out.write("\r\n");
			  out.close();
		 } 

		 catch (IOException e) 
		 {
		 }
	}	
	public static void writeOutput2(String filename,String output)
	{
		 try 
		 {
			  File logFile = new File(filename);
			  BufferedWriter out = new BufferedWriter(new FileWriter(logFile, true));
			//out.write(getDateTime()+"\n");
			  out.write(output+"\n");
			  //out.write("\r\n");
			  out.close();
		 } 

		 catch (IOException e) 
		 {
		 }
	}		
	public static String getDateTime(){
			  //Locale locale = Locale.US;
			  DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
			  return  formatter.format(new Date());
	
	}
	public static String getDate(){
		  //Locale locale = Locale.US;
		  DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		  return  formatter.format(new Date());

}
	public static void readRemoveFile(String filename){
		 try 
		 {
		File logFile = new File(filename);
		BufferedReader br =new BufferedReader(new FileReader(logFile));
		FileOutputStream fos =new FileOutputStream(logFile);
		String line = "";
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		   line = line.replaceAll("\\r\\n", "");
		    fos.write(line.getBytes());
		}
		br.close();
		fos.close();
		 } catch (IOException e) 
		 {
		 }
		
	}
	
}
