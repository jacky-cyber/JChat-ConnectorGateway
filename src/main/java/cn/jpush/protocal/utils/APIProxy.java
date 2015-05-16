package cn.jpush.protocal.utils;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.common.JPushTcpClientHandler;

import com.google.gson.Gson;
import com.qiniu.api.auth.digest.Mac;
import com.qiniu.api.io.IoApi;
import com.qiniu.api.io.PutExtra;
import com.qiniu.api.io.PutRet;
import com.qiniu.api.rs.PutPolicy;


/*
 * http 协议从 Server 拉 IM 数据
 */
public final class APIProxy {
	private static Logger log = (Logger) LoggerFactory.getLogger(APIProxy.class);
	private final static String API_URL = SystemConfig.getProperty("http.api.server.url");
	private final static String HTTPS_API_URL = SystemConfig.getProperty("https.api.server.url");
	private static Gson gson = new Gson();
	public APIProxy(){};
	
	// 获取token
	public static HttpResponseWrapper getToken(String uid, String password){
		String authCode = BASE64Utils.encodeString(uid+":"+password);
		String url = HTTPS_API_URL+"/token";
		HttpResponseWrapper result = null;
		try {
			result = NativeHttpClient.doGet(url, "", authCode);
		} catch (Exception e) {
			log.error(String.format("user: %s call sdk-api: (getToken) exception: %s", uid, e.getMessage()));
		}
		return result;
	}
	
	// 注册新的IM用户
	public static HttpResponseWrapper register(String appkey, String username, String password){
		password = StringUtils.toMD5(password);
		Map<String, String> data = new HashMap<String, String>();
		data.put("username", username);
		data.put("password", password);
	   data.put("appkey", appkey);
		String content = gson.toJson(data);
		String url = HTTPS_API_URL+"/users";
		HttpResponseWrapper result = null;
		try {
			result = NativeHttpClient.doPost(url, content);
		} catch (Exception e) {
			log.error(String.format("user: %s call sdk-api: (register) exception: %s", username, e.getMessage()));
		}
		return result;
	}
	
	//  获取用户信息
	public static HttpResponseWrapper getUserInfo(String appkey, String username){
		String url = API_URL + "/users/" + username + "?idtype=username";
		HttpResponseWrapper result = null;
		try {
			result = NativeHttpClient.doGet(url, appkey, true);
		} catch (Exception e) {
			log.error(String.format("user: %s call sdk-api: (getUserInfo) exception: %s", username, e.getMessage()));
		}
		return result;
	}
	
	public static HttpResponseWrapper getUserInfo(String appkey, String username, String token) {
		String url = API_URL + "/users/" + username + "?idtype=username";
		log.info(String.format("call sdk-api url: %s", url));
		HttpResponseWrapper result = null;
		try {
			result = NativeHttpClient.doGet(url, appkey, true, token);
		} catch (Exception e) {
			log.error(String.format("user: %s call sdk-api: (getUserInfo) exception: %s", username, e.getMessage()));
		}
		return result;
	}
	
	//  获取用户信息通过uid
	public static HttpResponseWrapper getUserInfoByUid(String appkey, String uid) {
		String url = API_URL + "/users/" + uid + "?idtype=uid";
		HttpResponseWrapper result = null;
		try {
			result = NativeHttpClient.doGet(url, appkey, true);
		} catch (Exception e) {
			log.error(String.format("user: %s call sdk-api: (getUserInfoByUid) exception: %s", uid, e.getMessage()));
		}
		return result;
	}
	
	public static HttpResponseWrapper getUserInfoByUid(String appkey, String uid, String token) {
		String url = API_URL + "/users/" + uid + "?idtype=uid";
		HttpResponseWrapper result = null;
		try {
			result = NativeHttpClient.doGet(url, appkey, true, token);
		} catch (Exception e) {
			log.error(String.format("user: %s call sdk-api: (getUserInfoByUid) exception: %s", uid, e.getMessage()));
		}
		return result;
	}
	
	//  获取群组列表
	public static HttpResponseWrapper getGroupList(String uid, String token) {
		String url = API_URL + "/users/"+uid+"/groups";
		HttpResponseWrapper result = null;
		try {
			result = NativeHttpClient.doGetByToken(url, "", token);
		} catch (Exception e) {
			log.error(String.format("user: %s call sdk-api: (getGroupList) exception: %s", uid, e.getMessage()));
		}
		return result;
	}
	
	//  获取群组信息
	public static HttpResponseWrapper getGroupInfo(String gid, String token) {
		String url = API_URL + "/groups/"+gid;
		HttpResponseWrapper result = null;
		try {
			result = NativeHttpClient.doGetByToken(url, "", token);
		} catch (Exception e) {
			log.error(String.format("group(gid): %s call sdk-api: (getGroupInfo) exception: %s", gid, e.getMessage()));
		}
		return result;
	}
	
	//  获取群组成员
	public static HttpResponseWrapper getGroupMemberList(String gid, String token) {
		String url = API_URL + "/groups/"+gid+"/members";
		HttpResponseWrapper result = null;
		try {
			result = NativeHttpClient.doGetByToken(url, "", token);
		} catch (Exception e) {
			log.error(String.format("group(gid): %s call sdk-api: (getGroupMemberList) exception: %s", gid, e.getMessage()));
		}
		return result;
	}

	
	public static void main(String[] argus) throws Exception{
		HttpResponseWrapper result = null;
		result = APIProxy.getToken("10000288", "C4CA4238A0B923820DCC509A6F75849B");
		//String token = BASE64Utils.encodeString("10000288"+":"+"JSyof+KglitsHcfg6keXNDipvhI=\n");
		//result = APIProxy.register(APPKEY, "p011", "p011");
		//result = APIProxy.getUserInfo(APPKEY, "p001", token);
		//result = APIProxy.getGroupInfo("10000487", token);
		//result = APIProxy.getGroupList("10000781", token);
	   //result = APIProxy.getGroupMemberList("10000487", token);
		System.out.println("result: "+result.isOK()+", "+result.content);
	}
	
}
