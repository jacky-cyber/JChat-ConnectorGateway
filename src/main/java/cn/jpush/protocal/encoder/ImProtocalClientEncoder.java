package cn.jpush.protocal.encoder;

import jpushim.s2b.JpushimSdk2B.Packet;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.common.JPushTcpClient;
import cn.jpush.protocal.common.JPushTcpClientHandler;
import cn.jpush.protocal.im.req.proto.ImAddGroupMemberRequestProto;
import cn.jpush.protocal.im.req.proto.ImCreateGroupRequestProto;
import cn.jpush.protocal.im.req.proto.ImDeleteGroupMemberRequestProto;
import cn.jpush.protocal.im.req.proto.ImExitGroupRequestProto;
import cn.jpush.protocal.im.req.proto.ImLoginRequestProto;
import cn.jpush.protocal.im.req.proto.ImLogoutRequestProto;
import cn.jpush.protocal.im.req.proto.ImSendGroupMsgRequestProto;
import cn.jpush.protocal.im.req.proto.ImSendSingleMsgRequestProto;
import cn.jpush.protocal.im.req.proto.ImUpdateGroupInfoRequestProto;
import cn.jpush.protocal.im.request.ImAddGroupMemberRequest;
import cn.jpush.protocal.im.request.ImCreateGroupRequest;
import cn.jpush.protocal.im.request.ImDeleteGroupMemberRequest;
import cn.jpush.protocal.im.request.ImExitGroupRequest;
import cn.jpush.protocal.im.request.ImLoginRequest;
import cn.jpush.protocal.im.request.ImLogoutRequest;
import cn.jpush.protocal.im.request.ImRequest;
import cn.jpush.protocal.im.request.ImSendGroupMsgRequest;
import cn.jpush.protocal.im.request.ImSendSingleMsgRequest;
import cn.jpush.protocal.im.request.ImUpdateGroupInfoRequest;
import cn.jpush.protocal.push.HeartBeatRequest;
import cn.jpush.protocal.push.PushLoginRequest;
import cn.jpush.protocal.push.PushLoginRequestBean;
import cn.jpush.protocal.push.PushLogoutRequest;
import cn.jpush.protocal.push.PushRegRequest;
import cn.jpush.protocal.push.PushRegRequestBean;
import cn.jpush.protocal.utils.ProtocolUtil;
import cn.jpush.webim.common.UidResourcesPool;
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
			PushRegRequest request = new PushRegRequest(7, 1, 0, 0, bean);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if (msg instanceof PushLoginRequestBean) {  // push login protocal
			log.info("push login request...");
			PushLoginRequestBean reqBean = (PushLoginRequestBean)msg;
			PushLoginRequest request = new PushLoginRequest(7, 1, 0, reqBean.getUid(), reqBean);
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
			ImRequest request = new ImRequest(1, 1, req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage(); 
			log.info("im login pkg size: "+data.length);
			log.info("im login pkg head data, version: "+request.getVersion()+
					", cmd: "+request.getCommand()+", juid:"+request.getJuid()+
					", rid: "+request.getRid()+", sid: "+request.getSid());
			log.info("im login protobuf data, cmd: "+req.getCmd()+", version: "+req.getVersion()+
					", uid: "+req.getUid()+", appkey: "+req.getAppkey()+", username: "+
					req.getLoginBuilder().getUsername().toStringUtf8()+", password: "+
					req.getLoginBuilder().getPassword().toStringUtf8()+", platform: "+
					req.getLoginBuilder().getPlatform());
			out.writeBytes(data);
		}
		if(msg instanceof ImLogoutRequestProto){  // im logout
			log.info("im logout request...");
			ImLogoutRequestProto req = (ImLogoutRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1,  2, req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImSendSingleMsgRequestProto){  // im send single message
			log.info("im send single message request...");
			ImSendSingleMsgRequestProto req = (ImSendSingleMsgRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 1, req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImSendGroupMsgRequestProto){  // im send group message
			log.info("im send group message request...");
			ImSendGroupMsgRequestProto req = (ImSendGroupMsgRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 1, req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImCreateGroupRequestProto){  // im create group message
			log.info("im create group message request...");
			ImCreateGroupRequestProto req = (ImCreateGroupRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 1, 0, 1153535375, reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImExitGroupRequestProto){  // im exit group message
			log.info("im exit group message request...");
			ImExitGroupRequestProto req = (ImExitGroupRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 1, 342, 343, reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		if(msg instanceof ImAddGroupMemberRequestProto){  // im add group members message
			log.info("im add group members message request...");
			ImAddGroupMemberRequestProto req = (ImAddGroupMemberRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			ImRequest request = new ImRequest(1, 1, req.getSid(), req.getJuid(), reqProtobuf);
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
			ImRequest request = new ImRequest(1, 1, req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
		}
		
	}

}
