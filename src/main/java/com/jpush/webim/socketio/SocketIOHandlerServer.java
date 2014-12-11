package com.jpush.webim.socketio;

import java.util.Iterator;
import java.util.UUID;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;

public class SocketIOHandlerServer {
	private static Logger log = (Logger) LoggerFactory.getLogger(SocketIOHandlerServer.class);
	
	public static void main(String[] args) throws InterruptedException {
		 Configuration config = new Configuration();
		 config.setHostname("127.0.0.1");
		 config.setPort(9092);
		 final SocketIOServer server = new SocketIOServer(config);
		 
		 //用户连接
		 server.addConnectListener(new ConnectListener() {
			@Override
			public void onConnect(SocketIOClient client) {
				log.info("connect from client:"+client.getSessionId()+","+client.getTransport());
			}
		 });
		 // 用户登陆
		 server.addEventListener("loginevent", ChatObject.class, new DataListener<ChatObject>() {
			@Override
			public void onData(SocketIOClient client, ChatObject data,
					AckRequest ackSender) throws Exception {
				log.info("user:"+data.getUserName()+"login success");
			}
		 });
		 // 用户聊天
		 server.addEventListener("chatevent", ChatObject.class, new DataListener<ChatObject>() {
			 @Override
			 public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
				 // broadcast messages to all clients
				 log.info("receive chatevent: "+data.getUserName()+","+data.getMessage());
				 //server.getBroadcastOperations().sendEvent("chatevent", data);
				 Iterator<SocketIOClient> iterator = server.getAllClients().iterator();
				 UUID uuid = client.getSessionId();
				 while(iterator.hasNext()){
					 SocketIOClient cli = iterator.next();
					 if(!uuid.equals(cli.getSessionId())){
						 cli.sendEvent("chatevent", data);
					 }
				 }
			 }
		 });
		 server.start();
		 Thread.sleep(Integer.MAX_VALUE);
		 server.stop();
	}

}
