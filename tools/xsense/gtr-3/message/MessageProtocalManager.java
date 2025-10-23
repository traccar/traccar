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
public class MessageProtocalManager {
	MessageObj msgObj;
	
	
	public MessageProtocalManager(byte[] msg,int size){
		//System.out.println(size);
		MessageDecode msgdecode =new MessageDecode(msg,size);
		msgObj=msgdecode.getMessageObject();
		System.out.println("MessageProtocalManager > "+msgObj.IsOK());
	   
	}
	
	public void setMessageObj(MessageObj msgObj){
		this.msgObj=msgObj;
	}
	public MessageObj getMessageObj(){
		return msgObj;
	}
	
}
