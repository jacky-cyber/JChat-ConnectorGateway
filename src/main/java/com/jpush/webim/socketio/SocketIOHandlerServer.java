package com.jpush.webim.socketio;

import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.objenesis.instantiator.basic.NewInstanceInstantiator;

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
import com.jpush.protocal.common.Command;
import com.jpush.protocal.common.NettyTcpClient;
import com.jpush.protocal.im.bean.LogoutRequestBean;
import com.jpush.protocal.im.bean.SendSingleMsgRequestBean;
import com.jpush.protocal.im.requestproto.ImLogoutRequestProto;
import com.jpush.protocal.im.requestproto.ImSendSingleMsgRequestProto;
import com.jpush.webim.common.RedisClient;
import com.jpush.webim.socketio.bean.ChatObject;
import com.jpush.webim.socketio.bean.ContracterObject;

public class SocketIOHandlerServer {
	private static Logger log = (Logger) LoggerFactory.getLogger(SocketIOHandlerServer.class);
	private RedisClient redisClient;
	
	private static final String HOST_NAME = "127.0.0.1";
	private static final int PORT = 9092;
	public static HashMap<String, SocketIOClient> userNameToSessionCilentMap = new HashMap<String, SocketIOClient>();
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
				log.info("connect from client: "+ client.getSessionId()+", "+client.getTransport());
				//  创建 jpush tcp 连接
			}
		 });
		 
		 // 用户断开
		 server.addDisconnectListener(new DisconnectListener() {
			@Override
			public void onDisconnect(SocketIOClient client) {
				log.error("disconnect from client: "+client.getSessionId()+" disconnect.");
				
				// 向其他成员发送通知
			}
		});
		 
		 // 用户登陆
		 server.addEventListener("loginevent", ChatObject.class, new DataListener<ChatObject>() {
			@Override
			public void onData(SocketIOClient client, ChatObject data,
					AckRequest ackSender) throws Exception {
				log.info("user: "+data.getUserName()+" login success");
				String user_name = data.getUserName();
				log.info("add user and session client to map.");
				userNameToSessionCilentMap.put(user_name,	client);
				//  jpush 接入相关
				log.info("build user connection to jpush.");
				NettyTcpClient pushConnect = new NettyTcpClient();
				Channel pushChannel = pushConnect.getChannel();
				userNameToPushChannelMap.put(user_name, pushChannel);
				pushChannelToUsernameMap.put(pushChannel, user_name);
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
				    Set<String> userNameSet = jedis.smembers("im_users");
				    log.info("当前用户数量："+userNameSet.size());
				    for(String username: userNameSet){
				    	if(!curUserName.equals(username)){
				    		ContracterObject contracter = new ContracterObject();
					    	SocketIOClient userClient = userNameToSessionCilentMap.get(username);
					    	String sessionId = userClient.getSessionId().toString();
					    	contracter.setUser_name(username);
					    	contracter.setSession_id(sessionId);
					    	contractersList.add(contracter);
				    	}
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
		 
		 // 用户聊天
		 server.addEventListener("chatevent", ChatObject.class, new DataListener<ChatObject>() {
			 @Override
			 public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
				 log.info("message: "+ data.getMessage() +" from: "+data.getUserName()+" to: "+data.getToUserName());
				 /*Iterator<SocketIOClient> iterator = server.getAllClients().iterator();
				 UUID uuid = client.getSessionId();
				 while(iterator.hasNext()){
					 SocketIOClient cli = iterator.next();
					 if(!uuid.equals(cli.getSessionId())){
						 cli.sendEvent("chatevent", data);
					 }
				 }*/
				 //SocketIOClient toClient = userNameToSessionCilentMap.get(data.getToUserName());
				 //toClient.sendEvent("chatevent", data);
				 // 模拟接入 Jpush 测试
				 Channel channel = userNameToPushChannelMap.get(data.getUserName());
				 SendSingleMsgRequestBean bean = new SendSingleMsgRequestBean(Long.parseLong(data.getToUserName()), data.getMessage());
				 List<Integer> cookie = new ArrayList<Integer>();
				 cookie.add(123);
				 ImSendSingleMsgRequestProto req = new ImSendSingleMsgRequestProto(Command.JPUSH_IM.SENDMSG_SINGAL, 1, Long.parseLong(data.getUserName()), cookie, bean);
				 channel.writeAndFlush(req);
			 }
		 });
		 
		 server.start();
		 Thread.sleep(Integer.MAX_VALUE);
		 server.stop();
	}
	
	public void run(){
		log.info("启动 im server......");
		SocketIOHandlerServer socketServer = new SocketIOHandlerServer();
		socketServer.init();
		try {
			socketServer.configMessageEventAndStart();
		} catch (InterruptedException e) {
			log.info("exception when start im server: "+e.getMessage());
		}
		log.info("启动 im server 成功.");
	}
	
	public void stop(){
		
	}
	
	
	public static void main(String[] args) throws InterruptedException {
		SocketIOHandlerServer socketServer = new SocketIOHandlerServer();
		socketServer.init();
		socketServer.configMessageEventAndStart();
	}

}
