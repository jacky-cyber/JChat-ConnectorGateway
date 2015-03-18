package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import jpushim.s2b.JpushimSdk2B.EventNotification;
import cn.jpush.protocal.im.bean.LoginRequestBean;
import cn.jpush.protocal.utils.Command;

import com.google.protobuf.ByteString;

public class ImEventSyncRequestProto extends BaseProtobufRequest {
	private int sid;
	private long juid;

	public ImEventSyncRequestProto(int cmd, int version, long uid, String appkey, int sid, long juid, 
			List<Integer> cookie, Object bean) {
		super(cmd, version, uid, appkey, cookie, bean);
		this.sid = sid;
		this.juid = juid;
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		EventNotification bean = (EventNotification) obj;
		bodyBuilder.setEventNotification(bean);
		protocalBuilder.setBody(bodyBuilder);
	}

	public int getSid() {
		return sid;
	}

	public void setSid(int sid) {
		this.sid = sid;
	}

	public long getJuid() {
		return juid;
	}

	public void setJuid(long juid) {
		this.juid = juid;
	}

}
