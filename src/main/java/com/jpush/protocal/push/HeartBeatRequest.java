package com.jpush.protocal.push;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.jpush.protocal.common.Command;
import com.jpush.protocal.im.request.BaseRequest;
import com.jpush.protocal.utils.ProtocolUtil;

public class HeartBeatRequest extends BaseRequest {
	private int cmd = 2;  //  push login command
	private int ver = 2;
	public HeartBeatRequest(int version, long rid, int sid, long juid, int ver) {
		super(version, rid, sid, juid);
		this.command = Command.KKPUSH_HEARTBEAT.COMMAND;
		this.ver = ver;
	}

	@Override
	public void buidRequestBody() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			bos.write(ProtocolUtil.intToByteArray(this.cmd, 1));  
			bos.write(ProtocolUtil.intToByteArray(this.ver, 1));
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
