package com.jpush.protocal.im.requestproto;

import java.util.List;

import com.jpush.protobuf.Im;
import com.jpush.protobuf.Message;
import com.jpush.protocal.im.bean.SendGroupMsgRequestBean;

public class ImSendGroupMsgRequestProto extends BaseProtobufRequest {

	public ImSendGroupMsgRequestProto(int cmd, int version, long uid,
			List cookie, Object bean) {
		super(cmd, version, uid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		Im.ProtocolBody.Builder bodyBuilder = Im.ProtocolBody.newBuilder();
		SendGroupMsgRequestBean bean = (SendGroupMsgRequestBean) obj;
		Message.GroupMsg.Builder groupBuilder = Message.GroupMsg.newBuilder();
		groupBuilder.setTargetGid(bean.getTarget_gid());
		Message.MessageContent.Builder msgContent = Message.MessageContent.newBuilder();
		msgContent.setText(bean.getMsg_content());
		groupBuilder.setContent(msgContent);
		bodyBuilder.setGroupMsg(groupBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
