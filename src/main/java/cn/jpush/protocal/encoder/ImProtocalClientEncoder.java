package cn.jpush.protocal.encoder;

import jpushim.s2b.JpushimSdk2B.Packet;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.im.req.proto.ImAddGroupMemberRequestProto;
import cn.jpush.protocal.im.req.proto.ImChatMsgSyncRequestProto;
import cn.jpush.protocal.im.req.proto.ImCreateGroupRequestProto;
import cn.jpush.protocal.im.req.proto.ImDeleteGroupMemberRequestProto;
import cn.jpush.protocal.im.req.proto.ImEventSyncRequestProto;
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
import cn.jpush.protocal.utils.StringUtils;
import cn.jpush.protocal.utils.SystemConfig;
import cn.jpush.webim.common.UidResourcesPool;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * IM 协议编码器
 * 把Bean中的数据按照IM协议封装
 */
public class ImProtocalClientEncoder extends MessageToByteEncoder<Object> {
	private static Logger log = (Logger) LoggerFactory.getLogger(ImProtocalClientEncoder.class);
	private static final int JPUSH_VERSION = SystemConfig.getIntProperty("jpush.version");  //  协议中的JPush版本号
	private static final int JMESSAGE_VERSION = SystemConfig.getIntProperty("jmessage.version");  // 协议中IM版本号
	
	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out)
			throws Exception {
		log.info("--------- protocol client encode ---------------");
		if (msg instanceof PushRegRequestBean) {  // JPush 注册
			log.info("encode PushReg request");
			PushRegRequestBean bean = (PushRegRequestBean) msg;
			long rid = StringUtils.getRID();
			PushRegRequest request = new PushRegRequest(7, rid, 0, 0, bean);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send PushReg success");
		}
		if (msg instanceof PushLoginRequestBean) {  // JPush 登陆
			log.info("encode PushLogin request");
			PushLoginRequestBean reqBean = (PushLoginRequestBean)msg;
			PushLoginRequest request = new PushLoginRequest(JPUSH_VERSION, 1, 0, reqBean.getUid(), reqBean);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send PushLogin success");
		}
		if (msg instanceof PushLogoutRequest) {  //  JPush 登出，未使用
			log.info("encode PushLogout request");
			PushLogoutRequest request = (PushLogoutRequest) msg;
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send PushLogout success");
		}
		if (msg instanceof HeartBeatRequest) {  // JPush 和 IM 心跳
			log.info("encode HeartBeat request");
			HeartBeatRequest request = (HeartBeatRequest) msg;
			log.info(String.format("-- heart beat data -- command: %s -- version: %s -- rid: %s -- sid: %s -- juid: %s", 
					request.getCommand(), request.getVersion(), request.getRid(), request.getSid(), request.getJuid()));
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send HeartBeat success");
		}
		if(msg instanceof ImLoginRequestProto){  // IM 登陆
			log.info("encode IMLogin request");
			ImLoginRequestProto req = (ImLoginRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			log.info(String.format("IM Login request package: %s", reqProtobuf.toString()));
			ImRequest request = new ImRequest(JMESSAGE_VERSION, req.getRid(), req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage(); 
			out.writeBytes(data);
			log.info("client send IMLogin success");
		}
		if(msg instanceof ImLogoutRequestProto){  // IM 登出
			log.info("encode IMLogout request");
			ImLogoutRequestProto req = (ImLogoutRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			log.info(String.format("IM Logout request package: %s", reqProtobuf.toString()));
			ImRequest request = new ImRequest(JMESSAGE_VERSION, req.getRid(), req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send IMLogout success");
		}
		if(msg instanceof ImSendSingleMsgRequestProto){  // IM 单聊消息
			log.info("encode IMSendSingleMsg request");
			ImSendSingleMsgRequestProto req = (ImSendSingleMsgRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			log.info(String.format("IM Single Msg request package: %s", reqProtobuf.toString()));
			ImRequest request = new ImRequest(JMESSAGE_VERSION, req.getRid(), req.getSid(), req.getJuid(), reqProtobuf);  
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send IMSendSingleMsg success");
		}
		if(msg instanceof ImSendGroupMsgRequestProto){  // IM 群聊消息
			log.info("encode IMSendGroupMsg request");
			ImSendGroupMsgRequestProto req = (ImSendGroupMsgRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			log.info(String.format("IM Group Msg request package: %s", reqProtobuf.toString()));
			ImRequest request = new ImRequest(JMESSAGE_VERSION, req.getRid(), req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send IMSendGroupMsg success");
		}
		if(msg instanceof ImCreateGroupRequestProto){  // IM 创建群组
			log.info("encode IMCreateGroup request");
			ImCreateGroupRequestProto req = (ImCreateGroupRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			log.info(String.format("IM Create Group request package: %s", reqProtobuf.toString()));
			ImRequest request = new ImRequest(JMESSAGE_VERSION, req.getRid(), req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send IMCreateGroup success");
		}
		if(msg instanceof ImExitGroupRequestProto){  // IM 退出群组
			log.info("encode IMExitGroup request");
			ImExitGroupRequestProto req = (ImExitGroupRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			log.info(String.format("IM Exit Group request package: %s", reqProtobuf.toString()));
			ImRequest request = new ImRequest(JMESSAGE_VERSION, req.getRid(), req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send IMExitGroup success");
		}
		if(msg instanceof ImAddGroupMemberRequestProto){  // IM 添加群成员
			log.info("encode IMAddGroupMember request");
			ImAddGroupMemberRequestProto req = (ImAddGroupMemberRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			log.info(String.format("IM Add Group Member request package: %s", reqProtobuf.toString()));
			ImRequest request = new ImRequest(JMESSAGE_VERSION, req.getRid(), req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send IMAddGroupMember success");
		}
		if(msg instanceof ImDeleteGroupMemberRequestProto){  // IM 删除群成员
			log.info("encode IMDeleteGroupMember request");
			ImDeleteGroupMemberRequestProto req = (ImDeleteGroupMemberRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			log.info(String.format("IM Delete Group Member request package: %s", reqProtobuf.toString()));
			ImRequest request = new ImRequest(JMESSAGE_VERSION, req.getRid(), req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send IMDeleteGroupMember success");
		}
		if(msg instanceof ImUpdateGroupInfoRequestProto){  // IM 修改群信息
			log.info("encode IMUpdateGroupMember request");
			ImUpdateGroupInfoRequestProto req = (ImUpdateGroupInfoRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			log.info(String.format("IM Update Group Info request package: %s", reqProtobuf.toString()));
			ImRequest request = new ImRequest(JMESSAGE_VERSION, req.getRid(), req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send IMUpdateGroupMember success");
		}
		if(msg instanceof ImChatMsgSyncRequestProto){  //  IM 消息下发反馈，表示已收到
			log.info("encode IMRespMsgReceived request");
			ImChatMsgSyncRequestProto req = (ImChatMsgSyncRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			log.info(String.format("IM ChatMsg Sync FeedBack request package: %s", reqProtobuf.toString()));
			ImRequest request = new ImRequest(JMESSAGE_VERSION, req.getRid(), req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send IMRespMsgReceived success");
		}
		if(msg instanceof ImEventSyncRequestProto){  //  IM 事件下发反馈，表示已收到
			log.info("encode IMRespEventReceived request");
			ImEventSyncRequestProto req = (ImEventSyncRequestProto) msg;
			Packet reqProtobuf = req.buildProtoBufProtocal();
			log.info(String.format("IM Event Sync FeedBack request package: %s", reqProtobuf.toString()));
			ImRequest request = new ImRequest(JMESSAGE_VERSION, req.getRid(), req.getSid(), req.getJuid(), reqProtobuf);
			byte[] data = request.getRequestPackage();
			out.writeBytes(data);
			log.info("client send IMRespEventReceived success");
		}
	}

}
