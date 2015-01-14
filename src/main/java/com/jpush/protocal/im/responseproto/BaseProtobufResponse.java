package com.jpush.protocal.im.responseproto;

import com.jpush.protobuf.Im.Protocol;

public class BaseProtobufResponse {
	private int code = 0;
	private String message = "success";
	protected Protocol protocol;
	public BaseProtobufResponse(Protocol protocol) {
		this.protocol = protocol;
	}
	
	protected void buildResposneBody(){
		
	}
	
	public Protocol getResponseProtocol(){
		this.buildResposneBody();
		return protocol;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public BaseProtobufResponse setMessage(String message) {
		this.message = message;
		return this;
	}
	
}
