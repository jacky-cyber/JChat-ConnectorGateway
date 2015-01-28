package cn.jpush.protocal.im.responseproto;

import com.google.protobuf.ByteString;

import jpushim.s2b.JpushimSdk2B;
import jpushim.s2b.JpushimSdk2B.Packet;
import jpushim.s2b.JpushimSdk2B.ProtocolBody;


public class ImAddGroupMemberResponseProto extends BaseProtobufResponse {
	public ImAddGroupMemberResponseProto(Packet protocol) {
		super(protocol);
	}

	@Override
	protected void buildResposneBody() {
		JpushimSdk2B.Response.Builder responseBuilder = JpushimSdk2B.Response.newBuilder();
		responseBuilder.setCode(this.getCode());
		responseBuilder.setMessage(ByteString.copyFromUtf8(this.getMessage()));
		
		JpushimSdk2B.ProtocolBody body = this.protocol.getBody();
		body = ProtocolBody.newBuilder(body).setCommonRep(responseBuilder).build();
		protocol = Packet.newBuilder(protocol).setBody(body).build();
	}
	
}
