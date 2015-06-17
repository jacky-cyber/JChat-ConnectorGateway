package cn.jpush.webim.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
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

import com.couchbase.client.CouchbaseClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qiniu.api.auth.AuthException;
import com.qiniu.api.auth.digest.Mac;
import com.qiniu.api.io.IoApi;
import com.qiniu.api.io.PutExtra;
import com.qiniu.api.io.PutRet;
import com.qiniu.api.net.CallRet;
import com.qiniu.api.rs.PutPolicy;
import com.qiniu.api.rs.RSClient;

import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.common.JPushTcpClient;
import cn.jpush.protocal.common.JPushTcpClientHandler;
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
import cn.jpush.protocal.push.PushLoginRequestBean;
import cn.jpush.protocal.push.PushLoginResponseBean;
import cn.jpush.protocal.utils.APIProxy;
import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.Configure;
import cn.jpush.protocal.utils.HttpResponseWrapper;
import cn.jpush.protocal.utils.JMessage;
import cn.jpush.protocal.utils.ProtocolUtil;
import cn.jpush.protocal.utils.Sign;
import cn.jpush.protocal.utils.StringUtils;
import cn.jpush.protocal.utils.SystemConfig;
import cn.jpush.socketio.Configuration;
import cn.jpush.socketio.SocketIOClient;
import cn.jpush.socketio.SocketIOServer;
import cn.jpush.webim.common.CouchBaseManager;
import cn.jpush.webim.common.RedisClient;
import cn.jpush.webim.common.UidResourcesPool;
import cn.jpush.webim.socketio.bean.GroupMember;
import cn.jpush.webim.socketio.bean.HttpErrorObject;
import cn.jpush.webim.socketio.bean.ImageMsgBody;
import cn.jpush.webim.socketio.bean.InnerGroupObject;
import cn.jpush.webim.socketio.bean.MsgContentBean;
import cn.jpush.webim.socketio.bean.SdkCommonErrorRespObject;
import cn.jpush.webim.socketio.bean.SdkCommonSuccessRespObject;
import cn.jpush.webim.socketio.bean.SdkGroupDetailObject;
import cn.jpush.webim.socketio.bean.SdkGroupInfoObject;
import cn.jpush.webim.socketio.bean.SdkRequestObject;
import cn.jpush.webim.socketio.bean.SdkUserInfoObject;
import cn.jpush.webim.socketio.bean.TextMsgBody;
import cn.jpush.webim.socketio.bean.User;
import cn.jpush.webim.socketio.bean.UserInfo;

/**
 * JS-SDK V1 版本的后台处理逻辑
 *
 */
public class V1 {
	private static Logger log = (Logger) LoggerFactory.getLogger(V1.class);
	private static final String VERSION = "1.0";
	private static final String DATA_AISLE = "data";
	private static final String FILE_STORE_PATH = SystemConfig.getProperty("im.file.store.path");  //  JS-SDK 发送图片时图片文件的缓存临时目录
	private static Gson gson = new Gson();
	private static RedisClient redisClient = new RedisClient();
	public static CountDownLatch pushLoginInCountDown;  //  同步变量，用于控制在 JPush Login 完成获取到数据后再开始 IM Login
	private JPushTcpClient jpushIMTcpClient;

	/**
	 * SDK 配置校验逻辑
	 * 
	 * @param client  到客户端的长连接对象
	 * @param data  自定义Object，用来封装 JS-SDK API 请求中的数据
	 */
	public void config(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		String appKey = data.getParams().getAppKey();
		String timestamp = data.getParams().getTimestamp();
		String randomStr = data.getParams().getRandomStr();
		String signature = data.getParams().getSignature();
		log.info(String.format("v1 config -- request data -- data -- %s", gson.toJson(data)));
		if (StringUtils.isEmpty(signature) || StringUtils.isEmpty(randomStr)
				|| StringUtils.isEmpty(timestamp) || StringUtils.isEmpty(appKey)) {
			log.error(String.format("v1 config -- user pass null arguments exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.CONFIG);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			/*
			 * couchbase 获取 appkey 对应的 masterSecret
			 * 生成签名与客户端签名比较看签名是否正确
			 */
			/*CouchbaseClient cbClient = CouchBaseManager.getCouchbaseClientInstance("appbucket");
			String masterSecrect_json = (String) cbClient.get(appKey);
			Map dataMap = gson.fromJson(masterSecrect_json, HashMap.class);
			String masterSecrect = (String) dataMap.get("apiMasterSecret");
			log.info("couchbase get value: "+masterSecrect_json);
			log.info("from cb get masterSecect: "+masterSecrect);
			String _signature = Sign.getSignature(appKey, timestamp, randomStr, masterSecrect);*/
			String _signature = Sign.getSignature(appKey, timestamp, randomStr, "054d6103823a726fc12d0466");
			if(signature.equals(_signature)){
				SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(V1.VERSION, id, JMessage.Method.CONFIG, "");
				log.info(String.format("v1 config -- resp data -- %s", gson.toJson(resp)));
				client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
				
				Jedis jedis = null;
				try {
					jedis = redisClient.getJeids();
					jedis.set(appKey+signature, signature);
					jedis.expire(appKey, 7200);  // 签名2小时过期
				} catch (JedisConnectionException e) {
					log.error(String.format("v1 config -- redis exception: %s", e.getMessage()));
					redisClient.returnBrokenResource(jedis);
					throw new JedisConnectionException(e);
				} finally {
					redisClient.returnResource(jedis);
				}
				
			} else {
				SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.CONFIG);
				resp.setErrorInfo(JMessage.Error.CONFIG_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.CONFIG_EXCEPTION));
				client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
				log.error(String.format("v1 config -- user signature exception"));
			}
		}
	}
	
	public JPushTcpClient getTcpClient(){
		return this.jpushIMTcpClient;
	}
	
	/**
	 * 获取到 IM Server 的新的长连接
	 * @param appKey
	 * @return
	 */
	public Channel getPushChannel(String appKey){
		log.info("v1 user build connect channel to im server");
		jpushIMTcpClient = new JPushTcpClient(appKey);
		Channel pushChannel = null;                                   
		try {
			pushChannel = jpushIMTcpClient.getChannel();
		} catch (Exception e) {
			log.warn(String.format("v1 user get build connect channel to im server exception: %s", e.getMessage()));
			pushChannel = getPushChannel(appKey);
		}
		return pushChannel;
	}
	
	/**
	 * 用户登陆
	 * 
	 * @param client
	 * @param data  客户端请求数据的封装对象
	 * @throws InterruptedException
	 */
	public void login(SocketIOClient client, SdkRequestObject data) throws InterruptedException {
		long startTime=System.currentTimeMillis();
		String id = data.getId();
		String appkey = data.getParams().getAppKey();
		String username = data.getParams().getUsername();
		String password = data.getParams().getPassword();
		String signature = data.getParams().getSignature();
		String isReLogin = data.getParams().getIsReLogin();
		log.info(String.format("v1 login -- request data -- data: %s", gson.toJson(data)));
		
		long phrse1StartTime = System.currentTimeMillis();
		if (StringUtils.isEmpty(appkey) || StringUtils.isEmpty(username)
				|| StringUtils.isEmpty(password) || StringUtils.isEmpty(signature)) {
			log.error("v1 login -- use pass null arguments exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.LOGIN);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		long phrse1EndTime = System.currentTimeMillis();
		log.info("---- phrse1 耗时 ---- "+(phrse1EndTime-phrse1StartTime));
		
		long phrse2StartTime = System.currentTimeMillis();
		SocketIOClient preClient = WebImServer.userNameToSessionCilentMap.get(appkey+":"+username);
		// 对在浏览器侧多次登陆的同一用户的之前的连接发下线通知
		if(preClient!=null){
			if(!"true".equals(isReLogin)){  //  断线重连的不可下发该通知
				if((!preClient.getSessionId().equals(client.getSessionId()))){  // 区分是否同一个连接，避免断开重新登陆的时候下发该通知
					log.error("v1 login -- offline to pre client -- user --"+username);
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, "100001", JMessage.Method.INFO_NOTIFICATION);
					resp.setErrorInfo(JMessage.Error.Multi_Login, JMessage.Error.getErrorMessage(JMessage.Error.Multi_Login));
					preClient.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
					log.info("send data to client: "+gson.toJson(resp));
					Channel preChannel = WebImServer.userNameToPushChannelMap.get(appkey+":"+username);
					if(preChannel!=null){
						WebImServer.pushChannelToUsernameMap.remove(preChannel);
						preChannel.close();
					}
				}
			}
		}
		long phrse2EndTime = System.currentTimeMillis();
		log.info("---- phrse2 耗时 ---- "+(phrse2EndTime-phrse2StartTime));
		
		long phrse3StartTime = System.currentTimeMillis();
		// 关系绑定
		WebImServer.userNameToSessionCilentMap.put(appkey+":"+username, client);
		WebImServer.sessionClientToUserNameMap.put(client, appkey+":"+username);
		
		Channel pushChannel = getPushChannel(appkey);
		
		// 关系绑定
		String dataCacheKey = appkey+":"+username;
		WebImServer.userNameToPushChannelMap.put(dataCacheKey, pushChannel);
		WebImServer.pushChannelToUsernameMap.put(pushChannel, dataCacheKey);
		log.info(String.format("v1 login -- add data cache username: %s <--> channel: %s data cache", dataCacheKey, pushChannel));
		long phrse3_map_StartTime = System.currentTimeMillis();
		log.info("---- phrse3 map 耗时 ---- "+(phrse3_map_StartTime-phrse3StartTime));
		
		// 签名有效性验证
		long phrse3_1StartTime = System.currentTimeMillis();
		if(!V1.isSignatureValid(appkey, signature)){
			log.error("v1 login -- user signature exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.LOGIN);
			resp.setErrorInfo(JMessage.Error.SIGNATURE_INVALID, JMessage.Error.getErrorMessage(JMessage.Error.SIGNATURE_INVALID));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		long phrse3_1EndTime = System.currentTimeMillis();
		log.info("---- phrse3_1 耗时 ---- "+(phrse3_1EndTime-phrse3_1StartTime));
		
		long phrse3_2StartTime = System.currentTimeMillis();
		Map<String, String> juidData = UidResourcesPool.getUidAndPassword();
		if(juidData==null){
			log.error(String.format("v1 login -- get juid and password exception"));
			return;
		}
		
		long juid = Long.parseLong(String.valueOf(juidData.get("uid")));
		String juid_password = String.valueOf(juidData.get("password"));
		
		pushLoginInCountDown = new CountDownLatch(1);
		long phrse3_2EndTime = System.currentTimeMillis();
		log.info("---- phrse3_2 耗时 ---- "+(phrse3_2EndTime-phrse3_2StartTime));
		
		long phrse3EndTime = System.currentTimeMillis();
		log.info("---- phrse3 耗时 ---- "+(phrse3EndTime-phrse3StartTime));
		
		
		long pushLoginStartTime = System.currentTimeMillis();
		PushLoginRequestBean pushLoginBean = new PushLoginRequestBean(juid, "a", ProtocolUtil.md5Encrypt(juid_password), 10800, appkey, 0);
		pushChannel.writeAndFlush(pushLoginBean);
		log.info(String.format("v1 login -- user: ..%s begin jpush login -- juid: %d, password: %s", username, juid, juid_password));
		
		pushLoginInCountDown.await(); // 等待push login返回数据
		PushLoginResponseBean pushLoginResponseBean = jpushIMTcpClient.getjPushClientHandler().getPushLoginResponseBean();
		int sid = pushLoginResponseBean.getSid();
		log.info(String.format("v1 login -- user: %s jpush login response -- code: %d, sid: %d", username, pushLoginResponseBean.getResponse_code(), sid));
		if(0 != pushLoginResponseBean.getResponse_code()) {
			log.error(String.format("v1 login -- user: %s jpush login exception", username));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.LOGIN);
			resp.setErrorInfo(JMessage.Error.USER_LOGIN_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.USER_LOGIN_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		long pushLoginEndTime = System.currentTimeMillis();
		log.info("---- push login 耗时 ---- "+(pushLoginEndTime-pushLoginStartTime));

		// 存储用户信息
		long redisStartTime = System.currentTimeMillis();
		Jedis jedis = null;
		try {
			jedis = redisClient.getJeids();
			Map<String, String> map = new HashMap<String, String>();
			map.put("appKey", appkey);
			map.put("password", password);
			map.put("juid", String.valueOf(juid));
			map.put("sid", String.valueOf(sid));
			map.put("juidPassword", juid_password);
			jedis.hmset(appkey + ":" + username, map);
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 login -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		long redisEndTime = System.currentTimeMillis();
		log.info("---- redis 操作耗时 ----- "+(redisEndTime-redisStartTime));

		long phrse4StartTime = System.currentTimeMillis();
		// 设置数据 用于心跳
		jpushIMTcpClient.getjPushClientHandler().setSid(pushLoginResponseBean.getSid());
		jpushIMTcpClient.getjPushClientHandler().setJuid(juid);

		// IM Login
		long rid = Long.parseLong(id);
		LoginRequestBean bean = new LoginRequestBean(username, StringUtils.toMD5(password));
		List<Integer> cookie = new ArrayList<Integer>();
		ImLoginRequestProto req = new ImLoginRequestProto(Command.JPUSH_IM.LOGIN, 1, 0, pushLoginResponseBean.getSid(),
				juid, appkey, rid, cookie, bean);
		pushChannel.writeAndFlush(req);
		long phrse4EndTime = System.currentTimeMillis();
		log.info("---- phrse4 耗时 ---- "+(phrse4EndTime-phrse4StartTime));
		
		long endTime=System.currentTimeMillis();
		log.info("----- 处理 login 请求耗时 ----: "+(endTime-startTime));
	}

	/**
	 * 用户登出
	 * 
	 * @param client
	 * @param data
	 */
	public void logout(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		long rid = Long.parseLong(id);
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("v1 logout -- request data -- data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(keyAndname)) {
			log.error("v1 logout -- search data cache exception, maybe you have not login");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.LOGOUT);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.error("v1 logout -- search data cache resolve appkey and username exception");
				return;
			}
		}
		Jedis jedis = null;
		int sid = 0;
		long juid = 0L;
		long uid = 0L;
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName, "appKey", "sid", "juid", "uid");
			appKey = dataList.get(0);
			sid = Integer.parseInt(dataList.get(1));
			juid = Long.parseLong(dataList.get(2));
			uid = Long.parseLong(dataList.get(3));
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 logout -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":" + userName);
		if (channel == null) {
			log.error("v1 logout -- search data cache resolve channel is null, so can not send data to im server");
			SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
					V1.VERSION, id, JMessage.Method.LOGOUT, "");
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		} else {
			LogoutRequestBean bean = new LogoutRequestBean(userName);
			List<Integer> cookie = new ArrayList<Integer>();
			ImLogoutRequestProto req = new ImLogoutRequestProto(Command.JPUSH_IM.LOGOUT, 1, uid, appKey, sid, juid, rid, cookie, bean);
			channel.writeAndFlush(req);
		}
	}
	
	/**
	 * 获取用户信息
	 * 
	 * @param client
	 * @param data
	 */
	public void getUserInfo(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId(); // 请求rid
		String username = data.getParams().getUsername();
		String signature = data.getParams().getSignature();
		log.info(String.format("v1 getUserInfo -- request data -- data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(username) || StringUtils.isEmpty(signature)) {
			log.error("v1 getUserInfo -- user pass null arguments exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.USERINFO_GET);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		if (StringUtils.isEmpty(keyAndname)) {
			log.error(String.format("v1 getUserInfo -- search data cache exception, maybe you have not login"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.USERINFO_GET);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.error("v1 getUserInfo -- search data cache resolve appkey and username exception");
				return;
			} else {
				// 签名有效性验证
				if(!V1.isSignatureValid(appKey, signature)) {
					log.error(String.format("v1 getUserInfo -- user: %s signature exception", username));
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
							id, JMessage.Method.USERINFO_GET);
					resp.setErrorInfo(JMessage.Error.SIGNATURE_INVALID, JMessage.Error.getErrorMessage(JMessage.Error.SIGNATURE_INVALID));
					client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
					return;
				}
			}
		}
		Jedis jedis = null;
		String token = "";
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName, "appKey", "token");
			appKey = dataList.get(0);
			token = dataList.get(1);
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 getUserInfo -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		
		log.info(String.format("v1 getUserInfo -- begin call sdk-api-getUserInfo"));
		HttpResponseWrapper responseWrapper = APIProxy.getUserInfo(appKey, username, token);
		if (responseWrapper!=null && responseWrapper.isOK()) {
			log.info(String.format("v1 getUserInfo -- call sdk-api-getUserInfo success"));
			SdkUserInfoObject userInfo = new SdkUserInfoObject();
			userInfo = gson.fromJson(responseWrapper.content, SdkUserInfoObject.class);
			SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(V1.VERSION, id, JMessage.Method.USERINFO_GET, gson.toJson(userInfo));
			log.info(String.format("v1 getUserInfo -- response client data: %s", gson.toJson(userInfo)));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		} else {
			log.warn(String.format("v1 getUserInfo -- call sdk-api-getUserInfo exception: %s", responseWrapper.content));
			HttpErrorObject errorObject = gson.fromJson(responseWrapper.content, HttpErrorObject.class);
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.USERINFO_GET);
			resp.setErrorInfo(errorObject.getError().getCode(), errorObject.getError().getMessage());
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		}
	}

	/**
	 * 发送文本消息
	 * 
	 * @param client
	 * @param data
	 */
	public void sendTextMessage(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		String signature = data.getParams().getSignature();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		String targetId = data.getParams().getTargetId();
		String targetType = data.getParams().getTargetType();
		String text = data.getParams().getText();
		log.info(String.format("v1 sendTextMessage -- request data -- data: %s", gson.toJson(data)));
		if(StringUtils.isEmpty(targetId) || StringUtils.isEmpty(targetType) || StringUtils.isEmpty(signature)) {
			log.error(String.format("v1 sendTextMessage -- user pass null arguments exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.TEXTMESSAGE_SEND);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		if (StringUtils.isEmpty(keyAndname)) {
			log.error(String.format("v1 sendTextMessage -- search data cache to get appkey and username null exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.TEXTMESSAGE_SEND);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("v1 sendTextMessage -- search data cache resolve appkey and username exception");
				return;
			} else {
				// 签名有效性验证
				if(!V1.isSignatureValid(appKey, signature)){
					log.error(String.format("v1 sendTextMessage -- user signature exception"));
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.TEXTMESSAGE_SEND);
					resp.setErrorInfo(JMessage.Error.SIGNATURE_INVALID, JMessage.Error.getErrorMessage(JMessage.Error.SIGNATURE_INVALID));
					client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
					return;
				}
			}
		}
		long rid = Long.parseLong(id);
		int version = 1;
		Jedis jedis = null;
		int sid = 0;
		long juid = 0L;
		String token = "";
		long uid = 0L;
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName,
					"appKey", "sid", "juid", "token", "uid");
			appKey = dataList.get(0);
			sid = Integer.parseInt(dataList.get(1));
			juid = Long.parseLong(dataList.get(2));
			token = dataList.get(3);
			uid = Long.parseLong(dataList.get(4));
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 sendTextMessage -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		
		MsgContentBean msgContent = new MsgContentBean();
		msgContent.setVersion(version);
		msgContent.setTarget_type(targetType);
		msgContent.setTarget_id(targetId);
		msgContent.setTarget_name(targetId);
		msgContent.setFrom_type("user");
		msgContent.setFrom_platform("web");
		msgContent.setFrom_id(userName);
		msgContent.setFrom_name(userName);
		HttpResponseWrapper _wrapper = APIProxy.getUserInfo(appKey, userName, token);
		if (_wrapper.isOK()) {
			UserInfo userInfo = gson.fromJson(_wrapper.content, UserInfo.class);
			String _nickName = userInfo.getNickname();
			if(!StringUtils.isEmpty(_nickName)){
				msgContent.setFrom_name(_nickName);
			}
		}
		msgContent.setCreate_time(StringUtils.getCreateTime());
		msgContent.setMsg_type("text");
		TextMsgBody msgBody = new TextMsgBody();
		msgBody.setText(text);
		msgBody.setExtras(new HashMap());
		msgContent.setMsg_body(msgBody);

		log.info(String.format("v1 sendTextMessage -- user: %s send chat msg, content is: ", userName, gson.toJson(data)));
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":" + userName);
		if (channel == null) {
			log.warn(String.format("v1 sendTextMessage -- search data cache through key and name get channel null exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, String.valueOf(rid), 
					JMessage.Method.TEXTMESSAGE_SEND);
			resp.setErrorInfo(JMessage.Error.CONNECTION_DISCONNECT, JMessage.Error.getErrorMessage(JMessage.Error.CONNECTION_DISCONNECT));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		if ("single".equals(targetType)) {
			log.info(String.format("v1 sendTextMessage -- begin call sdk-api-getUserInfo..."));
			HttpResponseWrapper responseWrapper = APIProxy.getUserInfo(appKey, targetId, token);
			long target_uid = 0L;
			if (responseWrapper.isOK()) {
				log.info(String.format("v1 sendTextMessage -- call sdk-api-getUserInfo success"));
				UserInfo userInfo = gson.fromJson(responseWrapper.content, UserInfo.class);
				target_uid = userInfo.getUid();
				SendSingleMsgRequestBean bean = new SendSingleMsgRequestBean(target_uid, gson.toJson(msgContent));
				List<Integer> cookie = new ArrayList<Integer>();
				ImSendSingleMsgRequestProto req = new ImSendSingleMsgRequestProto(Command.JPUSH_IM.SENDMSG_SINGAL, 1, uid, appKey, sid,
						juid, rid, cookie, bean);
				channel.writeAndFlush(req);
			} else {
				log.warn(String.format("v1 sendTextMessage -- user: %s call sdk-api-getUserInfo exception: %s", userName, responseWrapper.content));
			}
		} else if ("group".equals(targetType)) {
			SendGroupMsgRequestBean bean = new SendGroupMsgRequestBean(Long.parseLong(targetId), gson.toJson(msgContent));
			List<Integer> cookie = new ArrayList<Integer>();
			ImSendGroupMsgRequestProto req = new ImSendGroupMsgRequestProto(Command.JPUSH_IM.SENDMSG_GROUP, 1, uid, appKey, sid, juid,
					rid, cookie, bean);
			channel.writeAndFlush(req);
		}
	}

	/**
	 * 发送图片消息
	 * 
	 * @param client
	 * @param data
	 */
	public void sendImageMessage(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		String signature = data.getParams().getSignature();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		String targetId = data.getParams().getTargetId();
		String targetType = data.getParams().getTargetType();
		String fileId = data.getParams().getFileId();
		log.info(String.format("v1 sendImageMessage -- request data -- data: %s", gson.toJson(data)));
		if(StringUtils.isEmpty(targetId)||StringUtils.isEmpty(targetType)
				||StringUtils.isEmpty(fileId)||StringUtils.isEmpty(signature)) {
			log.error(String.format("v1 sendImageMessage -- user pass null arguments exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.IMAGEMESSAGE_SEND);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		if (StringUtils.isEmpty(keyAndname)) {
			log.error(String.format("v1 sendImageMessage -- search data cache get appkey and username null exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.IMAGEMESSAGE_SEND);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.error(String.format("v1 sendImageMessage -- search data cache resolve appkey and username null exception"));
				return;
			} else {
				// 签名有效性验证
				if (!V1.isSignatureValid(appKey, signature)) {
					log.error(String.format("v1 sendImageMessage -- user signature exception"));
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.IMAGEMESSAGE_SEND);
					resp.setErrorInfo(JMessage.Error.SIGNATURE_INVALID, JMessage.Error.getErrorMessage(JMessage.Error.SIGNATURE_INVALID));
					client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
					return;
				}
			}
		}
		long rid = Long.parseLong(id);
		int version = 1;
		Jedis jedis = null;
		int sid = 0;
		long juid = 0L;
		String token = "";
		long uid = 0L;
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName, "appKey", "sid", "juid", "token", "uid");
			appKey = dataList.get(0);
			sid = Integer.parseInt(dataList.get(1));
			juid = Long.parseLong(dataList.get(2));
			token = dataList.get(3);
			uid = Long.parseLong(dataList.get(4));
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 sendImageMessage -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		// msg json 封装，详情参考wiki文档
		MsgContentBean msgContent = new MsgContentBean();
		msgContent.setVersion(version);
		msgContent.setTarget_type(targetType);
		msgContent.setTarget_id(targetId);
		msgContent.setTarget_name(targetId);
		msgContent.setFrom_type("user");
		msgContent.setFrom_platform("web");
		msgContent.setFrom_id(userName);
		msgContent.setFrom_name(userName);
		HttpResponseWrapper _wrapper = APIProxy.getUserInfo(appKey, userName, token);
		if (_wrapper.isOK()) {
			UserInfo userInfo = gson.fromJson(_wrapper.content, UserInfo.class);
			String _nickName = userInfo.getNickname();
			if(!StringUtils.isEmpty(_nickName)){
				msgContent.setFrom_name(_nickName);
			}
		}
		msgContent.setCreate_time(StringUtils.getCreateTime());
		msgContent.setMsg_type("image");
		ImageMsgBody msgBody = new ImageMsgBody();
		String filePath = FILE_STORE_PATH + fileId; // 获取上传的图片文件在服务器临时目录的地址
		String mediaId = V1.getMediaId(uid); 
		Map fileInfoMap = null;
		try {
			String response = V1.uploadFile(mediaId, filePath); //  上传该图片文件到七牛
			log.info(String.format("v1 sendImageMessage -- upload image file response: %s", response));
			fileInfoMap = V1.getFileMetaInfo(mediaId, filePath);  //  获取该图片文件的元信息，IM Message JSON 需要图片长宽等信息
		} catch (AuthException | JSONException e) {
			log.error(String.format("v1 sendImageMessage -- upload file or getMetaInfo exception: %s", e.getMessage()));
		} catch (IOException e) {
			log.error(String.format("v1 sendImageMessage -- upload file or getMetaInfo exception: %s", e.getMessage()));
		}
		msgBody.setMedia_id(mediaId);
		msgBody.setMedia_crc32(Long.parseLong(String.valueOf(fileInfoMap.get("crc32"))));
		msgBody.setWidth(Integer.parseInt(String.valueOf(fileInfoMap.get("width"))));
		msgBody.setHeight(Integer.parseInt(String.valueOf(fileInfoMap.get("height"))));
		msgBody.setExtras(new HashMap());
		msgContent.setMsg_body(msgBody);
		log.info(String.format("v1 sendImageMessage -- user: %s send chat msg, content is: ", userName, gson.toJson(data)));
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":" + userName);
		if (channel == null) {
			log.warn("v1 sendImageMessage -- user search data cache get channel to im server exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, String.valueOf(rid), 
					JMessage.Method.TEXTMESSAGE_SEND);
			resp.setErrorInfo(JMessage.Error.CONNECTION_DISCONNECT, JMessage.Error.getErrorMessage(JMessage.Error.CONNECTION_DISCONNECT));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		if ("single".equals(targetType)) {
			log.info(String.format("v1 sendImageMessage -- begin call sdk-api-getUserInfo ..."));
			HttpResponseWrapper responseWrapper = APIProxy.getUserInfo(appKey, targetId, token);
			long target_uid = 0L;
			if (responseWrapper.isOK()) {
				log.info(String.format("v1 sendImageMessage -- call sdk-api-getUserInfo success"));
				UserInfo userInfo = gson.fromJson(responseWrapper.content, UserInfo.class);
				target_uid = userInfo.getUid();
				SendSingleMsgRequestBean bean = new SendSingleMsgRequestBean(target_uid, gson.toJson(msgContent));
				List<Integer> cookie = new ArrayList<Integer>();
				ImSendSingleMsgRequestProto req = new ImSendSingleMsgRequestProto(Command.JPUSH_IM.SENDMSG_SINGAL, 1, uid, appKey, sid,
						juid, rid, cookie, bean);
				channel.writeAndFlush(req);
			} else {
				log.warn(String.format("v1 sendImageMessage -- user: %s call sdk-api-getUserInfo exception: %s",userName, responseWrapper.content));
			}
		} else if ("group".equals(targetType)) {
			SendGroupMsgRequestBean bean = new SendGroupMsgRequestBean(Long.parseLong(targetId), gson.toJson(msgContent));
			List<Integer> cookie = new ArrayList<Integer>();
			ImSendGroupMsgRequestProto req = new ImSendGroupMsgRequestProto(Command.JPUSH_IM.SENDMSG_GROUP, 1, uid, appKey, sid, juid,
					rid, cookie, bean);
			channel.writeAndFlush(req);
		}
	}

	/**
	 * 消息送达反馈
	 * 
	 * @param client
	 * @param data
	 */
	public void respMessageReceived(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("v1 respMessageReceived -- request data -- data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(keyAndname)) {
			log.error(String.format("v1 respMessageReceived -- search data cache get appkey and username null exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.MESSAGE_RECEIVED);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn(String.format("v1 respMessageReceived -- search data cache resolve appkey and username null exception"));
				return;
			}
		}
		long rid = Long.parseLong(id);
		Jedis jedis = null;
		long uid = 0L;
		int sid = 0;
		long juid = 0L;
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName, "appKey", "sid", "juid", "uid");
			appKey = dataList.get(0);
			sid = Integer.parseInt(dataList.get(1));
			juid = Long.parseLong(dataList.get(2));
			uid = Long.parseLong(dataList.get(3));
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 respMessageReceived -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		long messageId = data.getParams().getMessageId();
		int iMsgType = data.getParams().getMsgType();
		long from_uid = data.getParams().getFromUid();
		long from_gid = data.getParams().getFromGid();
		log.info(String.format("v1 respMessageReceived -- user: %s receive msg feedback, msgId is %s, msgType is %s", userName, messageId, iMsgType));
		List<Integer> cookie = new ArrayList<Integer>();
		ChatMsg.Builder chatMsg = ChatMsg.newBuilder();
		chatMsg.setMsgid(messageId);
		chatMsg.setMsgType(iMsgType);
		chatMsg.setFromUid(from_uid);
		chatMsg.setFromGid(from_gid);
		ChatMsg chatMsgBean = chatMsg.build();
		ImChatMsgSyncRequestProto req = new ImChatMsgSyncRequestProto(Command.JPUSH_IM.SYNC_MSG, 1, uid, appKey, rid, sid, juid,
				cookie, chatMsgBean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":" + userName);
		if (channel!=null) {
			channel.writeAndFlush(req);
		} else {
			log.error(String.format("v1 respMessageReceived -- search data cache to get channel to im server null exception"));
		}
	}

	/**
	 * 事件送达反馈
	 * 
	 * @param client
	 * @param data
	 */
	public void respEventReceived(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		long rid = Long.parseLong(id);
		long eventId = data.getParams().getEventId();
		int eventType = data.getParams().getEventType();
		long from_uid = data.getParams().getFromUid();
		long gid = data.getParams().getGid();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("v1 respEventReceived -- request data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(keyAndname)) {
			log.error(String.format("v1 respEventReceived -- search data cache to get appkey and username null exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.EVENT_RECEIVED);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.error(String.format("v1 respEventReceived -- search data cache resolve appkey and username null exception"));
				return;
			}
		}
		Jedis jedis = null;
		int sid = 0;
		long juid = 0L;
		long uid = 0L;
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName, "appKey", "sid", "juid", "uid");
			appKey = dataList.get(0);
			sid = Integer.parseInt(dataList.get(1));
			juid = Long.parseLong(dataList.get(2));
			uid = Long.parseLong(dataList.get(3));
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 respEventReceived -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		log.info(String.format("v1 respEventReceived -- user: %s event received feedback, eventId is %s, eventType is %s", userName, eventId, eventType));
		List<Integer> cookie = new ArrayList<Integer>();
		EventNotification.Builder eventNotification = EventNotification.newBuilder();
		eventNotification.setEventId(eventId);
		eventNotification.setEventType(eventType);
		eventNotification.setFromUid(from_uid);
		eventNotification.setGid(gid);
		EventNotification eventNotificationBean = eventNotification.build();
		ImEventSyncRequestProto req = new ImEventSyncRequestProto(Command.JPUSH_IM.SYNC_EVENT, 1, uid, appKey, rid, sid, juid,
				cookie, eventNotificationBean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":" + userName);
		if (channel!=null) {
			channel.writeAndFlush(req);
		} else {
			log.error(String.format("v1 respEventReceived -- search data cache to get channel to im server null exception"));
		}
	}

	/**
	 * 创建群组
	 * 
	 * @param client
	 * @param data
	 */
	public void createGroup(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		String groupname = data.getParams().getGroupName();
		String group_description = data.getParams().getGroupDescription();
		String signature = data.getParams().getSignature();
		log.info(String.format("v1 createGroup -- request data -- data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(groupname) || StringUtils.isEmpty(group_description)
				||StringUtils.isEmpty(signature)) {
			log.error(String.format("v1 createGroup -- user pass null arguments exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUP_CREATE);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		if (StringUtils.isEmpty(keyAndname)) {
			log.error(String.format("v1 createGroup -- search data cache to get appkey and username null exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUP_CREATE);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.error(String.format("v1 createGroup -- search data cache resolve appkey and username null exception"));
				return;
			} else {
				// 签名有效性验证
				if(!V1.isSignatureValid(appKey, signature)){
					log.error(String.format("v1 createGroup -- user signature exception"));
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUP_CREATE);
					resp.setErrorInfo(JMessage.Error.SIGNATURE_INVALID, JMessage.Error.getErrorMessage(JMessage.Error.SIGNATURE_INVALID));
					client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
					return;
				}
			}
		}
		long rid = Long.parseLong(id);
		Jedis jedis = null;
		int sid = 0;
		long juid = 0L;
		long uid = 0L;
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName, "appKey", "sid", "juid", "uid");
			appKey = dataList.get(0);
			sid = Integer.parseInt(dataList.get(1));
			juid = Long.parseLong(dataList.get(2));
			uid = Long.parseLong(dataList.get(3));
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 createGroup -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		
		int group_level = 200;
		int flag = 0;
		CreateGroupRequestBean bean = new CreateGroupRequestBean(groupname, group_description, group_level, flag);
		List<Integer> cookie = new ArrayList<Integer>();
		ImCreateGroupRequestProto req = new ImCreateGroupRequestProto(Command.JPUSH_IM.CREATE_GROUP, 1, uid, appKey, rid, sid, juid,
				cookie, bean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":" + userName);
		if(channel!=null){
			channel.writeAndFlush(req);
		} else {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUP_CREATE);
			resp.setErrorInfo(JMessage.Error.CONNECTION_DISCONNECT, JMessage.Error.getErrorMessage(JMessage.Error.CONNECTION_DISCONNECT));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			log.error(String.format("v1 createGroup -- search data cache to get channel to im server null exception"));
		}
	}

	/**
	 * 获取群信息
	 * 
	 * @param client
	 * @param data
	 */
	public void getGroupInfo(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		long group_id = data.getParams().getGroupId();
		String signature = data.getParams().getSignature();
		log.info(String.format("v1 getGroupInfo -- request data -- data: %s", gson.toJson(data)));
		if (0 == group_id || StringUtils.isEmpty(signature)) {
			log.error(String.format("v1 getGroupInfo -- user pass null arguments exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPINFO_GET);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		if (StringUtils.isEmpty(keyAndname)) {
			log.error(String.format("v1 getGroupInfo -- search data cache to get appkey and username exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.USERINFO_GET);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.error(String.format("v1 getGroupInfo -- search data cache resolve appkey and username exception"));
				return;
			} else {
				// 签名有效性验证
				if(!V1.isSignatureValid(appKey, signature)){
					log.error("v1 getGroupInfo -- user signature exception");
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPINFO_GET);
					resp.setErrorInfo(JMessage.Error.SIGNATURE_INVALID, JMessage.Error.getErrorMessage(JMessage.Error.SIGNATURE_INVALID));
					client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
					return;
				}
			}
		}
		Jedis jedis = null;
		String token = "";
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName, "token");
			token = dataList.get(0);
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 getGroupInfo -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		log.info(String.format("v1 getGroupInfo -- begin call sdk-api-getGroupInfo ..."));
		HttpResponseWrapper responseWrapper = APIProxy.getGroupInfo(String.valueOf(group_id), token);
		if (responseWrapper.isOK()) {
			log.info("v1 getGroupInfo -- call sdk-api-getGroupInfo success");
			String groupInfoJson = responseWrapper.content;
			InnerGroupObject tmp_group = gson.fromJson(groupInfoJson, InnerGroupObject.class);
			SdkGroupDetailObject groupInfoObject = new SdkGroupDetailObject();
			groupInfoObject.setGid(tmp_group.getGid());
			groupInfoObject.setGroupName(tmp_group.getName());
			groupInfoObject.setGroupDesc(tmp_group.getDesc());
			log.info(String.format("v1 getGroupInfo -- begin call sdk-api-getGroupMemberList ..."));
			HttpResponseWrapper resultWrapper = APIProxy.getGroupMemberList(String.valueOf(group_id), token);
			if (resultWrapper.isOK()) {
				log.info(String.format("v1 getGroupInfo -- call sdk-api-getGroupMemberList success"));
				ArrayList<SdkUserInfoObject> userInfoList = new ArrayList<SdkUserInfoObject>();
				List<GroupMember> groupList = gson.fromJson(resultWrapper.content, new TypeToken<ArrayList<GroupMember>>(){}.getType());
				for (GroupMember member : groupList) {
					log.info(String.format("v1 getGroupInfo -- call sdk-api-getUserInfoByUid ..."));
					HttpResponseWrapper wrapper = APIProxy.getUserInfoByUid(appKey, String.valueOf(member.getUid()), token);
					SdkUserInfoObject userInfo = new SdkUserInfoObject();
					if (wrapper.isOK()) {
						log.info("v1 getGroupInfo -- call sdk-api-getUserInfoByUid success");
						HashMap map = gson.fromJson(wrapper.content, HashMap.class);
						String name = String.valueOf(map.get("username"));
						if (1 == member.getFlag()) {
							groupInfoObject.setOwnerUsername(name);
						}
						
						userInfo = gson.fromJson(wrapper.content, SdkUserInfoObject.class);
						/*Map infoData = gson.fromJson(wrapper.content, HashMap.class);
						String _username = infoData.containsKey("username")?(String)infoData.get("username"):"";
						String _nickname = infoData.containsKey("nickname")?(String)infoData.get("nickname"):"";
						int _star = infoData.containsKey("star")?(int)infoData.get("star"):0;
						String _avatar = infoData.containsKey("avatar")?(String)infoData.get("avatar"):"";
						int _gender = infoData.containsKey("gender")?(int)infoData.get("gender"):0;
						String _signature = infoData.containsKey("signature")?(String)infoData.get("signature"):"";
						String _region = infoData.containsKey("region")?(String)infoData.get("region"):"";
						String _address = infoData.containsKey("address")?(String)infoData.get("address"):"";
						String _mtime = infoData.containsKey("mtime")?(String)infoData.get("mtime"):"";
						String _ctime = infoData.containsKey("ctime")?(String)infoData.get("ctime"):"";
						userInfo.setUsername(_username);
						userInfo.setNickname(_nickname);
						userInfo.setStar(_star);
						userInfo.setAvatar(_avatar);
						userInfo.setGender(_gender);
						userInfo.setSignature(_signature);
						userInfo.setRegion(_region);
						userInfo.setAddress(_address);
						userInfo.setMtime(_mtime);
						userInfo.setCtime(_ctime);*/
					} else {
						log.error("v1 getGroupInfo -- call sdk-api-getUserInfoByUid exception: %s", wrapper.content);
					}
					userInfoList.add(userInfo);
				}
				groupInfoObject.setMembers(userInfoList);
			} else {
				log.error("v1 getGroupInfo -- call sdk-api-getGroupMemberList exception: %s", resultWrapper.content);
			}
			SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(V1.VERSION, id, JMessage.Method.GROUPINFO_GET,
					gson.toJson(groupInfoObject));
			log.info(String.format("v1 getGroupInfo -- response client data: ", gson.toJson(groupInfoObject)));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		} else {
			log.warn(String.format("v1 getGroupInfo -- call sdk-api-getGroupInfo exception: %s", responseWrapper.content));
			HttpErrorObject errorObject = gson.fromJson(responseWrapper.content, HttpErrorObject.class);
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPINFO_GET);
			resp.setErrorInfo(errorObject.getError().getCode(), errorObject.getError().getMessage());
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		}
	}

	/**
	 * 添加群组成员
	 * 
	 * @param client
	 * @param data
	 */
	public void addGroupMembers(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		long group_id = data.getParams().getGroupId();
		List<String> member_usernames = data.getParams().getMemberUsernames();
		String signature = data.getParams().getSignature();
		int memberCount = member_usernames.size();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("v1 addGroupMembers -- request data -- data: %s", gson.toJson(data)));
		if (0==group_id || 0==memberCount || StringUtils.isEmpty(signature)) {
			log.error(String.format("v1 addGroupMembers -- user pass null arguments exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPMEMBERS_ADD);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		}
		if (StringUtils.isEmpty(keyAndname)) {
			log.error("v1 addGroupMembers -- search data cache to get appkey and username null exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPMEMBERS_ADD);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.error("v1 addGroupMembers -- search data cache resolve appkey and username null exception");
				return;
			} else {
				// 签名有效性验证
				if (!V1.isSignatureValid(appKey, signature)) {
					log.error("v1 addGroupMembers -- user signature exception");
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPMEMBERS_ADD);
					resp.setErrorInfo(JMessage.Error.SIGNATURE_INVALID, JMessage.Error.getErrorMessage(JMessage.Error.SIGNATURE_INVALID));
					client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
					return;
				}
			}
		}
		Jedis jedis = null;
		int sid = 0;
		long uid = 0L;
		String token = "";
		long addUid = 0L;
		long juid = 0L;
		long rid = Long.parseLong(id);
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName, "appKey", "sid", "uid", "token", "juid");
			appKey = dataList.get(0);
			sid = Integer.parseInt(dataList.get(1));
			uid = Long.parseLong(dataList.get(2));
			token = dataList.get(3);
			juid = Long.parseLong(dataList.get(4));
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 addGroupMembers -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		List<Long> list = new ArrayList<Long>();
		for (String name : member_usernames) {
			log.info("v1 addGroupMembers -- begin call sdk-api-getUserInfo ...");
			HttpResponseWrapper resultWrapper = APIProxy.getUserInfo(appKey, name, token);
			if (resultWrapper.isOK()) {
				log.info("v1 addGroupMembers -- call sdk-api-getUserInfo success");
				User userInfo = gson.fromJson(resultWrapper.content, User.class);
				addUid = userInfo.getUid();
				list.add(addUid);
			} else {
				log.error(String.format("v1 addGroupMembers -- call sdk-api-getUserInfo exception: %s", resultWrapper.content));
			}
		}
		AddGroupMemberRequestBean bean = new AddGroupMemberRequestBean(group_id, memberCount, list);
		List<Integer> cookie = new ArrayList<Integer>();
		ImAddGroupMemberRequestProto req = new ImAddGroupMemberRequestProto(Command.JPUSH_IM.ADD_GROUP_MEMBER, 1, uid, appKey, rid, sid,
				juid, cookie, bean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":" + userName);
		if (channel!=null) {
			channel.writeAndFlush(req);
		} else {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPMEMBERS_ADD);
			resp.setErrorInfo(JMessage.Error.CONNECTION_DISCONNECT, JMessage.Error.getErrorMessage(JMessage.Error.CONNECTION_DISCONNECT));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			log.error(String.format("v1 addGroupMembers -- search data cache to get channel to im server null exception"));
		}
	}

	/**
	 * 移除群组成员
	 * 
	 * @param client
	 * @param data
	 */
	public void removeGroupMembers(SocketIOClient client,
			SdkRequestObject data) {
		String id = data.getId();
		long group_id = data.getParams().getGroupId();
		List<String> member_usernames = data.getParams().getMemberUsernames();
		String signature = data.getParams().getSignature();
		int memberCount = member_usernames.size();
		long rid = Long.parseLong(id);
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("v1 removeGroupMembers -- request data -- data: %s", gson.toJson(data)));
		if (0==group_id || 0==memberCount || StringUtils.isEmpty(signature)) {
			log.error("v1 removeGroupMembers -- user pass null arguments exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPMEMBERS_REMOVE);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		if (StringUtils.isEmpty(keyAndname)) {
			log.error("v1 removeGroupMembers -- search data cache to get appkey and username null exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPMEMBERS_REMOVE);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.error("v1 removeGroupMembers -- search data cache resolve appkey and username null exception");
				return;
			} else {
				// 签名有效性验证
				if (!V1.isSignatureValid(appKey, signature)) {
					log.error("v1 removeGroupMembers --   user signature exception");
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPMEMBERS_REMOVE);
					resp.setErrorInfo(JMessage.Error.SIGNATURE_INVALID, JMessage.Error.getErrorMessage(JMessage.Error.SIGNATURE_INVALID));
					client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
					return;
				}
			}
		}
		Jedis jedis = null;
		int sid = 0;
		long uid = 0L;
		String token = "";
		long addUid = 0L;
		long juid = 0L;
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName, "appKey", "sid", "uid", "token", "juid");
			appKey = dataList.get(0);
			sid = Integer.parseInt(dataList.get(1));
			uid = Long.parseLong(dataList.get(2));
			token = dataList.get(3);
			juid = Long.parseLong(dataList.get(4));
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 removeGroupMembers -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		List<Long> list = new ArrayList<Long>();
		for (String name : member_usernames) {
			log.info(String.format("v1 removeGroupMembers -- begin call sdk-api-getUserInfo ..."));
			HttpResponseWrapper resultWrapper = APIProxy.getUserInfo(appKey, name, token);
			if (resultWrapper.isOK()) {
				log.info(String.format("v1 removeGroupMembers -- call sdk-api-getUserInfo success"));
				User userInfo = gson.fromJson(resultWrapper.content, User.class);
				addUid = userInfo.getUid();
				list.add(addUid);
			} else {
				log.warn(String.format("v1 removeGroupMembers -- call sdk-api-getUserInfo exception: %s", resultWrapper.content));
			}
		}
		DeleteGroupMemberRequestBean bean = new DeleteGroupMemberRequestBean(group_id, memberCount, list);
		List<Integer> cookie = new ArrayList<Integer>();
		ImDeleteGroupMemberRequestProto req = new ImDeleteGroupMemberRequestProto(Command.JPUSH_IM.DEL_GROUP_MEMBER, 1, uid, appKey, rid, sid,
				juid, cookie, bean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":" + userName);
		if (channel!=null) {
			channel.writeAndFlush(req);
		} else {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPMEMBERS_REMOVE);
			resp.setErrorInfo(JMessage.Error.CONNECTION_DISCONNECT, JMessage.Error.getErrorMessage(JMessage.Error.CONNECTION_DISCONNECT));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			log.error(String.format("v1 removeGroupMembers -- search data cache to get channel to im server null exception"));
		}
	}

	/**
	 * 退出群组
	 * 
	 * @param client
	 * @param data
	 */
	public void exitGroup(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		long group_id = data.getParams().getGroupId();
		String signature = data.getParams().getSignature();
		long rid = Long.parseLong(id);
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("v1 exitGroup -- request data -- data: %s", gson.toJson(data)));
		if (0==group_id || StringUtils.isEmpty(signature)) {
			log.error("v1 exitGroup -- user pass null arguments exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUP_EXIT);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		if (StringUtils.isEmpty(keyAndname)) {
			log.error("v1 exitGroup -- serach data cache to get appkey and username null exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUP_EXIT);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.error("v1 exitGroup -- serach data cache resolve appkey and username null exception");
				return;
			} else {
				// 签名有效性验证
				if(!V1.isSignatureValid(appKey, signature)){
					log.error("v1 exitGroup -- user signature exception");
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUP_EXIT);
					resp.setErrorInfo(JMessage.Error.SIGNATURE_INVALID, JMessage.Error.getErrorMessage(JMessage.Error.SIGNATURE_INVALID));
					client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
					return;
				}
			}
		}
		Jedis jedis = null;
		int sid = 0;
		long juid = 0L;
		long uid = 0L;
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName,
					"appKey", "sid", "juid", "uid");
			appKey = dataList.get(0);
			sid = Integer.parseInt(dataList.get(1));
			juid = Long.parseLong(dataList.get(2));
			uid = Long.parseLong(dataList.get(3));
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 exitGroup -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		ExitGroupRequestBean bean = new ExitGroupRequestBean(group_id);
		List<Integer> cookie = new ArrayList<Integer>();
		ImExitGroupRequestProto req = new ImExitGroupRequestProto(Command.JPUSH_IM.EXIT_GROUP, 1, uid, appKey, rid, sid, juid,
				cookie, bean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":" + userName);
		if (channel!=null) {
			channel.writeAndFlush(req);
		} else {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUP_EXIT);
			resp.setErrorInfo(JMessage.Error.CONNECTION_DISCONNECT, JMessage.Error.getErrorMessage(JMessage.Error.CONNECTION_DISCONNECT));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			log.error(String.format("v1 exitGroup -- search data cache to get channel to im server null exception"));
		}
	}

	/**
	 * 获取群组列表
	 * 
	 * @param client
	 * @param data
	 */
	public void getGroupList(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		String signature = data.getParams().getSignature();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("v1 getGroupList -- request data -- data: %s", data));
		if(StringUtils.isEmpty(signature)){
			log.error("v1 getGroupList -- user pass null arguments exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPLIST_GET);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		if (StringUtils.isEmpty(keyAndname)) {
			log.error("v1 getGroupList -- search data cache to get appkey and username null exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPLIST_GET);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("v1 getGroupList -- search data cache resolve appkey and username null exception");
				return;
			} else {
				// 签名有效性验证
				if(!V1.isSignatureValid(appKey, signature)){
					log.error("v1 getGroupList -- user signature exception");
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPLIST_GET);
					resp.setErrorInfo(JMessage.Error.SIGNATURE_INVALID, JMessage.Error.getErrorMessage(JMessage.Error.SIGNATURE_INVALID));
					client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
					return;
				}
			}
		}
		Jedis jedis = null;
		String token = "";
		long uid = 0L;
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName, "token", "uid");
			token = dataList.get(0);
			uid = Long.parseLong(dataList.get(1));
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 getGroupList -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		List<SdkGroupInfoObject> groupsList = new ArrayList<SdkGroupInfoObject>();
		log.info(String.format("v1 getGroupList -- begin call sdk-api-getGroupList ..."));
		HttpResponseWrapper result = APIProxy.getGroupList(String.valueOf(uid), token);
		if (result.isOK()) {
			log.info("v1 getGroupList -- call sdk-api-getGroupList success");
			String groupListJson = result.content;
			List<Long> gidList = gson.fromJson(groupListJson, new TypeToken<ArrayList<Long>>(){}.getType());
			for (Long gid : gidList) {
				log.info("v1 getGroupList -- begin call sdk-api-getGroupInfo ...");
				HttpResponseWrapper groupInfoResult = APIProxy.getGroupInfo(String.valueOf(gid), token);
				ArrayList<String> members_name = new ArrayList<String>();
				if (groupInfoResult.isOK()) {
					log.info("v1 getGroupList -- call sdk-api-getGroupInfo success");
					String groupInfoJson = groupInfoResult.content;
					InnerGroupObject tmp_group = gson.fromJson(groupInfoJson, InnerGroupObject.class);
					SdkGroupInfoObject groupInfoObject = new SdkGroupInfoObject();
					groupInfoObject.setGid(tmp_group.getGid());
					groupInfoObject.setGroupName(tmp_group.getName());
					groupInfoObject.setGroupDesc(tmp_group.getDesc());
					log.info("v1 getGroupList -- begin call sdk-api-getGroupMemberList ...");
					HttpResponseWrapper resultWrapper = APIProxy.getGroupMemberList(String.valueOf(gid), token);
					if (resultWrapper.isOK()) {
						log.info("v1 getGroupList -- call sdk-api-getGroupMemberList success");
						List<GroupMember> groupList = gson.fromJson(resultWrapper.content, new TypeToken<ArrayList<GroupMember>>(){}.getType());
						for (GroupMember member : groupList) {
							log.info("v1 getGroupList -- begin call sdk-api-getUserInfoByUid ...");
							HttpResponseWrapper wrapper = APIProxy.getUserInfoByUid(appKey, String.valueOf(member.getUid()), token);
							if (wrapper.isOK()) {
								log.info("v1 getGroupList -- call sdk-api-getUserInfoByUid success");
								HashMap map = gson.fromJson(wrapper.content, HashMap.class);
								String name = String.valueOf(map.get("username"));
								members_name.add(name);
								if (1 == member.getFlag()) {
									groupInfoObject.setOwnerUsername(name);
								}
							} else {
								log.error(String.format("v1 getGroupList -- call sdk-api getUserInfoByUid exception: %s", wrapper.content));
							}
						}
						groupInfoObject.setMembersUsername(members_name);
					} else {
						log.error(String.format("v1 getGroupList -- call sdk-api-getGroupMemberList exception: %s", resultWrapper.content));
					}
					groupsList.add(groupInfoObject);
				} else {
					log.error(String.format("v1 getGroupList -- call sdk-api-getGroupInfo exception: %s", groupInfoResult.content));
				}
			}
			SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(V1.VERSION, id, JMessage.Method.GROUPLIST_GET, gson.toJson(groupsList));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		} else {
			log.error(String.format("v1 getGroupList -- call sdk-api-getGroupList exception: %s", result.content));
			HttpErrorObject errorObject = gson.fromJson(result.content, HttpErrorObject.class);
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPLIST_GET);
			resp.setErrorInfo(errorObject.getError().getCode(), errorObject.getError().getMessage());
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		}
	}

	/**
	 * 更新群组信息
	 * 
	 * @param client
	 * @param data
	 */
	public void updateGroupInfo(SocketIOClient client,
			SdkRequestObject data) {
		String id = data.getId();
		long gid = data.getParams().getGroupId();
		String group_name = data.getParams().getGroupName();
		String group_desc = data.getParams().getGroupDescription();
		String signature = data.getParams().getSignature();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("v1 updateGroupInfo -- request data -- data: %s", gson.toJson(data)));
		if (0==gid || StringUtils.isEmpty(signature) || (group_name==null&&group_desc==null)) {
			log.error("v1 updateGroupInfo -- user pass null arguments exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPINFO_UPDATE);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		if (StringUtils.isEmpty(keyAndname)) {
			log.error("v1 updateGroupInfo -- search data cache to get appkey and username null exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPINFO_UPDATE);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.error("v1 updateGroupInfo -- search data cache resolve appkey and username null exception");
				return;
			} else {
				// 签名有效性验证
				if(!V1.isSignatureValid(appKey, signature)){
					log.error("v1 updateGroupInfo -- user signature exception");
					SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION, id, JMessage.Method.GROUPINFO_UPDATE);
					resp.setErrorInfo(JMessage.Error.SIGNATURE_INVALID, JMessage.Error.getErrorMessage(JMessage.Error.SIGNATURE_INVALID));
					client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
					return;
				}
			}
		}
		Jedis jedis = null;
		long rid = Long.parseLong(id);
		long uid = 0L;
		int sid = 0;
		long juid = 0L;
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName, "appKey", "uid", "sid", "juid");
			appKey = dataList.get(0);
			uid = Long.parseLong(dataList.get(1));
			sid = Integer.parseInt(dataList.get(2));
			juid = Long.parseLong(dataList.get(3));
		} catch (JedisConnectionException e) {
			log.error(String.format("v1 updateGroupInfo -- redis exception: %s", e.getMessage()));
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		log.info(String.format("v1 updateGroupInfo -- user: %s update group, new group name is %s", userName, group_name));
		UpdateGroupInfoRequestBean bean = new UpdateGroupInfoRequestBean(gid, group_name, group_desc);
		List<Integer> cookie = new ArrayList<Integer>();
		ImUpdateGroupInfoRequestProto req = new ImUpdateGroupInfoRequestProto(Command.JPUSH_IM.UPDATE_GROUP_INFO, 1, uid, appKey, rid, sid,
				juid, cookie, bean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":" + userName);
		if (channel!=null) {
			channel.writeAndFlush(req);
		} else {
			log.error(String.format("v1 updateGroupInfo -- search data cache to get channel to im server null exception"));
		}
	}

	public static String getMediaId(long uid) {
		Date d = new Date();
		long time = d.getTime();
		long random = (Math.max(Math.min(Math.round(Math.random() * (100 - 0)), 100), 0));
		String mediaId = "qiniu/image/" + StringUtils.toMD5(String.valueOf(uid) + time + random);
		return mediaId;
	}

	public static String uploadFile(String mediaId, String filePath)
			throws AuthException, JSONException {
		Mac mac = new Mac(Configure.QNCloudInterface.QN_ACCESS_KEY, Configure.QNCloudInterface.QN_SECRET_KEY);
		PutPolicy putPolicy = new PutPolicy(Configure.QNCloudInterface.QN_BUCKETNAME);
		putPolicy.expires = 14400;
		String token = putPolicy.token(mac);
		PutExtra extra = new PutExtra();
		String key = mediaId;
		PutRet ret = IoApi.putFile(token, key, filePath, extra);
		return ret.response;
	}

	public static Map getFileMetaInfo(String mediaId, String filePath)
			throws IOException {
		File file = new File(filePath);
		FileInputStream inStream = new FileInputStream(file);
		BufferedImage src = ImageIO.read(inStream);
		int width = src.getWidth();
		int height = src.getHeight();
		CheckedInputStream cis = new CheckedInputStream(inStream, new CRC32());
		byte[] buf = new byte[128];
		while (cis.read(buf) >= 0) {
		}
		long checksum = cis.getChecksum().getValue();
		inStream.close();
		file.delete();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("width", width);
		map.put("height", height);
		map.put("mediaId", mediaId);
		map.put("crc32", checksum);
		return map;
	}
	
	public static boolean isSignatureValid(String appKey, String signature){
		log.info("confirm user signature");
		Jedis jedis = null;
		boolean result = false;
		try {
			jedis = redisClient.getJeids();
			if(jedis.exists(appKey+signature)){
				result = true;
			}
		} catch (JedisConnectionException e) {
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		return result;
	}
	
	// http://7xjfat.dl1.z0.glb.clouddn.com
	public static void main(String argus[]) throws AuthException, JSONException{
		//String filePath = "/home/chujieyang/eclipse_projects/js-im-sdk/demo/js/jmessage-1.0.0.js";
		//String filePath = "/home/chujieyang/eclipse_projects/js-im-sdk/demo/js/jquery.min.js";
		//String filePath = "/home/chujieyang/eclipse_projects/js-im-sdk/demo/js/socket.io.min.js";
		String filePath = "/home/chujieyang/eclipse_projects/js-im-sdk/demo/js/min/jmessage-1.0.0.min.js";
		//String filePath = "/home/chujieyang/eclipse_projects/js-im-sdk/demo/js/min/ajaxfileupload.min.js";
		Mac mac = new Mac(Configure.QNCloudInterface.QN_ACCESS_KEY, Configure.QNCloudInterface.QN_SECRET_KEY);
		PutPolicy putPolicy = new PutPolicy("jmsgweb");
		//putPolicy.expires = 14400;
		String token = putPolicy.token(mac);
		PutExtra extra = new PutExtra();
		String key = "jmessage-1.0.0.min.js"; 
		PutRet ret = IoApi.putFile(token, key, filePath, extra);
		log.info("result: "+ret.response);
		
		// delete 
		/*RSClient client = new RSClient(mac);
		CallRet ret = client.delete("jmsgweb", "jmessage-1.0.0.min.js");
		System.out.println(ret.getResponse());*/
	}

}
