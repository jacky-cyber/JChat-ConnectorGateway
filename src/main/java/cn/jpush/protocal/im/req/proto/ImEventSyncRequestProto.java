package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import jpushim.s2b.JpushimSdk2B.EventNotification;
import cn.jpush.protocal.im.bean.LoginRequestBean;
import cn.jpush.protocal.utils.Command;

import com.google.protobuf.ByteString;

public class ImEventSyncRequestProto extends BaseProtobufRequest {
	private long rid;
	public ImEventSyncRequestProto(int cmd, int version, long uid, String appkey, long rid, int sid, long juid, 
			List<Integer> cookie, Object bean) {
		super(cmd, version, uid, appkey, sid, juid, cookie, bean);
		this.rid = rid;
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		EventNotification bean = (EventNotification) obj;
		bodyBuilder.setEventNotification(bean);
		protocalBuilder.setBody(bodyBuilder);
	}

	public long getRid() {
		return rid;
	}

	public void setRid(long rid) {
		this.rid = rid;
	}

}
