/*
 * Created on 13 ¡.¾. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.util;

/**
 * @author amnuay
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class HexTool {

	public static void main(String[] args) {
		String sh ="6900a92110ca4b1059050000ff003af9be43cd7c0e0b05eb59050c84ff0029f9ffe1d03d0e0be5eb59050b38ff0019f95315e4fa0e2bb5eb59050da5ff0009f9a42dee880e2ba5eb590508c1ff00e9f9f02e02d70e2a85ec59050152ff00d9f92c12087b0e4a65ec59050b97ff00c9f95ea109d20e4a45ec59050dbbff00b9f9a99e0d6e0e4a15ec59050d8fff00adf9e5b8152d0e4a25ec59050db6ff0098f94ddc152d0e6ac5ec04b9307f";
		sh =sh.toUpperCase();
		for(int i=0;i<sh.length()/2;i++ ){
			System.out.print(sh.indexOf(0,i) +"\t");
		}
		System.out.println(sh.length());
		
	}
}
