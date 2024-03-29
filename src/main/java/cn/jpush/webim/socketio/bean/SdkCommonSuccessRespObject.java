package cn.jpush.webim.socketio.bean;

public class SdkCommonSuccessRespObject {
	private String apiVersion;
	private String id;
	private String method;
	private String data;
	
	public SdkCommonSuccessRespObject(String apiVersion, String id,
			String method, String data) {
		super();
		this.apiVersion = apiVersion;
		this.id = id;
		this.method = method;
		this.data = data;
	}
	public String getApiVersion() {
		return apiVersion;
	}
	public void setApiVersion(String apiVersion) {
		this.apiVersion = apiVersion;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	 
}
