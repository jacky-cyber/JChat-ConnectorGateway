package cn.jpush.protocal.push;

import cn.jpush.protocal.im.request.BaseRequest;
import cn.jpush.protocal.utils.Command;

public class HeartBeatRequest extends BaseRequest {

	public HeartBeatRequest(int version, long rid, int sid, long juid, int ver) {
		super(version, rid, sid, juid);
		this.command = Command.KKPUSH_HEARTBEAT.COMMAND;
	}

	@Override
	public void buidRequestBody() {
		/*ByteArrayOutputStream bos = new ByteArrayOutputStream();
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
		}*/
	}

}