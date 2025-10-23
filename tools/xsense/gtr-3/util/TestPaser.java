/*
 * Created on 9 ¡.Â. 2547
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.xsense.util;

/**
 * @author amnuay
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestPaser {
	public static void main(String a[]){
	   DataPaser dpaser =new DataPaser("I1024,100804,230317,1,13543391,100309249,1,00047,09055,,");
	   System.out.println(dpaser.getBoxID()+":"+dpaser.getDate()+" "+dpaser.getTime());
	   System.out.println(dpaser.getDateTime());
	   System.exit(0);
	}

}
