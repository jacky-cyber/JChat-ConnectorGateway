package cn.jpush.protocal.im.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jpushim.s2b.JpushimSdk2B.Packet;


public class ImDeleteGroupMemberResponse extends BaseResponse {
	
	private Packet protocol;
	public ImDeleteGroupMemberResponse(int version, long rid, long juid, Packet protocol) {
		super(version, rid, juid);
		this.protocol = protocol;
	}
	
	@Override
	public void buidResponseBody() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			bos.write(protocol.toByteArray());
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
