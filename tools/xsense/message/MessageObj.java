/*
 * Created on 30 ¾.Â. 2547
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.xsense.message;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MessageObj{
	byte[] type;
	int fullmsgsize;
	byte[] fullmsg;
	byte[] datasize;
	byte[] datamsg;
	byte[] version;
	byte[] tid;
	byte[] seqno;
	byte[] extend;
	byte[] crc16ccitt;

	boolean chk = false;
	public void setType(byte[] type){
		this.type=type;
	}
	public void setFullMsgSize(int fullmsgsize){
		this.fullmsgsize=fullmsgsize;
	}
	public void setFullMsg(byte[] fullmsg){
		this.fullmsg=fullmsg;
	}
	public void setDataSize(byte[] datasize){
		this.datasize=datasize;
	}
	public void setDataMsg(byte[] datamsg){
		this.datamsg=datamsg;
	}
	
	
	public void setVersion(byte[] version){
		this.version=version;
	}
	public void setTID(byte[] tid){
		this.tid=tid;
	}
	public void setSeqNO(byte[] seqno){
		this.seqno=seqno;
	}
	
	public void setExtends(byte[] extend){
		this.extend=extend;
	}
	public void setCRC16CCITT(byte[] crc16ccitt){
		this.crc16ccitt=crc16ccitt;
	}	
	public void setCHK(boolean chk){
		this.chk=chk;
	}
	
	
	public byte[] getType(){
		return type;
	}
	public int getFullMsgSize(){
		return fullmsgsize;
	}
	public byte[] getFullMsg(){
		return fullmsg;
	}
	public byte[] getDataSize(){
		return datasize;
	}
	public byte[] getDataMsg(){
		return datamsg;
	}
	
	public byte[] getVersion(){
		return version;
	}
	public byte[] getTID(){
		return tid;
	}
	public byte[]  getSeqNO(){
		return seqno;
	}

	public byte[] getExtends(){
		return extend;
	}
	public byte[] getCRC16CCITT(){
		return crc16ccitt;
	}
	public boolean IsOK(){
		return chk;
	}
	
	
}
