package com.jpush.protocal.push;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.jpush.protocal.common.Command;
import com.jpush.protocal.im.response.BaseResponse;
import com.jpush.protocal.utils.ProtocolUtil;

public class PushLogoutResponse extends BaseResponse {
	private PushLogoutResponseBean content;
	public PushLogoutResponse(int version, long rid, long juid, PushLogoutResponseBean bean) {
		super(version, rid, juid);
		this.command = Command.KKPUSH_LOGOUT.COMMAND;
		this.content = bean;
	}
	
	@Override
	public void buidResponseBody() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			bos.write(ProtocolUtil.intToByteArray(content.getResponse_code(), 2));  
			this.mBody = bos.toByteArray();
		} catch (Exception e) {
			try {
				bos.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

}
