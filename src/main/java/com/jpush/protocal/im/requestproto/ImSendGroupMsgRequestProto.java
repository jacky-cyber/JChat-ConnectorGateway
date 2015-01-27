package com.jpush.protocal.im.requestproto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;

import com.google.protobuf.ByteString;
import com.jpush.protocal.im.bean.SendGroupMsgRequestBean;

public class ImSendGroupMsgRequestProto extends BaseProtobufRequest {
	public ImSendGroupMsgRequestProto(int cmd, int version, long uid,
			String appkey, List cookie, Object bean) {
		super(cmd, version, uid, appkey, cookie, bean);
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

}
