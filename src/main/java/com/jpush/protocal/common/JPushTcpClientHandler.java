package com.jpush.protocal.common;

import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import com.corundumstudio.socketio.SocketIOClient;
import com.jpush.protobuf.Group.AddGroupMember;
import com.jpush.protobuf.Group.CreateGroup;
import com.jpush.protobuf.Group.DelGroupMember;
import com.jpush.protobuf.Group.ExitGroup;
import com.jpush.protobuf.Group.UpdateGroupInfo;
import com.jpush.protobuf.Im.Protocol;
import com.jpush.protobuf.Im.Response;
import com.jpush.protobuf.Message.GroupMsg;
import com.jpush.protobuf.Message.SingleMsg;
import com.jpush.protobuf.User.Login;
import com.jpush.protobuf.User.Logout;
import com.jpush.protocal.im.response.ImLoginResponse;
import com.jpush.protocal.im.responseproto.ImLoginResponseProto;
import com.jpush.protocal.push.PushLoginResponseBean;
import com.jpush.protocal.push.PushLogoutResponseBean;
import com.jpush.protocal.push.PushRegResponseBean;
import com.jpush.protocal.utils.Command;
import com.jpush.protocal.utils.ProtocolUtil;
import com.jpush.webim.common.RedisClient;
import com.jpush.webim.common.UidPool;
import com.jpush.webim.socketio.WebImServer;
import com.jpush.webim.socketio.bean.ChatObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class JPushTcpClientHandler extends ChannelInboundHandlerAdapter {
	private static Logger log = (Logger) LoggerFactory.getLogger(JPushTcpClientHandler.class);
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		log.info("handler Added");
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		log.info("handler Removed");
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		log.info("channel active");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		log.info("client handler receive msg from server");
		if(msg instanceof PushRegResponseBean){
			PushRegResponseBean bean = (PushRegResponseBean) msg;
			log.info("客户端解析push reg response后的结果为：");
			log.info(bean.getResponse_code()+","+bean.getReg_id()+","+bean.getDevice_id()+", "+bean.getPasswd());
			UidPool.addUidToPool(bean.getUid());
			UidPool.semaphore.release();
			log.info("add data to pool success.");
		}
		if(msg instanceof PushLoginResponseBean){
			PushLoginResponseBean bean = (PushLoginResponseBean) msg;
			log.info("客户端解析push login response后的结果为：");
			log.info(bean.getResponse_code()+","+bean.getServer_time()+","+bean.getServer_version()+", "+bean.getSession_key());
		}
		if(msg instanceof PushLogoutResponseBean){
			PushLogoutResponseBean bean = (PushLogoutResponseBean) msg;
			log.info("客户端解析push logout response后的结果为：");
			log.info("code: "+bean.getResponse_code());
		}
		if(msg instanceof Protocol){   //  im 业务
			SocketIOClient sessionClient;
			Protocol protocol = (Protocol) msg;
			if(Command.JPUSH_IM.LOGIN==protocol.getHead().getCmd()){  // im login
				log.info("im login response...");
				Login loginBean = protocol.getBody().getLogin();
				log.info("login data, username: "+loginBean.getUsername()+", password: "+loginBean.getPassword());
				Response resp = protocol.getBody().getCommonRep();
				log.info("login response data: code: "+resp.getCode()+", message: "+resp.getMessage());
			}
			if(Command.JPUSH_IM.LOGOUT==protocol.getHead().getCmd()){  // im logout
				log.info("im logout response...");
				Logout logoutBean = protocol.getBody().getLogout();
				log.info("logout data, username: "+logoutBean.getUsername()+", appkey: "+logoutBean.getAppkey());
				Response resp = protocol.getBody().getCommonRep();
				log.info("logout response data: code: "+resp.getCode()+", message: "+resp.getMessage());
			}
			if(Command.JPUSH_IM.SENDMSG_SINGAL==protocol.getHead().getCmd()){  // im send single msg
				log.info("im send single msg response...");
				SingleMsg singleMsgBean = protocol.getBody().getSingleMsg();
				log.info("single msg data, target uid: "+singleMsgBean.getTargetUid()+", msgid: "+singleMsgBean.getMsgid());
				Response resp = protocol.getBody().getCommonRep();
				log.info("single msg response data: code: "+resp.getCode()+", message: "+resp.getMessage());
				// 模拟 Jpush 测试
				ChatObject data = new ChatObject();
				/****** channel map test   ******/
				sessionClient = WebImServer.userNameToSessionCilentMap.get(singleMsgBean.getTargetUid()+"");
				
				log.info("get username's session client: "+sessionClient.getSessionId());
				
				data.setToUserName(singleMsgBean.getTargetUid()+"");
				data.setUserName(protocol.getHead().getUid()+"");
				data.setMessage(singleMsgBean.getContent().getText());
				sessionClient.sendEvent("chatevent", data);
			}
			if(Command.JPUSH_IM.SENDMSG_GROUP==protocol.getHead().getCmd()){  // im send group msg
				log.info("im send group msg response...");
				GroupMsg groupMsgBean = protocol.getBody().getGroupMsg();
				log.info("group msg data, target gid: "+groupMsgBean.getTargetGid()+", msgid: "+groupMsgBean.getMsgid());
				Response resp = protocol.getBody().getCommonRep();
				log.info("group msg response data: code: "+resp.getCode()+", message: "+resp.getMessage());
			}
			if(Command.JPUSH_IM.CREATE_GROUP==protocol.getHead().getCmd()){  // im create group msg
				log.info("im create group msg response...");
				CreateGroup createGroupBean = protocol.getBody().getCreateGroup();
				log.info("create group data, gid: "+createGroupBean.getGid());
				Response resp = protocol.getBody().getCommonRep();
				log.info("create group response data: code: "+resp.getCode()+", message: "+resp.getMessage());
			}
			if(Command.JPUSH_IM.EXIT_GROUP==protocol.getHead().getCmd()){  // im exit group msg
				log.info("im exit group msg response...");
				ExitGroup exitGroupBean = protocol.getBody().getExitGroup();
				log.info("exit group data, gid: "+exitGroupBean.getGid());
				Response resp = protocol.getBody().getCommonRep();
				log.info("exit group response data: code: "+resp.getCode()+", message: "+resp.getMessage());
			}
			if(Command.JPUSH_IM.ADD_GROUP_MEMBER==protocol.getHead().getCmd()){  // im add group member msg
				log.info("im add group member msg response...");
				AddGroupMember addGroupMemberBean = protocol.getBody().getAddGroupMember();
				log.info("add group member data, gid: "+addGroupMemberBean.getGid());
				Response resp = protocol.getBody().getCommonRep();
				log.info("add group member response data: code: "+resp.getCode()+", message: "+resp.getMessage());
			}
			if(Command.JPUSH_IM.DEL_GROUP_MEMBER==protocol.getHead().getCmd()){  // im delete group member msg
				log.info("im delete group member msg response...");
				DelGroupMember delGroupMemberBean = protocol.getBody().getDelGroupMember();
				log.info("del group member data, gid: "+delGroupMemberBean.getGid());
				Response resp = protocol.getBody().getCommonRep();
				log.info("del group member response data: code: "+resp.getCode()+", message: "+resp.getMessage());
			}
			if(Command.JPUSH_IM.UPDATE_GROUP_INFO==protocol.getHead().getCmd()){  // im update group info msg
				log.info("im update group info msg response...");
				UpdateGroupInfo bean = protocol.getBody().getUpdateGroupInfo();
				log.info("update group info data, gid: "+bean.getGid()+", info: "+bean.getInfo());
				Response resp = protocol.getBody().getCommonRep();
				log.info("update group info response data: code: "+resp.getCode()+", message: "+resp.getMessage());
			}
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		if(evt instanceof IdleStateEvent){
			IdleStateEvent e = (IdleStateEvent) evt;
			if(e.state()==IdleState.READER_IDLE){
				log.info("client heartbeat..., it is too long to read.");
			} else if(e.state()==IdleState.WRITER_IDLE){
				log.info("client heartbeat..., it is too long to write.");
				//ctx.close(); 
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

}
