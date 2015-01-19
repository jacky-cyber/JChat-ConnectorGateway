package com.jpush.protocal.push;

import com.jpush.protocal.utils.ProtocolUtil;

import io.netty.buffer.ByteBuf;

public class JHead {
	private ByteBuf buf;
	public JHead(ByteBuf buf){
		this.buf = buf;
	}
	public int getCommandInRequest(){
		this.buf.readBytes(3).array();
		int command = ProtocolUtil.byteArrayToInt(this.buf.readBytes(1).array());
		this.buf.readBytes(20);
		return command;
	}
	
	public int getCommandInResponse(){
		this.buf.readBytes(3).array();
		int command = ProtocolUtil.byteArrayToInt(this.buf.readBytes(1).array());
		this.buf.readBytes(16).array();
		return command;
	}
}
