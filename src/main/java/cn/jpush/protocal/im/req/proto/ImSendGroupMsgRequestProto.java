package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import cn.jpush.protocal.im.bean.SendGroupMsgRequestBean;

import com.google.protobuf.ByteString;

public class ImSendGroupMsgRequestProto extends BaseProtobufRequest {
	private long rid;
	public ImSendGroupMsgRequestProto(int cmd, int version, long uid,
			String appkey, int sid, long juid, long rid, List cookie, Object bean) {
		super(cmd, version, uid, appkey, sid, juid, cookie, bean);
		this.rid = rid;
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		SendGroupMsgRequestBean bean = (SendGroupMsgRequestBean) obj;
		JpushimSdk2B.GroupMsg.Builder groupBuilder = JpushimSdk2B.GroupMsg.newBuilder();
		groupBuilder.setTargetGid(bean.getTarget_gid());
		JpushimSdk2B.MessageContent.Builder msgContent = JpushimSdk2B.MessageContent.newBuilder();
		msgContent.setContent(ByteString.copyFromUtf8(bean.getMsg_content()));
		groupBuilder.setContent(msgContent);
		bodyBuilder.setGroupMsg(groupBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

	public long getRid() {
		return rid;
	}

	public void setRid(long rid) {
		this.rid = rid;
	}

}
