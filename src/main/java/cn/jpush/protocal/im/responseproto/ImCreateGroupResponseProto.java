package cn.jpush.protocal.im.responseproto;

import com.google.protobuf.ByteString;

import jpushim.s2b.JpushimSdk2B;
import jpushim.s2b.JpushimSdk2B.CreateGroup;
import jpushim.s2b.JpushimSdk2B.Packet;
import jpushim.s2b.JpushimSdk2B.ProtocolBody;


public class ImCreateGroupResponseProto extends BaseProtobufResponse {
	private long gid;
	public ImCreateGroupResponseProto(Packet protocol) {
		super(protocol);
	}

	@Override
	protected void buildResposneBody() {
		JpushimSdk2B.Response.Builder responseBuilder = JpushimSdk2B.Response.newBuilder();
		responseBuilder.setCode(this.getCode());
		responseBuilder.setMessage(ByteString.copyFromUtf8(this.getMessage()));
		
		JpushimSdk2B.ProtocolBody body = this.protocol.getBody();
		
		JpushimSdk2B.CreateGroup createGroupBean = this.protocol.getBody().getCreateGroup();
		createGroupBean = CreateGroup.newBuilder(createGroupBean).setGid(this.gid).build();
		body = ProtocolBody.newBuilder(body).setCreateGroup(createGroupBean).build();
		body = ProtocolBody.newBuilder(body).setCommonRep(responseBuilder).build();
		protocol = Packet.newBuilder(protocol).setBody(body).build();
	}

	public ImCreateGroupResponseProto setGid(long gid) {
		this.gid = gid;
		return this;
	}
	
}
