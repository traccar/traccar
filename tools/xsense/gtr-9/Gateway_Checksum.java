package org.siemens;

public final class Gateway_Checksum
{
	public static String CRC16(byte[] testBytes)throws Exception
	{
			short crc = (short) 0xFFFF;       // initial contents of LFBSR
			///byte[] testBytes = "123456789".getBytes("ASCII");
		
			for (int j = 0; j < testBytes.length; j++)
			{
				byte c = testBytes[j];
				for (int i = 0; i < 8; i++)
				{
					boolean c15 = ((crc >> 15      & 1) == 1);
					boolean bit = ((c   >> (7 - i) & 1) == 1);
					crc <<= 1;
					if (c15 ^ bit) crc ^= 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)
				}
			}
			
			String scrc =Integer.toHexString(crc);
			if(scrc.length()>4)
			{
				scrc=scrc.substring(scrc.length()-4,scrc.length());
			}
			
			else if(scrc.length()== 3)
			{
				scrc = "0"+scrc;
			}
			
			else if(scrc.length()== 2)
			{
				scrc = "00"+scrc;
			}
			
			else if(scrc.length() == 1)
			{
				scrc = "000"+scrc;
			}
			
			return scrc;
	}
}