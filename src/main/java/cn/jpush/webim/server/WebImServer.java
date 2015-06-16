package cn.jpush.webim.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.imageio.ImageIO;

import jpushim.s2b.JpushimSdk2B.ChatMsg;
import jpushim.s2b.JpushimSdk2B.EventNotification;

import org.json.JSONException;
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
import cn.jpush.protocal.utils.JMessage;
import cn.jpush.protocal.utils.ProtocolUtil;
import cn.jpush.protocal.utils.StringUtils;
import cn.jpush.protocal.utils.SystemConfig;
import cn.jpush.socketio.AckMode;
import cn.jpush.socketio.AckRequest;
import cn.jpush.socketio.Configuration;
import cn.jpush.socketio.SocketIOClient;
import cn.jpush.socketio.SocketIOServer;
import cn.jpush.socketio.Transport;
import cn.jpush.socketio.listener.ConnectListener;
import cn.jpush.socketio.listener.DataListener;
import cn.jpush.socketio.listener.DisconnectListener;
import cn.jpush.socketio.listener.ExceptionListener;
import cn.jpush.socketio.transport.NamespaceClient;
import cn.jpush.webim.common.RedisClient;
import cn.jpush.webim.common.UidResourcesPool;
import cn.jpush.webim.socketio.bean.AddFriendCmd;
import cn.jpush.webim.socketio.bean.Group;
import cn.jpush.webim.socketio.bean.GroupMember;
import cn.jpush.webim.socketio.bean.ImageMsgBody;
import cn.jpush.webim.socketio.bean.InnerGroupObject;
import cn.jpush.webim.socketio.bean.LogoutBean;
import cn.jpush.webim.socketio.bean.SdkRequestObject;
import cn.jpush.webim.socketio.bean.TextMsgBody;
import cn.jpush.webim.socketio.bean.MsgContentBean;
import cn.jpush.webim.socketio.bean.SdkAddOrRemoveGroupMembersObject;
import cn.jpush.webim.socketio.bean.SdkCommonErrorRespObject;
import cn.jpush.webim.socketio.bean.SdkCommonSuccessRespObject;
import cn.jpush.webim.socketio.bean.SdkConfigObject;
import cn.jpush.webim.socketio.bean.SdkCreateGroupObject;
import cn.jpush.webim.socketio.bean.SdkExitGroupObject;
import cn.jpush.webim.socketio.bean.SdkGetGroupInfoObject;
import cn.jpush.webim.socketio.bean.SdkGetUserInfoObject;
import cn.jpush.webim.socketio.bean.SdkGroupDetailObject;
import cn.jpush.webim.socketio.bean.SdkGroupInfoObject;
import cn.jpush.webim.socketio.bean.SdkLoginObject;
import cn.jpush.webim.socketio.bean.SdkSendImageMsgObject;
import cn.jpush.webim.socketio.bean.SdkSendTextMsgObject;
import cn.jpush.webim.socketio.bean.SdkSyncEventRespObject;
import cn.jpush.webim.socketio.bean.SdkSyncMsgRespObject;
import cn.jpush.webim.socketio.bean.SdkUpdateGroupInfoObject;
import cn.jpush.webim.socketio.bean.SdkUserInfoObject;
import cn.jpush.webim.socketio.bean.UpdateGroupInfoBean;
import cn.jpush.webim.socketio.bean.User;
import cn.jpush.webim.socketio.bean.UserInfo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qiniu.api.auth.AuthException;
import com.qiniu.api.auth.digest.Mac;
import com.qiniu.api.io.IoApi;
import com.qiniu.api.io.PutExtra;
import com.qiniu.api.io.PutRet;
import com.qiniu.api.rs.PutPolicy;

/**
 * Web IM 客户端接入
 * 
 * 启动该独立进程监听端口，完成 Web 客户端的连接
 * 
 */
public class WebImServer {
	private static Logger log = (Logger) LoggerFactory.getLogger(WebImServer.class);
	/*
	 * 采用 Map 缓存 Web IM 用户标识和客户端到gateway长连接以及gateway到 IM Server 的长连接的对应关系
	 * 方便根据用户找到双向的长连接来发送消息
	 */
	public static Hashtable<String, SocketIOClient> userNameToSessionCilentMap = new Hashtable<String, SocketIOClient>();  //  appKey:用户名 --> 客户端
	public static Hashtable<SocketIOClient, String> sessionClientToUserNameMap = new Hashtable<SocketIOClient, String>();  //  客户端  --> appKey:用户名
	public static Hashtable<String, Channel> userNameToPushChannelMap = new Hashtable<String, Channel>();   //  appKey:用户名 --> IM Server
	public static Hashtable<Channel, String> pushChannelToUsernameMap = new Hashtable<Channel, String>();   //  IM Server --> appKey:用户名
	
	private static final int PORT = SystemConfig.getIntProperty("webim.server.port");  // WebIM 监听端口，配置文件中配置
	private static final String SDK_VERSION_V1 = "1.0";  //  Gateway 与 JS-SDK 交互数据结构中的版本标识
	private static final String DATA_AISLE = "data";  //  Gateway 返回给 JS-SDK 的数据结构中表示i数据内容的key
	
	private Gson gson = new Gson();
	private Configuration config;
	private SocketIOServer server;
	
	public void init() {
		 config = new Configuration();
		 config.setPort(PORT);config.setAllowCustomRequests(true);
		 log.info("----origin ----"+config.getOrigin());
		 config.setExceptionListener(new ExceptionListener() {	
				@Override
				public void onMessageException(Exception e, String data,
						SocketIOClient client) {
					log.warn("client onMessageException: "+e.getMessage());
				}
				@Override
				public void onJsonException(Exception e, Object data, SocketIOClient client) {
					
				}
				@Override
				public void onEventException(Exception e, List<Object> args,
						SocketIOClient client) {
					log.warn("client onEventException: "+e.getMessage());
				}
				@Override
				public void onDisconnectException(Exception e, SocketIOClient client) {
					log.warn("client onDisconnectException: "+e.getMessage());
				}
				@Override
				public void onConnectException(Exception e, SocketIOClient client) {
					log.warn("client exception: "+e.getMessage());
				}
				@Override
				public boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e)
						throws Exception {
					log.warn("client exceptionCaught: "+e.getMessage());
					return false;
				}
		 });
		 server = new SocketIOServer(config);
	}
	
	public void configMessageEventAndStart() throws InterruptedException{
		if(config==null || server==null){
			log.error("you have not init the config and server. please do this first.");
			return;
		}
		
		 //用户连接
		 server.addConnectListener(new ConnectListener() {
			@Override
			public void onConnect(SocketIOClient client) {   //  客户端连接成功后需要向客户端发送连接成功响应
				log.info(String.format("connect from web client -- the session id is %s, client transport method is %s", client.getSessionId(), client.getTransport()));
				SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject("1.0", "1000010", JMessage.Method.CONNECT, null);
				client.sendEvent("onConnected", gson.toJson(resp));
			}
		 });
		 
		 // 用户断开
		 server.addDisconnectListener(new DisconnectListener() {
			@Override
			public void onDisconnect(SocketIOClient client) {
				if(client!=null){
					SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject("1.0", "1000010", JMessage.Method.DISCONNECT, "");
					client.sendEvent("onDisconnected", gson.toJson(resp));
					log.info(String.format("the connection is disconnect -- the session id is %s", client.getSessionId()));
					String kan = "";
					if(WebImServer.sessionClientToUserNameMap!=null){
						try{
							kan = WebImServer.sessionClientToUserNameMap.get(client);   //  从缓存中根据客户端长连接获取用户标识
						} catch (Exception e){
							log.error(String.format("through client get username exception: %s, so can not close connect to im server", e.getMessage()));
							return;
						}
					}
					if(StringUtils.isNotEmpty(kan)){  //  根据用户标识来清空该断开用户的所有缓存数据
						//WebImServer.userNameToSessionCilentMap.remove(kan);
						Channel channel = WebImServer.userNameToPushChannelMap.get(kan);
						WebImServer.userNameToPushChannelMap.remove(kan);
						WebImServer.sessionClientToUserNameMap.remove(client);
						if(channel!=null){
							WebImServer.pushChannelToUsernameMap.remove(channel);
							channel.close();   //  断开与 IM Server的长连接
						} else {
							log.error(String.format("user: %s get tcp connector to im server exception", kan));
						}
					}
				}	
			}
		});
	
		 /*
		  * Web IM Server 处理 JS-SDK 定义的 API 所对应的后台逻辑
		  */
		 server.addEventListener(WebImServer.DATA_AISLE, SdkRequestObject.class, new DataListener<SdkRequestObject>() {
				@Override
				public void onData(SocketIOClient client, SdkRequestObject data,
						AckRequest ackSender) throws Exception {
					String version = data.getApiVersion();  //  JS-SDK 版本号
					String method = data.getMethod();       //  JS-SDK API 对应的 method   
					
					if(StringUtils.isEmpty(method)||StringUtils.isEmpty(version)){
						log.error(String.format("user pass empty method or version arguments, deny to execute"));
						return;
					}
					if(client==null){
						log.error(String.format("data eventlistener SocketIOClient object is null, deny to execute"));
						return;
					}
					if(version.equals(SDK_VERSION_V1)){   //  JS-SDK V1版本的处理逻辑
						V1 v1 = new V1();  
						switch (method) { 
							case JMessage.Method.CONFIG:
								v1.config(client, data);
								break;
							case JMessage.Method.LOGIN:
								v1.login(client, data);
								break;
							case JMessage.Method.LOGOUT:
								v1.logout(client, data);
								break;
							case JMessage.Method.USERINFO_GET:
								v1.getUserInfo(client, data);
								break;
							case JMessage.Method.TEXTMESSAGE_SEND:
								v1.sendTextMessage(client, data);
								break;
							case JMessage.Method.IMAGEMESSAGE_SEND:
								v1.sendImageMessage(client, data);
								break;
							case JMessage.Method.MESSAGE_RECEIVED:
								v1.respMessageReceived(client, data);
								break;
							case JMessage.Method.EVENT_RECEIVED:
								v1.respEventReceived(client, data);
								break;
							case JMessage.Method.GROUP_CREATE:
								v1.createGroup(client, data);
								break;
							case JMessage.Method.GROUPMEMBERS_ADD:
								v1.addGroupMembers(client, data);
								break;
							case JMessage.Method.GROUPMEMBERS_REMOVE:
								v1.removeGroupMembers(client, data);
								break;
							case JMessage.Method.GROUPINFO_GET:
								v1.getGroupInfo(client, data);
								break;
							case JMessage.Method.GROUPINFO_UPDATE:
								v1.updateGroupInfo(client, data);
								break;
							case JMessage.Method.GROUP_EXIT:
								v1.exitGroup(client, data);
								break;
							case JMessage.Method.GROUPLIST_GET:
								v1.getGroupList(client, data);
								break;
							default:
								log.error("Undefined JMessage method Error");
								break;
						}
					} else {
						log.error("Undefined SDK Version Error");
					}
				}	
		 });
		 server.start();
	}
	
	public static void main(String[] args) throws InterruptedException {
		WebImServer socketServer = new WebImServer();
		socketServer.init();
		socketServer.configMessageEventAndStart();
	}

}
