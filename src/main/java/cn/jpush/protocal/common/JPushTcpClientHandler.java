package cn.jpush.protocal.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import cn.jpush.protocal.im.req.proto.ImChatMsgSyncRequestProto;
import cn.jpush.protocal.im.resp.proto.ImLoginResponseProto;
import cn.jpush.protocal.im.response.ImLoginResponse;
import cn.jpush.protocal.push.HeartBeatRequest;
import cn.jpush.protocal.push.PushLoginResponseBean;
import cn.jpush.protocal.push.PushLogoutResponseBean;
import cn.jpush.protocal.push.PushMessageRequestBean;
import cn.jpush.protocal.push.PushRegResponseBean;
import cn.jpush.protocal.utils.APIProxy;
import cn.jpush.protocal.utils.BASE64Utils;
import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.HttpResponseWrapper;
import cn.jpush.protocal.utils.ProtocolUtil;
import cn.jpush.protocal.utils.StringUtils;
import cn.jpush.protocal.utils.TcpCode;
import cn.jpush.socketio.SocketIOClient;
import cn.jpush.webim.common.RedisClient;
import cn.jpush.webim.common.UidResourcesPool;
import cn.jpush.webim.server.WebImServer;
import cn.jpush.webim.socketio.bean.ChatMessage;
import cn.jpush.webim.socketio.bean.ChatMessageObject;
import cn.jpush.webim.socketio.bean.ChatObject;
import cn.jpush.webim.socketio.bean.ContracterObject;
import cn.jpush.webim.socketio.bean.Group;
import cn.jpush.webim.socketio.bean.GroupMember;
import cn.jpush.webim.socketio.bean.GroupMemberList;
import cn.jpush.webim.socketio.bean.MsgContentBean;
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
		log.info(String.format("handler: %s added", ctx.channel().toString()));
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) {
		Channel channel = ctx.channel();
		log.warn(String.format("handler: %s removed from push server", channel.toString()));
		// 下线相关用户
		/*if(channel!=null){
			long uid = WebImServer.pushChannelToUsernameMap.get(channel);
			if(0!=uid){
				SocketIOClient sessionClient = WebImServer.userNameToSessionCilentMap.get(uid);
				sessionClient.sendEvent("disconnect", "");
			}
		}*/
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		log.info(String.format("handler: %s active", ctx.channel().toString()));
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		log.info(String.format("handler: %s recv data from push server", ctx.channel().toString()));
		if(msg instanceof PushRegResponseBean){
			PushRegResponseBean bean = (PushRegResponseBean) msg;
			log.info(String.format("client handler resolve jpush reg response data: %s", gson.toJson(bean)));
			long juid = bean.getUid();
			String password = bean.getPasswd();
			log.info(String.format("client handler add jpush reg resp juid: %d and password: %s to pool", juid, password));
			UidResourcesPool.addUidToPool(juid, password);
			UidResourcesPool.capacityCountDown.countDown();
			if(UidResourcesPool.capacityCountDown.getCount()==0){
				UidResourcesPool.produceResourceSemaphore.release();
				ctx.channel().close();
			}
			log.info("client handler add jpush reg resp data to pool success.");
		}
		if(msg instanceof PushLoginResponseBean){
			pushLoginResponseBean = (PushLoginResponseBean) msg;
			WebImServer.pushLoginInCountDown.countDown();
			log.info(String.format("client handler resolve jpush login response data: %s", gson.toJson(pushLoginResponseBean)));
		}
		if(msg instanceof PushLogoutResponseBean){
			PushLogoutResponseBean bean = (PushLogoutResponseBean) msg;
			log.info(String.format("client handler resolve jpush logout response data: %s", gson.toJson(bean)));
		}
		if(msg instanceof Packet){  
			SocketIOClient sessionClient = null;
			Packet protocol = (Packet) msg;
			int command = protocol.getHead().getCmd();
			log.info(String.format("client handler resolve IM module, the IM Command is: %d", command));
			switch (command) {
				case Command.JPUSH_IM.LOGIN:
					log.info(String.format("client handler resolve IM login resp data: %s", protocol.toString()));
					Login loginBean = ProtocolUtil.getLogin(protocol);
					String userName = loginBean.getUsername().toStringUtf8();
					Response loginResp = ProtocolUtil.getCommonResp(protocol);
					int imLoginRespCode = loginResp.getCode();
					String imLoginRespMsg = loginResp.getMessage().toStringUtf8();
					log.info(String.format("client handler resolve IM login resp code: %d, message: %s", imLoginRespCode, imLoginRespMsg));
	
					if(imLoginRespCode==TcpCode.IM.SUCCESS){
						long uid = protocol.getHead().getUid();
						String password = loginBean.getPassword().toStringUtf8();	
						String stoken = (APIProxy.getToken(String.valueOf(uid), password)).content;
						Map tokenMap = gson.fromJson(stoken, HashMap.class);
						String _token = (String) tokenMap.get("token");
						String token = BASE64Utils.encodeString(uid+":"+_token);
						WebImServer.uidToTokenMap.put(uid, token);
						WebImServer.userToSessionCilentMap.get(userName).sendEvent("loginEventGetUID", uid);
						log.info(String.format("client handler send event: %s to webclient", "loginEventGetUID"));
						Channel channel = WebImServer.userToPushChannelMap.get(userName);
						WebImServer.userNameToPushChannelMap.put(uid, channel);
						WebImServer.pushChannelToUsernameMap.put(channel, uid);
						WebImServer.userToPushChannelMap.remove(userName);
					} 
					if(imLoginRespCode==TcpCode.IM.LOGIN_UNLEAGAL_PASSWORD){
						WebImServer.userToSessionCilentMap.get(userName).sendEvent("IMException", TcpCode.IM.LOGIN_UNLEAGAL_PASSWORD);
						log.warn(String.format("client handler send event: %s, exception code: %d", "IMException", TcpCode.IM.LOGIN_UNLEAGAL_PASSWORD));
					}
					
					break;
				case Command.JPUSH_IM.LOGOUT:
					log.info(String.format("client handler resolve IM logout resp data: %s",protocol.toString()));
					//Logout logoutBean = ProtocolUtil.getLogout(protocol);
					Response logoutResp = ProtocolUtil.getCommonResp(protocol);
					log.info("logout response data: code: "+logoutResp.getCode()+", message: "+logoutResp.getMessage().toStringUtf8());
					if(logoutResp.getCode()==0){
						//  to do
					}
					break;
				case Command.JPUSH_IM.SENDMSG_SINGAL:
					log.info(String.format("client handler resolve IM single msg resp data: %s", protocol.toString()));
					SingleMsg singleMsgBean = ProtocolUtil.getSingleMsg(protocol);
					Response singleMsgResp = ProtocolUtil.getCommonResp(protocol);
					int imSingleMsgRespCode = singleMsgResp.getCode();
					String imSingleMsgRespMsg = singleMsgResp.getMessage().toStringUtf8();
					log.info(String.format("client handler resolve IM single msg resp code: %d, message: %s", imSingleMsgRespCode, imSingleMsgRespMsg));
					//  消息发送状态下发
					@SuppressWarnings("unchecked")
					HashMap<String, Object> _dataMap = gson.fromJson(singleMsgBean.getContent().getContent().toStringUtf8(), HashMap.class);
					//String _appkey = (String) _dataMap.get("appKey");
					long ss_uid = protocol.getHead().getUid();
					//String ss_token = WebImServer.uidToTokenMap.get(ss_uid);
					
					sessionClient = WebImServer.userNameToSessionCilentMap.get(ss_uid);
					
					ChatMessageObject _content = gson.fromJson(gson.toJson(_dataMap), ChatMessageObject.class);
					if(sessionClient!=null){
						log.info("send single msg status to client");
						ChatObject chatMsgData = new ChatObject();	
						chatMsgData.setCode(imSingleMsgRespCode);
						chatMsgData.setCreate_time(_content.getCreate_time());
						sessionClient.sendEvent("msgFeedBackEvent", chatMsgData);
					} else {
						log.warn("用户不在线......");
					}
					
					break;
				case Command.JPUSH_IM.SENDMSG_GROUP:
					log.info(String.format("client handler resolve IM group msg resp data: %s", protocol.toString()));
					GroupMsg groupMsgBean = ProtocolUtil.getGroupMsg(protocol);
					Response groupMsgResp = ProtocolUtil.getCommonResp(protocol);
					int imGroupMsgRespCode = groupMsgResp.getCode();
					String imGroupMsgRespMsg = groupMsgResp.getMessage().toStringUtf8();
					log.info(String.format("client handler resolve IM group msg resp code: %d, message: %s", imGroupMsgRespCode, imGroupMsgRespMsg));			
					//  消息发送状态下发
					@SuppressWarnings("unchecked")
					HashMap<String, Object> gdataMap = gson.fromJson(groupMsgBean.getContent().getContent().toStringUtf8(), HashMap.class);
					//long gid = Long.parseLong((String) gdataMap.get("target_id"));
					ChatMessageObject gcontent = gson.fromJson(gson.toJson(gdataMap), ChatMessageObject.class);
					//  找群成员
					Channel mchannel = ctx.channel();
					if(mchannel!=null){
						long _uid = WebImServer.pushChannelToUsernameMap.get(mchannel);
						sessionClient = WebImServer.userNameToSessionCilentMap.get(_uid);
						ChatObject chatMsgData = new ChatObject();	
						chatMsgData.setCode(imGroupMsgRespCode);
						chatMsgData.setCreate_time(gcontent.getCreate_time());
						sessionClient.sendEvent("msgFeedBackEvent", chatMsgData);
					} else {
						log.warn("用户不在线....消息");
					}
					break;
				case Command.JPUSH_IM.CREATE_GROUP:
					log.info(String.format("client handler resolve IM create group resp data: %s", protocol.toString()));
					//CreateGroup createGroupBean = ProtocolUtil.getCreateGroup(protocol);
					//log.info("create group data, gid: "+createGroupBean.getGid());
					Response createGroupResp = ProtocolUtil.getCommonResp(protocol);
					log.info("create group response data: code: "+createGroupResp.getCode()+", message: "+createGroupResp.getMessage().toStringUtf8());
					break;
				case Command.JPUSH_IM.EXIT_GROUP:
					log.info(String.format("client handler resolve IM exit group resp data: %s", protocol.toString()));
					//ExitGroup exitGroupBean = ProtocolUtil.getExitGroup(protocol);
					//log.info("exit group data, gid: "+exitGroupBean.getGid());
					Response exitGroupResp = protocol.getBody().getCommonRep();
					log.info("exit group response data: code: "+exitGroupResp.getCode()+", message: "+exitGroupResp.getMessage().toStringUtf8());
					break;
				case Command.JPUSH_IM.ADD_GROUP_MEMBER:
					log.info(String.format("client handler resolve IM add group member resp data: %s", protocol.toString()));
					//AddGroupMember addGroupMemberBean = ProtocolUtil.getAddGroupMember(protocol);
					Response addGroupMemberResp = ProtocolUtil.getCommonResp(protocol);
					long uid = protocol.getHead().getUid();
					int addGroupMemRespcode = addGroupMemberResp.getCode();
					String addGroupMemRespMsg = addGroupMemberResp.getMessage().toStringUtf8();
					log.info(String.format("add group member response data: code: %d, message: %s", addGroupMemRespcode, addGroupMemRespMsg));
					sessionClient = WebImServer.userNameToSessionCilentMap.get(uid);
					if(addGroupMemRespcode==TcpCode.IM.ADDGROUP_USER_UNEXIST){
						if(null!=sessionClient){
							sessionClient.sendEvent("IMException", TcpCode.IM.ADDGROUP_USER_UNEXIST);
						}
					} else if(addGroupMemRespcode==TcpCode.IM.ADDGROUP_USER_REPEATADD){
						if(null!=sessionClient){
							sessionClient.sendEvent("IMException", TcpCode.IM.ADDGROUP_USER_REPEATADD);
						}
					}
					break;
				case Command.JPUSH_IM.DEL_GROUP_MEMBER:
					log.info(String.format("client handler resolve IM del group member data: %s", protocol.toString()));
					//DelGroupMember delGroupMemberBean = ProtocolUtil.getDelGroupMember(protocol);
					//log.info("del group member data, gid: "+delGroupMemberBean.getGid());
					Response delGroupMemberResp = ProtocolUtil.getCommonResp(protocol);
					int delGroupMemRespcode = delGroupMemberResp.getCode();
					String delGroupMemRespMsg = delGroupMemberResp.getMessage().toStringUtf8();
					log.info(String.format("del group member response data: code: %d, message: %s", delGroupMemRespcode, delGroupMemRespMsg));
					break;
				case Command.JPUSH_IM.UPDATE_GROUP_INFO:
					log.info(String.format("client handler resolve IM update group info data: %s", protocol.toString()));
					UpdateGroupInfo bean = ProtocolUtil.getUpdateGroupInfo(protocol);
					//log.info("update group info data, gid: "+bean.getGid()+", info: "+bean.getInfo());
					Response updateGroupInfoResp = ProtocolUtil.getCommonResp(protocol);
					int updateGroupInfoRespcode = updateGroupInfoResp.getCode();
					String updateGroupInfoRespMsg = updateGroupInfoResp.getMessage().toStringUtf8();
					log.info(String.format("update group info response data: code: %d, message: %s", updateGroupInfoRespcode, updateGroupInfoRespMsg));
					if(updateGroupInfoRespcode==TcpCode.IM.SUCCESS){
						log.info("send update group info success event to webclient");
						long _uid = protocol.getHead().getUid();
						Group group = new Group();
						group.setGid(bean.getGid());
						group.setName(bean.getName().toStringUtf8());
						sessionClient = WebImServer.userNameToSessionCilentMap.get(_uid);
						if(null!=sessionClient){
							sessionClient.sendEvent("updateGroupInfoEventNotification", group);
						} else {
							log.warn(String.format("user: %d is not online", _uid));
						}
					} else {
						log.warn(String.format("modify group info failture, code: %s, message: %s", updateGroupInfoRespcode, updateGroupInfoRespMsg));
					}
					break;
					
				case Command.JPUSH_IM.SYNC_EVENT:
					log.info(String.format("client handler resolve IM event sync data: %s", protocol.toString()));
					EventNotification eventNotification = ProtocolUtil.getEventNotification(protocol);
					int sync_eventType = eventNotification.getEventType();
					long sync_uid = eventNotification.getFromUid();
					long sync_gid = eventNotification.getGid();
					long sync_toUid = eventNotification.getToUidlist(0);
					//String description = eventNotification.getDescription().toStringUtf8();
					String mtoken = WebImServer.uidToTokenMap.get(sync_uid);
					HashMap<String, Object> data = new HashMap<String, Object>();
					data.put("eventId", eventNotification.getEventId());
					data.put("eventType", eventNotification.getEventType());
					if(sync_eventType == Command.JPUSH_IM.ADD_GROUP_MEMBER){  // 添加群成员的事件通知
						log.info("client handler im sync event call sdk api getGroupMemberList");
						HttpResponseWrapper mresultWrapper = APIProxy.getGroupMemberList(String.valueOf(sync_gid), mtoken);
						if(mresultWrapper.isOK()){
							JsonParser parser = new JsonParser();
							JsonArray Jarray = parser.parse(mresultWrapper.content).getAsJsonArray();
							String memberName = "";
							for(JsonElement obj : Jarray){
								GroupMember member = gson.fromJson(obj, GroupMember.class);
								long m_uid = member.getUid();
								if(sync_toUid == m_uid){
									HttpResponseWrapper resultWrapper = APIProxy.getUserInfoByUid(protocol.getHead().getAppkey().toStringUtf8(), String.valueOf(m_uid), mtoken);
									if(resultWrapper.isOK()){
										User userInfo = gson.fromJson(resultWrapper.content, User.class);
										memberName = userInfo.getUsername();
									}
									String message = memberName+" 加入群聊";
									data.put("eventType", sync_eventType);
									data.put("gid", sync_gid);
									data.put("memberUid", m_uid);
									data.put("username", memberName);
									data.put("message", message);
								}
							}
							log.info("client handler im sync event call sdk api getGroupMemberList success");
						} else {
							log.warn("client handler im sync event call sdk api getGroupMemberList failture");
						}
					} else if (sync_eventType == Command.JPUSH_IM.EXIT_GROUP || sync_eventType == Command.JPUSH_IM.DEL_GROUP_MEMBER){ // 退出群的事件通知
						//String message = sync_toUid+" 退出群聊";
						data.put("eventType", sync_eventType);
						data.put("gid", sync_gid);
						data.put("toUid", sync_toUid);
					}
					long userId = WebImServer.pushChannelToUsernameMap.get(ctx.channel());
					if(userId!=0){
						sessionClient = WebImServer.userNameToSessionCilentMap.get(userId);
					}
					if(sessionClient!=null){
						sessionClient.sendEvent("eventNotification", gson.toJson(data));
					} else {
						log.warn("用户已断开");
					}
					break;
	
				case Command.JPUSH_IM.SYNC_MSG:
					log.info(String.format("client handler resolve IM msg sync data: %s", protocol.toString()));
					//String appkey = protocol.getHead().getAppkey().toStringUtf8();
					ChatMsgSync chatMsgSync = ProtocolUtil.getChatMsgSync(protocol);
					int msgCount = chatMsgSync.getChatMsgCount();
					List<ChatMsg> chatMsgList = chatMsgSync.getChatMsgList();
					//long _uid = protocol.getHead().getUid();
					//String _mtoken = WebImServer.uidToTokenMap.get(_uid);
					for(int i=0; i<msgCount; i++){
						ChatMsg chatMsg = chatMsgList.get(i);
						@SuppressWarnings("unchecked")
						HashMap<String, Object> dataMap = gson.fromJson(chatMsg.getContent().getContent().toStringUtf8(), HashMap.class);
						String target_type = (String)dataMap.get("target_type");
						if("single".equals(target_type)){  
							Channel _channel = ctx.channel();
							long _muid = 0L;
							if(_channel!=null){
								_muid = WebImServer.pushChannelToUsernameMap.get(_channel);
								sessionClient = WebImServer.userNameToSessionCilentMap.get(_muid);
							}
							MsgContentBean content = gson.fromJson(gson.toJson(dataMap), MsgContentBean.class);
							
							if(sessionClient!=null){
								ChatObject chatMsgData = new ChatObject();	
								chatMsgData.setUid(chatMsg.getFromUid());
								chatMsgData.setToUserName(content.getTarget_id()+"");
								chatMsgData.setUserName(content.getFrom_id()+"");
								chatMsgData.setMessage(content.getMsg_body().toString());
								chatMsgData.setCreate_time(content.getCreate_time());
								chatMsgData.setMsgType("single");
								chatMsgData.setMessageId(chatMsg.getMsgid());  //  消息ID
								chatMsgData.setiMsgType(chatMsg.getMsgType());  //  消息类型
								if("text".equals(content.getMsg_type())){
									chatMsgData.setContentType("text");
								} else if("image".equals(content.getMsg_type())){
									chatMsgData.setContentType("image");
								} else if("voice".equals(content.getMsg_type())){
									chatMsgData.setContentType("voice");
								}
								sessionClient.sendEvent("chatEvent", chatMsgData);
								log.info(String.format("send ChatEvent Single Msg to Client: %s", gson.toJson(chatMsgData)));
							} else {
								log.warn("用户不在线......");
							}
						} else if("group".equals(target_type)){ 
							long gid = Long.parseLong((String) dataMap.get("target_id"));
							MsgContentBean content = gson.fromJson(gson.toJson(dataMap), MsgContentBean.class);
							//  找群成员
							Channel _channel = ctx.channel();
							if(_channel!=null){
								long _muid = WebImServer.pushChannelToUsernameMap.get(_channel);
								sessionClient = WebImServer.userNameToSessionCilentMap.get(_muid);
								ChatObject chatMsgData = new ChatObject();	
								chatMsgData.setUid(Long.parseLong(content.getTarget_id()));
								chatMsgData.setToUserName(content.getTarget_id()+"");
								chatMsgData.setUserName(content.getFrom_id()+"");
								chatMsgData.setMessage(content.getMsg_body().toString());
								chatMsgData.setCreate_time(content.getCreate_time());
								chatMsgData.setMsgType("group");
								chatMsgData.setMessageId(chatMsg.getMsgid());  //  消息ID
								chatMsgData.setiMsgType(chatMsg.getMsgType());  //  消息类型
								if("text".endsWith(content.getMsg_type())){
									chatMsgData.setContentType("text");
								} else if("image".endsWith(content.getMsg_type())){
									chatMsgData.setContentType("image");
								} else if("voice".equals(content.getMsg_type())){
									chatMsgData.setContentType("voice");
								}
								log.info("return group msg string: "+gson.toJson(chatMsgData));
								if(sessionClient!=null){
									sessionClient.sendEvent("chatEvent", chatMsgData);
									log.info(String.format("send ChatEvent Group Msg to Client: %s", gson.toJson(chatMsgData)));
								} else {
									log.warn(String.format("user: %d get connection to webclient is empty", _muid));
								}
							}
						}
					}	
					break;
					
				default:
					log.warn("未定义的 IM 业务消息类型.");
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
				log.info("client heartbeat...write idle:"+ctx.channel().toString());
				// 太长时间没发数据，发送心跳避免连接被断开
				HeartBeatRequest request = new HeartBeatRequest(2, 1, this.getSid(), this.getJuid());
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
