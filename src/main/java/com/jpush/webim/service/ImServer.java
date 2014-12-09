package com.jpush.webim.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import com.jpush.webim.common.RedisClient;

@Controller
public class ImServer extends TextWebSocketHandler{
	
	@Autowired
	private RedisClient redisClient;
	private List<WebSocketSession> sessionList = new ArrayList<WebSocketSession>();
	@Override
	public void afterConnectionClosed(WebSocketSession session,
			CloseStatus status) throws Exception {
		// TODO Auto-generated method stub
		super.afterConnectionClosed(session, status);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session)
			throws Exception {
		String uri = session.getUri().toString();
		String str[] = uri.split("[?]");
		String res[] = str[1].split("[=]");
		String user_name = res[1];
		Jedis jedis = null;
		try {
		     jedis = redisClient.getJeids();
		     jedis.set("user:"+user_name, session.getId());
		     jedis.set("session:"+session.getId(), user_name);
		     sessionList.add(session);
		} catch (JedisConnectionException e) {
		     redisClient.returnBrokenResource(jedis);
		     throw new JedisConnectionException(e);
		} finally {
		     redisClient.returnResource(jedis);
		}
	}

	@Override
	public void handleMessage(WebSocketSession session,
			WebSocketMessage<?> message) throws Exception {
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

	@Override
	protected void handlePongMessage(WebSocketSession session,
			PongMessage message) throws Exception {
		// TODO Auto-generated method stub
		super.handlePongMessage(session, message);
	}

	@Override
	protected void handleTextMessage(WebSocketSession session,
			TextMessage message) throws Exception {
		this.handleMessage(session, message);
	}

	@Override
	public void handleTransportError(WebSocketSession session,
			Throwable exception) throws Exception {
		super.handleTransportError(session, exception);
	}

	@Override
	public boolean supportsPartialMessages() {
		return super.supportsPartialMessages();
	}

}
