package cn.jpush.protocal.push;

import cn.jpush.protocal.utils.ProtocolUtil;
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
		//this.buf.readBytes(16).array();
		return command;
	}
	
	public long getRid(){
		long rid = ProtocolUtil.byteArrayToLong(this.buf.readBytes(8).array());
		this.buf.readBytes(8).array();
		return rid;
	}
}
