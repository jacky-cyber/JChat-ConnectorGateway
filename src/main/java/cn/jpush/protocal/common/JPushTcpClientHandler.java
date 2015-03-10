package cn.jpush.protocal.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jpushim.s2b.JpushimSdk2B.AddGroupMember;
import jpushim.s2b.JpushimSdk2B.ChatMsg;
import jpushim.s2b.JpushimSdk2B.ChatMsgSync;
import jpushim.s2b.JpushimSdk2B.CreateGroup;
import jpushim.s2b.JpushimSdk2B.DelGroupMember;
import jpushim.s2b.JpushimSdk2B.EventNotification;
import jpushim.s2b.JpushimSdk2B.ExitGroup;
import jpushim.s2b.JpushimSdk2B.GroupMsg;
import jpushim.s2b.JpushimSdk2B.Login;
import jpushim.s2b.JpushimSdk2B.Logout;
import jpushim.s2b.JpushimSdk2B.Packet;
import jpushim.s2b.JpushimSdk2B.Response;
import jpushim.s2b.JpushimSdk2B.SingleMsg;
import jpushim.s2b.JpushimSdk2B.UpdateGroupInfo;

import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.im.resp.proto.ImLoginResponseProto;
import cn.jpush.protocal.im.response.ImLoginResponse;
import cn.jpush.protocal.push.HeartBeatRequest;
import cn.jpush.protocal.push.PushLoginResponseBean;
import cn.jpush.protocal.push.PushLogoutResponseBean;
import cn.jpush.protocal.push.PushMessageRequestBean;
import cn.jpush.protocal.push.PushRegResponseBean;
import cn.jpush.protocal.utils.APIProxy;
import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.HttpResponseWrapper;
import cn.jpush.protocal.utils.ProtocolUtil;
import cn.jpush.socketio.SocketIOClient;
import cn.jpush.webim.common.RedisClient;
import cn.jpush.webim.common.UidResourcesPool;
import cn.jpush.webim.server.WebImServer;
import cn.jpush.webim.socketio.bean.ChatMessage;
import cn.jpush.webim.socketio.bean.ChatObject;
import cn.jpush.webim.socketio.bean.ContracterObject;
import cn.jpush.webim.socketio.bean.GroupMember;
import cn.jpush.webim.socketio.bean.GroupMemberList;
import cn.jpush.webim.socketio.bean.User;
import cn.jpush.webim.socketio.bean.UserList;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class JPushTcpClientHandler extends ChannelInboundHandlerAdapter {
	private static Logger log = (Logger) LoggerFactory.getLogger(JPushTcpClientHandler.class);
	private Gson gson = new Gson();
	private PushLoginResponseBean pushLoginResponseBean;
	private int sid;
	private long juid;

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		log.info("handler Added");
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		log.info("handler Removed...channel: "+channel.toString());
		// 下线相关用户
		if(channel!=null){
			long uid = WebImServer.pushChannelToUsernameMap.get(channel);
			SocketIOClient sessionClient = WebImServer.userNameToSessionCilentMap.get(uid);
			sessionClient.sendEvent("disconnect", "");
		}
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
			//  添加uid到pool
			UidResourcesPool.addUidToPool(bean.getUid());
			UidResourcesPool.capacityCountDown.countDown();
			if(UidResourcesPool.capacityCountDown.getCount()==0){
				UidResourcesPool.produceResourceSemaphore.release();
				ctx.channel().close();
			}
			log.info("add data to pool success.");
		}
		if(msg instanceof PushLoginResponseBean){
			pushLoginResponseBean = (PushLoginResponseBean) msg;
			WebImServer.pushLoginInCountDown.countDown();
			log.info("客户端解析push login response后的结果为：");
			log.info("code: "+pushLoginResponseBean.getResponse_code()+", server-time: "+pushLoginResponseBean.getServer_time()+", server-version: "+pushLoginResponseBean.getServer_version());
		}
		if(msg instanceof PushLogoutResponseBean){
			PushLogoutResponseBean bean = (PushLogoutResponseBean) msg;
			log.info("客户端解析push logout response后的结果为：");
			log.info("code: "+bean.getResponse_code());
		}
		/*if(msg instanceof PushMessageRequestBean){  // im消息走jpush
			Gson gson = new Gson();
			PushMessageRequestBean bean = (PushMessageRequestBean) msg;
			String json_content = bean.getMessage();
			Map<String, String> map = gson.fromJson(json_content, HashMap.class);
			json_content = map.get("message");
			ChatMessage content = gson.fromJson(json_content, ChatMessage.class);
			ChatObject data = new ChatObject();
			String type = (String)content.getTarget_type();
			if("single".equals(type)){
				SocketIOClient client = WebImServer.userNameToSessionCilentMap.get(content.getTarget_id());	
				data.setToUserName(content.getTarget_id()+"");
				data.setUserName(content.getFrom_id()+"");
				data.setMessage(content.getMsg_body().toString());
				data.setMsgType("single");
				if("text".endsWith(content.getMsg_type())){
					data.setContentType("text");
				} else if("image".endsWith(content.getMsg_type())){
					data.setContentType("image");
				}
				if(client!=null){
					client.sendEvent("chatEvent", data);
				} else {
					log.info("该用户此时不在线.");
				}
			} else if("group".equals(type)){
				String gid = content.getTarget_id()+"";
				HttpResponseWrapper result = APIProxy.getGroupMemberList(gid);
				if(result.isOK()){
					String userListJson = result.content;
					UserList userList = gson.fromJson(userListJson, UserList.class);
					ArrayList<User> list = userList.getUids();
					for(int i=0; i<list.size(); i++){
						User user = (User) list.get(i);
						long id = user.getUid();
						if(id!=content.getFrom_id()){
							SocketIOClient client = WebImServer.userNameToSessionCilentMap.get(id);	
							data.setToUserName(gid);
							data.setUserName(content.getFrom_id()+"");
							data.setMessage(content.getMsg_body().toString());
							data.setMsgType("group");
							if(client!=null){
								client.sendEvent("chatEvent", data);  // send message to web client
							} else {
								log.info("该用户此时不在线.");
							}
						}
					}   
				} else {
					log.info("获取群组成员失败.");
				}
			}
		}*/
		if(msg instanceof Packet){   //  im 业务
			SocketIOClient sessionClient = null;
			Packet protocol = (Packet) msg;
			int command = protocol.getHead().getCmd();
			switch (command) {
				case Command.JPUSH_IM.LOGIN:
					log.info("im login response...");
					Login loginBean = ProtocolUtil.getLogin(protocol);
					log.info("login data, username: "+loginBean.getUsername().toStringUtf8()+", password: "+loginBean.getPassword().toStringUtf8());
					Response loginResp = ProtocolUtil.getCommonResp(protocol);
					log.info("login response data: code: "+loginResp.getCode()+", message: "+loginResp.getMessage().toStringUtf8());
					break;
				case Command.JPUSH_IM.LOGOUT:
					log.info("im logout response...");
					Logout logoutBean = ProtocolUtil.getLogout(protocol);
					log.info("logout data, username: "+logoutBean.getUsername().toStringUtf8());
					Response logoutResp = ProtocolUtil.getCommonResp(protocol);
					log.info("logout response data: code: "+logoutResp.getCode()+", message: "+logoutResp.getMessage().toStringUtf8());
					if(logoutResp.getCode()==0){
						//  to do
					}
					break;
				case Command.JPUSH_IM.SENDMSG_SINGAL:
					log.info("im send single msg response...");
					SingleMsg singleMsgBean = ProtocolUtil.getSingleMsg(protocol);
					log.info("single msg data, target uid: "+singleMsgBean.getTargetUid()+", msgid: "+singleMsgBean.getMsgid()+", content: "+singleMsgBean.getContent().getContent().toStringUtf8());
					Response singleMsgResp = ProtocolUtil.getCommonResp(protocol);
					log.info("single msg response data: code: "+singleMsgResp.getCode()+", message: "+singleMsgResp.getMessage().toStringUtf8());
					// 模拟 Jpush 单发消息测试
					/*ChatObject singleMsgdata = new ChatObject();
					sessionClient = WebImServer.userNameToSessionCilentMap.get(singleMsgBean.getTargetUid());
					
					if(sessionClient!=null){
						log.info("get username's session client: "+sessionClient.getSessionId());

						ChatMessage content = gson.fromJson(singleMsgBean.getContent().getContent().toStringUtf8(), ChatMessage.class);
						singleMsgdata.setToUserName(content.getTarget_id()+"");
						singleMsgdata.setUserName(content.getFrom_id()+"");
						singleMsgdata.setMessage(content.getMsg_body().toString());
						singleMsgdata.setMsgType("single");
						if("text".endsWith(content.getMsg_type())){
							singleMsgdata.setContentType("text");
						} else if("image".endsWith(content.getMsg_type())){
							singleMsgdata.setContentType("image");
						}
						sessionClient.sendEvent("chatEvent", singleMsgdata);
					} else {
						log.info("用户不在线......");
					}*/
					break;
				case Command.JPUSH_IM.SENDMSG_GROUP:
					log.info("im send group msg response...");
					GroupMsg groupMsgBean = ProtocolUtil.getGroupMsg(protocol);
					log.info("group msg data, target gid: "+groupMsgBean.getTargetGid()+", msgid: "+groupMsgBean.getMsgid());
					Response groupMsgResp = ProtocolUtil.getCommonResp(protocol);
					log.info("group msg response data: code: "+groupMsgResp.getCode()+", message: "+groupMsgResp.getMessage().toStringUtf8());				
					break;
				case Command.JPUSH_IM.CREATE_GROUP:
					log.info("im create group msg response...");
					CreateGroup createGroupBean = ProtocolUtil.getCreateGroup(protocol);
					log.info("create group data, gid: "+createGroupBean.getGid());
					Response createGroupResp = ProtocolUtil.getCommonResp(protocol);
					log.info("create group response data: code: "+createGroupResp.getCode()+", message: "+createGroupResp.getMessage().toStringUtf8());
					break;
				case Command.JPUSH_IM.EXIT_GROUP:
					log.info("im exit group msg response...");
					ExitGroup exitGroupBean = ProtocolUtil.getExitGroup(protocol);
					log.info("exit group data, gid: "+exitGroupBean.getGid());
					Response exitGroupResp = protocol.getBody().getCommonRep();
					log.info("exit group response data: code: "+exitGroupResp.getCode()+", message: "+exitGroupResp.getMessage().toStringUtf8());
					break;
				case Command.JPUSH_IM.ADD_GROUP_MEMBER:
					log.info("im add group member msg response...");
					AddGroupMember addGroupMemberBean = ProtocolUtil.getAddGroupMember(protocol);
					log.info("add group member data, gid: "+addGroupMemberBean.getGid());
					Response addGroupMemberResp = ProtocolUtil.getCommonResp(protocol);
					log.info("add group member response data: code: "+addGroupMemberResp.getCode()+", message: "+addGroupMemberResp.getMessage().toStringUtf8());
					break;
				case Command.JPUSH_IM.DEL_GROUP_MEMBER:
					log.info("im delete group member msg response...");
					DelGroupMember delGroupMemberBean = ProtocolUtil.getDelGroupMember(protocol);
					log.info("del group member data, gid: "+delGroupMemberBean.getGid());
					Response delGroupMemberResp = ProtocolUtil.getCommonResp(protocol);
					log.info("del group member response data: code: "+delGroupMemberResp.getCode()+", message: "+delGroupMemberResp.getMessage().toStringUtf8());
					break;
				case Command.JPUSH_IM.UPDATE_GROUP_INFO:
					log.info("im update group info msg response...");
					UpdateGroupInfo bean = ProtocolUtil.getUpdateGroupInfo(protocol);
					log.info("update group info data, gid: "+bean.getGid()+", info: "+bean.getInfo());
					Response updateGroupInfoResp = ProtocolUtil.getCommonResp(protocol);
					log.info("update group info response data: code: "+updateGroupInfoResp.getCode()+", message: "+updateGroupInfoResp.getMessage().toStringUtf8());
					break;
					
				case Command.JPUSH_IM.SYNC_EVENT:
					log.info("im 业务事件通知...");
					EventNotification eventNotification = ProtocolUtil.getEventNotification(protocol);
					int sync_eventType = eventNotification.getEventType();
					long sync_uid = eventNotification.getFromUid();
					long sync_gid = eventNotification.getGid();
					long sync_toUid = eventNotification.getToUidlist(0);
					String description = eventNotification.getDescription().toStringUtf8();
					log.info("im event -- event type: "+sync_eventType+", uid: "+sync_uid+
								", gid: "+sync_gid+", toUid: "+sync_toUid+", desc: "+description);
					HashMap<String, Object> data = new HashMap<String, Object>();
					if(sync_eventType == Command.JPUSH_IM.ADD_GROUP_MEMBER){  // 添加群成员的事件通知
						HttpResponseWrapper _resultWrapper = APIProxy.getGroupMemberList(String.valueOf(sync_gid));
						if(_resultWrapper.isOK()){
							JsonParser parser = new JsonParser();
							JsonArray Jarray = parser.parse(_resultWrapper.content).getAsJsonArray();
							String memberName = "";
							for(JsonElement obj : Jarray){
								GroupMember member = gson.fromJson(obj, GroupMember.class);
								if(sync_toUid == member.getUid()){
									memberName = member.getUsername();
									String message = memberName+" 加入群聊";
									data.put("eventType", sync_eventType);
									data.put("gid", sync_gid);
									data.put("message", message);
								}
							}
						} else {
							log.info("get group member exception...");
						}
					} else if (sync_eventType == Command.JPUSH_IM.EXIT_GROUP || sync_eventType == Command.JPUSH_IM.DEL_GROUP_MEMBER){
						//String message = sync_toUid+" 退出群聊";
						data.put("eventType", sync_eventType);
						data.put("gid", sync_gid);
						data.put("toUid", sync_toUid);
					}
					long userId = WebImServer.pushChannelToUsernameMap.get(ctx.channel());
					sessionClient = WebImServer.userNameToSessionCilentMap.get(userId);
					if(sessionClient!=null){
						sessionClient.sendEvent("eventNotification", gson.toJson(data));
					} else {
						log.info("用户已断开");
					}
					break;
	
				case Command.JPUSH_IM.SYNC_MSG:
					log.info("im sync msg......");
					String appkey = protocol.getHead().getAppkey().toStringUtf8();
					ChatMsgSync chatMsgSync = ProtocolUtil.getChatMsgSync(protocol);
					int msgCount = chatMsgSync.getChatMsgCount();
					List<ChatMsg> chatMsgList = chatMsgSync.getChatMsgList();
					for(int i=0; i<msgCount; i++){
						ChatMsg chatMsg = chatMsgList.get(i);
						log.info("sync msg data: "+chatMsg.getContent().getContent().toStringUtf8());
						HashMap<String, Object> dataMap = gson.fromJson(chatMsg.getContent().getContent().toStringUtf8(), HashMap.class);
						String target_type = (String)dataMap.get("target_type");
						if("single".equals(target_type)){   //  single
							HttpResponseWrapper resultWrapper = APIProxy.getUserInfo(appkey, String.valueOf(dataMap.get("target_id")));
							if(resultWrapper.isOK()){
								User userInfo = gson.fromJson(resultWrapper.content, User.class);
								dataMap.put("target_id", userInfo.getUid());
								sessionClient = WebImServer.userNameToSessionCilentMap.get(userInfo.getUid());
							}
							ChatMessage content = gson.fromJson(gson.toJson(dataMap), ChatMessage.class);
							
							if(sessionClient!=null){
								log.info("get username's session client: "+sessionClient.getSessionId());
								ChatObject chatMsgData = new ChatObject();	
								chatMsgData.setToUserName(content.getTarget_id()+"");
								chatMsgData.setUserName(content.getFrom_id()+"");
								chatMsgData.setMessage(content.getMsg_body().toString());
								chatMsgData.setMsgType("single");
								if("text".equals(content.getMsg_type())){
									chatMsgData.setContentType("text");
								} else if("image".equals(content.getMsg_type())){
									chatMsgData.setContentType("image");
								} else if("voice".equals(content.getMsg_type())){
									chatMsgData.setContentType("voice");
								}
								sessionClient.sendEvent("chatEvent", chatMsgData);
							} else {
								log.info("用户不在线......");
							}
						} else if("group".equals(target_type)){   //  group
							long gid = Long.parseLong((String) dataMap.get("target_id"));
							ChatMessage content = gson.fromJson(gson.toJson(dataMap), ChatMessage.class);
							//  找群成员
							Channel channel = ctx.channel();
							if(channel!=null){
								long uid = WebImServer.pushChannelToUsernameMap.get(channel);
								sessionClient = WebImServer.userNameToSessionCilentMap.get(uid);
								ChatObject chatMsgData = new ChatObject();	
								chatMsgData.setToUserName(content.getTarget_id()+"");
								chatMsgData.setUserName(content.getFrom_id()+"");
								chatMsgData.setMessage(content.getMsg_body().toString());
								chatMsgData.setMsgType("group");
								if("text".endsWith(content.getMsg_type())){
									chatMsgData.setContentType("text");
								} else if("image".endsWith(content.getMsg_type())){
									chatMsgData.setContentType("image");
								} else if("voice".equals(content.getMsg_type())){
									chatMsgData.setContentType("voice");
								}
								if(uid!=content.getFrom_id()){
									sessionClient.sendEvent("chatEvent", chatMsgData);
								}
							}
							/*HttpResponseWrapper resultWrapper = APIProxy.getGroupMemberList(String.valueOf(gid));
							if(resultWrapper.isOK()){
								JsonParser parser = new JsonParser();
								JsonArray Jarray = parser.parse(resultWrapper.content).getAsJsonArray();
								for(JsonElement obj : Jarray){
									GroupMember member = gson.fromJson(obj, GroupMember.class);
									if(content.getFrom_id()!=member.getUid()){
										sessionClient = WebImServer.userNameToSessionCilentMap.get(member.getUid());
										if(sessionClient!=null){
											log.info("get username's session client: "+sessionClient.getSessionId());
											ChatObject chatMsgData = new ChatObject();	
											chatMsgData.setToUserName(content.getTarget_id()+"");
											chatMsgData.setUserName(content.getFrom_id()+"");
											chatMsgData.setMessage(content.getMsg_body().toString());
											chatMsgData.setMsgType("group");
											if("text".endsWith(content.getMsg_type())){
												chatMsgData.setContentType("text");
											} else if("image".endsWith(content.getMsg_type())){
												chatMsgData.setContentType("image");
											} else if("voice".equals(content.getMsg_type())){
												chatMsgData.setContentType("voice");
											}
											sessionClient.sendEvent("chatEvent", chatMsgData);
										} else {
											log.info("用户不在线......");
										}
									}
							    } 
							} else {
								log.info("获取群成员信息异常.");
							}*/
						}
					}	
					break;
					
				default:
					log.info("未定义的 IM 业务消息类型.");
					break;
			}
			
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		if(evt instanceof IdleStateEvent){
			IdleStateEvent e = (IdleStateEvent) evt;
			/*if(e.state()==IdleState.READER_IDLE){
				log.info("client heartbeat...client read idle...channel:"+ctx.channel().toString()+", count:"+count++);
			}*/
			if(e.state()==IdleState.WRITER_IDLE){
				log.info("client heartbeat...client write idle...channel:"+ctx.channel().toString());
				// 太长时间没发数据，发送心跳避免连接被断开
				HeartBeatRequest request = new HeartBeatRequest(1, 1, this.getSid(), this.getJuid());
				ctx.channel().writeAndFlush(request);
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	public PushLoginResponseBean getPushLoginResponseBean() {
		return pushLoginResponseBean;
	}

	public void setPushLoginResponseBean(PushLoginResponseBean pushLoginResponseBean) {
		this.pushLoginResponseBean = pushLoginResponseBean;
	}

	public int getSid() {
		return sid;
	}

	public void setSid(int sid) {
		this.sid = sid;
	}

	public long getJuid() {
		return juid;
	}

	public void setJuid(long juid) {
		this.juid = juid;
	}

}
