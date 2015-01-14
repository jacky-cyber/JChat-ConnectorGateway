package com.jpush.protocal.push;

public class PushLoginResponseBean {
	private int response_code;
	private int sid;
	private int server_version;
	private String session_key;
	private int server_time;
	public PushLoginResponseBean(){}
	public PushLoginResponseBean(int response_code, int sid,
			int server_version, String session_key, int server_time) {
		this.response_code = response_code;
		this.sid = sid;
		this.server_version = server_version;
		this.session_key = session_key;
		this.server_time = server_time;
	}
	public int getResponse_code() {
		return response_code;
	}
	public void setResponse_code(int response_code) {
		this.response_code = response_code;
	}
	public int getSid() {
		return sid;
	}
	public void setSid(int sid) {
		this.sid = sid;
	}
	public int getServer_version() {
		return server_version;
	}
	public void setServer_version(int server_version) {
		this.server_version = server_version;
	}
	public String getSession_key() {
		return session_key;
	}
	public void setSession_key(String session_key) {
		this.session_key = session_key;
	}
	public int getServer_time() {
		return server_time;
	}
	public void setServer_time(int server_time) {
		this.server_time = server_time;
	}
	
	
}
