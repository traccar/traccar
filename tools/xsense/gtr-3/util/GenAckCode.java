/////// Louis on 31 March 2009  //////

package com.xsense.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import com.xsense.boxmanager.UDPServer;

public class GenAckCode {
	
	// generate code for function IP and port.
	public String genCode(int _box_code, String _ip_ads, int _port) throws Exception
	{
		String str_return = "";
		int _size = 0;
		int _version = 2;
		int _type = 113;
		_ip_ads = _ip_ads.replace(".", ",");
		String _ip[] = _ip_ads.split(",");
		
		if(((""+_box_code).length() == 7) && (_ip.length == 4))
		{
			if((_ip[0].length() <= 3) && (_ip[1].length() <= 3) 
					&& (_ip[2].length() <= 3) && (_ip[3].length() <= 3))
			{
				int _ip0 = Integer.parseInt(_ip[0]);
				int _ip1 = Integer.parseInt(_ip[1]);
				int _ip2 = Integer.parseInt(_ip[2]);
				int _ip3 = Integer.parseInt(_ip[3]);
				
				String sof = "!#";
				String ver = (""+_version).toUpperCase();
				String box_code = (""+Integer.toHexString(_box_code)).toUpperCase();
				String type = ""+String.valueOf(Character.toChars(_type));
				String ip0 = findIP(""+Integer.toHexString(_ip0)).toUpperCase();
				String ip1 = findIP(""+Integer.toHexString(_ip1)).toUpperCase();
				String ip2 = findIP(""+Integer.toHexString(_ip2)).toUpperCase();
				String ip3 = findIP(""+Integer.toHexString(_ip3)).toUpperCase();
				String port = findPort(""+Integer.toHexString(_port)).toUpperCase();
				_size = (ver+box_code+type+ip0+ip1+ip2+ip3+port).length() +2; // 2 = crc
				String size = findSize(Integer.toHexString(_size)).toUpperCase();
				String msg = sof+size+ver+box_code+type+ip0+ip1+ip2+ip3+port;
				String csum = myCRC(msg);
				str_return = (msg += csum);
			}
			else
			{
				str_return = "IP address are error.";
			}
		}
		else
		{
			str_return = "box_code or IP address are error.";
		}
		return str_return;
	}
	
	public String myCRC(String str) throws Exception
	{
        char crc = 0;
        for (int i = 0; i < str.length(); i++)
        {
        	crc = (char) (crc^str.charAt(i));
        }
        return ""+Integer.toHexString(crc).toUpperCase();
	}
	
	public String findIP(String ip) throws Exception
	{
		int a = 2-ip.length();
		for(int i=0; i<a; i++)
		{
			ip = "0"+ip;
		}
		return ip;
	}
	
	public String findSize(String size) throws Exception
	{
		int a = 4-size.length();
		for(int i=0; i<a; i++)
		{
			size = "0"+size;
		}
		return size;
	}
	
	public String findPort(String port) throws Exception
	{
		int a = 4-port.length();
		for(int i=0; i<a; i++)
		{
			port = "0"+port;
		}
		return port;
	}
	
    public void createLogGenAckError(String box_code, String ip, int port, String gencode) throws Exception
    {
    	File ff = new File(UDPServer.GenAckErrorFile);
    	FileWriter fw = new FileWriter(ff,true);
    	BufferedWriter out = new BufferedWriter(fw);
    	out.write(box_code+","+ip+","+port+","+gencode+"\r\n");
    	out.flush();
    	out.close();
    }
    
    public void createLogGenAckComplete(String box_code, String ip, int port, String gencode) throws Exception
    {
    	File ff = new File(UDPServer.GenAckCompleteFile);
    	FileWriter fw = new FileWriter(ff,true);
    	BufferedWriter out = new BufferedWriter(fw);
    	out.write(box_code+","+ip+","+port+","+gencode+"\r\n");
    	out.flush();
    	out.close();
    }
}
