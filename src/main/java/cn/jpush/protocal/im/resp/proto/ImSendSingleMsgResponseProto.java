package cn.jpush.protocal.im.resp.proto;

import com.google.protobuf.ByteString;

import jpushim.s2b.JpushimSdk2B;
import jpushim.s2b.JpushimSdk2B.Packet;
import jpushim.s2b.JpushimSdk2B.ProtocolBody;
import jpushim.s2b.JpushimSdk2B.SingleMsg;
/**
 * IM 发送单聊消息响应 protobuf 封装
 * 详细内容参考jpush wiki文档
 */
public class ImSendSingleMsgResponseProto extends BaseProtobufResponse {
	private long msgid;
	public ImSendSingleMsgResponseProto(Packet protocol) {
		super(protocol);
	}

	@Override
	protected void buildResposneBody() {
		JpushimSdk2B.Response.Builder responseBuilder = JpushimSdk2B.Response.newBuilder();
		responseBuilder.setCode(this.getCode());
		responseBuilder.setMessage(ByteString.copyFromUtf8(this.getMessage()));
		
		JpushimSdk2B.ProtocolBody body = this.protocol.getBody();
		
		JpushimSdk2B.SingleMsg singleMsgBean = this.protocol.getBody().getSingleMsg();
		singleMsgBean = SingleMsg.newBuilder(singleMsgBean).setMsgid(this.msgid).build();
		body = ProtocolBody.newBuilder(body).setSingleMsg(singleMsgBean).build();
		body = ProtocolBody.newBuilder(body).setCommonRep(responseBuilder).build();
		protocol = Packet.newBuilder(protocol).setBody(body).build();
	}

	public ImSendSingleMsgResponseProto setMsgid(long msgid) {
		this.msgid = msgid;
		return this;
	}
	
}
