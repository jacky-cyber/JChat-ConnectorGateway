package com.jpush.protocal.push;

import com.jpush.protocal.im.response.BaseResponse;

public class HeartBeatResponse extends BaseResponse {

	public HeartBeatResponse(int version, long rid, long juid, int command) {
		super(version, rid, juid);
		this.command = command;
		// TODO Auto-generated constructor stub
	}

}
