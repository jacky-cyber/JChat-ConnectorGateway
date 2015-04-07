package cn.jpush.protocal.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jpushim.s2b.JpushimSdk2B.AddGroupMember;
import jpushim.s2b.JpushimSdk2B.CreateGroup;
import jpushim.s2b.JpushimSdk2B.DelGroupMember;
import jpushim.s2b.JpushimSdk2B.ExitGroup;
import jpushim.s2b.JpushimSdk2B.GroupMsg;
import jpushim.s2b.JpushimSdk2B.Login;
import jpushim.s2b.JpushimSdk2B.Logout;
import jpushim.s2b.JpushimSdk2B.Packet;
import jpushim.s2b.JpushimSdk2B.SingleMsg;
import jpushim.s2b.JpushimSdk2B.UpdateGroupInfo;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.im.resp.proto.ImAddGroupMemberResponseProto;
import cn.jpush.protocal.im.resp.proto.ImCreateGroupResponseProto;
import cn.jpush.protocal.im.resp.proto.ImDeleteGroupMemberResponseProto;
import cn.jpush.protocal.im.resp.proto.ImExitGroupResponseProto;
import cn.jpush.protocal.im.resp.proto.ImLoginResponseProto;
import cn.jpush.protocal.im.resp.proto.ImLogoutResponseProto;
import cn.jpush.protocal.im.resp.proto.ImSendGroupMsgResponseProto;
import cn.jpush.protocal.im.resp.proto.ImSendSingleMsgResponseProto;
import cn.jpush.protocal.im.resp.proto.ImUpdateGroupInfoResponseProto;
import cn.jpush.protocal.im.response.ImAddGroupMemberResponse;
import cn.jpush.protocal.im.response.ImCreateGroupResponse;
import cn.jpush.protocal.im.response.ImDeleteGroupMemberResponse;
import cn.jpush.protocal.im.response.ImExitGroupResponse;
import cn.jpush.protocal.im.response.ImGroupMsgResponse;
import cn.jpush.protocal.im.response.ImLoginResponse;
import cn.jpush.protocal.im.response.ImLogoutResponse;
import cn.jpush.protocal.im.response.ImResponse;
import cn.jpush.protocal.im.response.ImSingleMsgResponse;
import cn.jpush.protocal.im.response.ImUpdateGroupInfoResponse;
import cn.jpush.protocal.push.HeartBeatRequest;
import cn.jpush.protocal.push.HeartBeatResponse;
import cn.jpush.protocal.push.PushLoginRequestBean;
import cn.jpush.protocal.push.PushLoginResponseBean;
import cn.jpush.protocal.push.PushLogoutResponseBean;
import cn.jpush.protocal.push.PushMessageRequest;
import cn.jpush.protocal.push.PushMessageRequestBean;
import cn.jpush.protocal.push.PushRegRequestBean;
import cn.jpush.protocal.push.PushRegResponseBean;
import cn.jpush.protocal.utils.Command;
import cn.jpush.webim.common.UidResourcesPool;

import com.google.gson.Gson;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class JPushTcpServerHandler extends ChannelInboundHandlerAdapter {
	private static Logger log = (Logger) LoggerFactory.getLogger(JPushTcpServerHandler.class);
	private Gson gson = new Gson();
	private int count = 0;
	@SuppressWarnings("unchecked")
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		log.info("server handler receive msg and track......");
		count = 0;
		if(msg instanceof PushRegRequestBean){
			PushRegRequestBean bean = (PushRegRequestBean) msg;
			log.info("request bean: "+bean.getStrClientInfo()+", "+bean.getStrKey()+", "+bean.getStrApkVersion());
			Random random = new Random();
			long uid = Math.abs(random.nextLong());
			log.info("随机生成uid: "+uid);
			PushRegResponseBean respBean = new PushRegResponseBean(0, uid, "passwd", "reg_id:2342w34", "device_id:34523");
			ctx.writeAndFlush(respBean); 
		} else if(msg instanceof PushLoginRequestBean){
			PushLoginRequestBean bean = (PushLoginRequestBean) msg;
			log.info("request bean: "+bean.getFrom_resource()+", "+bean.getAppkey()+", "+bean.getPasswdmd5());
			PushLoginResponseBean respBean = new PushLoginResponseBean(0, 2314, 1, "session key", 1234567);
			ctx.writeAndFlush(respBean); 
		} else if(msg instanceof Packet){
			Packet protocol = (Packet) msg;
			if(Command.JPUSH_IM.LOGIN==protocol.getHead().getCmd()){  // im login
				log.info("im login request...");
				Login loginBean = protocol.getBody().getLogin();
				log.info("login data, username: "+loginBean.getUsername().toStringUtf8()+", password: "+loginBean.getPassword().toStringUtf8());
				protocol = new ImLoginResponseProto(protocol).setMessage("login success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 32, 34, protocol);
				ctx.writeAndFlush(response);
			}
			if(Command.JPUSH_IM.LOGOUT==protocol.getHead().getCmd()){  // im logout
				log.info("im logout request...");
				Logout logoutBean = protocol.getBody().getLogout();
				log.info("logout data, username: "+logoutBean.getUsername().toStringUtf8());
				protocol = new ImLogoutResponseProto(protocol).setMessage("logout success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 23, 23, protocol);
				ctx.writeAndFlush(response);
			}
			if(Command.JPUSH_IM.SENDMSG_SINGAL==protocol.getHead().getCmd()){  // im send single msg
				log.info("im send single msg request...");
				SingleMsg singleMsgBean = protocol.getBody().getSingleMsg();
				log.info("single msg data, target uid: "+singleMsgBean.getTargetUid());
				/*protocol = new ImSendSingleMsgResponseProto(protocol).setMsgid(12889).setMessage("send single message success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 23, 23, protocol);
				ctx.writeAndFlush(response);*/
				//  IM 消息走 jpush message
				Map map = new HashMap();
				map.put("type", "single");
				map.put("target_uid", String.valueOf(singleMsgBean.getTargetUid()));
				map.put("uid", String.valueOf(protocol.getHead().getUid()));
				map.put("message", singleMsgBean.getContent().getContent().toStringUtf8());
				PushMessageRequestBean bean = new PushMessageRequestBean(1, 123456, gson.toJson(map));
				PushMessageRequest request = new PushMessageRequest(1, 23, 32, 321, bean);
				ctx.writeAndFlush(request);
			}
			if(Command.JPUSH_IM.SENDMSG_GROUP==protocol.getHead().getCmd()){  // im send group msg
				log.info("im send group msg request...");
				GroupMsg groupMsgBean = protocol.getBody().getGroupMsg();
				log.info("group msg data, target uid: "+groupMsgBean.getTargetGid());
				//  IM 消息走 jpush message
				Map map = new HashMap();
				map.put("type", "group");
				map.put("target_gid", String.valueOf(groupMsgBean.getTargetGid()));
				map.put("uid", String.valueOf(protocol.getHead().getUid()));
				map.put("message", groupMsgBean.getContent().getContent().toStringUtf8());
				PushMessageRequestBean bean = new PushMessageRequestBean(1, 123456, gson.toJson(map));
				PushMessageRequest request = new PushMessageRequest(1, 23, 32, 321, bean);
				ctx.writeAndFlush(request);
			}
			if(Command.JPUSH_IM.CREATE_GROUP==protocol.getHead().getCmd()){  // im create group msg
				log.info("im create group msg request...");
				CreateGroup createGroupBean = protocol.getBody().getCreateGroup();
				log.info("create group data, group name: "+createGroupBean.getGroupName().toStringUtf8()+", desc: "+createGroupBean.getGroupDesc().toStringUtf8());
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
				log.info("modify group info data, group gid: "+bean.getGid()+", count: "+bean.getInfo().toStringUtf8());
				protocol = new ImUpdateGroupInfoResponseProto(protocol).setMessage("update group info success").getResponseProtocol();
				ImResponse response = new ImResponse(1, 23, 23, protocol);
				ctx.writeAndFlush(response);
			}

		}
		else {
			int cmd = Integer.parseInt((String) msg);
			if(cmd==Command.KKPUSH_LOGOUT.COMMAND){  //  push 登出
				log.info("logout bean cmd:"+cmd);
				PushLogoutResponseBean respBean = new PushLogoutResponseBean(0); 
				ctx.writeAndFlush(respBean); 
			} else if(cmd==Command.KKPUSH_HEARTBEAT.COMMAND){   //  push 心跳
				log.info("heartbeat bean cmd:"+cmd);
				HeartBeatResponse resp = new HeartBeatResponse(1, 231, 321, Command.KKPUSH_HEARTBEAT.COMMAND);
				ctx.writeAndFlush(resp); 
			}
		}
		
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		log.info("handler Removed...channel: "+ctx.channel().toString());
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		if(evt instanceof IdleStateEvent){
			IdleStateEvent e = (IdleStateEvent) evt;
			if(e.state()==IdleState.READER_IDLE){
				log.info("servet heartbeat...server read idle...channel: "+ctx.channel().toString()+", count:"+count++);
				if(count>=3){
					ctx.channel().close();
					log.info("the client:"+ctx.channel().toString()+" didn't send message for a long time, so close it.");
				}
			}/* else if(e.state()==IdleState.WRITER_IDLE){
				log.info("server heartbeat...server write idle...channel: "+ctx.channel().toString());
				//ctx.close(); 
			}*/
		}
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.info("inactive...channel: "+ctx.channel().toString());
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
