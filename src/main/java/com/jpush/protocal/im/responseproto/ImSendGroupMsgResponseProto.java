package com.jpush.protocal.im.responseproto;

import com.jpush.protobuf.Im;
import com.jpush.protobuf.Message;
import com.jpush.protobuf.Im.Protocol;
import com.jpush.protobuf.Im.ProtocolBody;
import com.jpush.protobuf.Message.GroupMsg;
import com.jpush.protobuf.Message.SingleMsg;

public class ImSendGroupMsgResponseProto extends BaseProtobufResponse {
	private long msgid;
	public ImSendGroupMsgResponseProto(Protocol protocol) {
		super(protocol);
	}

	@Override
	protected void buildResposneBody() {
		Im.Response.Builder responseBuilder = Im.Response.newBuilder();
		responseBuilder.setCode(this.getCode());
		responseBuilder.setMessage(this.getMessage());
		
		Im.ProtocolBody body = this.protocol.getBody();
		
		Message.GroupMsg groupMsgBean = this.protocol.getBody().getGroupMsg();
		groupMsgBean = GroupMsg.newBuilder(groupMsgBean).setMsgid(this.msgid).build();
		body = ProtocolBody.newBuilder(body).setGroupMsg(groupMsgBean).build();
		body = ProtocolBody.newBuilder(body).setCommonRep(responseBuilder).build();
		protocol = Protocol.newBuilder(protocol).setBody(body).build();
	}

	public ImSendGroupMsgResponseProto setMsgid(long msgid) {
		this.msgid = msgid;
		return this;
	}
	
}
