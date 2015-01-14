package com.jpush.protocal.common;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.jpush.protobuf.Group.AddGroupMember;
import com.jpush.protobuf.Group.CreateGroup;
import com.jpush.protobuf.Group.DelGroupMember;
import com.jpush.protobuf.Group.ExitGroup;
import com.jpush.protobuf.Group.UpdateGroupInfo;
import com.jpush.protobuf.Im.Protocol;
import com.jpush.protobuf.Message.GroupMsg;
import com.jpush.protobuf.Message.SingleMsg;
import com.jpush.protobuf.User.Login;
import com.jpush.protobuf.User.Logout;
import com.jpush.protocal.im.response.ImAddGroupMemberResponse;
import com.jpush.protocal.im.response.ImCreateGroupResponse;
import com.jpush.protocal.im.response.ImDeleteGroupMemberResponse;
import com.jpush.protocal.im.response.ImExitGroupResponse;
import com.jpush.protocal.im.response.ImGroupMsgResponse;
import com.jpush.protocal.im.response.ImLoginResponse;
import com.jpush.protocal.im.response.ImLogoutResponse;
import com.jpush.protocal.im.response.ImResponse;
import com.jpush.protocal.im.response.ImSingleMsgResponse;
import com.jpush.protocal.im.response.ImUpdateGroupInfoResponse;
import com.jpush.protocal.im.responseproto.ImAddGroupMemberResponseProto;
import com.jpush.protocal.im.responseproto.ImCreateGroupResponseProto;
import com.jpush.protocal.im.responseproto.ImDeleteGroupMemberResponseProto;
import com.jpush.protocal.im.responseproto.ImExitGroupResponseProto;
import com.jpush.protocal.im.responseproto.ImLoginResponseProto;
import com.jpush.protocal.im.responseproto.ImLogoutResponseProto;
import com.jpush.protocal.im.responseproto.ImSendGroupMsgResponseProto;
import com.jpush.protocal.im.responseproto.ImSendSingleMsgResponseProto;
import com.jpush.protocal.im.responseproto.ImUpdateGroupInfoResponseProto;
import com.jpush.protocal.push.HeartBeatRequest;
import com.jpush.protocal.push.PushLoginRequestBean;
import com.jpush.protocal.push.PushLoginResponseBean;
import com.jpush.protocal.push.PushLogoutResponseBean;
import com.jpush.protocal.push.PushRegRequestBean;
import com.jpush.protocal.push.PushRegResponseBean;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class NettyTcpServerHandler extends ChannelInboundHandlerAdapter {
	private static Logger log = (Logger) LoggerFactory.getLogger(NettyTcpServerHandler.class);
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		log.info("server handler receive msg and track......");
		if(msg instanceof PushRegRequestBean){
			PushRegRequestBean bean = (PushRegRequestBean) msg;
			log.info("request bean: "+bean.getStrClientInfo()+", "+bean.getStrKey()+", "+bean.getStrApkVersion());
			PushRegResponseBean respBean = new PushRegResponseBean(0, 21, "passwd", "reg_id:2342w34", "device_id:34523");
			ctx.writeAndFlush(respBean); 
		} else if(msg instanceof PushLoginRequestBean){
			PushLoginRequestBean bean = (PushLoginRequestBean) msg;
			log.info("request bean: "+bean.getFrom_resource()+", "+bean.getAppkey()+", "+bean.getPasswdmd5());
			PushLoginResponseBean respBean = new PushLoginResponseBean(0, 2314, 1, "session key", 1234567);
			ctx.writeAndFlush(respBean); 
		} else if(msg instanceof Protocol){
			Protocol protocol = (Protocol) msg;
			if(Command.JPUSH_IM.LOGIN==protocol.getHead().getCmd()){  // im login
				log.info("im login request...");
				Login loginBean = protocol.getBody().getLogin();
				log.info("login data, username: "+loginBean.getUsername()+", password: "+loginBean.getPassword());
				protocol = new ImLoginResponseProto(protocol).setMessage("login success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 32, 34, protocol);
				ctx.writeAndFlush(response);
			}
			if(Command.JPUSH_IM.LOGOUT==protocol.getHead().getCmd()){  // im logout
				log.info("im logout request...");
				Logout logoutBean = protocol.getBody().getLogout();
				log.info("logout data, username: "+logoutBean.getUsername()+", appkey"+logoutBean.getAppkey());
				protocol = new ImLogoutResponseProto(protocol).setMessage("logout success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 23, 23, protocol);
				ctx.writeAndFlush(response);
			}
			if(Command.JPUSH_IM.SENDMSG_SINGAL==protocol.getHead().getCmd()){  // im send single msg
				log.info("im send single msg request...");
				SingleMsg singleMsgBean = protocol.getBody().getSingleMsg();
				log.info("single msg data, target uid: "+singleMsgBean.getTargetUid());
				protocol = new ImSendSingleMsgResponseProto(protocol).setMsgid(12889).setMessage("send single message success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 23, 23, protocol);
				ctx.writeAndFlush(response);
			}
			if(Command.JPUSH_IM.SENDMSG_GROUP==protocol.getHead().getCmd()){  // im send group msg
				log.info("im send group msg request...");
				GroupMsg groupMsgBean = protocol.getBody().getGroupMsg();
				log.info("group msg data, target uid: "+groupMsgBean.getTargetGid());
				protocol = new ImSendGroupMsgResponseProto(protocol).setMsgid(12899).setMessage("send group message success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 23, 23, protocol);
				ctx.writeAndFlush(response);
			}
			if(Command.JPUSH_IM.CREATE_GROUP==protocol.getHead().getCmd()){  // im create group msg
				log.info("im create group msg request...");
				CreateGroup createGroupBean = protocol.getBody().getCreateGroup();
				log.info("create group data, group name: "+createGroupBean.getGroupName()+", desc: "+createGroupBean.getGroupDesc());
				protocol = new ImCreateGroupResponseProto(protocol).setGid(8998).setMessage("create group success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 23, 23, protocol);
				ctx.writeAndFlush(response);
			}
			if(Command.JPUSH_IM.EXIT_GROUP==protocol.getHead().getCmd()){  // im exit group msg
				log.info("im exit group msg request...");
				ExitGroup exitGroupBean = protocol.getBody().getExitGroup();
				log.info("exit group data, group gid: "+exitGroupBean.getGid());
				protocol = new ImExitGroupResponseProto(protocol).setMessage("exit group success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 23, 23, protocol);
				ctx.writeAndFlush(response);
			}
			if(Command.JPUSH_IM.ADD_GROUP_MEMBER==protocol.getHead().getCmd()){  // im add group member msg
				log.info("im add group member msg request...");
				AddGroupMember addGroupMemberBean = protocol.getBody().getAddGroupMember();
				log.info("add group member data, group gid: "+addGroupMemberBean.getGid()+", count: "+addGroupMemberBean.getMemberCount());
				protocol = new ImAddGroupMemberResponseProto(protocol).setMessage("add group member success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 23, 23, protocol);
				ctx.writeAndFlush(response);
			}
			if(Command.JPUSH_IM.DEL_GROUP_MEMBER==protocol.getHead().getCmd()){  // im delete group member msg
				log.info("im delete group member msg request...");
				DelGroupMember deleteGroupMemberBean = protocol.getBody().getDelGroupMember();
				log.info("delete group member data, group gid: "+deleteGroupMemberBean.getGid()+", count: "+deleteGroupMemberBean.getMemberCount());
				protocol = new ImDeleteGroupMemberResponseProto(protocol).setMessage("delete group member success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 23, 23, protocol);
				ctx.writeAndFlush(response);
			}
			if(Command.JPUSH_IM.UPDATE_GROUP_INFO==protocol.getHead().getCmd()){  // im modify group info msg
				log.info("im modify group info msg request...");
				UpdateGroupInfo bean = protocol.getBody().getUpdateGroupInfo();
				log.info("modify group info data, group gid: "+bean.getGid()+", count: "+bean.getInfo());
				protocol = new ImUpdateGroupInfoResponseProto(protocol).setMessage("update group info success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 23, 23, protocol);
				ctx.writeAndFlush(response);
			}

		}
		else {
			int cmd = (int) msg;
			log.info("logout bean cmd:"+cmd);
			PushLogoutResponseBean respBean = new PushLogoutResponseBean(0); 
			ctx.writeAndFlush(respBean); 
		}
		
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		if(evt instanceof IdleStateEvent){
			IdleStateEvent e = (IdleStateEvent) evt;
			if(e.state()==IdleState.READER_IDLE){
				log.info("servet heartbeat..., it is too long to read.");
				HeartBeatRequest request = new HeartBeatRequest(1, 32, 23, 34, 43);
				ctx.channel().writeAndFlush(request);
			} else if(e.state()==IdleState.WRITER_IDLE){
				log.info("server heartbeat..., it is too long to write.");
				//ctx.close(); 
			}
		}
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		log.info("server channelReadComplete");
		//ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		 	cause.printStackTrace();
	      ctx.close();
	}

}
