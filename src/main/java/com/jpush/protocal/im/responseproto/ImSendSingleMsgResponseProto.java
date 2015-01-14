package com.jpush.protocal.im.responseproto;

import com.jpush.protobuf.Im;
import com.jpush.protobuf.Message;
import com.jpush.protobuf.Im.Protocol;
import com.jpush.protobuf.Im.ProtocolBody;
import com.jpush.protobuf.Message.GroupMsg;
import com.jpush.protobuf.Message.SingleMsg;

public class ImSendSingleMsgResponseProto extends BaseProtobufResponse {
	private long msgid;
	public ImSendSingleMsgResponseProto(Protocol protocol) {
		super(protocol);
	}

	@Override
	protected void buildResposneBody() {
		Im.Response.Builder responseBuilder = Im.Response.newBuilder();
		responseBuilder.setCode(this.getCode());
		responseBuilder.setMessage(this.getMessage());
		
		Im.ProtocolBody body = this.protocol.getBody();
		
		Message.SingleMsg singleMsgBean = this.protocol.getBody().getSingleMsg();
		singleMsgBean = SingleMsg.newBuilder(singleMsgBean).setMsgid(this.msgid).build();
		body = ProtocolBody.newBuilder(body).setSingleMsg(singleMsgBean).build();
		body = ProtocolBody.newBuilder(body).setCommonRep(responseBuilder).build();
		protocol = Protocol.newBuilder(protocol).setBody(body).build();
	}

	public ImSendSingleMsgResponseProto setMsgid(long msgid) {
		this.msgid = msgid;
		return this;
	}
	
}
