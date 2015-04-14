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
import cn.jpush.webim.socketio.bean.Group;
import cn.jpush.webim.socketio.bean.LogoutBean;
import cn.jpush.webim.socketio.bean.MsgBody;
import cn.jpush.webim.socketio.bean.MsgContentBean;
import cn.jpush.webim.socketio.bean.SdkAddOrRemoveGroupMembersObject;
import cn.jpush.webim.socketio.bean.SdkCommonErrorRespObject;
import cn.jpush.webim.socketio.bean.SdkCommonSuccessRespObject;
import cn.jpush.webim.socketio.bean.SdkConfigObject;
import cn.jpush.webim.socketio.bean.SdkCreateGroupObject;
import cn.jpush.webim.socketio.bean.SdkExitGroupObject;
import cn.jpush.webim.socketio.bean.SdkGetGroupInfoObject;
import cn.jpush.webim.socketio.bean.SdkGetUserInfoObject;
import cn.jpush.webim.socketio.bean.SdkLoginObject;
import cn.jpush.webim.socketio.bean.SdkSendTextMsgObject;
import cn.jpush.webim.socketio.bean.SdkSyncEventRespObject;
import cn.jpush.webim.socketio.bean.SdkSyncMsgRespObject;
import cn.jpush.webim.socketio.bean.SdkUpdateGroupInfoObject;
import cn.jpush.webim.socketio.bean.SdkUserInfo;
import cn.jpush.webim.socketio.bean.UpdateGroupInfoBean;
import cn.jpush.webim.socketio.bean.User;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qiniu.api.auth.digest.Mac;
import com.qiniu.api.rs.PutPolicy;

/**
 * Web Im 业务 Server
 *
 */
public class WebImServer {
	private static Logger log = (Logger) LoggerFactory.getLogger(WebImServer.class);
	public static HashMap<String, SocketIOClient> userNameToSessionCilentMap = new HashMap<String, SocketIOClient>();  //  appKey:用户名 --> 客户端
	public static HashMap<SocketIOClient, String> sessionClientToUserNameMap = new HashMap<SocketIOClient, String>();  //  客户端  --> appKey:用户名
	public static HashMap<String, Channel> userNameToPushChannelMap = new HashMap<String, Channel>();   //  appKey:用户名 --> IM Server
	public static HashMap<Channel, String> pushChannelToUsernameMap = new HashMap<Channel, String>();   //  IM Server --> appKey:用户名
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
					client.sendEvent("onDisconnected", "");
					log.debug(String.format("the connection is disconnect -- the session id is %s", client.getSessionId()));
					String kan = "";
					if(WebImServer.sessionClientToUserNameMap!=null){
						try{
							kan = WebImServer.sessionClientToUserNameMap.get(client);
						} catch (Exception e){
							log.warn(String.format("user disconnect exception: %s", e.getMessage()));
						}
					}
					if(StringUtils.isNotEmpty(kan)){
						WebImServer.userNameToSessionCilentMap.remove(kan);
						Channel channel = WebImServer.userNameToPushChannelMap.get(kan);
						WebImServer.userNameToPushChannelMap.remove(kan);
						WebImServer.sessionClientToUserNameMap.remove(client);
						if(channel!=null){
							WebImServer.pushChannelToUsernameMap.remove(channel);
							channel.close();   //  断开与push server的长连接
						} else {
							log.warn(String.format("user: %s get channel to jpush server is empty", kan));
						}
					}
				}	
			}
		});
		 
		// 用户配置检查
		server.addEventListener("config", SdkConfigObject.class, new DataListener<SdkConfigObject>() {
			@Override
			public void onData(SocketIOClient client, SdkConfigObject data,
					AckRequest ackSender) throws Exception {
				String appKey = data.getAppKey();
				String timestamp = data.getTimestamp();
				String randomStr = data.getRandom_str();
				String signature = data.getSignature();
				if(StringUtils.isEmpty(signature)||StringUtils.isEmpty(randomStr)
						||StringUtils.isEmpty(timestamp)||StringUtils.isEmpty(appKey)){
					log.warn(String.format("Sdk config arguments exception"));
					return;
				} else {
					// TODO 加入验证过程
					SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject();
					log.info("config resp： "+gson.toJson(resp));
					client.sendEvent("config", gson.toJson(resp));
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
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
					resp.setErrorInfo(1000, "您还未配置");
					client.sendEvent("login", gson.toJson(resp));
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
				userNameToSessionCilentMap.put(appkey+":"+username, client);
				sessionClientToUserNameMap.put(client, appkey+":"+username);
				userNameToPushChannelMap.put(appkey+":"+username, pushChannel);
				pushChannelToUsernameMap.put(pushChannel, appkey+":"+username);
				
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
					jedis.hmset(appkey+":"+username, map);
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
				String appKey = "";
				String userName = "";
				String keyAndname = sessionClientToUserNameMap.get(client);
				if(StringUtils.isEmpty(keyAndname)){
					log.warn("user have logout");
					return;
				} else {
					appKey = StringUtils.getAppKey(keyAndname);
					userName = StringUtils.getUserName(keyAndname);
					if(StringUtils.isEmpty(appKey)||StringUtils.isEmpty(userName)){
						log.warn("resovle username exception");
						return;
					}
				}
				Jedis jedis = null;
				int sid = 0;
				long juid = 0L;
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(appKey+":"+userName, "appKey", "sid", "juid", "uid");
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
				Channel channel = userNameToPushChannelMap.get(appKey+":"+userName);
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
				String appKey = "";
				String userName = "";
				String keyAndname = sessionClientToUserNameMap.get(client);
				if(StringUtils.isEmpty(keyAndname)){
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
					resp.setErrorInfo(1000, "您还未登陆");
					client.sendEvent("getUserInfo", gson.toJson(resp));
					return;
				} else {
					appKey = StringUtils.getAppKey(keyAndname);
					userName = StringUtils.getUserName(keyAndname);
					if(StringUtils.isEmpty(appKey)||StringUtils.isEmpty(userName)){
						log.warn("resovle username exception");
						return;
					}
				}
				Jedis jedis = null;
				String token = "";
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(appKey+":"+userName, "appKey", "token");
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
				String appKey = "";
				String userName = "";
				String keyAndname = sessionClientToUserNameMap.get(client);
				if(StringUtils.isEmpty(keyAndname)){
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
					resp.setErrorInfo(1000, "您还未登陆");
					client.sendEvent("sendTextMessage", gson.toJson(resp));
					return;
				} else {
					appKey = StringUtils.getAppKey(keyAndname);
					userName = StringUtils.getUserName(keyAndname);
					if(StringUtils.isEmpty(appKey)||StringUtils.isEmpty(userName)){
						log.warn("resovle username exception");
						return;
					}
				}
				long rid = StringUtils.getRID();
				int version = 1;
				Jedis jedis = null;
				int sid = 0;
				long juid = 0L;
				String token = "";
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(appKey+":"+userName, "appKey", "sid", "juid", "token", "uid");
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
				 msgContent.setTarget_type(data.getTarget_type());
				 msgContent.setTarget_id(data.getTarget_id());
				 msgContent.setTarget_name(data.getTarget_id());
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
				 Channel channel = userNameToPushChannelMap.get(appKey+":"+userName);
				 if(channel==null){
					 log.warn("current user get channel to push server exception");
					 return;
				 } 
				 if("single".equals(data.getTarget_type())){
					 HttpResponseWrapper responseWrapper = APIProxy.getUserInfo(appKey, data.getTarget_id(), token);
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
				 } else if("group".equals(data.getTarget_type())){
					 SendGroupMsgRequestBean bean = new SendGroupMsgRequestBean(Long.parseLong(data.getTarget_id()), gson.toJson(msgContent));
					 List<Integer> cookie = new ArrayList<Integer>();
					 ImSendGroupMsgRequestProto req = new ImSendGroupMsgRequestProto(Command.JPUSH_IM.SENDMSG_GROUP, 1, uid, appKey, sid, juid, rid, cookie, bean);
					 channel.writeAndFlush(req);
					 log.info(String.format("user: %s begin send group chat msg", userName));
				 }
			 }
		 });
		
		//  离线消息送达返回
		server.addEventListener("respMessageReceived", SdkSyncMsgRespObject.class, new DataListener<SdkSyncMsgRespObject>() {
			@Override
			public void onData(SocketIOClient client, SdkSyncMsgRespObject data,
					AckRequest ackSender) throws Exception {
				String appKey = "";
				String userName = "";
				String keyAndname = sessionClientToUserNameMap.get(client);
				if(StringUtils.isEmpty(keyAndname)){
					log.warn("user have logout");
					return;
				} else {
					appKey = StringUtils.getAppKey(keyAndname);
					userName = StringUtils.getUserName(keyAndname);
					if(StringUtils.isEmpty(appKey)||StringUtils.isEmpty(userName)){
						log.warn("resovle username exception");
						return;
					}
				}
				long rid = StringUtils.getRID();
				Jedis jedis = null;
				long uid = 0L;
				int sid = 0;
				long juid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(appKey+":"+userName, "appKey", "sid", "juid", "uid");
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
				long messageId = data.getMessage_id();
				int iMsgType = data.getMsg_type();   
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
				Channel channel = userNameToPushChannelMap.get(appKey+":"+userName);
				channel.writeAndFlush(req);
				log.info(String.format("user: %d send sync msg feedback request", uid));
			}
		});
			
		//  事件下发送达返回
		server.addEventListener("respEventReceived", SdkSyncEventRespObject.class, new DataListener<SdkSyncEventRespObject>() {
			@Override
			public void onData(SocketIOClient client, SdkSyncEventRespObject data,
					AckRequest ackSender) throws Exception {
				long rid = StringUtils.getRID();
				long eventId = data.getEvent_id();
				int eventType = data.getEvent_type();
				long from_uid = data.getFrom_uid();
				long gid = data.getGid();
				String appKey = "";
				String userName = "";
				String keyAndname = sessionClientToUserNameMap.get(client);
				if(StringUtils.isEmpty(keyAndname)){
					log.warn("user have logout");
					return;
				} else {
					appKey = StringUtils.getAppKey(keyAndname);
					userName = StringUtils.getUserName(keyAndname);
					if(StringUtils.isEmpty(appKey)||StringUtils.isEmpty(userName)){
						log.warn("resovle username exception");
						return;
					}
				}
				Jedis jedis = null;
				int sid = 0;
				long juid = 0L;
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(appKey+":"+userName, "appKey", "sid", "juid", "uid");
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
				Channel channel = userNameToPushChannelMap.get(appKey+":"+userName);
				channel.writeAndFlush(req);
				log.info(String.format("user: %d send sync event feedback request", uid));
			}
		});
		 
		//  创建群组
		server.addEventListener("createGroup", SdkCreateGroupObject.class, new DataListener<SdkCreateGroupObject>(){
		 	@Override
			public void onData(SocketIOClient client, SdkCreateGroupObject data,
					AckRequest ackSender) throws Exception {
		 		log.info("create group event");
		 		String groupname = data.getGroup_name();
		 		String group_description = data.getGroup_description();
		 		if(StringUtils.isEmpty(groupname)||StringUtils.isEmpty(group_description)){
		 			log.warn(String.format("createGroup pass empty arguments exception"));
		 			return;
		 		}
		 		String appKey = "";
		 		String userName = "";
		 		String keyAndname = sessionClientToUserNameMap.get(client);
		 		if(StringUtils.isEmpty(keyAndname)){
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
					resp.setErrorInfo(1000, "您还未登陆");
					client.sendEvent("createGroup", gson.toJson(resp));
					return;
				} else {
					appKey = StringUtils.getAppKey(keyAndname);
					userName = StringUtils.getUserName(keyAndname);
					if(StringUtils.isEmpty(appKey)||StringUtils.isEmpty(userName)){
						log.warn("resovle username exception");
						return;
					}
				}
		 		long rid = StringUtils.getRID();
				Jedis jedis = null;
				int sid = 0;
				long juid = 0L;
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(appKey+":"+userName, "appKey", "sid", "juid", "uid");
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
				int group_level = 200;
				int flag = 0;
				CreateGroupRequestBean bean = new CreateGroupRequestBean(groupname, group_description, group_level, flag);
				List<Integer> cookie = new ArrayList<Integer>();
				ImCreateGroupRequestProto req = new ImCreateGroupRequestProto(Command.JPUSH_IM.CREATE_GROUP, 1, uid, appKey, rid, sid, juid, cookie, bean);
				Channel channel = userNameToPushChannelMap.get(appKey+":"+userName);
				channel.writeAndFlush(req);
			}
		});
		
		// 获取群组信息
		server.addEventListener("getGroupInfo", SdkGetGroupInfoObject.class, new DataListener<SdkGetGroupInfoObject>() {
			@Override
			public void onData(SocketIOClient client, SdkGetGroupInfoObject data,
					AckRequest ackSender) throws Exception {
				long group_id = data.getGroup_id();
				if(0==group_id){
					log.warn("user getGroupInfo pass empty data exception");
					return;
				}
				String appKey = "";
				String userName = "";
				String keyAndname = sessionClientToUserNameMap.get(client);
				if(StringUtils.isEmpty(keyAndname)){
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
					resp.setErrorInfo(1000, "您还未登陆");
					client.sendEvent("getGroupInfo", gson.toJson(resp));
					return;
				} else {
					appKey = StringUtils.getAppKey(keyAndname);
					userName = StringUtils.getUserName(keyAndname);
					if(StringUtils.isEmpty(appKey)||StringUtils.isEmpty(userName)){
						log.warn("resovle username exception");
						return;
					}
				}
				Jedis jedis = null;
				String token = "";
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(appKey+":"+userName, "token");
					token = dataList.get(0);
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				HttpResponseWrapper responseWrapper = APIProxy.getGroupInfo(String.valueOf(group_id), token);
				if(responseWrapper.isOK()){
					SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject();
					resp.setContent(responseWrapper.content);
					log.info("userinfo: "+responseWrapper.content);
					client.sendEvent("getGroupInfo", gson.toJson(resp));
				} else {
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
					resp.setErrorInfo(1000, "call sdk-api exception");
					client.sendEvent("getGroupInfo", gson.toJson(resp));
				}	
			}
		});
		
		//  添加群组成员
		server.addEventListener("addGroupMembers", SdkAddOrRemoveGroupMembersObject.class, new DataListener<SdkAddOrRemoveGroupMembersObject>(){
			@Override
				public void onData(SocketIOClient client, SdkAddOrRemoveGroupMembersObject data,
						AckRequest ackSender) throws Exception {
					long group_id = data.getGroup_id();
					List<String> member_usernames = data.getMember_usernames();
					int memberCount = member_usernames.size();
					String appKey = "";
					String userName = "";
					String keyAndname = sessionClientToUserNameMap.get(client);
					if(StringUtils.isEmpty(keyAndname)){
						SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
						resp.setErrorInfo(1000, "您还未登陆");
						client.sendEvent("addGroupMembers", gson.toJson(resp));
						return;
					} else {
						appKey = StringUtils.getAppKey(keyAndname);
						userName = StringUtils.getUserName(keyAndname);
						if(StringUtils.isEmpty(appKey)||StringUtils.isEmpty(userName)){
							log.warn("resovle username exception");
							return;
						}
					}
					log.info(String.format("user: %s add group member", userName));
					Jedis jedis = null;
					int sid = 0;
					long uid = 0L;
					String token = "";
					long addUid = 0L;
					long juid = 0L;
					long rid = StringUtils.getRID();
					try{
						jedis = redisClient.getJeids();
						List<String> dataList = jedis.hmget(appKey+":"+userName, "appKey", "sid", "uid", "token", "juid");
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
					List<Long> list = new ArrayList<Long>();
					for(String name: member_usernames){
						HttpResponseWrapper resultWrapper = APIProxy.getUserInfo(appKey, name, token);
						if(resultWrapper.isOK()){
							User userInfo = gson.fromJson(resultWrapper.content, User.class);
							addUid = userInfo.getUid();
							list.add(addUid);
							log.info("success call sdk api: getUserInfo");
						}
					}
					AddGroupMemberRequestBean bean = new AddGroupMemberRequestBean(group_id, memberCount, list);
					List<Integer> cookie = new ArrayList<Integer>();
					ImAddGroupMemberRequestProto req = new ImAddGroupMemberRequestProto(Command.JPUSH_IM.ADD_GROUP_MEMBER, 1, uid, appKey, rid, sid, juid,cookie, bean);
					Channel channel = userNameToPushChannelMap.get(appKey+":"+userName);
					channel.writeAndFlush(req);
					log.info(String.format("user: %d begin send add group member request", uid));
				}
		});
		
		//   删除群组成员
		server.addEventListener("removeGroupMembers", SdkAddOrRemoveGroupMembersObject.class, new DataListener<SdkAddOrRemoveGroupMembersObject>(){
		@Override
			public void onData(SocketIOClient client, SdkAddOrRemoveGroupMembersObject data,
					AckRequest ackSender) throws Exception {
				long group_id = data.getGroup_id();
				List<String> member_usernames = data.getMember_usernames();
				int memberCount = member_usernames.size();
				long rid = StringUtils.getRID();
				String appKey = "";
				String userName = "";
				String keyAndname = sessionClientToUserNameMap.get(client);
				if(StringUtils.isEmpty(keyAndname)){
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
					resp.setErrorInfo(1000, "您还未登陆");
					client.sendEvent("removeGroupMembers", gson.toJson(resp));
					return;
				} else {
					appKey = StringUtils.getAppKey(keyAndname);
					userName = StringUtils.getUserName(keyAndname);
					if(StringUtils.isEmpty(appKey)||StringUtils.isEmpty(userName)){
						log.warn("resovle username exception");
						return;
					}
				}
				Jedis jedis = null;
				int sid = 0;
				long uid = 0L;
				String token = "";
				long addUid = 0L;
				long juid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(appKey+":"+userName, "appKey", "sid", "uid", "token", "juid");
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
				log.info(String.format("user: %d delete group", uid));
				List<Long> list = new ArrayList<Long>();
				for(String name: member_usernames){
					HttpResponseWrapper resultWrapper = APIProxy.getUserInfo(appKey, name, token);
					if(resultWrapper.isOK()){
						User userInfo = gson.fromJson(resultWrapper.content, User.class);
						addUid = userInfo.getUid();
						list.add(addUid);
						log.info("success call sdk api: getUserInfo");
					}
				}
				DeleteGroupMemberRequestBean bean = new DeleteGroupMemberRequestBean(group_id, memberCount, list);
				List<Integer> cookie = new ArrayList<Integer>();
				ImDeleteGroupMemberRequestProto req = new ImDeleteGroupMemberRequestProto(Command.JPUSH_IM.DEL_GROUP_MEMBER, 1, uid, appKey, rid, sid, juid, cookie, bean);
				Channel channel = userNameToPushChannelMap.get(appKey+":"+userName);
				channel.writeAndFlush(req);
				log.info(String.format("user: %d send delGroupMember request", uid));
			}
		});
		
		//  退出群组
		server.addEventListener("exitGroup", SdkExitGroupObject.class, new DataListener<SdkExitGroupObject>(){
		 	@Override
			public void onData(SocketIOClient client, SdkExitGroupObject data,
					AckRequest ackSender) throws Exception {
		 		log.info("exit group event");
				long group_id = data.getGroup_id();
				long rid = StringUtils.getRID();
				String appKey = "";
				String userName = "";
				String keyAndname = sessionClientToUserNameMap.get(client);
				if(StringUtils.isEmpty(keyAndname)){
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
					resp.setErrorInfo(1000, "您还未登陆");
					client.sendEvent("exitGroup", gson.toJson(resp));
					return;
				} else {
					appKey = StringUtils.getAppKey(keyAndname);
					userName = StringUtils.getUserName(keyAndname);
					if(StringUtils.isEmpty(appKey)||StringUtils.isEmpty(userName)){
						log.warn("resovle username exception");
						return;
					}
				}
				Jedis jedis = null;
				int sid = 0;
				long juid = 0L;
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(appKey+":"+userName, "appKey", "sid", "juid", "uid");
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
				ExitGroupRequestBean bean = new ExitGroupRequestBean(group_id);
				List<Integer> cookie = new ArrayList<Integer>();
				ImExitGroupRequestProto req = new ImExitGroupRequestProto(Command.JPUSH_IM.EXIT_GROUP, 1, uid, appKey, rid, sid, juid, cookie, bean);
				Channel channel = userNameToPushChannelMap.get(appKey+":"+userName);
				channel.writeAndFlush(req);
			}
		});
		
		// 获取群组列表
		server.addEventListener("getGroupList", String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data,
					AckRequest ackSender) throws Exception {
				String appKey = "";
				String userName = "";
				String keyAndname = sessionClientToUserNameMap.get(client);
				if(StringUtils.isEmpty(keyAndname)){
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
					resp.setErrorInfo(1000, "您还未登陆");
					client.sendEvent("getGroupList", gson.toJson(resp));
					return;
				} else {
					appKey = StringUtils.getAppKey(keyAndname);
					userName = StringUtils.getUserName(keyAndname);
					if(StringUtils.isEmpty(appKey)||StringUtils.isEmpty(userName)){
						log.warn("resovle username exception");
						return;
					}
				}
				Jedis jedis = null;
				String token = "";
				long uid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(appKey+":"+userName, "token", "uid");
					token = dataList.get(0);
					uid = Long.parseLong(dataList.get(1));
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				log.info(String.format("user: %s begin to get group list", userName));
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
					SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject();
					resp.setContent(gson.toJson(groupsList));
					client.sendEvent("getGroupList", gson.toJson(resp));
					log.info(String.format("user: %d get group list success", uid));
				} else {
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
					resp.setErrorInfo(1000, "call sdk-api exception");
					client.sendEvent("getGroupList", gson.toJson(resp));
					log.warn(String.format("get groups failture because call sdk api exception: %s", result.content));
				}
			}	
		});
		
		//  更新群组信息
		server.addEventListener("updateGroupInfo", SdkUpdateGroupInfoObject.class, new DataListener<SdkUpdateGroupInfoObject>(){
			@Override
			public void onData(SocketIOClient client, SdkUpdateGroupInfoObject data,
					AckRequest ackSender) throws Exception {
				long gid = data.getGroup_id();
				String group_name = data.getGroup_name();
				String group_desc = data.getGroup_description();
				String appKey = "";
				String userName = "";
				String keyAndname = sessionClientToUserNameMap.get(client);
				if(StringUtils.isEmpty(keyAndname)){
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject();
					resp.setErrorInfo(1000, "您还未登陆");
					client.sendEvent("updateGroupInfo", gson.toJson(resp));
					return;
				} else {
					appKey = StringUtils.getAppKey(keyAndname);
					userName = StringUtils.getUserName(keyAndname);
					if(StringUtils.isEmpty(appKey)||StringUtils.isEmpty(userName)){
						log.warn("resovle username exception");
						return;
					}
				}
				Jedis jedis = null;
				long rid = StringUtils.getRID();
				long uid = 0L;
				int sid = 0;
				long juid = 0L;
				try{
					jedis = redisClient.getJeids();
					List<String> dataList = jedis.hmget(appKey+":"+userName, "appKey", "uid", "sid", "juid");
					appKey = dataList.get(0);
					uid = Long.parseLong(dataList.get(1));
					sid = Integer.parseInt(dataList.get(2));
					juid = Long.parseLong(dataList.get(3));
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				log.info(String.format("user: %s update group: %d name, new group name is %s", userName, gid, group_name));
				UpdateGroupInfoRequestBean bean = new UpdateGroupInfoRequestBean(gid, group_name, group_desc);
				List<Integer> cookie = new ArrayList<Integer>();
				ImUpdateGroupInfoRequestProto req = new ImUpdateGroupInfoRequestProto(Command.JPUSH_IM.UPDATE_GROUP_INFO, 1, uid, appKey, rid, sid, juid, cookie, bean);
				Channel channel = userNameToPushChannelMap.get(appKey+":"+userName);
				channel.writeAndFlush(req);
				log.info(String.format("user: %s send update group name request", userName));
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
		
		
		// 获取群组成员列表
//		server.addEventListener("getGroupMemberList", HashMap.class, new DataListener<HashMap>() {
//			@SuppressWarnings("rawtypes")
//			@Override
//			public void onData(SocketIOClient client, HashMap data,
//					AckRequest ackSender) throws Exception {
//				if(data==null || data.equals("")){
//					log.warn(String.format("user getcontracter error, because data is empty"));
//					return;
//				}
//				String gid = String.valueOf(data.get("gid"));
//				String userName = sessionClientToUserNameMap.get(client);
//				Jedis jedis = null;
//				String appKey = "";
//				String token = "";
//				long uid = 0L;
//				try{
//					jedis = redisClient.getJeids();
//					List<String> dataList = jedis.hmget(userName, "appKey", "token", "uid");
//					appKey = dataList.get(0);
//					token = dataList.get(1);
//					uid = Long.parseLong(dataList.get(2));
//				} catch (JedisConnectionException e) {
//					log.error(e.getMessage());
//					redisClient.returnBrokenResource(jedis);
//					throw new JedisConnectionException(e);
//				} finally {
//					redisClient.returnResource(jedis);
//				}
//				Channel channel = userNameToPushChannelMap.get(userName);
//				ArrayList<HashMap> resultList = new ArrayList<HashMap>();
//				log.info(String.format("user: %d begin get group: %s members", uid, gid));
//				HttpResponseWrapper resultWrapper = APIProxy.getGroupMemberList(gid, token);
//				if(resultWrapper.isOK()){
//					List<GroupMember> groupList = gson.fromJson(resultWrapper.content, new TypeToken<ArrayList<GroupMember>>(){}.getType());
//					for(GroupMember member:groupList){
//						HttpResponseWrapper wrapper = APIProxy.getUserInfoByUid(appKey, String.valueOf(member.getUid()), token);
//						if(wrapper.isOK()){
//							HashMap map = gson.fromJson(wrapper.content, HashMap.class);
//							resultList.add(map);
//						} 
//					}
//					client.sendEvent("getGroupMemberList", gson.toJson(resultList));
//					log.warn(String.format("user: %d get group: %s member success", uid, gid));
//				} else {
//					log.warn(String.format("user: %d get group: %s member exception because call sdk api", uid, gid));
//					client.sendEvent("getGroupMemberList", "false");
//				}
//			}	 
//		});
		
		 
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
		
		
		/* ------------------------------------------  to check -------------------------------------------------*/
		
		 server.start();
	}
	
	public static void main(String[] args) throws InterruptedException {
		WebImServer socketServer = new WebImServer();
		socketServer.init();
		socketServer.configMessageEventAndStart();
	}

}
