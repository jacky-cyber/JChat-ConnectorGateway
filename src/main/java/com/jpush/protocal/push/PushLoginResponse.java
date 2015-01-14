package com.jpush.protocal.push;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.jpush.protocal.common.Command;
import com.jpush.protocal.im.response.BaseResponse;
import com.jpush.protocal.utils.ProtocolUtil;

public class PushLoginResponse extends BaseResponse {
	private PushLoginResponseBean content;
	public PushLoginResponse(int version, long rid, long juid, PushLoginResponseBean bean) {
		super(version, rid, juid);
		this.command = Command.KKPUSH_LOGIN.COMMAND;
		this.content = bean;
	}
	
	@Override
	public void buidResponseBody() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			bos.write(ProtocolUtil.intToByteArray(content.getResponse_code(), 2));  
			bos.write(ProtocolUtil.longToByteArray(content.getSid(), 4));  
			bos.write(ProtocolUtil.longToByteArray(content.getServer_version(), 2));
			this.writeTLV2(bos, content.getSession_key()); // 2B的长度+数据内容
			bos.write(ProtocolUtil.longToByteArray(content.getServer_time(), 4));
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
