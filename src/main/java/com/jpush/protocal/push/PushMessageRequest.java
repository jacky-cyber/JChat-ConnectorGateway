package com.jpush.protocal.push;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.jpush.protocal.im.request.BaseRequest;
import com.jpush.protocal.utils.Command;
import com.jpush.protocal.utils.ProtocolUtil;

public class PushMessageRequest extends BaseRequest {
	private PushMessageRequestBean content;
	public PushMessageRequest(int version, long rid, int sid, long juid,  PushMessageRequestBean content) {
		super(version, rid, sid, juid);
		this.command = Command.KKPUSH_MESSAGE.COMMAND;
		this.content = content;
	}

	@Override
	public void buidRequestBody() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			bos.write(ProtocolUtil.intToByteArray(content.getMsgType(), 1));  
			bos.write(ProtocolUtil.longToByteArray(content.getMessageId(), 8));
			this.writeTLV2(bos, content.getMessage());
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
