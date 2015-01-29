package cn.jpush.webim.server;

import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.common.JPushTcpClient;
import cn.jpush.protocal.im.bean.LoginRequestBean;
import cn.jpush.protocal.im.bean.SendGroupMsgRequestBean;
import cn.jpush.protocal.im.bean.SendSingleMsgRequestBean;
import cn.jpush.protocal.im.requestproto.ImLoginRequestProto;
import cn.jpush.protocal.im.requestproto.ImSendGroupMsgRequestProto;
import cn.jpush.protocal.im.requestproto.ImSendSingleMsgRequestProto;
import cn.jpush.protocal.push.PushLoginRequest;
import cn.jpush.protocal.push.PushLoginRequestBean;
import cn.jpush.protocal.utils.APIProxy;
import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.Configure;
import cn.jpush.protocal.utils.HttpResponseWrapper;
import cn.jpush.protocal.utils.SystemConfig;
import cn.jpush.webim.common.UidResourcesPool;
import cn.jpush.webim.socketio.bean.ChatObject;
import cn.jpush.webim.socketio.bean.ContracterObject;
import cn.jpush.webim.socketio.bean.Group;
import cn.jpush.webim.socketio.bean.GroupList;
import cn.jpush.webim.socketio.bean.User;
import cn.jpush.webim.socketio.bean.UserList;

import com.google.gson.Gson;
import com.qiniu.api.auth.digest.Mac;
import com.qiniu.api.rs.PutPolicy;
import com.socketio.AckRequest;
import com.socketio.Configuration;
import com.socketio.SocketIOClient;
import com.socketio.SocketIOServer;
import com.socketio.Transport;
import com.socketio.listener.ConnectListener;
import com.socketio.listener.DataListener;
import com.socketio.listener.DisconnectListener;

/**
 * Web Im 业务 Server
 *
 */
public class WebImServer {
	private static Logger log = (Logger) LoggerFactory.getLogger(WebImServer.class);
	
  	private static final String HOST_NAME = SystemConfig.getProperty("webim.server.host");  
	private static final int PORT = SystemConfig.getIntProperty("webim.server.port");
	public static HashMap<String, SocketIOClient> userNameToSessionCilentMap = new HashMap<String, SocketIOClient>();
	private static HashMap<SocketIOClient, String> sessionClientToUserNameMap = new HashMap<SocketIOClient, String>();
	private static HashMap<String, Channel> userNameToPushChannelMap = new HashMap<String, Channel>();
	public static HashMap<Channel, String> pushChannelToUsernameMap = new HashMap<Channel, String>();
	private Gson gson = new Gson();
	private Configuration config = null;
	private SocketIOServer server = null;
	
	public void init() {
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
			}
		 });
		 
		 // 用户断开
		 server.addDisconnectListener(new DisconnectListener() {
			@Override
			public void onDisconnect(SocketIOClient client) {
				log.error("disconnect from client: "+client.getSessionId()+" disconnect.");
				// 处理缓存数据(管理在线用户列表)
			
				String user_name = sessionClientToUserNameMap.get(client);
				sessionClientToUserNameMap.remove(client);
				userNameToSessionCilentMap.remove(user_name);
				Channel channel = userNameToPushChannelMap.get(user_name);
				userNameToPushChannelMap.remove(user_name);
				pushChannelToUsernameMap.remove(channel);
				channel.close();
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
				String appkey = data.getAppKey();
				String user_name = data.getUserName();
				String password = data.getPassword();
				
				log.info("add user and session client to map.");
				userNameToSessionCilentMap.put(user_name,	client);
				sessionClientToUserNameMap.put(client, user_name);
				
				// 获取uid
				long juid = UidResourcesPool.getUid();
				log.info("用户："+user_name+"接入，获取juid："+juid);
				// jpush 接入相关
				log.info("build user connection to jpush.");
				JPushTcpClient pushConnect = new JPushTcpClient();
				Channel pushChannel = pushConnect.getChannel();
				userNameToPushChannelMap.put(user_name, pushChannel);
				pushChannelToUsernameMap.put(pushChannel, user_name);
				//  JPush login
				PushLoginRequestBean pushLoginBean = new PushLoginRequestBean("web", password, 306010, appkey, 1);
				PushLoginRequest pushLoginRequest = new PushLoginRequest(1, 1, 2, juid, pushLoginBean);
				pushChannel.writeAndFlush(pushLoginRequest);
				//   IM login
				LoginRequestBean bean = new LoginRequestBean(user_name,"password123");
				List<Integer> cookie = new ArrayList<Integer>();
				cookie.add(123);
				ImLoginRequestProto req = null;
				if("xMnTCP".equals(user_name))
					req = new ImLoginRequestProto(Command.JPUSH_IM.LOGIN, 1, 87386, SystemConfig.getProperty("jpush.appkey"), cookie, bean);
				if("2eav27".equals(user_name))
					req = new ImLoginRequestProto(Command.JPUSH_IM.LOGIN, 1, 85841, SystemConfig.getProperty("jpush.appkey"), cookie, bean);
				pushChannel.writeAndFlush(req);  //  发送 IM 登陆请求
			}
		 });
		 
		 //   改变map映射关系
		 server.addEventListener("updateMap", HashMap.class, new DataListener<HashMap>() {
			@Override
			public void onData(SocketIOClient client, HashMap data,
					AckRequest ackSender) throws Exception {
				String uid = data.get("uid")+"";
				String user_name = (String) data.get("user_name");
				userNameToSessionCilentMap.put(uid, userNameToSessionCilentMap.get(user_name));
				userNameToSessionCilentMap.remove(user_name);
				userNameToPushChannelMap.put(uid, userNameToPushChannelMap.get(user_name));
				userNameToPushChannelMap.remove(user_name);
			}
		});
		 
		 // 获取联系人列表
		 server.addEventListener("getContracterList", ContracterObject.class, new DataListener<ContracterObject>() {
			@Override
			public void onData(SocketIOClient client, ContracterObject data,
					AckRequest ackSender) throws Exception {
				long uid = data.getUid();
				if(uid==0 || data==null){
					log.error("client arguments error: no user name.");
					return;
				}
				
				List<User> contractersList = new ArrayList<User>();
				
				HttpResponseWrapper result = APIProxy.getGroupMemberList("123456");
				if(result.isOK()){
					String userListJson = result.content;
					UserList userList = gson.fromJson(userListJson, UserList.class);
					ArrayList<User> list = userList.getUids();
					for(int i=0; i<list.size(); i++){
						User user = (User) list.get(i);
						long id = user.getUid();
						HttpResponseWrapper userResult = APIProxy.getUserInfo(id+"");
						if(userResult.isOK()){
							User userInfo = gson.fromJson(userResult.content, User.class);
							contractersList.add(userInfo);
							log.info("userid: "+userInfo.getUid());
						}
					}   
				}
				// 2eav27 //xMnTCP
				client.sendEvent("getContracterList", contractersList);
				
			}	 
		});
		 
		// 获取群组列表
		server.addEventListener("getGroupsList", ContracterObject.class, new DataListener<ContracterObject>() {
			@Override
			public void onData(SocketIOClient client, ContracterObject data,
					AckRequest ackSender) throws Exception {
				//String curUserName = data.getUser_name();
				long uid = data.getUid();
				if(uid==0 || data==null){
					log.error("client arguments error: no user name.");
					return;
				}
				
				List<Group> groupsList = new ArrayList<Group>();
				
				//ResponseWrapper result = APIProxy.getGroupList(String.valueOf(uid));
				HttpResponseWrapper result = APIProxy.getGroupList("85841");
				if(result.isOK()){
					String groupListJson = result.content;
					log.info("group list: "+groupListJson);
					GroupList groupList = gson.fromJson(groupListJson, GroupList.class);
					ArrayList<Long> list = groupList.getGroups();
					for(Long gid:list){
						log.info("gid: "+gid);
						HttpResponseWrapper groupResult = APIProxy.getGroupInfo(gid+"");
						if(groupResult.isOK()){
							String groupInfoJson = groupResult.content;
							Group group = gson.fromJson(groupInfoJson, Group.class);
							groupsList.add(group);
						}
					}
				}
				
				/*ResponseWrapper result1 = APIProxy.getGroupMemberList("123456");
				if(result1.isOK()){
					String json = result1.content;
					log.info("group member list: "+json);
					//gson.fromJson(json, ArrayList.class);
				}*/
				
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
				 
				 Channel channel = userNameToPushChannelMap.get(data.getUid()+"");
				 if(channel==null)
					 log.info("当前用户与jpush的连接已断开.");
				 if("single".equals(data.getMsgType())){
					 // 模拟接入 Jpush 单发消息测试
					 SendSingleMsgRequestBean bean = new SendSingleMsgRequestBean(data.getToUid(), data.getMessage());
					 List<Integer> cookie = new ArrayList<Integer>();
					 cookie.add(123);
					 ImSendSingleMsgRequestProto req = new ImSendSingleMsgRequestProto(Command.JPUSH_IM.SENDMSG_SINGAL, 1, data.getUid(), SystemConfig.getProperty("jpush.appkey"), cookie, bean);
					 channel.writeAndFlush(req);
				 } else if("group".equals(data.getMsgType())){
					// 模拟接入 JPush 群发消息测试
					 SendGroupMsgRequestBean bean = new SendGroupMsgRequestBean(data.getToUid(), data.getMessage());
					 List<Integer> cookie = new ArrayList<Integer>();
					 cookie.add(123);
					 ImSendGroupMsgRequestProto req = new ImSendGroupMsgRequestProto(Command.JPUSH_IM.SENDMSG_GROUP, 1, data.getUid(), SystemConfig.getProperty("jpush.appkey"), cookie, bean);
					 channel.writeAndFlush(req);
				 }
			 }
		 });
		 
		 //  用户获取上传token
		 server.addEventListener("getUploadToken", String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data,
					AckRequest ackSender) throws Exception {
				/*HttpResponseWrapper result = APIProxy.getQiUploadToken();
				Map<String, String> resultMap = null;
				if(result.isOK()){
					resultMap = gson.fromJson(result.content, HashMap.class);
					String token = resultMap.get("token");
					String provider = resultMap.get("provider");
				}*/
				Mac mac = new Mac(Configure.QNCloudInterface.QN_ACCESS_KEY, Configure.QNCloudInterface.QN_SECRET_KEY);
				PutPolicy putPolicy = new PutPolicy(Configure.QNCloudInterface.QN_BUCKETNAME);
				putPolicy.expires = 14400;
				String token = putPolicy.token(mac);
				log.info("token: "+token);
				client.sendEvent("getUploadToken", token);
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
