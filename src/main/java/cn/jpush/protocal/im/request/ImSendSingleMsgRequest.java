package cn.jpush.protocal.im.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jpushim.s2b.JpushimSdk2B.Packet;
import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.ProtocolUtil;

public class ImSendSingleMsgRequest extends BaseRequest {
	private Packet protobuf;
	public ImSendSingleMsgRequest(int version, long rid, int sid, long juid, Packet protobuf) {
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
