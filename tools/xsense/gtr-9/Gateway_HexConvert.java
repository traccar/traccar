package org.siemens;

public class Gateway_HexConvert {
	
	public static int HextoInteger(String hex) throws Exception
	{
        int dec = 0;
        for (int i = 0; i < hex.length(); i++) {
           int digit = hexValue( hex.charAt(i) );
           if (digit == -1) {
        	   return -1;		// Error, Hexadecimal digits invalid.
           }
           dec = 16*dec + digit;
        }
        return dec;
	}
	
	public static int hexValue(char ch) throws Exception
	{
        // Returns the hexadecimal value of ch, or returns
        // -1 if ch is not one of the hexadecimal digits.
		switch (ch) {
			case '0':
				return 0;
			case '1':
        		return 1;
			case '2':
				return 2;
			case '3':
				return 3;
			case '4':
				return 4;
			case '5':
				return 5;
			case '6':
				return 6;
			case '7':
				return 7;
			case '8':
				return 8;
			case '9':
				return 9;
			case 'a':
			case 'A':
				return 10;
			case 'b':
			case 'B':
				return 11;
			case 'c':
        	case 'C':
        		return 12;
        	case 'd':
        	case 'D':
        		return 13;
        	case 'e':
        	case 'E':
        		return 14;
        	case 'f':
        	case 'F':
        		return 15;
        	default:
        		return -1;
		}
	}
	
	public static String HextoBinary(String hex)throws Exception
	{
		String hex2binary = "";
		
		for(int i=0; i<hex.length(); i++)
		{
			switch(hex.charAt(i))
			{
				case '0':
					hex2binary += "0000";
					break;
				case '1':
					hex2binary += "0001";
					break;
				case '2':
					hex2binary += "0010";
					break;
				case '3':
					hex2binary += "0011";
					break;
				case '4':
					hex2binary += "0100";
					break;
				case '5':
					hex2binary += "0101";
					break;
				case '6':
					hex2binary += "0110";
					break;
				case '7':
					hex2binary += "0111";
					break;
				case '8':
					hex2binary += "1000";
					break;
				case '9':
					hex2binary += "1001";
					break;
				case 'a':
				case 'A':
					hex2binary += "1010";
					break;
				case 'b':
				case 'B':
					hex2binary += "1011";
					break;
				case 'c':
	        	case 'C':
					hex2binary += "1100";
					break;
	        	case 'd':
	        	case 'D':
					hex2binary += "1101";
					break;
	        	case 'e':
	        	case 'E':
					hex2binary += "1110";
					break;
	        	case 'f':
	        	case 'F':
					hex2binary += "1111";
					break;
			}
		}
		return hex2binary;
	}

	
	
	public static String byte2HexString (byte[] bmsg, int bleng)throws Exception
	{
		String hexString = "";
		for(int i=0; i<bleng; i++)
		{
			int temp = (int)bmsg[i];
			temp &= 0xff;
			String tempHexString = Integer.toHexString(temp);
	        if(tempHexString.length() == 1)
	        {
	        	tempHexString = "0"+tempHexString;
	        }
	        tempHexString = tempHexString.toUpperCase();
	        hexString += tempHexString;
		}
		return hexString;
	}
	
	public static void string2hex (String str2hex)throws Exception
    {
    	String st = "this is roger";
    	
    	for(int i = 0; i<st.length(); i++){
    	System.out.println(); 
    	int ch=(int)st.charAt( i ); 
    	String s4="00"+Integer.toHexString( ch ); 
    	System.out.println(i + "output->"+s4); // String to Hex
    	}
    }
    
    public static String hex2string(String hex)throws Exception
    {
    	String temp = "";
    	for(int i=0; i<(hex.length()); i+=2)
    	{
    		String hex_buf = hex.substring(i, i+2);
    		if((hex_buf.equalsIgnoreCase("00")) || (hex_buf.equalsIgnoreCase("2C")) || (hex_buf.equalsIgnoreCase("27"))
    				|| (hex_buf.equalsIgnoreCase("22")) || (hex_buf.equalsIgnoreCase("60")))
    		{
    			// character like that , ' "
    		}
    		else
    		{
        		int intVal = Integer.parseInt(hex_buf, 16);
        		char charVal = (char) intVal;
        		temp += charVal;
    		}
    	}
		return ""+temp;
    }
    
    public static String hex2string_base_station (String hex)throws Exception
    {
    	String temp = "";
    	for(int i=0; i<(hex.length()); i+=2)
    	{
    		String hex_buf = hex.substring(i, i+2);
    		if(hex_buf.equalsIgnoreCase("00")) break;
    		if((hex_buf.equalsIgnoreCase("2C")) || (hex_buf.equalsIgnoreCase("27"))
    				|| (hex_buf.equalsIgnoreCase("22")) || (hex_buf.equalsIgnoreCase("60")))
    		{
    			// character like that , ' "
    		}
    		else
    		{
        		int intVal = Integer.parseInt(hex_buf, 16);
        		char charVal = (char) intVal;
        		temp += charVal;
    		}
    	}
		return ""+temp;
    }
    
    public static String hex2string_systemlog(String hex)throws Exception
    {
    	String temp = "";
    	for(int i=0; i<(hex.length()); i+=2)
    	{
    		String hex_buf = hex.substring(i, i+2);
    		if((hex_buf.equalsIgnoreCase("00")) || (hex_buf.equalsIgnoreCase("27"))
    				|| (hex_buf.equalsIgnoreCase("22")) || (hex_buf.equalsIgnoreCase("60")))
    		{
    			// character like that ' "
    		}
    		else
    		{
        		int intVal = Integer.parseInt(hex_buf, 16);
        		char charVal = (char) intVal;
        		temp += charVal;
    		}
    	}
		return ""+temp;
    }
    
    public String convertHexToString(String hex){

  	  StringBuilder sb = new StringBuilder();
  	  StringBuilder temp = new StringBuilder();
  	  
  	  //49204c6f7665204a617661 split into two characters 49, 20, 4c...
  	  for( int i=0; i<hex.length()-1; i+=2 ){
  		  
  	      //grab the hex in pairs
  	      String output = hex.substring(i, (i + 2));
  	      //convert hex to decimal
  	      int decimal = Integer.parseInt(output, 16);
  	      //convert the decimal to character
  	      sb.append((char)decimal);
  		  
  	      temp.append(decimal);
  	  }
  	  System.out.println("Decimal : " + temp.toString());
  	  
  	  return sb.toString();
    }
}
