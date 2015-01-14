package com.jpush.protocal.push;

public class PushLogoutResponseBean {
	private int response_code;

	public PushLogoutResponseBean(int response_code) {
		this.response_code = response_code;
	}

	public int getResponse_code() {
		return response_code;
	}

	public void setResponse_code(int response_code) {
		this.response_code = response_code;
	}
	
}
