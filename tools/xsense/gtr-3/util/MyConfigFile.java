package com.xsense.util;

import java.util.Properties;
//import java.util.Locale;
import java.io.FileInputStream;
//import java.io.FileOutputStream;

public class MyConfigFile {
                public static Properties loadManagementProperties = null;
                public static final String PROPERTIES_FILENAME ="server.properties";
               
                //private static String logdir ="";
                private static String system_log ="";
                private static String data_log ="";
                private static String protocol_db ="";
                private static String protocol_fleet ="";
                private static String protocol_realtime ="";
                private static String protocol_remote ="";
                private static String protocol_file ="";
                private static String gps_addr="";
                private static int gps_port=0;
                private static String gps_buffer="../buffer/buffer.log";
                
                private static String manage_address ="";
                static {
                  //reads the properties file
                  try {
                        loadManagementProperties = MyConfigFile.getPropertiesFromFile( PROPERTIES_FILENAME);
          
	                    try {
	                          		
	                       // logdir = loadManagementProperties.getProperty("logdir");
	                      }catch (Exception x) {} 
	                    try {
                        		
	                    		system_log = loadManagementProperties.getProperty("system_log");
		                 	}catch (Exception x) {} 
		                 try {
                     		
		                    	data_log = loadManagementProperties.getProperty("data_log");
			                 }catch (Exception x) {} 
	                    try {
                      		
	                    	protocol_db = loadManagementProperties.getProperty("protocol.db.sv");
	                      }catch (Exception x) {} 
	                    try {
                      		
	                    	protocol_fleet = loadManagementProperties.getProperty("protocol.fleet.sv");
	                      }catch (Exception x) {} 
	                    try {
                      		
	                    	protocol_realtime = loadManagementProperties.getProperty("protocol.realttime.sv");
	                      }catch (Exception x) {} 
	                    try {
                      		
	                    	protocol_remote = loadManagementProperties.getProperty("protocol.remote.sv");
	                      }catch (Exception x) {} 
	                    try {
                      		
	                    	protocol_file = loadManagementProperties.getProperty("protocol.file.sv");
	                      }catch (Exception x) {}  
	                     try {
	                      		
		                    gps_addr = loadManagementProperties.getProperty("gps.addr");
		                    }catch (Exception x) {}  
		                 try {
		                      		
			                 gps_port = Integer.parseInt(loadManagementProperties.getProperty("gps.port"));
			              }catch (Exception x) {}   
			              try {
	                      		
				                 gps_buffer = loadManagementProperties.getProperty("gps.buffer");
				            }catch (Exception x) {} 
			              
			              try {    		
			                	 manage_address = loadManagementProperties.getProperty("manage_address");
				              }catch (Exception x) {}           
			                 
			              
                  	}catch (Exception x) {
                         x.printStackTrace();
                  }
                } //end static block -----------------------------------------------------------------------
              
              
           /* public static  String getLogDir() {
            	return logdir;
            }*/
            

                public static final Properties getPropertiesFromFile(String filename) throws
                                  Exception {
                                Properties p = new Properties();
                                FileInputStream fis = new FileInputStream(filename); //throws FileNotFoundException, IOException
                                p.load(fis);
                                fis.close();
                                return p;
                  } //---------------------------------------------------------------------------


				public static String getProtocol_db() {
					return protocol_db;
				}


				public static void setProtocol_db(String protocol_db) {
					MyConfigFile.protocol_db = protocol_db;
				}


				public static String getProtocol_file() {
					return protocol_file;
				}


				public static void setProtocol_file(String protocol_file) {
					MyConfigFile.protocol_file = protocol_file;
				}


				public static String getProtocol_fleet() {
					return protocol_fleet;
				}


				public static void setProtocol_fleet(String protocol_fleet) {
					MyConfigFile.protocol_fleet = protocol_fleet;
				}


				public static String getProtocol_realtime() {
					return protocol_realtime;
				}


				public static void setProtocol_realtime(String protocol_realtime) {
					MyConfigFile.protocol_realtime = protocol_realtime;
				}


				public static String getProtocol_remote() {
					return protocol_remote;
				}


				public static void setProtocol_remote(String protocol_remote) {
					MyConfigFile.protocol_remote = protocol_remote;
				}


				public static String getGps_addr() {
					return gps_addr;
				}


				public static void setGps_addr(String gps_addr) {
					MyConfigFile.gps_addr = gps_addr;
				}


				public static int getGps_port() {
					return gps_port;
				}


				public static void setGps_port(int gps_port) {
					MyConfigFile.gps_port = gps_port;
				}


				public static String getGps_buffer() {
					return gps_buffer;
				}


				public static void setGps_buffer(String gps_buffer) {
					MyConfigFile.gps_buffer = gps_buffer;
				}


				public static String getData_log() {
					return data_log;
				}


				public static String getSystem_log() {
					return system_log;
				}
				
				public static String getManage_address() {
					return manage_address;
				}
				
				public static int getManagNumber() {
				
					return manage_address.split(",").length;
				}

                }


