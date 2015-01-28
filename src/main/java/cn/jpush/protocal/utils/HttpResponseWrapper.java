package cn.jpush.protocal.utils;

public class HttpResponseWrapper {
	public boolean isNeedRetry;
	public int httpCode;
	public String content;

	public HttpResponseWrapper() {
	}

	public HttpResponseWrapper(int httpCode, String content) {
		this.httpCode = httpCode;
		this.content = content;
	}

	@Override
	public String toString() {
		return "{httpCode:" + httpCode + ", content:" + content + "}";
	}

	public boolean isOK() {
		return httpCode >= 200 && httpCode < 300;
	}
}
