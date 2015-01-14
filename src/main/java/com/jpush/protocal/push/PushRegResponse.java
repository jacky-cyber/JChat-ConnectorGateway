package com.jpush.protocal.push;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.jpush.protocal.common.Command;
import com.jpush.protocal.im.response.BaseResponse;
import com.jpush.protocal.utils.ProtocolUtil;

public class PushRegResponse extends BaseResponse {
	private PushRegResponseBean content;
	public PushRegResponse(int version, long rid, long juid, PushRegResponseBean bean) {
		super(version, rid, juid);
		this.command = Command.KKPUSH_REG.COMMAND;
		this.content = bean;
	}
	
	@Override
	public void buidResponseBody() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			bos.write(ProtocolUtil.intToByteArray(content.getResponse_code(), 2));  
			bos.write(ProtocolUtil.longToByteArray(content.getUid(), 8));  
			this.writeTLV2(bos, content.getPasswd()); // 2B的长度+数据内容
			this.writeTLV2(bos, content.getReg_id());
			this.writeTLV2(bos, content.getDevice_id());
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
