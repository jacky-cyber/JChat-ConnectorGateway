package cn.jpush.webim.socketio.bean;

public class SdkConfigObject {
	private String appKey;
	private String timestamp;
	private String random_str;
	private String signature;
	public String getAppKey() {
		return appKey;
	}
	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	
	public String getRandom_str() {
		return random_str;
	}
	public void setRandom_str(String random_str) {
		this.random_str = random_str;
	}
	public String getSignature() {
		return signature;
	}
	public void setSignature(String signature) {
		this.signature = signature;
	}
	
}
