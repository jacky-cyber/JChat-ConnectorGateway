package cn.jpush.webim.server;

import io.netty.channel.Channel;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

import jpushim.s2b.JpushimSdk2B.UpdateGroupInfo;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.common.JPushTcpClient;
import cn.jpush.protocal.im.bean.AddGroupMemberRequestBean;
import cn.jpush.protocal.im.bean.DeleteGroupMemberRequestBean;
import cn.jpush.protocal.im.bean.LoginRequestBean;
import cn.jpush.protocal.im.bean.LogoutRequestBean;
import cn.jpush.protocal.im.bean.SendGroupMsgRequestBean;
import cn.jpush.protocal.im.bean.SendSingleMsgRequestBean;
import cn.jpush.protocal.im.bean.UpdateGroupInfoRequestBean;
import cn.jpush.protocal.im.req.proto.ImAddGroupMemberRequestProto;
import cn.jpush.protocal.im.req.proto.ImDeleteGroupMemberRequestProto;
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
import cn.jpush.webim.common.UidResourcesPool;
import cn.jpush.webim.socketio.bean.AddFriendCmd;
import cn.jpush.webim.socketio.bean.AddOrDelGroupMember;
import cn.jpush.webim.socketio.bean.ChatMessage;
import cn.jpush.webim.socketio.bean.ChatObject;
import cn.jpush.webim.socketio.bean.ContracterObject;
import cn.jpush.webim.socketio.bean.Group;
import cn.jpush.webim.socketio.bean.GroupList;
import cn.jpush.webim.socketio.bean.GroupMember;
import cn.jpush.webim.socketio.bean.LogoutBean;
import cn.jpush.webim.socketio.bean.UpdateGroupInfoBean;
import cn.jpush.webim.socketio.bean.User;
import cn.jpush.webim.socketio.bean.UserList;

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
	public static HashMap<Long, SocketIOClient> userNameToSessionCilentMap = new HashMap<Long, SocketIOClient>();
	private static HashMap<SocketIOClient, Long> sessionClientToUserNameMap = new HashMap<SocketIOClient, Long>();
	private static HashMap<Long, Channel> userNameToPushChannelMap = new HashMap<Long, Channel>();
	public static HashMap<Channel, Long> pushChannelToUsernameMap = new HashMap<Channel, Long>();
	public static CountDownLatch pushLoginInCountDown;
	private static String APPKEY;
  	private static final String HOST_NAME = SystemConfig.getProperty("webim.server.host");  
	private static final int PORT = SystemConfig.getIntProperty("webim.server.port");
	private Gson gson = new Gson();
	private Configuration config;
	private SocketIOServer server;
	private JPushTcpClient jpushIMTcpClient;
	private int onlineCount;
	
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
				client.sendEvent("connectEvent", "");
			}
		 });
		 
		 // 用户断开
		 server.addDisconnectListener(new DisconnectListener() {
			@Override
			public void onDisconnect(SocketIOClient client) {
				log.error("disconnect from client: "+client.getSessionId()+" disconnect.");
				// 处理缓存数据(管理在线用户列表)
			
				long uid = sessionClientToUserNameMap.get(client);
				sessionClientToUserNameMap.remove(client);
				userNameToSessionCilentMap.remove(uid);
				Channel channel = userNameToPushChannelMap.get(uid);
				userNameToPushChannelMap.remove(uid);
				pushChannelToUsernameMap.remove(channel);
				if(channel!=null)
					channel.close();   //  断开与push server的长连接
				// 向其他成员发送下线通知
				// ...
			}
		});
		 
		 // 用户登陆
		 server.addEventListener("loginEvent", ChatObject.class, new DataListener<ChatObject>() {
			@Override
			public void onData(SocketIOClient client, ChatObject data,
					AckRequest ackSender) throws Exception {
				log.info("user: "+data.getUserName()+" begin login");
				onlineCount++;
				log.info("当前在线人数： "+onlineCount);
				String appkey = data.getAppKey();
				String user_name = data.getUserName();
				String password = data.getPassword();
				log.info("login user info, appkey: "+appkey+", name: "+user_name+", pwd: "+password);
				long uid = 0L;
				 
				//  获取用户ID
				HttpResponseWrapper resultWrapper = APIProxy.getUserInfo(appkey, user_name);
				if(resultWrapper.isOK()){
					User userInfo = gson.fromJson(resultWrapper.content, User.class);
					uid = userInfo.getUid();
					data.setUid(uid);
					//client.sendEvent("loginEvent", data);  //  向客户端返回uid
					log.info("user login success.");
				} else {
					data.setUid(0);
					//client.sendEvent("loginEvent", data);
					log.info("user login failed.");
//					return;
				}
				
				log.info("add user and session client to map.");
				userNameToSessionCilentMap.put(uid,	client);
				sessionClientToUserNameMap.put(client, uid);
				
				// 获取uid
				long juid = UidResourcesPool.getUid();
				pushLoginInCountDown = new CountDownLatch(1);
				log.info("用户："+user_name+"接入，获取juid："+juid);
				// jpush 接入相关
				log.info("build user connection to jpush.");
				jpushIMTcpClient = new JPushTcpClient();
				Channel pushChannel = jpushIMTcpClient.getChannel();
				userNameToPushChannelMap.put(uid, pushChannel);
				pushChannelToUsernameMap.put(pushChannel, uid);
				//  JPush login
				PushLoginRequestBean pushLoginBean = new PushLoginRequestBean(juid, "a", ProtocolUtil.md5Encrypt("756371956"), 10800, appkey, 0);
				pushChannel.writeAndFlush(pushLoginBean);
				pushLoginInCountDown.await();  //  等待push login返回数据
				PushLoginResponseBean pushLoginResponseBean = jpushIMTcpClient.getjPushClientHandler().getPushLoginResponseBean();
				log.info("get response data: sid: "+pushLoginResponseBean.getSid());
				
				//   设置数据 用于心跳
				jpushIMTcpClient.getjPushClientHandler().setSid(pushLoginResponseBean.getSid());
				jpushIMTcpClient.getjPushClientHandler().setJuid(juid);
				//    向客户端发 SID 和 JUId
				data.setJuid(juid);
				data.setSid(pushLoginResponseBean.getSid());
				client.sendEvent("loginEvent", data);
				
				//   IM Login
				LoginRequestBean bean = new LoginRequestBean(user_name, StringUtils.toMD5(password));
				List<Integer> cookie = new ArrayList<Integer>();
				ImLoginRequestProto req = new ImLoginRequestProto(Command.JPUSH_IM.LOGIN, 1, 0, pushLoginResponseBean.getSid(), juid, appkey, cookie, bean);
				log.info("开始发送 IM Login 请求.......");
				pushChannel.writeAndFlush(req);
			}
		 });
		 
		 // 获取联系人列表
		 server.addEventListener("getContracterList", ContracterObject.class, new DataListener<ContracterObject>() {
			@Override
			public void onData(SocketIOClient client, ContracterObject data,
					AckRequest ackSender) throws Exception {
				String appkey = data.getAppkey();
				String user_name = data.getUser_name();
				if(user_name==null || "".equals(user_name) || data==null){
					log.error("client arguments error: no user name.");
					return;
				}
				
				List<User> contractersList = new ArrayList<User>();
				
				//  模拟用户列表
				for(int i=1; i<4; i++){
					String username = "p00"+i;
					HttpResponseWrapper userResult = APIProxy.getUserInfo(appkey, username);
					if(userResult.isOK()){
						User userInfo = gson.fromJson(userResult.content, User.class);
						contractersList.add(userInfo);
						log.info("userid: "+userInfo.getUid());
					}
				}
	  
				client.sendEvent("getContracterList", contractersList);
				
			}	 
		});
		 
		// 获取群组列表
		server.addEventListener("getGroupsList", ContracterObject.class, new DataListener<ContracterObject>() {
			@Override
			public void onData(SocketIOClient client, ContracterObject data,
					AckRequest ackSender) throws Exception {
				//String curUserName = data.getUser_name();
				String appkey = data.getAppkey();
				long uid = data.getUid();
				if(uid==0 || data==null){
					log.error("client arguments error: no user name.");
					return;
				}
				
				List<Group> groupsList = new ArrayList<Group>();
				Group group = new Group();
				group.setAppkey(appkey);
				group.setGid(72);
				group.setGroup_name("group01");
				groupsList.add(group);
				/*HttpResponseWrapper result = APIProxy.getGroupList(String.valueOf(uid));
				if(result.isOK()){
					String groupListJson = result.content;
					log.info("group list: "+groupListJson);
					ArrayList<Long> list = gson.fromJson(groupListJson, new TypeToken<ArrayList<Long>>(){}.getType());
					for(Long gid:list){
						log.info("gid: "+gid);
						HttpResponseWrapper groupResult = APIProxy.getGroupInfo(String.valueOf(gid));
						if(groupResult.isOK()){
							String groupInfoJson = groupResult.content;
							Group group = gson.fromJson(groupInfoJson, Group.class);
							groupsList.add(group);
						}
					}
				}*/
				client.sendEvent("getGroupsList", groupsList);
			}	 
		});
		
		// 获取群组成员列表
		server.addEventListener("getGroupMemberList", String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data,
					AckRequest ackSender) throws Exception {
				if(data==null || data.equals("")){
					log.error("client arguments error: no gid.");
					return;
				}
				log.info("get group member list...");
				String gid = data;
				HttpResponseWrapper resultWrapper = APIProxy.getGroupMemberList(gid);
				if(resultWrapper.isOK()){
					client.sendEvent("getGroupMemberList", resultWrapper.content);
				} else {
					client.sendEvent("getGroupMemberList", "false");
				}
			}	 
		});
		 
		 // 用户聊天
		server.addEventListener("chatEvent", ChatMessage.class, new DataListener<ChatMessage>() {
			 @Override
			 public void onData(SocketIOClient client, ChatMessage data, AckRequest ackRequest) {
				 log.info("message -- juid: "+data.getJuid()+", sid: "+data.getSid());
				 log.info("message: "+ data.getMsg_body() +" from: "+data.getFrom_name()+" to: "+data.getTarget_name());
				 String appKey = data.getAppKey();
				 int sid = data.getSid(); 
				 Channel channel = userNameToPushChannelMap.get(data.getFrom_id());
				 if(channel==null)
					 log.info("当前用户与jpush的连接已断开.");
				 if("single".equals(data.getTarget_type())){
					 SendSingleMsgRequestBean bean = new SendSingleMsgRequestBean(Long.parseLong(data.getTarget_name()), gson.toJson(data));  //  为了和移动端保持一致，注意这里用target_name来存储id，避免再查一次
					 List<Integer> cookie = new ArrayList<Integer>();
					 ImSendSingleMsgRequestProto req = new ImSendSingleMsgRequestProto(Command.JPUSH_IM.SENDMSG_SINGAL, 1, data.getFrom_id(), appKey, sid, data.getJuid(), cookie, bean);
					 channel.writeAndFlush(req);
				 } else if("group".equals(data.getTarget_type())){
					 SendGroupMsgRequestBean bean = new SendGroupMsgRequestBean(Long.parseLong(data.getTarget_id()), gson.toJson(data));
					 List<Integer> cookie = new ArrayList<Integer>();
					 ImSendGroupMsgRequestProto req = new ImSendGroupMsgRequestProto(Command.JPUSH_IM.SENDMSG_GROUP, 1, Long.parseLong(data.getTarget_id()), appKey, sid, data.getJuid(), cookie, bean);
					 channel.writeAndFlush(req);
				 }
			 }
		 });
		 
		 //  用户获取上传token
		 server.addEventListener("getUploadToken", String.class, new DataListener<String>() {
			@Override
			public void onData(SocketIOClient client, String data,
					AckRequest ackSender) throws Exception {
				Mac mac = new Mac(Configure.QNCloudInterface.QN_ACCESS_KEY, Configure.QNCloudInterface.QN_SECRET_KEY);
				PutPolicy putPolicy = new PutPolicy(Configure.QNCloudInterface.QN_BUCKETNAME);
				putPolicy.expires = 14400;
				String token = putPolicy.token(mac);
				client.sendEvent("getUploadToken", token);
			}
		 });
		 
		 //  向客户端返回上传的文件的属性信息
		 server.addEventListener("getUploadPicMetaInfo", String.class, new DataListener<String>(){
			@Override
			public void onData(SocketIOClient client, String data,
					AckRequest ackSender) throws Exception {
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
			}
		 });
		 
		 //  添加好友事件
		 server.addEventListener("addFriendCmd", AddFriendCmd.class, new DataListener<AddFriendCmd>(){
			@Override
			public void onData(SocketIOClient client, AddFriendCmd data,
					AckRequest ackSender) throws Exception {
				log.info("add new friend cmd, from: "+data.getFrom()+", to: "+data.getTo());
			}
		 });
		
		//  更新群组名称
		server.addEventListener("updateGroupName", UpdateGroupInfoBean.class, new DataListener<UpdateGroupInfoBean>(){
			@Override
			public void onData(SocketIOClient client, UpdateGroupInfoBean data,
					AckRequest ackSender) throws Exception {
				log.info("update group member info, group id: "+data.getGid()+", name: "+data.getGroup_name());
				String appKey = data.getAppKey();
				long gid = data.getGid();
				long uid = data.getUid();
				int sid = data.getSid();
				long juid = data.getJuid();
				String group_name = data.getGroup_name();
				UpdateGroupInfoRequestBean bean = new UpdateGroupInfoRequestBean(gid, group_name, "");
				List<Integer> cookie = new ArrayList<Integer>();
				ImUpdateGroupInfoRequestProto req = new ImUpdateGroupInfoRequestProto(Command.JPUSH_IM.UPDATE_GROUP_INFO, 1, uid, appKey, sid, juid, cookie, bean);
				Channel channel = userNameToPushChannelMap.get(data.getUid());
				channel.writeAndFlush(req);
			}
		});
		
		//  添加群组成员
		server.addEventListener("addGroupMember", AddOrDelGroupMember.class, new DataListener<AddOrDelGroupMember>(){
			@Override
				public void onData(SocketIOClient client, AddOrDelGroupMember data,
						AckRequest ackSender) throws Exception {
					log.info("add group member info, group id: "+data);
					String user_name = data.getUsername();
					String appKey = data.getAppKey();
					int sid = data.getSid();
					long uid = data.getUid();
					long addUid = 0L;
					long juid = data.getJuid();
					long gid = data.getGid();
					HttpResponseWrapper resultWrapper = APIProxy.getUserInfo(APPKEY, user_name);
					if(resultWrapper.isOK()){
						User userInfo = gson.fromJson(resultWrapper.content, User.class);
						addUid = userInfo.getUid();
					}
					List<Long> list = new ArrayList<Long>();
					list.add(addUid);
					AddGroupMemberRequestBean bean = new AddGroupMemberRequestBean(gid, 1, list);
					List<Integer> cookie = new ArrayList<Integer>();
					ImAddGroupMemberRequestProto req = new ImAddGroupMemberRequestProto(Command.JPUSH_IM.ADD_GROUP_MEMBER, 1, uid, appKey, sid, juid,cookie, bean);
					Channel channel = userNameToPushChannelMap.get(uid);
					channel.writeAndFlush(req);
				}
		});
		
		//   删除群组成员
		server.addEventListener("delGroupMember", AddOrDelGroupMember.class, new DataListener<AddOrDelGroupMember>(){
		@Override
			public void onData(SocketIOClient client, AddOrDelGroupMember data,
					AckRequest ackSender) throws Exception {
				log.info("del group member info, group id: "+data);
				String appKey = data.getAppKey();
				int sid = data.getSid();
				long uid = data.getUid();
				long delUid = data.getToUid();
				long juid = data.getJuid();
				long gid = data.getGid();
				List<Long> list = new ArrayList<Long>();
				list.add(delUid);	
				DeleteGroupMemberRequestBean bean = new DeleteGroupMemberRequestBean(gid, 1, list);
				List<Integer> cookie = new ArrayList<Integer>();
				ImDeleteGroupMemberRequestProto req = new ImDeleteGroupMemberRequestProto(Command.JPUSH_IM.DEL_GROUP_MEMBER, 1, uid, appKey, sid, juid, cookie, bean);
				Channel channel = userNameToPushChannelMap.get(uid);
				channel.writeAndFlush(req);
			}
		});
		
		//  用户退出
		 server.addEventListener("logout", LogoutBean.class, new DataListener<LogoutBean>() {
			@Override
			public void onData(SocketIOClient client, LogoutBean data,
					AckRequest ackSender) throws Exception {
				String appKey = data.getAppKey();
				int sid = data.getSid();
				long juid = data.getJuid();
				long uid = data.getUid();
				String username = data.getUser_name();
				Channel channel = userNameToPushChannelMap.get(uid);
				if(channel==null){
					 log.info("当前用户与jpush的连接已断开.");
					 client.sendEvent("logout", "true");
				} else {
					LogoutRequestBean bean = new LogoutRequestBean(username);
					List<Integer> cookie = new ArrayList<Integer>();
					ImLogoutRequestProto req = new ImLogoutRequestProto(Command.JPUSH_IM.LOGOUT, 1, uid, appKey, sid, juid, cookie, bean);
					channel.writeAndFlush(req);
				}
			}
		 });
		 
		 server.start();
		 //Thread.sleep(Integer.MAX_VALUE);
		 //server.stop();
	}
	
	public static void main(String[] args) throws InterruptedException {
		WebImServer socketServer = new WebImServer();
		socketServer.init();
		socketServer.configMessageEventAndStart();
	}

}
