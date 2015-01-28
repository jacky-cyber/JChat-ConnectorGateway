package cn.jpush.protocal.im.responseproto;

import com.google.protobuf.ByteString;

import jpushim.s2b.JpushimSdk2B;
import jpushim.s2b.JpushimSdk2B.GroupMsg;
import jpushim.s2b.JpushimSdk2B.Packet;
import jpushim.s2b.JpushimSdk2B.ProtocolBody;

public class ImSendGroupMsgResponseProto extends BaseProtobufResponse {
	private long msgid;
	public ImSendGroupMsgResponseProto(Packet protocol) {
		super(protocol);
	}

	@Override
	protected void buildResposneBody() {
		JpushimSdk2B.Response.Builder responseBuilder = JpushimSdk2B.Response.newBuilder();
		responseBuilder.setCode(this.getCode());
		responseBuilder.setMessage(ByteString.copyFromUtf8(this.getMessage()));
		
		JpushimSdk2B.ProtocolBody body = this.protocol.getBody();
		
		JpushimSdk2B.GroupMsg groupMsgBean = this.protocol.getBody().getGroupMsg();
		groupMsgBean = GroupMsg.newBuilder(groupMsgBean).setMsgid(this.msgid).build();
		body = ProtocolBody.newBuilder(body).setGroupMsg(groupMsgBean).build();
		body = ProtocolBody.newBuilder(body).setCommonRep(responseBuilder).build();
		protocol = Packet.newBuilder(protocol).setBody(body).build();
	}

	public ImSendGroupMsgResponseProto setMsgid(long msgid) {
		this.msgid = msgid;
		return this;
	}
	
}
