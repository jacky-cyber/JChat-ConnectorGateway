package cn.jpush.protocal.utils;

import java.util.HashMap;
import java.util.Map;

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
	private final static String APPKEY = SystemConfig.getProperty("jpush.appkey");
	private final static String API_URL = SystemConfig.getProperty("http.api.server.url");
	private static Gson gson = new Gson();
	public APIProxy(){};
	
	// 注册新的IM用户
	public static HttpResponseWrapper register(String appkey, String username, String password) throws Exception{
		password = StringUtils.toMD5(password);
		Map<String, String> data = new HashMap<String, String>();
		data.put("username", username);
		data.put("password", password);
	   data.put("appkey", appkey);
		String content = gson.toJson(data);
		String url = API_URL+"/users/";
		HttpResponseWrapper result = NativeHttpClient.doPost(url, content);
		return result;
	}
	
	//  获取用户信息
	public static HttpResponseWrapper getUserInfo(String appkey, String username) throws Exception{
		String url = API_URL + "/users/" + username + "?idtype=username";
		HttpResponseWrapper result = NativeHttpClient.doGet(url, appkey, true);
		return result;
	}
	
	//  获取群组列表
	public static HttpResponseWrapper getGroupList(String uid) throws Exception{
		String url = API_URL + "/users/"+uid+"/groups/";
		HttpResponseWrapper result = NativeHttpClient.doGet(url, "");
		return result;
	}
	
	//  获取群组信息
	public static HttpResponseWrapper getGroupInfo(String gid) throws Exception{
		String url = API_URL + "/groups/"+gid;
		HttpResponseWrapper result = NativeHttpClient.doGet(url, "");
		return result;
	}
	
	//  获取群组成员
	public static HttpResponseWrapper getGroupMemberList(String gid) throws Exception{
		String url = API_URL + "/groups/"+gid+"/members/";
		HttpResponseWrapper result = NativeHttpClient.doGet(url, "");
		return result;
	}
	
	
	public static void main(String[] argus) throws Exception{
		HttpResponseWrapper result = null;
		//result = APIProxy.register(APPKEY, "jpush003", "j001pwd");
		//result = APIProxy.getUserInfo(APPKEY, "jpush002");
		//result = APIProxy.getGroupInfo("123456");
		result = APIProxy.getGroupList("10000020");
	   //result = APIProxy.getGroupMemberList("10000004");
		System.out.println("result: "+result.isOK()+", "+result.content);
	}
	
}
