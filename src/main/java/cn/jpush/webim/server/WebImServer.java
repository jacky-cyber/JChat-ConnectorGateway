package cn.jpush.webim.server;

import io.netty.channel.Channel;

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
import cn.jpush.socketio.AckRequest;
import cn.jpush.socketio.Configuration;
import cn.jpush.socketio.SocketIOClient;
import cn.jpush.socketio.SocketIOServer;
import cn.jpush.socketio.Transport;
import cn.jpush.socketio.listener.ConnectListener;
import cn.jpush.socketio.listener.DataListener;
import cn.jpush.socketio.listener.DisconnectListener;
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
 * Web Im 业务 Server
 *
 */
public class WebImServer {
	private static Logger log = (Logger) LoggerFactory.getLogger(WebImServer.class);
	public static Hashtable<String, SocketIOClient> userNameToSessionCilentMap = new Hashtable<String, SocketIOClient>();  //  appKey:用户名 --> 客户端
	public static Hashtable<SocketIOClient, String> sessionClientToUserNameMap = new Hashtable<SocketIOClient, String>();  //  客户端  --> appKey:用户名
	public static Hashtable<String, Channel> userNameToPushChannelMap = new Hashtable<String, Channel>();   //  appKey:用户名 --> IM Server
	public static Hashtable<Channel, String> pushChannelToUsernameMap = new Hashtable<Channel, String>();   //  IM Server --> appKey:用户名
	private static final int PORT = SystemConfig.getIntProperty("webim.server.port");
	private static final String SDK_VERSION_V1 = "1.0";
	private static final String DATA_AISLE = "data";
	private Gson gson = new Gson();
	private Configuration config;
	private SocketIOServer server;
	
	public void init() {
		 config = new Configuration();
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
							kan = WebImServer.sessionClientToUserNameMap.get(client);
						} catch (Exception e){
							log.error(String.format("user disconnect exception: %s", e.getMessage()));
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
							log.error(String.format("user: %s gateway to push's channel is disconnected", kan));
						}
					}
				}	
			}
		});
		  
		 // gateway 数据接收处理
		 server.addEventListener(WebImServer.DATA_AISLE, SdkRequestObject.class, new DataListener<SdkRequestObject>() {
				@Override
				public void onData(SocketIOClient client, SdkRequestObject data,
						AckRequest ackSender) throws Exception {
					String version = data.getApiVersion();
					String method = data.getMethod();
					if(StringUtils.isEmpty(method)||StringUtils.isEmpty(version)){
						log.error(String.format("user pass empty method or version arguments, deny to execute"));
						return;
					}
					if(client==null){
						log.error(String.format("data eventlistener SocketIOClient object is null, deny to execute"));
						return;
					}
					if(version.equals(SDK_VERSION_V1)){
						if(JMessage.Method.CONFIG.equals(method)){
							V1.config(client, data);
						} else if(JMessage.Method.LOGIN.equals(method)){
							V1.login(client, data);
						} else if(JMessage.Method.LOGOUT.equals(method)){
							V1.logout(client, data);
						} else if(JMessage.Method.USERINFO_GET.equals(method)){
							V1.getUserInfo(client, data);
						} else if(JMessage.Method.TEXTMESSAGE_SEND.equals(method)){
							V1.sendTextMessage(client, data);
						} else if(JMessage.Method.IMAGEMESSAGE_SEND.equals(method)){
							V1.sendImageMessage(client, data);
						} else if(JMessage.Method.MESSAGE_FEEDBACK.equals(method)){
							V1.respMessageReceived(client, data);
						} else if(JMessage.Method.EVENT_FEEDBACK.equals(method)){
							V1.respEventReceived(client, data);
						} else if(JMessage.Method.GROUP_CREATE.equals(method)){
							V1.createGroup(client, data);
						} else if(JMessage.Method.GROUPMEMBERS_ADD.equals(method)){
							V1.addGroupMembers(client, data);
						} else if(JMessage.Method.GROUPMEMBERS_REMOVE.equals(method)){
							V1.removeGroupMembers(client, data);
						} else if(JMessage.Method.GROUPINFO_GET.equals(method)){
							V1.getGroupInfo(client, data);
						} else if(JMessage.Method.GROUPINFO_UPDATE.equals(method)){
							V1.updateGroupInfo(client, data);
						} else if(JMessage.Method.GROUP_EXIT.equals(method)){
							V1.exitGroup(client, data);
						} else if(JMessage.Method.GROUPLIST_GET.equals(method)){
							V1.getGroupList(client, data);
						} else {
							log.error("no correct mapping for JMessage method");
						}
					} else {
						log.error("SDK Version Error");
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
