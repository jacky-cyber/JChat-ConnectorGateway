package cn.jpush.protocal.push;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import cn.jpush.protocal.im.request.BaseRequest;
import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.ProtocolUtil;

public class PushRegRequest extends BaseRequest {
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
			this.writeTLV2(bos, content.getStrKey());
			this.writeTLV2(bos, content.getStrApkVersion());
			this.writeTLV2(bos, content.getStrClientInfo());
			this.writeTLV2(bos, content.getStrDeviceToken());
			bos.write(ProtocolUtil.intToByteArray(content.getBuild_type(), 1));
			bos.write(ProtocolUtil.intToByteArray(content.getAps_type(), 1));
			bos.write(ProtocolUtil.intToByteArray(content.getPlatform(), 1));
			this.writeTLV2(bos, content.getStrKeyExt());
			bos.write(ProtocolUtil.intToByteArray(1, 1));
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
		Gson gson = new Gson();
		Map data = new HashMap();
		data.put("version", this.version);
		data.put("rid", this.rid);
		data.put("content", gson.toJson(content));
		return gson.toJson(data);
	}

}
