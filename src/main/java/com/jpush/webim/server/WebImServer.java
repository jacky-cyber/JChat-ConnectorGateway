package com.jpush.webim.server;

import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import ch.qos.logback.classic.Logger;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.jpush.protocal.common.JPushTcpClient;
import com.jpush.protocal.im.bean.LoginRequestBean;
import com.jpush.protocal.im.bean.SendGroupMsgRequestBean;
import com.jpush.protocal.im.bean.SendSingleMsgRequestBean;
import com.jpush.protocal.im.requestproto.ImLoginRequestProto;
import com.jpush.protocal.im.requestproto.ImSendGroupMsgRequestProto;
import com.jpush.protocal.im.requestproto.ImSendSingleMsgRequestProto;
import com.jpush.protocal.utils.Command;
import com.jpush.protocal.utils.SerializeUtil;
import com.jpush.protocal.utils.SystemConfig;
import com.jpush.webim.common.RedisClient;
import com.jpush.webim.common.UidResourcesPool;
import com.jpush.webim.socketio.bean.ChatObject;
import com.jpush.webim.socketio.bean.ContracterObject;
import com.jpush.webim.socketio.bean.GroupObject;

/**
 * Web Im 业务 Server
 *
 */
public class WebImServer {
	private static Logger log = (Logger) LoggerFactory.getLogger(WebImServer.class);
	private RedisClient redisClient;
	
  	private static final String HOST_NAME = SystemConfig.getProperty("webim.server.host");  
	private static final int PORT = SystemConfig.getIntProperty("webim.server.port");
	public static HashMap<String, SocketIOClient> userNameToSessionCilentMap = new HashMap<String, SocketIOClient>();
	private static HashMap<SocketIOClient, String> sessionClientToUserNameMap = new HashMap<SocketIOClient, String>();
	private static HashMap<String, Channel> userNameToPushChannelMap = new HashMap<String, Channel>();
	public static HashMap<Channel, String> pushChannelToUsernameMap = new HashMap<Channel, String>();
	
	private Configuration config = null;
	private SocketIOServer server = null;
	
	public void init() {
		 redisClient = new RedisClient();
		 config = new Configuration();
		 config.setHostname(HOST_NAME);
		 config.setPort(PORT);
		 config.setTransports(Transport.WEBSOCKET);
		 server = new SocketIOServer(config);
	}
	
	public void configMessageEventAndStart() throws InterruptedException{
		if(config==null||server==null){
			log.error("you have not init the config and server. please do this first.");
			return;
		} 
		
		 //用户连接
		 server.addConnectListener(new ConnectListener() {
			@Override
			public void onConnect(SocketIOClient client) {
				log.info("connect from client, session id: "+ client.getSessionId()+", 接入方式: "+client.getTransport());
				//  创建 jpush tcp 连接
			}
		 });
		 
		 // 用户断开
		 server.addDisconnectListener(new DisconnectListener() {
			@Override
			public void onDisconnect(SocketIOClient client) {
				log.error("disconnect from client: "+client.getSessionId()+" disconnect.");
				// 处理缓存数据(管理在线用户列表)
				Jedis jedis = null;
				try{
					jedis = redisClient.getJeids();
					log.info("remote related data from im_online_users.");
					String user_name = sessionClientToUserNameMap.get(client);
					sessionClientToUserNameMap.remove(client);
					userNameToSessionCilentMap.remove(user_name);
					Channel channel = userNameToPushChannelMap.get(user_name);
					jedis.srem("im_online_users", user_name);  // 把该用户从在线用户列表中清除
				} catch (JedisConnectionException e) {
					log.error(e.getMessage());
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				// 向其他成员发送下线通知
				// ...
			}
		});
		 
		 // 用户登陆
		 server.addEventListener("loginevent", ChatObject.class, new DataListener<ChatObject>() {
			@Override
			public void onData(SocketIOClient client, ChatObject data,
					AckRequest ackSender) throws Exception {
				log.info("user: "+data.getUserName()+" login success");
				String user_name = data.getUserName();
				
				log.info("add current user to online users set.");
				Jedis jedis = redisClient.getJeids();
				jedis.sadd("im_online_users", user_name);
				
				log.info("add user and session client to map.");
				userNameToSessionCilentMap.put(user_name,	client);
				sessionClientToUserNameMap.put(client, user_name);
				//  测试 redis 缓存 sessionclient 对象测试
//				log.info("开始序列化session client...id："+client.getSessionId());
//				Map<byte[], byte[]> hash = new HashMap<byte[], byte[]>();
//				hash.put(SerializeUtil.serialize(user_name), SerializeUtil.serialize(client));
//				jedis.hmset(SerializeUtil.serialize("user_to_client"), hash);
//				log.info("序列化完成.");
//				log.info("开始反序列化测试....");
//				List<byte[]> list = jedis.hmget(SerializeUtil.serialize("user_to_client"), SerializeUtil.serialize(user_name));
//				log.info("list size: "+list.size());
//				SocketIOClient myclient = (SocketIOClient)SerializeUtil.unserialize(list.get(list.size()-1));
//				log.info("反序列化后的session client... id: "+myclient.getSessionId());
				/*------------------------------------*/
				redisClient.returnResource(jedis);
				
				// 获取uid
				long uid = UidResourcesPool.getUid();
				log.info("用户："+user_name+"接入，获取uid："+uid);
				// jpush 接入相关
				log.info("build user connection to jpush.");
				JPushTcpClient pushConnect = new JPushTcpClient();
				Channel pushChannel = pushConnect.getChannel();
				userNameToPushChannelMap.put(user_name, pushChannel);
				pushChannelToUsernameMap.put(pushChannel, user_name);
				LoginRequestBean bean = new LoginRequestBean(user_name,"password123","appkey783245jsdvgf");
				List<Integer> cookie = new ArrayList<Integer>();
				cookie.add(123);
				ImLoginRequestProto req = new ImLoginRequestProto(Command.JPUSH_IM.LOGIN, 1, 2324, cookie, bean);
				pushChannel.writeAndFlush(req);  //  发送 IM 登陆请求
			}
		 });
		 
		 // 获取联系人列表
		 server.addEventListener("getContracterList", ContracterObject.class, new DataListener<ContracterObject>() {
			@Override
			public void onData(SocketIOClient client, ContracterObject data,
					AckRequest ackSender) throws Exception {
				String curUserName = data.getUser_name();
				if(curUserName==null || data==null){
					log.error("client arguments error: no user name.");
					return;
				}
				List<ContracterObject> contractersList = new ArrayList<ContracterObject>();
				Jedis jedis = null;
				try {
				    jedis = redisClient.getJeids();
				    Set<String> onlineUserSet = jedis.smembers("im_online_users");
				    Set<String> userNameSet = jedis.smembers("im_users");
				    Set<String> offlineUserSet = jedis.sdiff("im_users", "im_online_users");
				    log.info("当前总用户数量："+userNameSet.size());
				    log.info("当前在线用户数量："+onlineUserSet.size());
				    log.info("当前不在线用户数量："+offlineUserSet.size());
				    for(String username: onlineUserSet){
				    	if(!curUserName.equals(username)){
				    		ContracterObject contracter = new ContracterObject();
					    	SocketIOClient userClient = userNameToSessionCilentMap.get(username); 
				    		if(userClient!=null){
				    			String sessionId = userClient.getSessionId().toString();
				    			contracter.setSession_id(sessionId);
				    		} else {
				    			contracter.setSession_id("");
				    		}
					    	contracter.setUser_name(username);
					    	contracter.setOnline(true);
					    	contractersList.add(contracter);
				    	}
				    }
				   for(String username: offlineUserSet){
				    	ContracterObject contracter = new ContracterObject();
				    	contracter.setUser_name(username);
				    	contracter.setOnline(false);
				    	contractersList.add(contracter);
				    }
				   client.sendEvent("getContracterList", contractersList);
				} catch (JedisConnectionException e) {
					 log.error(e.getMessage());
				    redisClient.returnBrokenResource(jedis);
				    throw new JedisConnectionException(e);
				} finally {
				    redisClient.returnResource(jedis);
				}
			}	 
		});
		 
		// 获取群组列表
		server.addEventListener("getGroupsList", ContracterObject.class, new DataListener<ContracterObject>() {
			@Override
			public void onData(SocketIOClient client, ContracterObject data,
					AckRequest ackSender) throws Exception {
				String curUserName = data.getUser_name();
				if(curUserName==null || data==null){
					log.error("client arguments error: no user name.");
					return;
				}
				List<GroupObject> groupsList = new ArrayList<GroupObject>();
				GroupObject group2 = new GroupObject();
				group2.setGroup_id("10011");
				group2.setGroup_name("JPush Company");
				GroupObject group3 = new GroupObject();
				group3.setGroup_id("10010");
				group3.setGroup_name("JPush dev");
				groupsList.add(group2);
				groupsList.add(group3);
				client.sendEvent("getGroupsList", groupsList);
			}	 
		});
		 
		 // 用户聊天
		 server.addEventListener("chatevent", ChatObject.class, new DataListener<ChatObject>() {
			 @Override
			 public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
				 log.info("message: "+ data.getMessage() +" from: "+data.getUserName()+" to: "+data.getToUserName());
				 //SocketIOClient toClient = userNameToSessionCilentMap.get(data.getToUserName());
				 //toClient.sendEvent("chatevent", data);
				 
				 Channel channel = userNameToPushChannelMap.get(data.getUserName());
				 if(channel==null)
					 log.info("当前用户与jpush的连接已断开.");
				 if("single".equals(data.getMsgType())){
					 // 模拟接入 Jpush 单发消息测试
					 SendSingleMsgRequestBean bean = new SendSingleMsgRequestBean(Long.parseLong(data.getToUserName()), data.getMessage());
					 List<Integer> cookie = new ArrayList<Integer>();
					 cookie.add(123);
					 ImSendSingleMsgRequestProto req = new ImSendSingleMsgRequestProto(Command.JPUSH_IM.SENDMSG_SINGAL, 1, Long.parseLong(data.getUserName()), cookie, bean);
					 channel.writeAndFlush(req);
				 } else if("group".equals(data.getMsgType())){
					// 模拟接入 JPush 群发消息测试
					 SendGroupMsgRequestBean bean = new SendGroupMsgRequestBean(Long.parseLong(data.getToUserName()), data.getMessage());
					 List<Integer> cookie = new ArrayList<Integer>();
					 cookie.add(123);
					 ImSendGroupMsgRequestProto req = new ImSendGroupMsgRequestProto(Command.JPUSH_IM.SENDMSG_GROUP, 1, Long.parseLong(data.getUserName()), cookie, bean);
					 channel.writeAndFlush(req);
				 }
			 }
		 });
		 
		 server.start();
		 Thread.sleep(Integer.MAX_VALUE);
		 server.stop();
	}
	
	/*public void run(){
		log.info("启动 im server......");
		WebImServer socketServer = new WebImServer();
		socketServer.init();
		try {
			socketServer.configMessageEventAndStart();
		} catch (InterruptedException e) {
			log.info("exception when start im server: "+e.getMessage());
		}
		log.info("启动 im server 成功.");
	}
	
	public void stop(){
		
	}*/
	
	
	public static void main(String[] args) throws InterruptedException {
		WebImServer socketServer = new WebImServer();
		socketServer.init();
		socketServer.configMessageEventAndStart();
	}

}
