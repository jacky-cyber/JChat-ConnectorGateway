package com.jpush.protocal.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.jpush.protocal.common.JPushTcpClient;

public final class NativeHttpClient {
	private static Logger log = (Logger) LoggerFactory.getLogger(JPushTcpClient.class);
	private final static String USER_AGENT = "JPush-IM-Android-Client";
	private final static String CONTENT_TYPE = "application/json";
	private final static String CHARSET = "utf-8";
	private final static String POST = "POST";
	private final static String GET = "GET";
	private final static String DELETE = "DELETE";

	private final static int DEFAULT_CONNECTION_TIMEOUT = 50 * 1000;
	private final static int DEFAULT_READ_TIMEOUT = 130 * 1000;

	/*-------- public method ---------*/
	public static ResponseWrapper doPost(String url, String content)
			throws Exception {
		return request(url, content, POST, null);
	}

	public static ResponseWrapper doPost(String url, String content,
			String authCode) throws Exception {
		return request(url, content, POST, authCode);
	}

	public static ResponseWrapper doGet(String url, String content)
			throws Exception {
		if (!StringUtils.isEmpty(content))
			return request(url + "?" + content, null, GET, null);
		else
			return request(url, null, GET, null);
	}

	public static ResponseWrapper doGet(String url, String content,
			String authCode) throws Exception {
		if (!StringUtils.isEmpty(content))
			return request(url + "?" + content, null, GET, authCode);
		else
			return request(url, null, GET, authCode);
	}

	public static ResponseWrapper doDelete(String url, String content,
			String authCode) throws Exception {
		if (!StringUtils.isEmpty(content))
			return request(url + "?" + content, null, DELETE, authCode);
		else
			return request(url, null, DELETE, authCode);
	}


	/**
	 * 请求
	 * 
	 * @param url
	 *            URL
	 * @param content
	 *            请求正文
	 * @param method
	 *            请求方法
	 * @param authCode
	 *            权限验证token
	 * @return
	 * @throws Exception
	 */
	private static ResponseWrapper request(String url, String content,
			String method, String authCode) throws Exception {
	
		HttpURLConnection conn = null;
		OutputStream out = null;
		StringBuffer sb = new StringBuffer();
		ResponseWrapper wrapper = new ResponseWrapper();

		try {
			URL aUrl = new URL(url);
			conn = (HttpURLConnection) aUrl.openConnection();
			conn.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT);
			conn.setReadTimeout(DEFAULT_READ_TIMEOUT);
			conn.setUseCaches(false);
			conn.setRequestMethod(method);
			conn.setRequestProperty("User-Agent", USER_AGENT);
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Accept-Charset", CHARSET);
			conn.setRequestProperty("Charset", CHARSET);
         conn.setRequestProperty("Content-Type", CONTENT_TYPE);
			if (!StringUtils.isEmpty(authCode)) {
				conn.setRequestProperty("Authorization", "Basic " + authCode);
			}
			if (POST.equals(method)) {// POST Request
				conn.setDoOutput(true);
				byte[] data = content.getBytes(CHARSET);
				conn.setRequestProperty("Content-Length",
						String.valueOf(data.length));
				out = conn.getOutputStream();
				out.write(data);
				out.flush();
			} else {
				conn.setDoOutput(false);
			}
			int status = conn.getResponseCode();
			InputStream in;
			if (status >= 200 && status < 300) {
				in = conn.getInputStream();
			} else {
				in = conn.getErrorStream();
			}
			InputStreamReader reader = new InputStreamReader(in, CHARSET);
			char[] buff = new char[1024];
			int len;
			while ((len = reader.read(buff)) > 0) {
				sb.append(buff, 0, len);
			}
			String responseContent = sb.toString();
			wrapper.httpCode = status;
			wrapper.content = responseContent;
		} catch (Exception e) {
			throw new Exception("Connect fail, please try again later.", e);
		} finally {
			if (null != out) {
				try {
					out.close();
				} catch (IOException e) {
					
				}
			}
			if (null != conn) {
				conn.disconnect();
			}
		}

		return wrapper;
	}

	private NativeHttpClient() {
	}
}
