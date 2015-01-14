package com.jpush.protocal.im.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.jpush.protobuf.Im.Protocol;
import com.jpush.protocal.common.Command;
import com.jpush.protocal.utils.ProtocolUtil;

public class ImDeleteGroupMemberRequest extends BaseRequest {
	private Protocol protobuf;
	public ImDeleteGroupMemberRequest(int version, long rid, int sid, long juid, Protocol protobuf) {
		super(version, rid, sid, juid);
		this.command = Command.JPUSH_IM.COMMAND;
		this.protobuf = protobuf;
	}

	@Override
	public void buidRequestBody() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			bos.write(this.protobuf.toByteArray());
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
