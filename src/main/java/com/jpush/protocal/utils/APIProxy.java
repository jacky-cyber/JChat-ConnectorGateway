package com.jpush.protocal.utils;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;


/*
 * http 协议从 Server 拉 IM 数据
 */
public final class APIProxy {
	private final static String APPKEY = "57b59f7968cbaf9ee8dcde77";
	private final static String API_URL = SystemConfig.getProperty("http.api.server.url");
	private static Gson gson = new Gson();
	public APIProxy(){};
	
	// 注册新的IM用户
	public static ResponseWrapper register(String appkey, String username, String password) throws Exception{
		password = StringUtils.toMD5(password);
		Map<String, String> data = new HashMap<String, String>();
		data.put("username", username);
		data.put("password", password);
	   data.put("appkey", appkey);
		String content = gson.toJson(data);
		String url = API_URL+"/users/";
		ResponseWrapper result = NativeHttpClient.doPost(url, content);
		return result;
	}
	
	//  获取用户信息
	public static ResponseWrapper getUserInfo(String uid) throws Exception{
		String url = API_URL + "/users/" + uid;
		ResponseWrapper result = NativeHttpClient.doGet(url, "");
		return result;
	}
	
	//  获取群组列表
	public static ResponseWrapper getGroupList(String uid) throws Exception{
		String url = API_URL + "/users/"+uid+"/groups/";
		ResponseWrapper result = NativeHttpClient.doGet(url, "");
		return result;
	}
	
	//  获取群组信息
	public static ResponseWrapper getGroupInfo(String gid) throws Exception{
		String url = API_URL + "/groups/"+gid;
		ResponseWrapper result = NativeHttpClient.doGet(url, "");
		return result;
	}
	
	//  获取群组成员
	public static ResponseWrapper getGroupMemberList(String gid) throws Exception{
		String url = API_URL + "/groups/"+gid+"/members/";
		ResponseWrapper result = NativeHttpClient.doGet(url, "");
		return result;
	}
	
	
	public static void main(String[] argus) throws Exception{
		ResponseWrapper result = null;
		//result = APIProxy.register(APIProxy.APPKEY, "j001", "j001pwd"); 
		//result = APIProxy.getUserInfo("85841");
		//result = APIProxy.getGroupInfo("123456");
		//result = APIProxy.getGroupList("85841");
	   result = APIProxy.getGroupMemberList("123456");
		System.out.println("result: "+result.isOK()+", "+result.content);
	}
	
}
