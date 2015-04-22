package cn.jpush.webim.socketio.bean;

public class SdkRequestObject {
	private String apiVersion;
	private String id;
	private String method;
	private SdkRequesrParamsObject params;
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
	public SdkRequesrParamsObject getParams() {
		return params;
	}
	public void setParams(SdkRequesrParamsObject params) {
		this.params = params;
	}
	
}
