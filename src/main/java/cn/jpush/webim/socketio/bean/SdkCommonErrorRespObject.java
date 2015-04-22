package cn.jpush.webim.socketio.bean;

public class SdkCommonErrorRespObject {
	private String apiVersion;
	private String id;
	private String method;
	private Error error;
	
	public SdkCommonErrorRespObject(String apiVersion, String id,
			String method) {
		super();
		this.apiVersion = apiVersion;
		this.id = id;
		this.method = method;
	}
	public Error getError() {
		return error;
	}
	public void setError(Error error) {
		this.error = error;
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
	public void setErrorInfo(int code, String message){
		error = new Error();
		this.error.setCode(code);
		this.error.setMessage(message);
	}

}