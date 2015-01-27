package com.jpush.protocal.encoder;

import jpushim.s2b.JpushimSdk2B.Packet;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.jpush.protocal.im.request.ImAddGroupMemberRequest;
import com.jpush.protocal.im.request.ImCreateGroupRequest;
import com.jpush.protocal.im.request.ImDeleteGroupMemberRequest;
import com.jpush.protocal.im.request.ImExitGroupRequest;
import com.jpush.protocal.im.request.ImLoginRequest;
import com.jpush.protocal.im.request.ImLogoutRequest;
import com.jpush.protocal.im.request.ImRequest;
import com.jpush.protocal.im.request.ImUpdateGroupInfoRequest;
import com.jpush.protocal.im.request.ImSendGroupMsgRequest;
import com.jpush.protocal.im.request.ImSendSingleMsgRequest;
import com.jpush.protocal.im.requestproto.ImAddGroupMemberRequestProto;
import com.jpush.protocal.im.requestproto.ImCreateGroupRequestProto;
import com.jpush.protocal.im.requestproto.ImDeleteGroupMemberRequestProto;
import com.jpush.protocal.im.requestproto.ImExitGroupRequestProto;
import com.jpush.protocal.im.requestproto.ImLoginRequestProto;
import com.jpush.protocal.im.requestproto.ImLogoutRequestProto;
import com.jpush.protocal.im.requestproto.ImSendGroupMsgRequestProto;
import com.jpush.protocal.im.requestproto.ImSendSingleMsgRequestProto;
import com.jpush.protocal.im.requestproto.ImUpdateGroupInfoRequestProto;
import com.jpush.protocal.push.HeartBeatRequest;
import com.jpush.protocal.push.PushLoginRequest;
import com.jpush.protocal.push.PushLoginRequestBean;
import com.jpush.protocal.push.PushLogoutRequest;
import com.jpush.protocal.push.PushRegRequest;
import com.jpush.protocal.push.PushRegRequestBean;
import com.jpush.protocal.utils.ProtocolUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * IM 协议编码器 
 */
public class ImProtocalClientEncoder extends MessageToByteEncoder<Object> {
	private static Logger log = (Logger) LoggerFactory.getLogger(ImProtocalClientEncoder.class);
	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out)
			throws Exception {
		log.info("客户端开始encode.....");
		if (msg instanceof PushRegRequestBean) {  // push reg protocal
			log.info("push reg request...");
			PushRegRequestBean bean = (PushRegRequestBean) msg;
			PushRegRequest request = new PushRegRequest(1, 12, 342, 342, bean);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if (msg instanceof PushLoginRequest) {  // push login protocal
			log.info("push login request...");
			PushLoginRequest request = (PushLoginRequest) msg;
			byte[] data = request.getRequestPackage();
			log.info("jpush login pkg size: "+data.length);
			out.writeBytes(data);
		}
		if (msg instanceof PushLogoutRequest) {  // push logout protocal
			log.info("push logout request...");
			PushLogoutRequest request = (PushLogoutRequest) msg;
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if (msg instanceof HeartBeatRequest) {  // push heart beat protocal
			log.info("push heart beat request...");
			HeartBeatRequest request = (HeartBeatRequest) msg;
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImLoginRequestProto){  // im login 
			log.info("im login request...");
			ImLoginRequestProto req = (ImLoginRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 12, 342, 343, reqProtobuf);
			byte[] data = request.getRequestPackage();
			log.info("im login pkg size: "+data.length);
			out.writeBytes(data);
		}
		if(msg instanceof ImLogoutRequestProto){  // im logout
			log.info("im logout request...");
			ImLogoutRequestProto req = (ImLogoutRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 12, 342, 343, reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImSendSingleMsgRequestProto){  // im send single message
			log.info("im send single message request...");
			ImSendSingleMsgRequestProto req = (ImSendSingleMsgRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 12, 342, 343, reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImSendGroupMsgRequestProto){  // im send group message
			log.info("im send group message request...");
			ImSendGroupMsgRequestProto req = (ImSendGroupMsgRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 12, 342, 343, reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImCreateGroupRequestProto){  // im create group message
			log.info("im create group message request...");
			ImCreateGroupRequestProto req = (ImCreateGroupRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 12, 342, 343, reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImExitGroupRequestProto){  // im exit group message
			log.info("im exit group message request...");
			ImExitGroupRequestProto req = (ImExitGroupRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 12, 342, 343, reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImAddGroupMemberRequestProto){  // im add group members message
			log.info("im add group members message request...");
			ImAddGroupMemberRequestProto req = (ImAddGroupMemberRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 12, 342, 343, reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImDeleteGroupMemberRequestProto){  // im delete group members message
			log.info("im delete group members message request...");
			ImDeleteGroupMemberRequestProto req = (ImDeleteGroupMemberRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 12, 342, 343, reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImUpdateGroupInfoRequestProto){  // im modify group info message
			log.info("im modify group info message request...");
			ImUpdateGroupInfoRequestProto req = (ImUpdateGroupInfoRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 12, 342, 343, reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		
	}

}
