package com.jpush.protocal.push;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.jpush.protocal.im.request.BaseRequest;
import com.jpush.protocal.utils.Command;
import com.jpush.protocal.utils.ProtocolUtil;

public class PushRegRequest extends BaseRequest {
	private int cmd = 0;  //  push reg command
	private PushRegRequestBean content;
	public PushRegRequest(int version, long rid, int sid, long juid, PushRegRequestBean bean) {
		super(version, rid, sid, juid);
		this.command = Command.KKPUSH_REG.COMMAND;
		this.content = bean;
	}

	@Override
	public void buidRequestBody() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			bos.write(ProtocolUtil.intToByteArray(this.cmd, 1));  
			this.writeTLV2(bos, content.getStrKey());
			this.writeTLV2(bos, content.getStrApkVersion());
			this.writeTLV2(bos, content.getStrClientInfo());
			this.writeTLV2(bos, content.getStrDeviceToken());
			bos.write(ProtocolUtil.intToByteArray(content.getBuild_type(), 1));
			bos.write(ProtocolUtil.intToByteArray(content.getAps_type(), 1));
			bos.write(ProtocolUtil.intToByteArray(content.getPlatform(), 1));
			this.writeTLV2(bos, content.getStrKeyExt());
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
