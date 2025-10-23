/*
 * Created on 10 ¡.Â. 2547
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.xsense.util;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.net.URL;
/**
 * @author amnuay
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PostAction extends Thread{
	String url;
	public PostAction(String url){
	  this.url=url;
	}
	public void run(){
	 try{
	 	
		getDataLogURL(url);
	 
	 }catch(Exception ex){}
	
	}
	public void getDataLogURL(String URL) {
			  try {
				ArrayList al = new ArrayList();
				URL url = new URL(URL);
				java.net.URLConnection connection = url.openConnection();
				if (connection instanceof java.net.HttpURLConnection) {
						java.net.HttpURLConnection httpConnection = (java.net.HttpURLConnection) connection;
						httpConnection.connect();
						BufferedReader br = new BufferedReader(new java.io.InputStreamReader(
						httpConnection.getInputStream()));
						  String line = "";
						  while ((line = br.readLine()) != null ) {
							al.add(line);
						   // System.out.println(isStop);
						  }

						  httpConnection.disconnect();

				}
			  }
			  catch (Exception ex) {
				System.out.println("Read Line URL error "+ex);
			  }

			}

}
