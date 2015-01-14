package com.jpush.protocal.push;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.jpush.protocal.common.Command;
import com.jpush.protocal.im.request.BaseRequest;
import com.jpush.protocal.utils.ProtocolUtil;

public class PushLogoutRequest extends BaseRequest {
	private int cmd = 5;  //  push logout command
	public PushLogoutRequest(int version, long rid, int sid, long juid) {
		super(version, rid, sid, juid);
		this.command = Command.KKPUSH_LOGOUT.COMMAND;
	}

	@Override
	public void buidRequestBody() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			bos.write(ProtocolUtil.intToByteArray(this.cmd, 1));  
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
