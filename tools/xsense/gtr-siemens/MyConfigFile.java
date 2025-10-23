package org.siemens;

import java.util.Properties;
//import java.util.Locale;
import java.io.FileInputStream;
//import java.io.FileOutputStream;

public class MyConfigFile {
                public static Properties loadManagementProperties = null;
                public static final String PROPERTIES_FILENAME ="server.properties";
               
                private static String logdir ="";
                private static String manage_address ="";
                
               
                static {
                  //reads the properties file
                  try {
                        loadManagementProperties = MyConfigFile.getPropertiesFromFile( PROPERTIES_FILENAME);
          
	                    try {
	                          		
	                        logdir = loadManagementProperties.getProperty("logdir");
	                      }catch (Exception x) {} 
	                    
		                 try {    		
		                	 manage_address = loadManagementProperties.getProperty("manage_address");
			              }catch (Exception x) {}           
		                 
		                 
		                 
                  	}catch (Exception x) {
                         x.printStackTrace();
                  }
                } //end static block -----------------------------------------------------------------------
              
              
           
            

                public static final Properties getPropertiesFromFile(String filename) throws
                                  Exception {
                                Properties p = new Properties();
                                FileInputStream fis = new FileInputStream(filename); //throws FileNotFoundException, IOException
                                p.load(fis);
                                fis.close();
                                return p;
                  } //---------------------------------------------------------------------------

                public static  String getLogDir() {
                	return logdir;
                }
                
				public static String getManage_address() {
					return manage_address;
				}
				
				public static int getManagNumber() {
				
					return manage_address.split(",").length;
				}
				
				

				


                }


