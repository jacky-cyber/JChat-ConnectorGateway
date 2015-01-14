package com.jpush.protocal.push;

public class PushRegResponseBean {
	private int response_code;
	private long uid;
	private String passwd;
	private String reg_id;
	private String device_id;
	public PushRegResponseBean(){}
	
	public PushRegResponseBean(int response_code, long uid, String passwd,
			String reg_id, String device_id) {
		this.response_code = response_code;
		this.uid = uid;
		this.passwd = passwd;
		this.reg_id = reg_id;
		this.device_id = device_id;
	}

	public int getResponse_code() {
		return response_code;
	}
	public void setResponse_code(int response_code) {
		this.response_code = response_code;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	public String getPasswd() {
		return passwd;
	}
	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}
	public String getReg_id() {
		return reg_id;
	}
	public void setReg_id(String reg_id) {
		this.reg_id = reg_id;
	}
	public String getDevice_id() {
		return device_id;
	}
	public void setDevice_id(String device_id) {
		this.device_id = device_id;
	}
	
}
