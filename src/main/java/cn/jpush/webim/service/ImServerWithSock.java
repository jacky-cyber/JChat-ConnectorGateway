package cn.jpush.webim.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import ch.qos.logback.classic.Logger;
import cn.jpush.webim.common.RedisClient;

@Controller
public class ImServerWithSock extends TextWebSocketHandler {
	@Autowired
	private RedisClient redisClient;
	private List<WebSocketSession> sessionList = new ArrayList<WebSocketSession>();
	private static Logger log = (Logger) LoggerFactory.getLogger(UserAction.class);
	@Override
	public void afterConnectionEstablished(WebSocketSession session)
			throws Exception {
		sessionList.add(session);
		log.info("a new user id connected success.");
	}
	
	public void storeUserNameAndSessionId(String user_name, String session_id){
		Jedis jedis = null;
		try {
		     jedis = redisClient.getJeids();
		     jedis.set("user:"+user_name, session_id);
		     jedis.set("session:"+session_id, user_name);
		     log.info("redis store session about "+user_name);
		} catch (JedisConnectionException e) {
		     redisClient.returnBrokenResource(jedis);
		     throw new JedisConnectionException(e);
		} finally {
		     redisClient.returnResource(jedis);
		}
	}
	
	// session_id在redis中是否与一个用户绑定
	public boolean isExistUser(String session_id){
		Jedis jedis = null;
		boolean exist = true;
		try {
		     jedis = redisClient.getJeids();
		     String user_name = jedis.get("session:"+session_id);
		     if(user_name==null||user_name.equals("")){
		    	 exist = false;
		     }
		} catch (JedisConnectionException e) {
		     redisClient.returnBrokenResource(jedis);
		     throw new JedisConnectionException(e);
		} finally {
		     redisClient.returnResource(jedis);
		}
		return exist;
	}
	
	@Override
	public void handleMessage(WebSocketSession session,
			WebSocketMessage<?> message) throws Exception {
		if(!isExistUser(session.getId())){
			String user_name = "";
			String msgHead[] = message.getPayload().toString().split(":"); 
			if(msgHead.length>0){
				user_name = msgHead[1];
			}
			this.storeUserNameAndSessionId(user_name, session.getId());
			log.info("session "+session.getId()+"do not have a user. now the relationship has been build for you.");
		} else {
			log.info("the user has been logined. can send message directly.");
			try{
				for(WebSocketSession s: sessionList){
					if(s.getId() != session.getId()){
						s.sendMessage(message);
					}
				}
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session,
			TextMessage message) throws Exception {
			this.handleMessage(session, message);
	}

	@Override
	protected void handlePongMessage(WebSocketSession session,
			PongMessage message) throws Exception {
		log.info("handlePongMessage");
	}

	@Override
	public void handleTransportError(WebSocketSession session,
			Throwable exception) throws Exception {
		this.deleteUserAndSession(session.getId());
		log.info("TransportError:"+"has delete data in redis about user and session");
		log.info(exception.getMessage());
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session,
			CloseStatus status) throws Exception {
		log.info("afterConnectionClosed:"+"has delete data in redis about user and session");
		this.deleteUserAndSession(session.getId());
	}

	@Override
	public boolean supportsPartialMessages() {
		return super.supportsPartialMessages();
	}
	
	// 清除redis里面暂存的user_name和session_id的数据
	public void deleteUserAndSession(String session_id){
		Jedis jedis = null;
		try {
		     jedis = redisClient.getJeids();
		     String user_name = jedis.get("session:"+session_id);
		     if(user_name==null||user_name.equals("")){
		    	 	jedis.del("user:"+user_name);
		       }
		     jedis.del("session:"+session_id);
		} catch (JedisConnectionException e) {
		     redisClient.returnBrokenResource(jedis);
		     throw new JedisConnectionException(e);
		} finally {
		     redisClient.returnResource(jedis);
		}
	}
	
}
