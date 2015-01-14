package com.jpush.protocal.im.requestproto;

import java.util.List;

import com.jpush.protobuf.Im;
import com.jpush.protobuf.Message;
import com.jpush.protocal.im.bean.SendSingleMsgRequestBean;

public class ImSendSingleMsgRequestProto extends BaseProtobufRequest {

	public ImSendSingleMsgRequestProto(int cmd, int version, long uid,
			List cookie, Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		Im.ProtocolBody.Builder bodyBuilder = Im.ProtocolBody.newBuilder();
		SendSingleMsgRequestBean bean = (SendSingleMsgRequestBean) obj;
		Message.SingleMsg.Builder singleBuilder = Message.SingleMsg.newBuilder();
		singleBuilder.setTargetUid(bean.getTarget_uid());
		Message.MessageContent.Builder msgContent = Message.MessageContent.newBuilder();
		msgContent.setText(bean.getMsg_content());
		singleBuilder.setContent(msgContent);
		bodyBuilder.setSingleMsg(singleBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
