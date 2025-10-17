/*
 * Created on 15 ¸.¤. 2547
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message.pack;

import java.text.DecimalFormat;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AlertStatus extends ExtendPosition{

	public AlertStatus(){
		nlatlongformatter = new DecimalFormat("#.000000");
		//ndigitalevent= new DecimalFormat("00000000");
		nspeed =new DecimalFormat("000.00");
		nspeed.setMaximumFractionDigits(2); 
		nspeed.setMaximumIntegerDigits(3); 
	}
	
	
}
