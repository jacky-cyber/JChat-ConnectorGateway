package com.jpush.protocal.im.requestproto;

import java.util.List;

import com.jpush.protobuf.Im;
import com.jpush.protobuf.Im.Protocol;
import com.jpush.protocal.utils.Command;

/*
 * IM 协议 Protobuf 结构请求封装
 */
public class BaseProtobufRequest {
	private int cmd;
	private int version;
	private long uid;
	private List<Integer> cookie;
	private Object bean;
	protected Im.Protocol.Builder protocalBuilder = Im.Protocol.newBuilder();
	public BaseProtobufRequest(int cmd, int version, long uid, List cookie, Object bean) {
		super();
		this.cmd = cmd;
		this.version = version;
		this.uid = uid;
		this.cookie = cookie;
		this.bean = bean;
	}
	
	public Protocol buildProtoBufProtocal(){
		this.buildHead();
		this.buildBody(this.bean);
		return protocalBuilder.build();
	}
	
	protected void buildBody(Object obj){}
	
	private void buildHead(){
		Im.ProtocolHead.Builder headBuilder = Im.ProtocolHead.newBuilder();
		headBuilder.setCmd(this.cmd);
		headBuilder.setVer(this.version);
		headBuilder.setUid(this.uid);
		Im.Cookie.Builder cookieBuilder = Im.Cookie.newBuilder();
		for(int i=0; i<cookie.size(); i++){
			cookieBuilder.addRes(cookie.get(i));
		}
		headBuilder.setCookie(cookieBuilder);
		protocalBuilder.setHead(headBuilder);
	}
	
	public int getCmd() {
		return cmd;
	}

	public void setCmd(int cmd) {
		this.cmd = cmd;
	}

	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}

	public List<Integer> getCookie() {
		return cookie;
	}

	public void setCookie(List<Integer> cookie) {
		this.cookie = cookie;
	}
	
}
