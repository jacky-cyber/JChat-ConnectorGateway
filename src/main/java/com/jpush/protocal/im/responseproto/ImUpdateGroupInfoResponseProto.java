package com.jpush.protocal.im.responseproto;

import com.jpush.protobuf.Im;
import com.jpush.protobuf.Im.Protocol;
import com.jpush.protobuf.Im.ProtocolBody;

public class ImUpdateGroupInfoResponseProto extends BaseProtobufResponse {
	public ImUpdateGroupInfoResponseProto(Protocol protocol) {
		super(protocol);
	}

	@Override
	protected void buildResposneBody() {
		Im.Response.Builder responseBuilder = Im.Response.newBuilder();
		responseBuilder.setCode(this.getCode());
		responseBuilder.setMessage(this.getMessage());
		
		Im.ProtocolBody body = this.protocol.getBody();
		body = ProtocolBody.newBuilder(body).setCommonRep(responseBuilder).build();
		protocol = Protocol.newBuilder(protocol).setBody(body).build();
	}
	
}
