package com.jpush.webim.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import ch.qos.logback.classic.Logger;

import com.jpush.webim.common.RedisClient;


@Controller
@RequestMapping(value="/uc")
public class UserAction {
	
	@Autowired
	private RedisClient redisClient;
	private static Logger log = (Logger) LoggerFactory.getLogger(UserAction.class);
	
	@RequestMapping(value="login", method=RequestMethod.GET)
	public void login(HttpServletRequest req, HttpServletResponse res) throws UnsupportedEncodingException{
		String user_name = req.getParameter("user_name");
		req.setCharacterEncoding("utf-8");
		log.info(user_name+" is logining now.");
		Jedis jedis = null;
	   try {
	           jedis = redisClient.getJeids();
	           jedis.sadd("im_users", user_name);
	           //res.sendRedirect("../page/demo.jsp?user_name="+user_name); // sockjs+spring4的实现
	           res.sendRedirect("../page/socketio/index.html?user_name="+user_name);  // socket.io.js + netty的实现
	       } catch (JedisConnectionException e) {
	           redisClient.returnBrokenResource(jedis);
	           throw new JedisConnectionException(e);
	       } catch (IOException e) {
	    	   log.error(e.getMessage());
		} finally {
	           redisClient.returnResource(jedis);
	    }
	}
}
