package cn.jpush.protocal.im.responseproto;

import jpushim.s2b.JpushimSdk2B.Packet;


public class BaseProtobufResponse {
	private int code = 0;
	private String message = "success";
	protected Packet protocol;
	public BaseProtobufResponse(Packet protocol) {
		this.protocol = protocol;
	}
	
	protected void buildResposneBody(){
		
	}
	
	public Packet getResponseProtocol(){
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
