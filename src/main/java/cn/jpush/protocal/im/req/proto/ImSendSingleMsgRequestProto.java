package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import cn.jpush.protocal.im.bean.SendSingleMsgRequestBean;

import com.google.protobuf.ByteString;

public class ImSendSingleMsgRequestProto extends BaseProtobufRequest {
	private long rid;
	public ImSendSingleMsgRequestProto(int cmd, int version, long uid,
			String appkey, int sid, long juid, long rid, List cookie, Object bean) {
		super(cmd, version, uid, appkey, sid, juid, cookie, bean);
		this.rid = rid;
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		SendSingleMsgRequestBean bean = (SendSingleMsgRequestBean) obj;
		JpushimSdk2B.SingleMsg.Builder singleBuilder = JpushimSdk2B.SingleMsg.newBuilder();
		singleBuilder.setTargetUid(bean.getTarget_uid());
		JpushimSdk2B.MessageContent.Builder msgContent = JpushimSdk2B.MessageContent.newBuilder();
		msgContent.setContent(ByteString.copyFromUtf8(bean.getMsg_content()));
		singleBuilder.setContent(msgContent);
		bodyBuilder.setSingleMsg(singleBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

	public long getRid() {
		return rid;
	}

	public void setRid(long rid) {
		this.rid = rid;
	}

}
