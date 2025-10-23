/*
 * Created on 1 Ê.¤. 2548
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message.pack;

import com.xsense.util.HexString;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class BaseStation {
	private byte[] LTCell; //2 byte
	//private byte[] Cell_ID; //4 byte
	private byte[] LAC; //2byte
	private byte[] CI; //2byte
	private byte[] Ta;//1 byte
	private byte[] Tc;// 1 byte
	private byte[] LTbs;//2 byte
	private byte[] Base_Station;// 32 byte
	public void setLTCell(byte[] LTCell /*2 byte*/){
		this.LTCell =LTCell;
	}
	//public void setCell_ID(byte[] Cell_ID/*4 byte*/){
	//	this.Cell_ID=Cell_ID;
	//}
	public void setLAC(byte[] LAC/*4 byte*/){
		this.LAC=LAC;
	}
	public void setCI(byte[] CI/*4 byte*/){
		this.CI=CI;
	}
	public void setTa(byte[] Ta/*1 byte*/){
		this.Ta=Ta;
	}
	public void setTc(byte[] Tc/*1 byte*/){
		this.Tc=Tc;
	}
	public void setLTbs(byte[] LTbs/*1 byte*/){
		this.LTbs=LTbs;
	}
	public void setBase_Station(byte[] Base_Station/*24 byte*/){
		this.Base_Station=Base_Station;
	}
	
	public long getLTCell(){
		return HexString.HextoInteger(HexString.HextoString(LTCell));
	}
	//public long getCell_ID(){
	//	return HexString.HextoInteger(HexString.HextoString(Cell_ID));
	//}
	public long getLAC(){
		return HexString.HextoInteger(HexString.HextoString(LAC));
	}
	public long getCI(){
		return HexString.HextoInteger(HexString.HextoString(CI));
	}
	public long getTa(){
		return HexString.HextoInteger(HexString.HextoString(Ta));
	}
	public long getTc(){
		return HexString.HextoInteger(HexString.HextoString(Tc));
	}
	public long getLTbs(){
		return HexString.HextoInteger(HexString.HextoString(LTbs));
	}
	public String getBase_Station(){
		String s =new String(Base_Station);
		s = s.replaceAll("\'","");
		return s;
	}	
}
