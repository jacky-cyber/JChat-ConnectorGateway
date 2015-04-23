package cn.jpush.webim.server;

import io.netty.channel.Channel;

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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qiniu.api.auth.AuthException;
import com.qiniu.api.auth.digest.Mac;
import com.qiniu.api.io.IoApi;
import com.qiniu.api.io.PutExtra;
import com.qiniu.api.io.PutRet;
import com.qiniu.api.rs.PutPolicy;

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
import cn.jpush.socketio.Configuration;
import cn.jpush.socketio.SocketIOClient;
import cn.jpush.socketio.SocketIOServer;
import cn.jpush.webim.common.RedisClient;
import cn.jpush.webim.common.UidResourcesPool;
import cn.jpush.webim.socketio.bean.GroupMember;
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
 * gateway V1 版本
 * 
 * @author chujieyang
 *
 */
public class V1 {
	private static Logger log = (Logger) LoggerFactory.getLogger(V1.class);
	private static final String VERSION = "1.0";
	private static final String DATA_AISLE = "data";
	private static final String FILE_STORE_PATH = SystemConfig.getProperty("im.file.store.path");
	private static Gson gson = new Gson();
	private static RedisClient redisClient = new RedisClient();
	public static CountDownLatch pushLoginInCountDown;
	private static JPushTcpClient jpushIMTcpClient;

	/**
	 * SDK 配置校验
	 * 
	 * @param client
	 * @param data
	 */
	public static void config(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		String appKey = data.getParams().getAppKey();
		String timestamp = data.getParams().getTimestamp();
		String randomStr = data.getParams().getRandomStr();
		String signature = data.getParams().getSignature();
		log.info(String.format("config request data -- data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(signature) || StringUtils.isEmpty(randomStr)
				|| StringUtils.isEmpty(timestamp)
				|| StringUtils.isEmpty(appKey)) {
			log.error(String.format("config -- user pass arguments exception"));
			return;
		} else {
			// TODO 加入验证过程
			StringBuffer buffer = new StringBuffer();
			// String token --> appKey
			String sign = buffer.append(timestamp).append(randomStr).toString();
			SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
					V1.VERSION, id, JMessage.Method.CONFIG, "");
			log.info(String.format("config resp data -- ", gson.toJson(resp)));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		}
	}

	/**
	 * 用户登陆
	 * 
	 * @param client
	 * @param data
	 * @throws InterruptedException
	 */
	public static void login(SocketIOClient client, SdkRequestObject data)
			throws InterruptedException {
		String id = data.getId();
		String appkey = data.getParams().getAppKey();
		String username = data.getParams().getUsername();
		String password = data.getParams().getPassword();
		log.info(String.format("login request data -- data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(appkey) || StringUtils.isEmpty(username)
				|| StringUtils.isEmpty(password)) {
			log.error("login -- use pass arguments exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.LOGIN);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		Map<String, String> juidData = UidResourcesPool.getUidAndPassword();
		if(juidData==null){
			log.error(String.format("login -- UidResourcePool getUidAndPassword exception"));
			return;
		}
		long juid = Long.parseLong(String.valueOf(juidData.get("uid")));
		String juid_password = String.valueOf(juidData.get("password"));
		pushLoginInCountDown = new CountDownLatch(1);

		jpushIMTcpClient = new JPushTcpClient(appkey);
		Channel pushChannel = jpushIMTcpClient.getChannel();

		// 关系绑定
		WebImServer.userNameToSessionCilentMap.put(appkey + ":" + username,
				client);
		WebImServer.sessionClientToUserNameMap.put(client, appkey + ":"
				+ username);
		WebImServer.userNameToPushChannelMap.put(appkey + ":" + username,
				pushChannel);
		WebImServer.pushChannelToUsernameMap.put(pushChannel, appkey + ":"
				+ username);

		PushLoginRequestBean pushLoginBean = new PushLoginRequestBean(juid,
				"a", ProtocolUtil.md5Encrypt(juid_password), 10800, appkey, 0);
		pushChannel.writeAndFlush(pushLoginBean);
		log.info(String.format(
				"user: %s begin jpush login, juid: %d, password: %s", username,
				juid, juid_password));

		pushLoginInCountDown.await(); // 等待push login返回数据
		PushLoginResponseBean pushLoginResponseBean = jpushIMTcpClient
				.getjPushClientHandler().getPushLoginResponseBean();
		int sid = pushLoginResponseBean.getSid();
		log.info(String.format(
				"user: %s jpush login response, code: %d, sid: %d", username,
				pushLoginResponseBean.getResponse_code(), sid));

		// 存储用户信息
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
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}

		// 设置数据 用于心跳
		jpushIMTcpClient.getjPushClientHandler().setSid(pushLoginResponseBean.getSid());
		jpushIMTcpClient.getjPushClientHandler().setJuid(juid);

		// IM Login
		long rid = Long.parseLong(id);
		LoginRequestBean bean = new LoginRequestBean(username,
				StringUtils.toMD5(password));
		List<Integer> cookie = new ArrayList<Integer>();
		ImLoginRequestProto req = new ImLoginRequestProto(
				Command.JPUSH_IM.LOGIN, 1, 0, pushLoginResponseBean.getSid(),
				juid, appkey, rid, cookie, bean);
		pushChannel.writeAndFlush(req);
	}

	/**
	 * 用户登出
	 * 
	 * @param client
	 * @param data
	 */
	public static void logout(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("logout request data -- data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(keyAndname)) {
			log.error("get keyAndname exception, maybe you have not login");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.LOGOUT);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.error("logout -- through keyAndname resovle username exception");
				return;
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
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":"
				+ userName);
		if (channel == null) {
			log.error("get channel to push exception, so cannot send request");
			SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
					V1.VERSION, id, JMessage.Method.LOGOUT, "");
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		} else {
			LogoutRequestBean bean = new LogoutRequestBean(userName);
			List<Integer> cookie = new ArrayList<Integer>();
			ImLogoutRequestProto req = new ImLogoutRequestProto(
					Command.JPUSH_IM.LOGOUT, 1, uid, appKey, sid, juid, cookie,
					bean);
			channel.writeAndFlush(req);
		}
	}
	
	/**
	 * 客户端心跳
	 * @param client
	 * @param data
	 */
	public static void heartbeat(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		log.info("client heartbeat request -- rid: "+id);
		SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
				V1.VERSION, id, JMessage.Method.HEARTBEAT, "");
		client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		log.info("client heartbeat response -- rid: "+id);
	}
	
	/**
	 * 获取用户信息
	 * 
	 * @param client
	 * @param data
	 */
	public static void getUserInfo(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		String username = data.getParams().getUsername();
		log.info(String.format("getUserInfo request data -- data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(username)) {
			log.error("getUserInfo -- pass empty arguments exception");
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.USERINFO_GET);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		if (StringUtils.isEmpty(keyAndname)) {
			log.error(String.format("get keyAndname exception, maybe you have not login"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.USERINFO_GET);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("through keyAndname resovle username exception");
				return;
			}
		}
		Jedis jedis = null;
		String token = "";
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName,
					"appKey", "token");
			appKey = dataList.get(0);
			token = dataList.get(1);
		} catch (JedisConnectionException e) {
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		HttpResponseWrapper responseWrapper = APIProxy.getUserInfo(appKey,
				username, token);
		if (responseWrapper.isOK()) {
			SdkUserInfoObject userInfo = new SdkUserInfoObject();
			Map infoData = gson.fromJson(responseWrapper.content, HashMap.class);
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
			userInfo.setCtime(_ctime);
			SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
					V1.VERSION, id, JMessage.Method.USERINFO_GET,
					gson.toJson(userInfo));
			log.info("getUserInfo resp data -- " + gson.toJson(userInfo));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		} else {
			log.warn(String.format("getUserInfo call sdk-api exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.USERINFO_GET);
			resp.setErrorInfo(1000, "call sdk-api exception");
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		}
	}

	/**
	 * 发送文本消息
	 * 
	 * @param client
	 * @param data
	 */
	public static void sendTextMessage(SocketIOClient client,
			SdkRequestObject data) {
		String id = data.getId();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("sendTextMessage request data -- data: %s", gson.toJson(data)));
		String targetId = data.getParams().getTargetId();
		String targetType = data.getParams().getTargetType();
		String text = data.getParams().getText();
		if(StringUtils.isEmpty(targetId)||StringUtils.isEmpty(targetType)
				||StringUtils.isEmpty(text)){
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.MESSAGE_FEEDBACK);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		if (StringUtils.isEmpty(keyAndname)) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.MESSAGE_FEEDBACK);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("through keyAndname resovle username exception");
				return;
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
			log.error(e.getMessage());
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
		msgContent.setCreate_time(StringUtils.getCreateTime());
		msgContent.setMsg_type("text");
		TextMsgBody msgBody = new TextMsgBody();
		msgBody.setText(text);
		msgContent.setMsg_body(msgBody);

		log.info(String.format("user: %s send chat msg, content is: ",
				userName, gson.toJson(data)));
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":"
				+ userName);
		if (channel == null) {
			log.warn("current user get channel to push server exception");
			return;
		}
		if ("single".equals(targetType)) {
			HttpResponseWrapper responseWrapper = APIProxy.getUserInfo(appKey,
					targetId, token);
			long target_uid = 0L;
			if (responseWrapper.isOK()) {
				UserInfo userInfo = gson.fromJson(responseWrapper.content,
						UserInfo.class);
				target_uid = userInfo.getUid();
				SendSingleMsgRequestBean bean = new SendSingleMsgRequestBean(
						target_uid, gson.toJson(msgContent)); // 为了和移动端保持一致，注意这里用target_name来存储id，避免再查一次
				List<Integer> cookie = new ArrayList<Integer>();
				ImSendSingleMsgRequestProto req = new ImSendSingleMsgRequestProto(
						Command.JPUSH_IM.SENDMSG_SINGAL, 1, uid, appKey, sid,
						juid, rid, cookie, bean);
				channel.writeAndFlush(req);
			} else {
				log.warn(String
						.format("user: %s sendTextMessage call sdk-api getUserInfo exception",
								userName));
			}
		} else if ("group".equals(targetType)) {
			SendGroupMsgRequestBean bean = new SendGroupMsgRequestBean(
					Long.parseLong(targetId),
					gson.toJson(msgContent));
			List<Integer> cookie = new ArrayList<Integer>();
			ImSendGroupMsgRequestProto req = new ImSendGroupMsgRequestProto(
					Command.JPUSH_IM.SENDMSG_GROUP, 1, uid, appKey, sid, juid,
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
	public static void sendImageMessage(SocketIOClient client,
			SdkRequestObject data) {
		String id = data.getId();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("sendImageMessage request data -- data: %s", gson.toJson(data)));
		String targetId = data.getParams().getTargetId();
		String targetType = data.getParams().getTargetType();
		String fileId = data.getParams().getFileId();
		if(StringUtils.isEmpty(targetId)||StringUtils.isEmpty(targetType)
				||StringUtils.isEmpty(fileId)){
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.MESSAGE_FEEDBACK);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		if (StringUtils.isEmpty(keyAndname)) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.MESSAGE_FEEDBACK);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("through keyAndname resovle username exception");
				return;
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
			log.error(e.getMessage());
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
		msgContent.setCreate_time(StringUtils.getCreateTime());
		msgContent.setMsg_type("image");
		ImageMsgBody msgBody = new ImageMsgBody();
		String filePath = FILE_STORE_PATH + fileId;
		String mediaId = V1.getMediaId(uid);
		Map fileInfoMap = null;
		try {
			String response = V1.uploadFile(mediaId, filePath);
			log.info("upload response: " + response);
			fileInfoMap = V1.getFileMetaInfo(mediaId, filePath);
		} catch (AuthException | JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		msgBody.setMedia_id(mediaId);
		msgBody.setMedia_crc32(Long.parseLong(String.valueOf(fileInfoMap
				.get("crc32"))));
		msgBody.setWidth(Integer.parseInt(String.valueOf(fileInfoMap
				.get("width"))));
		msgBody.setHeight(Integer.parseInt(String.valueOf(fileInfoMap
				.get("height"))));
		msgContent.setMsg_body(msgBody);

		log.info(String.format("user: %s send chat msg, content is: ",
				userName, gson.toJson(data)));
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":"
				+ userName);
		if (channel == null) {
			log.warn("current user get channel to push server exception");
			return;
		}
		if ("single".equals(targetType)) {
			HttpResponseWrapper responseWrapper = APIProxy.getUserInfo(appKey,
					targetId, token);
			long target_uid = 0L;
			if (responseWrapper.isOK()) {
				UserInfo userInfo = gson.fromJson(responseWrapper.content,
						UserInfo.class);
				target_uid = userInfo.getUid();
				SendSingleMsgRequestBean bean = new SendSingleMsgRequestBean(
						target_uid, gson.toJson(msgContent)); // 为了和移动端保持一致，注意这里用target_name来存储id，避免再查一次
				List<Integer> cookie = new ArrayList<Integer>();
				ImSendSingleMsgRequestProto req = new ImSendSingleMsgRequestProto(
						Command.JPUSH_IM.SENDMSG_SINGAL, 1, uid, appKey, sid,
						juid, rid, cookie, bean);
				channel.writeAndFlush(req);
				log.info(String.format("user: %s begin send single chat msg",
						userName));
			} else {
				log.warn(String
						.format("user: %s sendTextMessage call sdk-api getUserInfo exception",
								userName));
			}
		} else if ("group".equals(targetType)) {
			SendGroupMsgRequestBean bean = new SendGroupMsgRequestBean(
					Long.parseLong(targetId),
					gson.toJson(msgContent));
			List<Integer> cookie = new ArrayList<Integer>();
			ImSendGroupMsgRequestProto req = new ImSendGroupMsgRequestProto(
					Command.JPUSH_IM.SENDMSG_GROUP, 1, uid, appKey, sid, juid,
					rid, cookie, bean);
			channel.writeAndFlush(req);
			log.info(String.format("user: %s begin send group chat msg",
					userName));
		}
	}

	/**
	 * 消息送达反馈
	 * 
	 * @param client
	 * @param data
	 */
	public static void respMessageReceived(SocketIOClient client,
			SdkRequestObject data) {
		String id = data.getId();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("respMessageReceived request data -- data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(keyAndname)) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.MESSAGE_FEEDBACK);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("through keyAndname resovle username exception");
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
			List<String> dataList = jedis.hmget(appKey + ":" + userName,
					"appKey", "sid", "juid", "uid");
			appKey = dataList.get(0);
			sid = Integer.parseInt(dataList.get(1));
			juid = Long.parseLong(dataList.get(2));
			uid = Long.parseLong(dataList.get(3));
		} catch (JedisConnectionException e) {
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		long messageId = data.getParams().getMessageId();
		int iMsgType = data.getParams().getMsgType();
		long from_uid = data.getParams().getFromUid();
		long from_gid = data.getParams().getFromGid();
		log.info(String.format(
				"user: %d sync msg feedback, msgId is %d, msgType is %d", uid,
				messageId, iMsgType));
		List<Integer> cookie = new ArrayList<Integer>();
		ChatMsg.Builder chatMsg = ChatMsg.newBuilder();
		chatMsg.setMsgid(messageId);
		chatMsg.setMsgType(iMsgType);
		chatMsg.setFromUid(from_uid);
		chatMsg.setFromGid(from_gid);
		ChatMsg chatMsgBean = chatMsg.build();
		ImChatMsgSyncRequestProto req = new ImChatMsgSyncRequestProto(
				Command.JPUSH_IM.SYNC_MSG, 1, uid, appKey, rid, sid, juid,
				cookie, chatMsgBean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":"
				+ userName);
		channel.writeAndFlush(req);
		log.info(String.format("user: %d send sync msg feedback request", uid));
	}

	/**
	 * 事件送达反馈
	 * 
	 * @param client
	 * @param data
	 */
	public static void respEventReceived(SocketIOClient client,
			SdkRequestObject data) {
		String id = data.getId();
		long rid = Long.parseLong(id);
		long eventId = data.getParams().getEventId();
		int eventType = data.getParams().getEventType();
		long from_uid = data.getParams().getFromUid();
		long gid = data.getParams().getGid();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("respEventReceived request data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(keyAndname)) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.EVENT_FEEDBACK);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("through keyAndname resovle username exception");
				return;
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
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		log.info(String.format(
				"user: %d sync event feedback, eventId is %d, eventType is %d",
				uid, eventId, eventType));
		List<Integer> cookie = new ArrayList<Integer>();
		EventNotification.Builder eventNotification = EventNotification
				.newBuilder();
		eventNotification.setEventId(eventId);
		eventNotification.setEventType(eventType);
		eventNotification.setFromUid(from_uid);
		eventNotification.setGid(gid);
		EventNotification eventNotificationBean = eventNotification.build();
		ImEventSyncRequestProto req = new ImEventSyncRequestProto(
				Command.JPUSH_IM.SYNC_EVENT, 1, uid, appKey, rid, sid, juid,
				cookie, eventNotificationBean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":"
				+ userName);
		channel.writeAndFlush(req);
		log.info(String
				.format("user: %d send sync event feedback request", uid));
	}

	/**
	 * 创建群组
	 * 
	 * @param client
	 * @param data
	 */
	public static void createGroup(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		String groupname = data.getParams().getGroupName();
		String group_description = data.getParams().getGroupDescription();
		log.info(String.format("createGroup request data -- data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(groupname)
				|| StringUtils.isEmpty(group_description)) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUP_CREATE);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		if (StringUtils.isEmpty(keyAndname)) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUP_CREATE);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("through keyAndname resovle username exception");
				return;
			}
		}
		long rid = Long.parseLong(id);
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
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		int group_level = 200;
		int flag = 0;
		CreateGroupRequestBean bean = new CreateGroupRequestBean(groupname,
				group_description, group_level, flag);
		List<Integer> cookie = new ArrayList<Integer>();
		ImCreateGroupRequestProto req = new ImCreateGroupRequestProto(
				Command.JPUSH_IM.CREATE_GROUP, 1, uid, appKey, rid, sid, juid,
				cookie, bean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":"
				+ userName);
		channel.writeAndFlush(req);
	}

	/**
	 * 获取群信息
	 * 
	 * @param client
	 * @param data
	 */
	public static void getGroupInfo(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		long group_id = data.getParams().getGroupId();
		log.info(String.format("getgGroupInfo request data -- data: %s", gson.toJson(data)));
		if (0 == group_id) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUPINFO_GET);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		if (StringUtils.isEmpty(keyAndname)) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.USERINFO_GET);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("resovle username exception");
				return;
			}
		}
		Jedis jedis = null;
		String token = "";
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName,
					"token");
			token = dataList.get(0);
		} catch (JedisConnectionException e) {
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		HttpResponseWrapper responseWrapper = APIProxy.getGroupInfo(
				String.valueOf(group_id), token);
		if (responseWrapper.isOK()) {
			String groupInfoJson = responseWrapper.content;
			InnerGroupObject tmp_group = gson.fromJson(groupInfoJson,
					InnerGroupObject.class);
			SdkGroupDetailObject groupInfoObject = new SdkGroupDetailObject();
			groupInfoObject.setGid(tmp_group.getGid());
			groupInfoObject.setGroupName(tmp_group.getName());
			groupInfoObject.setGroupDesc(tmp_group.getDesc());
			HttpResponseWrapper resultWrapper = APIProxy.getGroupMemberList(
					String.valueOf(group_id), token);
			if (resultWrapper.isOK()) {
				ArrayList<SdkUserInfoObject> userInfoList = new ArrayList<SdkUserInfoObject>();
				List<GroupMember> groupList = gson.fromJson(
						resultWrapper.content,
						new TypeToken<ArrayList<GroupMember>>() {
						}.getType());
				for (GroupMember member : groupList) {
					HttpResponseWrapper wrapper = APIProxy.getUserInfoByUid(
							appKey, String.valueOf(member.getUid()), token);
					SdkUserInfoObject userInfo = new SdkUserInfoObject();
					if (wrapper.isOK()) {
						HashMap map = gson.fromJson(wrapper.content,
								HashMap.class);
						String name = String.valueOf(map.get("username"));
						if (1 == member.getFlag()) {
							groupInfoObject.setOwnerUsername(name);
						}
						Map infoData = gson.fromJson(wrapper.content, HashMap.class);
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
						userInfo.setCtime(_ctime);
					}
					userInfoList.add(userInfo);
				}
				groupInfoObject.setMembers(userInfoList);
			}

			SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
					V1.VERSION, id, JMessage.Method.GROUPINFO_GET,
					gson.toJson(groupInfoObject));
			log.info("getGroupInfo resp data -- data: " + gson.toJson(groupInfoObject));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		} else {
			log.warn(String.format("getGroupInfo call sdk-api exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUPINFO_GET);
			resp.setErrorInfo(1000, "call sdk-api exception");
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		}
	}

	/**
	 * 添加群组成员
	 * 
	 * @param client
	 * @param data
	 */
	public static void addGroupMembers(SocketIOClient client,
			SdkRequestObject data) {
		String id = data.getId();
		long group_id = data.getParams().getGroupId();
		List<String> member_usernames = data.getParams().getMemberUsernames();
		int memberCount = member_usernames.size();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("addGroupMembers request data -- data: %s", gson.toJson(data)));
		if(0==group_id||0==memberCount){
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUPMEMBERS_ADD);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		}
		if (StringUtils.isEmpty(keyAndname)) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUPMEMBERS_ADD);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("through keyAndname resovle username exception");
				return;
			}
		}
		log.info(String.format("user: %s add group member", userName));
		Jedis jedis = null;
		int sid = 0;
		long uid = 0L;
		String token = "";
		long addUid = 0L;
		long juid = 0L;
		long rid = Long.parseLong(id);
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName,
					"appKey", "sid", "uid", "token", "juid");
			appKey = dataList.get(0);
			sid = Integer.parseInt(dataList.get(1));
			uid = Long.parseLong(dataList.get(2));
			token = dataList.get(3);
			juid = Long.parseLong(dataList.get(4));
		} catch (JedisConnectionException e) {
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		List<Long> list = new ArrayList<Long>();
		for (String name : member_usernames) {
			HttpResponseWrapper resultWrapper = APIProxy.getUserInfo(appKey,
					name, token);
			if (resultWrapper.isOK()) {
				User userInfo = gson
						.fromJson(resultWrapper.content, User.class);
				addUid = userInfo.getUid();
				list.add(addUid);
			} else {
				log.warn(String.format("addGroupMembers call sdk-api getUserInfo exception"));
			}
		}
		AddGroupMemberRequestBean bean = new AddGroupMemberRequestBean(
				group_id, memberCount, list);
		List<Integer> cookie = new ArrayList<Integer>();
		ImAddGroupMemberRequestProto req = new ImAddGroupMemberRequestProto(
				Command.JPUSH_IM.ADD_GROUP_MEMBER, 1, uid, appKey, rid, sid,
				juid, cookie, bean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":"
				+ userName);
		channel.writeAndFlush(req);
		log.info(String.format("user: %s begin send add group member request",
				userName));
	}

	/**
	 * 移除群组成员
	 * 
	 * @param client
	 * @param data
	 */
	public static void removeGroupMembers(SocketIOClient client,
			SdkRequestObject data) {
		String id = data.getId();
		long group_id = data.getParams().getGroupId();
		List<String> member_usernames = data.getParams().getMemberUsernames();
		int memberCount = member_usernames.size();
		long rid = Long.parseLong(id);
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("removeGroupMembers request data -- data: %s", gson.toJson(data)));
		if(0==group_id||0==memberCount){
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUPMEMBERS_REMOVE);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		if (StringUtils.isEmpty(keyAndname)) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUPMEMBERS_REMOVE);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("through keyAndname resovle username exception");
				return;
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
			List<String> dataList = jedis.hmget(appKey + ":" + userName,
					"appKey", "sid", "uid", "token", "juid");
			appKey = dataList.get(0);
			sid = Integer.parseInt(dataList.get(1));
			uid = Long.parseLong(dataList.get(2));
			token = dataList.get(3);
			juid = Long.parseLong(dataList.get(4));
		} catch (JedisConnectionException e) {
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		List<Long> list = new ArrayList<Long>();
		for (String name : member_usernames) {
			HttpResponseWrapper resultWrapper = APIProxy.getUserInfo(appKey,
					name, token);
			if (resultWrapper.isOK()) {
				User userInfo = gson
						.fromJson(resultWrapper.content, User.class);
				addUid = userInfo.getUid();
				list.add(addUid);
			} else {
				log.warn(String.format("removeGroupMembers call sdk-api getUserInfo exception"));
			}
		}
		DeleteGroupMemberRequestBean bean = new DeleteGroupMemberRequestBean(
				group_id, memberCount, list);
		List<Integer> cookie = new ArrayList<Integer>();
		ImDeleteGroupMemberRequestProto req = new ImDeleteGroupMemberRequestProto(
				Command.JPUSH_IM.DEL_GROUP_MEMBER, 1, uid, appKey, rid, sid,
				juid, cookie, bean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":"
				+ userName);
		channel.writeAndFlush(req);
	}

	/**
	 * 退出群组
	 * 
	 * @param client
	 * @param data
	 */
	public static void exitGroup(SocketIOClient client, SdkRequestObject data) {
		log.info("exit group event");
		String id = data.getId();
		long group_id = data.getParams().getGroupId();
		long rid = Long.parseLong(id);
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("exitGroup request data -- data: %s", gson.toJson(data)));
		if(0==group_id){
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUP_EXIT);
			resp.setErrorInfo(JMessage.Error.ARGUMENTS_EXCEPTION, JMessage.Error.getErrorMessage(JMessage.Error.ARGUMENTS_EXCEPTION));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		}
		if (StringUtils.isEmpty(keyAndname)) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUP_EXIT);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("through keyAndname resovle username exception");
				return;
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
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		ExitGroupRequestBean bean = new ExitGroupRequestBean(group_id);
		List<Integer> cookie = new ArrayList<Integer>();
		ImExitGroupRequestProto req = new ImExitGroupRequestProto(
				Command.JPUSH_IM.EXIT_GROUP, 1, uid, appKey, rid, sid, juid,
				cookie, bean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":"
				+ userName);
		channel.writeAndFlush(req);
	}

	/**
	 * 获取群组列表
	 * 
	 * @param client
	 * @param data
	 */
	public static void getGroupList(SocketIOClient client, SdkRequestObject data) {
		String id = data.getId();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("getGroupList request data -- data: %s", data));
		if (StringUtils.isEmpty(keyAndname)) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUPLIST_GET);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("getGroupList through map resovle username exception");
				return;
			}
		}
		Jedis jedis = null;
		String token = "";
		long uid = 0L;
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName,
					"token", "uid");
			token = dataList.get(0);
			uid = Long.parseLong(dataList.get(1));
		} catch (JedisConnectionException e) {
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		log.info(String.format("user: %s begin to get group list", userName));
		List<SdkGroupInfoObject> groupsList = new ArrayList<SdkGroupInfoObject>();
		HttpResponseWrapper result = APIProxy.getGroupList(String.valueOf(uid),
				token);
		if (result.isOK()) {
			String groupListJson = result.content;
			List<Long> gidList = gson.fromJson(groupListJson,
					new TypeToken<ArrayList<Long>>() {
					}.getType());
			for (Long gid : gidList) {
				HttpResponseWrapper groupInfoResult = APIProxy.getGroupInfo(
						String.valueOf(gid), token);
				ArrayList<String> members_name = new ArrayList<String>();
				if (groupInfoResult.isOK()) {
					String groupInfoJson = groupInfoResult.content;
					InnerGroupObject tmp_group = gson.fromJson(groupInfoJson,
							InnerGroupObject.class);
					SdkGroupInfoObject groupInfoObject = new SdkGroupInfoObject();
					groupInfoObject.setGid(tmp_group.getGid());
					groupInfoObject.setGroupName(tmp_group.getName());
					groupInfoObject.setGroupDesc(tmp_group.getDesc());
					HttpResponseWrapper resultWrapper = APIProxy
							.getGroupMemberList(String.valueOf(gid), token);
					if (resultWrapper.isOK()) {
						List<GroupMember> groupList = gson.fromJson(
								resultWrapper.content,
								new TypeToken<ArrayList<GroupMember>>() {
								}.getType());
						for (GroupMember member : groupList) {
							HttpResponseWrapper wrapper = APIProxy
									.getUserInfoByUid(appKey,
											String.valueOf(member.getUid()),
											token);
							if (wrapper.isOK()) {
								HashMap map = gson.fromJson(wrapper.content,
										HashMap.class);
								String name = String.valueOf(map
										.get("username"));
								members_name.add(name);
								if (1 == member.getFlag()) {
									groupInfoObject.setOwnerUsername(name);
								}
							} else {
								log.warn(String.format("getGroupList call sdk-api getUserInfoByUid exception"));
							}
						}
						groupInfoObject.setMembersUsername(members_name);
					} else {
						log.warn(String.format("getGroupList call sdk-api getGroupMemberList exception"));
					}
					groupsList.add(groupInfoObject);
				} else {
					log.warn(String.format("getGroupList call sdk-api getGroupInfo exception"));
				}
			}
			SdkCommonSuccessRespObject resp = new SdkCommonSuccessRespObject(
					V1.VERSION, id, JMessage.Method.GROUPLIST_GET,
					gson.toJson(groupsList));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			log.info(String.format("user: %d get group list success", uid));
		} else {
			log.warn(String.format("getGroupList call sdk-api getGroupList exception"));
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUPLIST_GET);
			resp.setErrorInfo(1000, "call sdk-api exception");
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
		}
	}

	/**
	 * 更新群组信息
	 * 
	 * @param client
	 * @param data
	 */
	public static void updateGroupInfo(SocketIOClient client,
			SdkRequestObject data) {
		String id = data.getId();
		long gid = data.getParams().getGroupId();
		String group_name = data.getParams().getGroupName();
		String group_desc = data.getParams().getGroupDescription();
		String appKey = "";
		String userName = "";
		String keyAndname = WebImServer.sessionClientToUserNameMap.get(client);
		log.info(String.format("updateGroupInfo request data -- data: %s", gson.toJson(data)));
		if (StringUtils.isEmpty(keyAndname)) {
			SdkCommonErrorRespObject resp = new SdkCommonErrorRespObject(V1.VERSION,
					id, JMessage.Method.GROUPINFO_UPDATE);
			resp.setErrorInfo(JMessage.Error.USER_NOT_LOGIN, JMessage.Error.getErrorMessage(JMessage.Error.USER_NOT_LOGIN));
			client.sendEvent(V1.DATA_AISLE, gson.toJson(resp));
			return;
		} else {
			appKey = StringUtils.getAppKey(keyAndname);
			userName = StringUtils.getUserName(keyAndname);
			if (StringUtils.isEmpty(appKey) || StringUtils.isEmpty(userName)) {
				log.warn("through keyAndname resovle username exception");
				return;
			}
		}
		Jedis jedis = null;
		long rid = Long.parseLong(id);
		long uid = 0L;
		int sid = 0;
		long juid = 0L;
		try {
			jedis = redisClient.getJeids();
			List<String> dataList = jedis.hmget(appKey + ":" + userName,
					"appKey", "uid", "sid", "juid");
			appKey = dataList.get(0);
			uid = Long.parseLong(dataList.get(1));
			sid = Integer.parseInt(dataList.get(2));
			juid = Long.parseLong(dataList.get(3));
		} catch (JedisConnectionException e) {
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		log.info(String.format(
				"user: %s update group: %d name, new group name is %s",
				userName, gid, group_name));
		UpdateGroupInfoRequestBean bean = new UpdateGroupInfoRequestBean(gid,
				group_name, group_desc);
		List<Integer> cookie = new ArrayList<Integer>();
		ImUpdateGroupInfoRequestProto req = new ImUpdateGroupInfoRequestProto(
				Command.JPUSH_IM.UPDATE_GROUP_INFO, 1, uid, appKey, rid, sid,
				juid, cookie, bean);
		Channel channel = WebImServer.userNameToPushChannelMap.get(appKey + ":"
				+ userName);
		channel.writeAndFlush(req);
		log.info(String.format("user: %s send update group name request",
				userName));
	}

	public static String getMediaId(long uid) {
		Date d = new Date();
		long time = d.getTime();
		long random = (Math.max(
				Math.min(Math.round(Math.random() * (100 - 0)), 100), 0));
		String mediaId = "qiniu/image/"
				+ StringUtils.toMD5(String.valueOf(uid) + time + random);
		return mediaId;
	}

	public static String uploadFile(String mediaId, String filePath)
			throws AuthException, JSONException {
		Mac mac = new Mac(Configure.QNCloudInterface.QN_ACCESS_KEY,
				Configure.QNCloudInterface.QN_SECRET_KEY);
		PutPolicy putPolicy = new PutPolicy(
				Configure.QNCloudInterface.QN_BUCKETNAME);
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

}
