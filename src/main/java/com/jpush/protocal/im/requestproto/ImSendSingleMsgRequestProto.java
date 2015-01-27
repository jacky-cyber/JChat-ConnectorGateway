package com.jpush.protocal.im.requestproto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;

import com.google.protobuf.ByteString;
import com.jpush.protocal.im.bean.SendSingleMsgRequestBean;

public class ImSendSingleMsgRequestProto extends BaseProtobufRequest {

	public ImSendSingleMsgRequestProto(int cmd, int version, long uid,
			String appkey, List cookie, Object bean) {
		super(cmd, version, uid, appkey, cookie, bean);
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

}
