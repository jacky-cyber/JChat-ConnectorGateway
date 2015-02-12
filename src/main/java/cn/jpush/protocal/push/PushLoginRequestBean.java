package cn.jpush.protocal.push;

public class PushLoginRequestBean {
	private long uid;
	private String from_resource;
	private String passwdmd5;
	private int client_version;
	private String appkey;
	private int playform;
	public PushLoginRequestBean(){}
	public PushLoginRequestBean(long uid, String from_resource, String passwdmd5,
			int client_version, String appkey, int playform) {
		this.uid = uid;
		this.from_resource = from_resource;
		this.passwdmd5 = passwdmd5;
		this.client_version = client_version;
		this.appkey = appkey;
		this.playform = playform;
	}
	
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	public String getFrom_resource() {
		return from_resource;
	}
	public void setFrom_resource(String from_resource) {
		this.from_resource = from_resource;
	}
	public String getPasswdmd5() {
		return passwdmd5;
	}
	public void setPasswdmd5(String passwdmd5) {
		this.passwdmd5 = passwdmd5;
	}
	public int getClient_version() {
		return client_version;
	}
	public void setClient_version(int client_version) {
		this.client_version = client_version;
	}
	public String getAppkey() {
		return appkey;
	}
	public void setAppkey(String appkey) {
		this.appkey = appkey;
	}
	public int getPlayform() {
		return playform;
	}
	public void setPlayform(int playform) {
		this.playform = playform;
	}
	
}
