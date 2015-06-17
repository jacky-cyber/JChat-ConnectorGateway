package cn.jpush.protocal.im.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jpushim.s2b.JpushimSdk2B.Packet;
import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.ProtocolUtil;
/**
 * JPush 创建群组请求协议包
 * 详细内容参考jpush wiki文档
 */
public class ImCreateGroupRequest extends BaseRequest {
	private Packet protobuf;
	public ImCreateGroupRequest(int version, long rid, int sid, long juid, Packet protobuf) {
		super(version, rid, sid, juid);
		this.command = Command.JPUSH_IM.COMMAND;
		this.protobuf = protobuf;
	}

	@Override
	public void buidRequestBody() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			byte[] bodyBytes = this.protobuf.toByteArray();
			bos.write(ProtocolUtil.intToByteArray(bodyBytes.length, 2));
			bos.write(bodyBytes);
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
