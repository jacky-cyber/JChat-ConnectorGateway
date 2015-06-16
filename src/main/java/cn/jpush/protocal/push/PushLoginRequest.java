package cn.jpush.protocal.push;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import cn.jpush.protocal.im.request.BaseRequest;
import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.ProtocolUtil;

public class PushLoginRequest extends BaseRequest {
	private PushLoginRequestBean content;
	public PushLoginRequest(int version, long rid, int sid, long juid, PushLoginRequestBean bean) {
		super(version, rid, sid, juid);
		this.command = Command.KKPUSH_LOGIN.COMMAND;
		this.content = bean;
	}

	@Override
	public void buidRequestBody() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try{
			bos.write(ProtocolUtil.copyArray(content.getFrom_resource().getBytes(), 4)); 
			this.writeTLV2(bos, content.getPasswdmd5());
			bos.write(ProtocolUtil.intToByteArray(content.getClient_version(), 4));
			this.writeTLV2(bos, content.getAppkey());
			bos.write(ProtocolUtil.intToByteArray(content.getPlayform(), 1));
			bos.write(ProtocolUtil.intToByteArray(0, 1));
			this.mBody = bos.toByteArray();
		} catch (Exception e) {
			try {
				bos.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public String toString() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("version", this.version);
		map.put("command", this.command);
		map.put("rid", this.rid);
		map.put("sid", this.sid);
		map.put("juid", this.juid);
		map.put("from_resource", content.getFrom_resource());
		map.put("pwd", content.getPasswdmd5());
		map.put("client_version", content.getClient_version());
		map.put("appkey", content.getAppkey());
		map.put("platform", content.getPlayform());
		map.put("flag", 64);
		Gson gson = new Gson();
		return gson.toJson(map);
	}

}
