package cn.jpush.webim.server;

import io.netty.channel.Channel;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.imageio.ImageIO;

import jpushim.s2b.JpushimSdk2B.ChatMsg;
import jpushim.s2b.JpushimSdk2B.EventNotification;

import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.common.JPushTcpClient;
import cn.jpush.protocal.im.bean.AddGroupMemberRequestBean;
import cn.jpush.protocal.im.bean.CreateGroupRequestBean;
import cn.jpush.protocal.im.bean.DeleteGroupMemberRequestBean;
import cn.jpush.protocal.im.bean.ExitGroupRequestBean;
import cn.jpush.protocal.im.bean.LoginRequestBean;
import cn.jpush.protocal.im.bean.LogoutRequestBean;
import cn.jpush.protocal.im.bean.SendGroupMsgRequestBean;
import cn.jpush.protocal.im.bean.SendSingleMsgRequestBean;
import cn.jpush.protocal.im.bean.UpdateGroupInfoRequestBean;
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
import cn.jpush.protocal.push.PushLoginRequest;
import cn.jpush.protocal.push.PushLoginRequestBean;
import cn.jpush.protocal.push.PushLoginResponseBean;
import cn.jpush.protocal.utils.APIProxy;
import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.Configure;
import cn.jpush.protocal.utils.HttpResponseWrapper;
import cn.jpush.protocal.utils.ProtocolUtil;
import cn.jpush.protocal.utils.StringUtils;
import cn.jpush.protocal.utils.SystemConfig;
import cn.jpush.socketio.AckRequest;
import cn.jpush.socketio.Configuration;
import cn.jpush.socketio.SocketIOClient;
import cn.jpush.socketio.SocketIOServer;
import cn.jpush.socketio.Transport;
import cn.jpush.socketio.listener.ConnectListener;
import cn.jpush.socketio.listener.DataListener;
import cn.jpush.socketio.listener.DisconnectListener;
import cn.jpush.webim.common.RedisClient;
import cn.jpush.webim.common.UidResourcesPool;
import cn.jpush.webim.socketio.bean.AddFriendCmd;
import cn.jpush.webim.socketio.bean.AddOrDelGroupMember;
import cn.jpush.webim.socketio.bean.ChatMessage;
import cn.jpush.webim.socketio.bean.ChatObject;
import cn.jpush.webim.socketio.bean.ContracterObject;
import cn.jpush.webim.socketio.bean.CreateGroupBean;
import cn.jpush.webim.socketio.bean.EventSyncRespBean;
import cn.jpush.webim.socketio.bean.ExitGroupBean;
import cn.jpush.webim.socketio.bean.Group;
import cn.jpush.webim.socketio.bean.GroupList;
import cn.jpush.webim.socketio.bean.GroupMember;
import cn.jpush.webim.socketio.bean.LogoutBean;
import cn.jpush.webim.socketio.bean.MsgBody;
import cn.jpush.webim.socketio.bean.MsgContentBean;
import cn.jpush.webim.socketio.bean.SdkCommonErrorRespObject;
import cn.jpush.webim.socketio.bean.SdkCommonSuccessRespObject;
import cn.jpush.webim.socketio.bean.SdkConfigObject;
import cn.jpush.webim.socketio.bean.SdkGetUserInfoObject;
import cn.jpush.webim.socketio.bean.SdkLoginObject;
import cn.jpush.webim.socketio.bean.SdkSendTextMsgObject;
import cn.jpush.webim.socketio.bean.SdkSyncEventRespObject;
import cn.jpush.webim.socketio.bean.SdkSyncMsgRespObject;
import cn.jpush.webim.socketio.bean.SdkUserInfo;
import cn.jpush.webim.socketio.bean.UpdateGroupInfoBean;
import cn.jpush.webim.socketio.bean.User;
import cn.jpush.webim.socketio.bean.UserList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.qiniu.api.auth.digest.Mac;
import com.qiniu.api.rs.PutPolicy;

/**
 * Web Im 业务 Server
 *
 */
public class WebImServer {
	private static Logger log = (Logger) LoggerFactory.getLogger(WebImServer.class);
	public static HashMap<String, SocketIOClient> userNameToSessionCilentMap = new HashMap<String, SocketIOClient>();  //  用户 --> 客户端
	public static HashMap<SocketIOClient, String> sessionClientToUserNameMap = new HashMap<SocketIOClient, String>();  //  客户端  --> 用户
	public static HashMap<String, Channel> userNameToPushChannelMap = new HashMap<String, Channel>();   //  用户 --> IM Server
	public static HashMap<Channel, String> pushChannelToUsernameMap = new HashMap<Channel, String>();   //  IM Server --> 用户
	private static final int PORT = SystemConfig.getIntProperty("webim.server.port");
	public static CountDownLatch pushLoginInCountDown;
	private RedisClient redisClient;
	private Gson gson = new Gson();
	private Configuration config;
	private SocketIOServer server;
	private JPushTcpClient jpushIMTcpClient;
	
	public void init() {
		 config = new Configuration();
		 config.setPort(PORT);
		 config.setTransports(Transport.WEBSOCKET);
		 server = new SocketIOServer(config);
		 redisClient = new RedisClient();
	}
	
	public void configMessageEventAndStart() throws InterruptedException{
		if(config==null||server==null){
			log.warn("you have not init the config and server. please do this first.");
			return;
		} 
		
		 //用户连接
		 server.addConnectListener(new ConnectListener() {
			@Override
			public void onConnect(SocketIOClient client) {
				log.debug(String.format("connect from web client -- the session id is %s, client transport method is %s", client.getSessionId(), client.getTransport()));
				client.sendEvent("onConnected", "");
			}
		 }); 
		 
		 // 用户断开
		 server.addDisconnectListener(new DisconnectListener() {
			@Override
			public void onDisconnect(SocketIOClient client) {
				if(client!=null){
					client.sendEvent("onDisConnected", "");
					log.debug(String.format("the connection is disconnect -- the session id is %s", client.getSessionId()));
					String userName = "";
					if(WebImServer.sessionClientToUserNameMap!=null){
						try{
							userName = WebImServer.sessionClientToUserNameMap.get(client);
						} catch (Exception e){
							log.warn(String.format("user disconnect exception: %s", e.getMessage()));
						}
					}
					if(StringUtils.isNotEmpty(userName)){
						WebImServer.userNameToSessionCilentMap.remove(userName);
						Channel channel = WebImServer.userNameToPushChannelMap.get(userName);
						WebImServer.userNameToPushChannelMap.remove(userName);
						WebImServer.sessionClientToUserNameMap.remove(client);
						if(channel!=null){
							WebImServer.pushChannelToUsernameMap.remove(channel);
							channel.close();   //  断开与push server的长连接
						} else {
							log.warn(String.format("user: %s get channel to jpush server is empty", userName));
						}
					}
				}	
			}
		});
		 
		// 用户配置检查
		server.addEventListener("onConfigValidate", SdkConfigObject.class, new DataListener<SdkConfigObject>() {
			@Override
			public void onData(SocketIOClient client, SdkConfigObject data,
					AckRequest ackSender) throws Exception {
				String appKey = data.getAppKey();
				String timestamp = data.getTimestamp();
				String randomStr = data.getRandomStr();
				String signature = data.getSignature();
				if(StringUtils.isEmpty(signature)||StringUtils.isEmpty(randomStr)
						||StringUtils.isEmpty(timestamp)||StringUtils.isEmpty(appKey)){
					log.warn(String.format("Sdk config arguments exception"));
				} else {
					// TODO 加入验证过程
					SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject();
					log.info("config resp： "+gson.toJson(resp));
					client.sendEvent("onConfigValidate", gson.toJson(resp));
				}
			}
		});
		 
		 // 用户登陆
		 server.addEventListener("login", SdkLoginObject.class, new DataListener<SdkLoginObject>() {
			@Override
			public void onData(SocketIOClient client, SdkLoginObject data,
					AckRequest ackSender) throws Exception {
				log.info(String.format("user: %s login", data.getUsername()));
				String appkey = data.getAppKey();
				String username = data.getUsername();
				String password = data.getPassword();
				if(StringUtils.isEmpty(appkey)||StringUtils.isEmpty(username)||StringUtils.isEmpty(password)){
					log.warn("user loginEvent pass empty data exception");
					return;
				}
				log.info(String.format("user info appkey: %s, username: %s, password: ", appkey, username, password));
			
				Map<String, String> juidData = UidResourcesPool.getUidAndPassword();
				long juid = Long.parseLong(String.valueOf(juidData.get("uid")));
				String juid_password = String.valueOf(juidData.get("password"));
				pushLoginInCountDown = new CountDownLatch(1);
				 
				jpushIMTcpClient = new JPushTcpClient(appkey);
				Channel pushChannel = jpushIMTcpClient.getChannel();
				
				// 关系绑定
				userNameToSessionCilentMap.put(username, client);
				sessionClientToUserNameMap.put(client, username);
				userNameToPushChannelMap.put(username, pushChannel);
				pushChannelToUsernameMap.put(pushChannel, username);
				
				PushLoginRequestBean pushLoginBean = new PushLoginRequestBean(juid, "a", ProtocolUtil.md5Encrypt(juid_password), 10800, appkey, 0);
				pushChannel.writeAndFlush(pushLoginBean);
				log.info(String.format("user: %s begin jpush login, juid: %d, password: %s", username, juid, juid_password));
				
				pushLoginInCountDown.await();  //  等待push login返回数据
				PushLoginResponseBean pushLoginResponseBean = jpushIMTcpClient.getjPushClientHandler().getPushLoginResponseBean();
				int sid = pushLoginResponseBean.getSid();
				log.info(String.format("user: %s jpush login response, code: %d, sid: %d", username, pushLoginResponseBean.getResponse_code(), sid));
				
				//  存储用户信息
				Jedis jedis = null;
				try{
					jedis = redisClient.getJeids();
					Map<String, String> map = new HashMap<String, String>();
					map.put("appKey", appkey);
					map.put("password", password);
					map.put("juid", String.valueOf(juid));
					map.put("sid", String.valueOf(sid));
					map.put("juidPassword", juid_password);
					jedis.hmset(username, map);
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				
				// 设置数据 用于心跳
				jpushIMTcpClient.getjPushClientHandler().setSid(pushLoginResponseBean.getSid());
				jpushIMTcpClient.getjPushClientHandler().setJuid(juid);
				
				// IM Login
				long rid = StringUtils.getRID();
				LoginRequestBean bean = new LoginRequestBean(username, StringUtils.toMD5(password));
				List<Integer> cookie = new ArrayList<Integer>();
				ImLoginRequestProto req = new ImLoginRequestProto(Command.JPUSH_IM.LOGIN, 1, 0, pushLoginResponseBean.getSid(), juid, appkey, rid, cookie, bean);
				pushChannel.writeAndFlush(req);
				log.info(String.format("user: %s begin IM login", username));
			}
		 });
		 
		 //  用户退出
		 server.addEventListener("logout", LogoutBean.class, new DataListener<LogoutBean>() {
			@Override
			public void onData(SocketIOClient client, LogoutBean data,
					AckRequest ackSender) throws Exception {
				String userName = sessionClientToUserNameMap.get(client);
				Jedis jedis = null;
				String appKey = "";
				int sid = 0;
				long juid = 0L;
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(userName, "appKey", "sid", "juid", "uid");
					appKey = dataList.get(0);
					sid = Integer.parseInt(dataList.get(1));
					juid = Long.parseLong(dataList.get(2));
					uid = Long.parseLong(dataList.get(3));
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				Channel channel = userNameToPushChannelMap.get(userName);
				if(channel==null){
					 log.info("当前用户与jpush的连接已断开.");
					 client.sendEvent("logout", "true");
				} else {
					log.info(String.format("user: %s auto logout", userName));
					LogoutRequestBean bean = new LogoutRequestBean(userName);
					List<Integer> cookie = new ArrayList<Integer>();
					ImLogoutRequestProto req = new ImLogoutRequestProto(Command.JPUSH_IM.LOGOUT, 1, uid, appKey, sid, juid, cookie, bean);
					channel.writeAndFlush(req);
				}
			}
		});
		 
		// 获取用户信息
		server.addEventListener("getUserInfo", SdkGetUserInfoObject.class, new DataListener<SdkGetUserInfoObject>() {
			@Override
			public void onData(SocketIOClient client, SdkGetUserInfoObject data,
					AckRequest ackSender) throws Exception {
				String username = data.getUsername();
				if(StringUtils.isEmpty(username)){
					log.warn("user getUserInfo pass empty data exception");
					return;
				}
				String userName = sessionClientToUserNameMap.get(client);
				Jedis jedis = null;
				String appKey = "";
				String token = "";
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(userName, "appKey", "token");
					appKey = dataList.get(0);
					token = dataList.get(1);
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				HttpResponseWrapper responseWrapper = APIProxy.getUserInfo(appKey, username, token);
				if(responseWrapper.isOK()){
					//SdkUserInfo userInfo = gson.fromJson(responseWrapper.content, SdkUserInfo.class);
					SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject();
					resp.setContent(responseWrapper.content);
					log.info("userinfo: "+responseWrapper.content);
					client.sendEvent("getUserInfo", gson.toJson(resp));
				} else {
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
					resp.setErrorInfo(1000, "call sdk-api exception");
					client.sendEvent("getUserInfo", gson.toJson(resp));
				}	
			}
		});
		
		// 用户聊天
		server.addEventListener("sendTextMessage", SdkSendTextMsgObject.class, new DataListener<SdkSendTextMsgObject>() {
			 @Override
			 public void onData(SocketIOClient client, SdkSendTextMsgObject data, AckRequest ackRequest) {
				String userName = sessionClientToUserNameMap.get(client);
				long rid = StringUtils.getRID();
				int version = 1;
				Jedis jedis = null;
				String appKey = "";
				int sid = 0;
				long juid = 0L;
				String token = "";
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(userName, "appKey", "sid", "juid", "token", "uid");
					appKey = dataList.get(0);
					sid = Integer.parseInt(dataList.get(1));
					juid = Long.parseLong(dataList.get(2));
					token = dataList.get(3);
					uid = Long.parseLong(dataList.get(4));
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				 
				 MsgContentBean msgContent = new MsgContentBean();
				 msgContent.setVersion(version);
				 msgContent.setTarget_type(data.getTargetType());
				 msgContent.setTarget_id(data.getTargetId());
				 msgContent.setTarget_name(data.getTargetId());
				 msgContent.setFrom_type("user");
				 msgContent.setFrom_platform("web");
				 msgContent.setFrom_id(userName);
				 msgContent.setFrom_name(userName);
				 msgContent.setCreate_time(StringUtils.getCreateTime());
				 msgContent.setMsg_type("text");
				 MsgBody msgBody = new MsgBody();
				 msgBody.setText(data.getText());
				 msgContent.setMsg_body(msgBody);
				 
				 log.info(String.format("user: %s send chat msg, content is: ", userName, gson.toJson(data)));
				 Channel channel = userNameToPushChannelMap.get(userName);
				 if(channel==null){
					 log.warn("current user get channel to push server exception");
					 return;
				 } 
				 if("single".equals(data.getTargetType())){
					 HttpResponseWrapper responseWrapper = APIProxy.getUserInfo(appKey, data.getTargetId(), token);
					 long target_uid = 0L;
					 if(responseWrapper.isOK()){
						 SdkUserInfo userInfo = gson.fromJson(responseWrapper.content, SdkUserInfo.class);
						 target_uid = userInfo.getUid();
						 SendSingleMsgRequestBean bean = new SendSingleMsgRequestBean(target_uid, gson.toJson(msgContent));  //  为了和移动端保持一致，注意这里用target_name来存储id，避免再查一次
						 List<Integer> cookie = new ArrayList<Integer>();
						 ImSendSingleMsgRequestProto req = new ImSendSingleMsgRequestProto(Command.JPUSH_IM.SENDMSG_SINGAL, 1, uid, appKey, sid,  juid, rid, cookie, bean);
						 channel.writeAndFlush(req);
						 log.info(String.format("user: %s begin send single chat msg", userName));
					 } else {
						 log.warn(String.format("user: %s sendTextMessage call sdk-api getUserInfo exception", userName)); 
					 }
				 } else if("group".equals(data.getTargetType())){
					 SendGroupMsgRequestBean bean = new SendGroupMsgRequestBean(Long.parseLong(data.getTargetId()), gson.toJson(msgContent));
					 List<Integer> cookie = new ArrayList<Integer>();
					 ImSendGroupMsgRequestProto req = new ImSendGroupMsgRequestProto(Command.JPUSH_IM.SENDMSG_GROUP, 1, uid, appKey, sid, juid, rid, cookie, bean);
					 channel.writeAndFlush(req);
					 log.info(String.format("user: %s begin send group chat msg", userName));
				 }
			 }
		 });
		
		//  离线消息送达返回
		server.addEventListener("onMessageReceivedResp", SdkSyncMsgRespObject.class, new DataListener<SdkSyncMsgRespObject>() {
			@Override
			public void onData(SocketIOClient client, SdkSyncMsgRespObject data,
					AckRequest ackSender) throws Exception {
				String userName = sessionClientToUserNameMap.get(client);
				long rid = StringUtils.getRID();
				Jedis jedis = null;
				String appKey = "";
				long uid = 0L;
				int sid = 0;
				long juid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(userName, "appKey", "sid", "juid", "uid");
					appKey = dataList.get(0);
					sid = Integer.parseInt(dataList.get(1));
					juid = Long.parseLong(dataList.get(2));
					uid = Long.parseLong(dataList.get(3));
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				long messageId = data.getMessageId();
				int iMsgType = data.getiMsgType();   
				long from_uid = data.getFrom_uid();
				long from_gid = data.getFrom_gid();
				log.info(String.format("user: %d sync msg feedback, msgId is %d, msgType is %d", uid, messageId, iMsgType));
				List<Integer> cookie = new ArrayList<Integer>();
				ChatMsg.Builder chatMsg = ChatMsg.newBuilder();
				chatMsg.setMsgid(messageId);
				chatMsg.setMsgType(iMsgType);
				chatMsg.setFromUid(from_uid);
				chatMsg.setFromGid(from_gid);
				ChatMsg chatMsgBean = chatMsg.build();
				ImChatMsgSyncRequestProto req = new ImChatMsgSyncRequestProto(Command.JPUSH_IM.SYNC_MSG, 1, uid,
						appKey, rid, sid, juid, cookie, chatMsgBean);
				Channel channel = userNameToPushChannelMap.get(userName);
				channel.writeAndFlush(req);
				log.info(String.format("user: %d send sync msg feedback request", uid));
			}
		});
			
		//  事件下发送达返回
		server.addEventListener("onEventReceivedResp", SdkSyncEventRespObject.class, new DataListener<SdkSyncEventRespObject>() {
			@Override
			public void onData(SocketIOClient client, SdkSyncEventRespObject data,
					AckRequest ackSender) throws Exception {
				long rid = StringUtils.getRID();
				long eventId = data.getEventId();
				int eventType = data.getEventType();
				long from_uid = data.getFrom_uid();
				long gid = data.getGid();
				String userName = sessionClientToUserNameMap.get(client);
				Jedis jedis = null;
				String appKey = "";
				int sid = 0;
				long juid = 0L;
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(userName, "appKey", "sid", "juid", "uid");
					appKey = dataList.get(0);
					sid = Integer.parseInt(dataList.get(1));
					juid = Long.parseLong(dataList.get(2));
					uid = Long.parseLong(dataList.get(3));
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				log.info(String.format("user: %d sync event feedback, eventId is %d, eventType is %d", uid, eventId, eventType));
				List<Integer> cookie = new ArrayList<Integer>();
				EventNotification.Builder eventNotification = EventNotification.newBuilder();
				eventNotification.setEventId(eventId);
				eventNotification.setEventType(eventType);
				eventNotification.setFromUid(from_uid);
				eventNotification.setGid(gid);
				EventNotification eventNotificationBean = eventNotification.build();
				ImEventSyncRequestProto req = new ImEventSyncRequestProto(Command.JPUSH_IM.SYNC_EVENT, 1, uid,
						appKey, rid, sid, juid, cookie, eventNotificationBean);
				Channel channel = userNameToPushChannelMap.get(userName);
				channel.writeAndFlush(req);
				log.info(String.format("user: %d send sync event feedback request", uid));
			}
		});
		 
		 // 获取联系人列表
//		 server.addEventListener("getContracterList", ContracterObject.class, new DataListener<ContracterObject>() {
//			@Override
//			public void onData(SocketIOClient client, ContracterObject data,
//					AckRequest ackSender) throws Exception {
//				String appkey = data.getAppKey();
//				String user_name = data.getUser_name();
//				long uid = data.getUid();
//				String token = WebImServer.uidToTokenMap.get(uid);
//				log.info(String.format("user: %s begin to get contracter list", user_name));
//				if(user_name==null || "".equals(user_name) || data==null){
//					log.warn(String.format("user getcontracter error, because data is empty"));
//					return;
//				}
//				List<User> contractersList = new ArrayList<User>();
//				//  模拟用户列表
//				for(int i=1; i<4; i++){
//					String username = "p00"+i;
//					HttpResponseWrapper userResult = APIProxy.getUserInfo(appkey, username, token);
//					if(userResult.isOK()){
//						User userInfo = gson.fromJson(userResult.content, User.class);
//						contractersList.add(userInfo);
//					}
//				}
//				client.sendEvent("getContracterList", contractersList);
//				log.info(String.format("user: %s get contracter success", user_name));
//			}	 
//		});
		 
		// 获取群组列表
		server.addEventListener("getGroupsList", ContracterObject.class, new DataListener<ContracterObject>() {
			@Override
			public void onData(SocketIOClient client, ContracterObject data,
					AckRequest ackSender) throws Exception {
				String userName = sessionClientToUserNameMap.get(client);
				Jedis jedis = null;
				String token = "";
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(userName, "token", "uid");
					token = dataList.get(0);
					uid = Long.parseLong(dataList.get(1));
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				Channel channel = userNameToPushChannelMap.get(userName);
				log.info(String.format("getGroupList toke: %s", token));
				log.info(String.format("user: %d begin to get group list", uid));
				if(uid==0 || data==null){
					log.warn(String.format("user getgrouplist error, because data is empty"));
					return;
				}
				List<Group> groupsList = new ArrayList<Group>();
				HttpResponseWrapper result = APIProxy.getGroupList(String.valueOf(uid), token);
				if(result.isOK()){
					String groupListJson = result.content;
					List<Long> gidList = gson.fromJson(groupListJson, new TypeToken<ArrayList<Long>>(){}.getType());
					for(Long gid: gidList){
						HttpResponseWrapper groupInfoResult = APIProxy.getGroupInfo(String.valueOf(gid), token);
						if(groupInfoResult.isOK()){
							String groupInfoJson = groupInfoResult.content;
							Group group = gson.fromJson(groupInfoJson, Group.class);
							groupsList.add(group);
						}
					}
				} else {
					log.warn(String.format("get groups failture because call sdk api exception: %s", result.content));
				}
				client.sendEvent("getGroupsList", groupsList);
				log.info(String.format("user: %d get group list success", uid));
			}	 
		});
		
		// 获取群组成员列表
		server.addEventListener("getGroupMemberList", HashMap.class, new DataListener<HashMap>() {
			@SuppressWarnings("rawtypes")
			@Override
			public void onData(SocketIOClient client, HashMap data,
					AckRequest ackSender) throws Exception {
				if(data==null || data.equals("")){
					log.warn(String.format("user getcontracter error, because data is empty"));
					return;
				}
				String gid = String.valueOf(data.get("gid"));
				String userName = sessionClientToUserNameMap.get(client);
				Jedis jedis = null;
				String appKey = "";
				String token = "";
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(userName, "appKey", "token", "uid");
					appKey = dataList.get(0);
					token = dataList.get(1);
					uid = Long.parseLong(dataList.get(2));
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				Channel channel = userNameToPushChannelMap.get(userName);
				ArrayList<HashMap> resultList = new ArrayList<HashMap>();
				log.info(String.format("user: %d begin get group: %s members", uid, gid));
				HttpResponseWrapper resultWrapper = APIProxy.getGroupMemberList(gid, token);
				if(resultWrapper.isOK()){
					List<GroupMember> groupList = gson.fromJson(resultWrapper.content, new TypeToken<ArrayList<GroupMember>>(){}.getType());
					for(GroupMember member:groupList){
						HttpResponseWrapper wrapper = APIProxy.getUserInfoByUid(appKey, String.valueOf(member.getUid()), token);
						if(wrapper.isOK()){
							HashMap map = gson.fromJson(wrapper.content, HashMap.class);
							resultList.add(map);
						} 
					}
					client.sendEvent("getGroupMemberList", gson.toJson(resultList));
					log.warn(String.format("user: %d get group: %s member success", uid, gid));
				} else {
					log.warn(String.format("user: %d get group: %s member exception because call sdk api", uid, gid));
					client.sendEvent("getGroupMemberList", "false");
				}
			}	 
		});
		
		 // 用户聊天
		/*server.addEventListener("chatEvent", ChatMessage.class, new DataListener<ChatMessage>() {
			 @Override
			 public void onData(SocketIOClient client, ChatMessage data, AckRequest ackRequest) {
				 String appKey = data.getAppKey();
				 int sid = data.getSid(); 
				 Channel channel = userNameToPushChannelMap.get(data.getFrom_id());
				 
				 long rid = data.getRid();  // 消息标示id
				 MsgContentBean msgContent = new MsgContentBean();
				 msgContent.setVersion(Integer.parseInt(data.getVersion()));
				 msgContent.setTarget_type(data.getTarget_type());
				 msgContent.setTarget_id(data.getTarget_id());
				 msgContent.setTarget_name(data.getTarget_name());
				 msgContent.setFrom_type(data.getFrom_type());
				 msgContent.setFrom_id(data.getFrom_name());
				 msgContent.setFrom_name(data.getFrom_name());
				 msgContent.setFrom_platform("web");
				 msgContent.setCreate_time(data.getCreate_time());
				 msgContent.setMsg_type(data.getMsg_type());
				 msgContent.setMsg_body(gson.fromJson(data.getMsg_body().toString(), MsgBody.class));
				 
				 String userName = data.getFrom_name();
				 log.info(String.format("user: %s send chat msg, content is: ", userName, gson.toJson(data)));
				 
				 if(channel==null){
					 log.warn("current user get channel to push server exception");
					 return;
				 } 
				 if("single".equals(data.getTarget_type())){
					 SendSingleMsgRequestBean bean = new SendSingleMsgRequestBean(Long.parseLong(data.getTarget_name()), gson.toJson(msgContent));  //  为了和移动端保持一致，注意这里用target_name来存储id，避免再查一次
					 List<Integer> cookie = new ArrayList<Integer>();
					 ImSendSingleMsgRequestProto req = new ImSendSingleMsgRequestProto(Command.JPUSH_IM.SENDMSG_SINGAL, 1, data.getFrom_id(), appKey, sid, data.getJuid(), rid, cookie, bean);
					 channel.writeAndFlush(req);
					 log.info(String.format("user: %s begin send single chat msg", userName));
				 } else if("group".equals(data.getTarget_type())){
					 SendGroupMsgRequestBean bean = new SendGroupMsgRequestBean(Long.parseLong(data.getTarget_id()), gson.toJson(msgContent));
					 List<Integer> cookie = new ArrayList<Integer>();
					 ImSendGroupMsgRequestProto req = new ImSendGroupMsgRequestProto(Command.JPUSH_IM.SENDMSG_GROUP, 1, data.getFrom_id(), appKey, sid, data.getJuid(), rid, cookie, bean);
					 channel.writeAndFlush(req);
					 log.info(String.format("user: %s begin send group chat msg", userName));
				 }
			 }
		 });*/
		 
		 //  用户获取上传token
		 server.addEventListener("getUploadToken", String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data,
					AckRequest ackSender) throws Exception {
				log.info("user get upload token");
				Mac mac = new Mac(Configure.QNCloudInterface.QN_ACCESS_KEY, Configure.QNCloudInterface.QN_SECRET_KEY);
				PutPolicy putPolicy = new PutPolicy(Configure.QNCloudInterface.QN_BUCKETNAME);
				putPolicy.expires = 14400;
				String token = putPolicy.token(mac);
				client.sendEvent("getUploadToken", token);
				log.info("user get upload token success");
			}
		 });
		 
		 //  向客户端返回上传的文件的属性信息
		 server.addEventListener("getUploadPicMetaInfo", String.class, new DataListener<String>(){
			@Override
			public void onData(SocketIOClient client, String data,
					AckRequest ackSender) throws Exception {
				log.info("user get upload file metainfo");
				URL url = new URL(data);
				URLConnection conn = url.openConnection();
	         conn.setConnectTimeout(5 * 1000);
				InputStream inStream = conn.getInputStream();
			   BufferedImage src = ImageIO.read(inStream); // 读入文件
		      int width = src.getWidth(); // 得到源图宽
		      int height = src.getHeight(); // 得到源图长
				CheckedInputStream cis = new CheckedInputStream(inStream, new CRC32());
	         byte[] buf = new byte[128];
	         while(cis.read(buf) >= 0) {}
	         long checksum = cis.getChecksum().getValue();
	         inStream.close();
	         Map<String, Object> map = new HashMap<String, Object>();
	         map.put("width", width);
	         map.put("height", height);
	         map.put("crc32", checksum);
	         client.sendEvent("getUploadPicMetaInfo", gson.toJson(map));
	         log.info("user get upload file metainfo success");
			}
		 });
		
		//  更新群组名称
		server.addEventListener("updateGroupName", UpdateGroupInfoBean.class, new DataListener<UpdateGroupInfoBean>(){
			@Override
			public void onData(SocketIOClient client, UpdateGroupInfoBean data,
					AckRequest ackSender) throws Exception {
				String appKey = data.getAppKey();
				long gid = data.getGid();
				long uid = data.getUid();
				int sid = data.getSid();
				long rid = data.getRid();
				long juid = data.getJuid();
				String group_name = data.getGroup_name();
				String userName = data.getUser_name();
				log.info(String.format("user: %s update group: %d name, new group name is %s", userName, gid, group_name));
				UpdateGroupInfoRequestBean bean = new UpdateGroupInfoRequestBean(gid, group_name, "");
				List<Integer> cookie = new ArrayList<Integer>();
				ImUpdateGroupInfoRequestProto req = new ImUpdateGroupInfoRequestProto(Command.JPUSH_IM.UPDATE_GROUP_INFO, 1, uid, appKey, rid, sid, juid, cookie, bean);
				Channel channel = userNameToPushChannelMap.get(data.getUid());
				channel.writeAndFlush(req);
				log.info(String.format("user: %s send update group name request", userName));
			}
		});
		
		//  添加群组成员
		server.addEventListener("addGroupMember", AddOrDelGroupMember.class, new DataListener<AddOrDelGroupMember>(){
			@Override
				public void onData(SocketIOClient client, AddOrDelGroupMember data,
						AckRequest ackSender) throws Exception {
					String user_name = data.getUsername();  //  列表
					long gid = data.getGid();
					String userName = sessionClientToUserNameMap.get(client);
					log.info(String.format("user: %s add group member, the member is %s", userName, user_name));
					Jedis jedis = null;
					String appKey = "";
					int sid = 0;
					long uid = 0L;
					String token = "";
					long addUid = 0L;
					long juid = 0L;
					long rid = StringUtils.getRID();
					try{
						jedis = redisClient.getJeids();
						List<String> dataList = jedis.hmget(userName, "appKey", "sid", "uid", "token", "juid");
						appKey = dataList.get(0);
						sid = Integer.parseInt(dataList.get(1));
						uid = Long.parseLong(dataList.get(2));
						token = dataList.get(3);
						juid = Long.parseLong(dataList.get(4));
					} catch (JedisConnectionException e) {
						log.error(e.getMessage());
						redisClient.returnBrokenResource(jedis);
						throw new JedisConnectionException(e);
					} finally {
						redisClient.returnResource(jedis);
					}
					log.info("call sdk api: getUserInfo");
					HttpResponseWrapper resultWrapper = APIProxy.getUserInfo(appKey, user_name, token);
					if(resultWrapper.isOK()){
						User userInfo = gson.fromJson(resultWrapper.content, User.class);
						addUid = userInfo.getUid();
						log.info("success call sdk api: getUserInfo");
					}
					List<Long> list = new ArrayList<Long>();
					list.add(addUid);
					AddGroupMemberRequestBean bean = new AddGroupMemberRequestBean(gid, 1, list);
					List<Integer> cookie = new ArrayList<Integer>();
					ImAddGroupMemberRequestProto req = new ImAddGroupMemberRequestProto(Command.JPUSH_IM.ADD_GROUP_MEMBER, 1, uid, appKey, rid, sid, juid,cookie, bean);
					Channel channel = userNameToPushChannelMap.get(userName);
					channel.writeAndFlush(req);
					log.info(String.format("user: %d begin send add group member request", uid));
				}
		});
		
		//   删除群组成员
		server.addEventListener("delGroupMember", AddOrDelGroupMember.class, new DataListener<AddOrDelGroupMember>(){
		@Override
			public void onData(SocketIOClient client, AddOrDelGroupMember data,
					AckRequest ackSender) throws Exception {
				long delUid = data.getToUid();  // username list
				long gid = data.getGid();
				long rid = StringUtils.getRID();
				String userName = sessionClientToUserNameMap.get(client);
				Jedis jedis = null;
				String appKey = "";
				int sid = 0;
				long uid = 0L;
				String token = "";
				long addUid = 0L;
				long juid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(userName, "appKey", "sid", "uid", "token", "juid");
					appKey = dataList.get(0);
					sid = Integer.parseInt(dataList.get(1));
					uid = Long.parseLong(dataList.get(2));
					token = dataList.get(3);
					juid = Long.parseLong(dataList.get(4));
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				log.info(String.format("user: %d delete group: %d member uid: %d", uid, gid, delUid));
				List<Long> list = new ArrayList<Long>();
				list.add(delUid);	
				DeleteGroupMemberRequestBean bean = new DeleteGroupMemberRequestBean(gid, 1, list);
				List<Integer> cookie = new ArrayList<Integer>();
				ImDeleteGroupMemberRequestProto req = new ImDeleteGroupMemberRequestProto(Command.JPUSH_IM.DEL_GROUP_MEMBER, 1, uid, appKey, rid, sid, juid, cookie, bean);
				Channel channel = userNameToPushChannelMap.get(uid);
				channel.writeAndFlush(req);
				log.info(String.format("user: %d send delGroupMember request", uid));
			}
		});
		 
		 
		/* -----------------------------------------  to check ---------------------------------------------*/
		//  添加好友事件
		server.addEventListener("addFriendCmd", String.class, new DataListener<String>(){
		 	@Override
			public void onData(SocketIOClient client, String data,
					AckRequest ackSender) throws Exception {
				log.info("add new friend event");
			}
		});
		
		//  删除好友
		server.addEventListener("delFriendCmd", String.class, new DataListener<String>(){
		 	@Override
			public void onData(SocketIOClient client, String data,
					AckRequest ackSender) throws Exception {
		 		log.info("delete friend event");
			}
		});
		
		//  修改好友备注
		server.addEventListener("updateFriendNickNameCmd", String.class, new DataListener<String>(){
		 	@Override
			public void onData(SocketIOClient client, String data,
					AckRequest ackSender) throws Exception {
		 		log.info("update friend nickname event");
			}
		});
		
		//  创建群组
		server.addEventListener("createGroupCmd", CreateGroupBean.class, new DataListener<CreateGroupBean>(){
		 	@Override
			public void onData(SocketIOClient client, CreateGroupBean data,
					AckRequest ackSender) throws Exception {
		 		log.info("create group event");
		 		String userName = sessionClientToUserNameMap.get(client);
		 		if(StringUtils.isEmpty(userName)){
		 			log.warn(String.format("through client get username exception"));
		 			return;
		 		}
		 		long rid = StringUtils.getRID();
				Jedis jedis = null;
				String appKey = "";
				int sid = 0;
				long juid = 0L;
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(userName, "appKey", "sid", "juid", "uid");
					appKey = dataList.get(0);
					sid = Integer.parseInt(dataList.get(1));
					juid = Long.parseLong(dataList.get(2));
					uid = Long.parseLong(dataList.get(3));
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				String group_name = data.getGroup_name();
				String group_desc = data.getGroup_desc();
				int group_level = 200;
				int flag = 0;
				CreateGroupRequestBean bean = new CreateGroupRequestBean(group_name, group_desc, group_level, flag);
				List<Integer> cookie = new ArrayList<Integer>();
				ImCreateGroupRequestProto req = new ImCreateGroupRequestProto(Command.JPUSH_IM.CREATE_GROUP, 1, uid, appKey, rid, sid, juid, cookie, bean);
				Channel channel = userNameToPushChannelMap.get(userName);
				channel.writeAndFlush(req);
			}
		});
		
		//  退出群组
		server.addEventListener("exitGroupCmd", ExitGroupBean.class, new DataListener<ExitGroupBean>(){
		 	@Override
			public void onData(SocketIOClient client, ExitGroupBean data,
					AckRequest ackSender) throws Exception {
		 		log.info("exit group event");
				long gid = data.getGid();
				long rid = StringUtils.getRID();
				String userName = sessionClientToUserNameMap.get(client);
				Jedis jedis = null;
				String appKey = "";
				int sid = 0;
				long juid = 0L;
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(userName, "appKey", "sid", "juid", "uid");
					appKey = dataList.get(0);
					sid = Integer.parseInt(dataList.get(1));
					juid = Long.parseLong(dataList.get(2));
					uid = Long.parseLong(dataList.get(3));
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				ExitGroupRequestBean bean = new ExitGroupRequestBean(gid);
				List<Integer> cookie = new ArrayList<Integer>();
				ImExitGroupRequestProto req = new ImExitGroupRequestProto(Command.JPUSH_IM.EXIT_GROUP, 1, uid, appKey, rid, sid, juid, cookie, bean);
				Channel channel = userNameToPushChannelMap.get(userName);
				channel.writeAndFlush(req);
			}
		});
		/* ------------------------------------------  to check -------------------------------------------------*/
		
		 server.start();
	}
	
	public static void main(String[] args) throws InterruptedException {
		WebImServer socketServer = new WebImServer();
		socketServer.init();
		socketServer.configMessageEventAndStart();
	}

}
