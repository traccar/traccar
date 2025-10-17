/////// Louis on 02 April 2009  //////

package com.xsense.util;

import java.io.*;
import java.util.*;

import com.xsense.boxmanager.UDPServer;

public class ReadBoxConf extends TimerTask
{
	@Override
	public void run()
	{
		try
		{
			ReadBoxConf obj = new ReadBoxConf();
			UDPServer.s = obj.ReadConfigureFile();
			/*
			Set<String> s = obj.ReadConfigureFile();
			Iterator<String> it = s.iterator();
			String box_code = "1100009";
			
			while(it.hasNext())
			{
				String buf = it.next();
				if(buf.indexOf(box_code) != -1)
				{
					System.out.println(buf);
				}
			}
			*/
			System.out.println("Read Configure File");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
    public Set<String> ReadConfigureFile() throws Exception
    {
    	ReadBoxConf obj = new ReadBoxConf();
    	
		File ff = new File(UDPServer.conFigureFile);
		if(!ff.exists())
		{
			obj.createConfigureFile();
			System.out.println("Please configure Box_code, IP and Port at file >> \r\n" 
					+ff.getAbsolutePath()+"\r\n"
					+"The program will read this file every 5 minute.");
		}
		
    	Set<String> s = new HashSet<String>();
    	FileReader fr = new FileReader(UDPServer.conFigureFile);
    	BufferedReader br = new BufferedReader(fr);
    	String line = br.readLine();
    	
    	while(line != null)
    	{
    		line = line.trim();
    		if((line.length() > 0) && (!line.startsWith("#")))
    		{
    			s.add(line);
    		}
    		line = br.readLine();
    	}
    	br.close();
    	return s;
    	
    	/* example
		1100009,127.0.0.1,30050
		1100012,127.0.0.1,30050
		1100015,127.0.0.1,30050
		1100010,127.0.0.1,30050
		*/
    }
    
    public Set<String> ReadFirstConfigureFile() throws Exception
    {
    	ReadBoxConf obj = new ReadBoxConf();
    	
		File ff = new File(UDPServer.conFigureFile);
		if(!ff.exists())
		{
			obj.createConfigureFile();
		}
		
		System.out.println("Please configure Box_code, IP and Port at file >> \r\n" 
				+ff.getAbsolutePath()+"\r\n"
				+"The program will read this file every 5 minute.");
		
    	Set<String> s = new HashSet<String>();
    	FileReader fr = new FileReader(UDPServer.conFigureFile);
    	BufferedReader br = new BufferedReader(fr);
    	String line = br.readLine();
    	
    	while(line != null)
    	{
    		line = line.trim();
    		if((line.length() > 0) && (!line.startsWith("#")))
    		{
    			s.add(line);
    		}
    		line = br.readLine();
    	}
    	br.close();
    	return s;
    	
    	/* example
		1100009,127.0.0.1,30050
		1100012,127.0.0.1,30050
		1100015,127.0.0.1,30050
		1100010,127.0.0.1,30050
		*/
    }
    
    public void createConfigureFile() throws Exception
    {
    	File ff = new File(UDPServer.conFigureFile);
    	FileWriter fw = new FileWriter(ff,true);
    	BufferedWriter out = new BufferedWriter(fw);
    	out.write("#<box_code>,<IP address>,<Port>\r\n");
    	out.write("#Example 1100009,127.0.0.1,30150\r\n");
    	out.write("#Note, start with \"#\" mean comment this line.");
    	out.flush();
    	out.close();
    }
}
