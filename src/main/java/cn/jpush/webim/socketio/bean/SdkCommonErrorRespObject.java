package cn.jpush.webim.socketio.bean;

public class SdkCommonErrorRespObject {
	private Error error;
	public Error getError() {
		return error;
	}
	public void setError(Error error) {
		this.error = error;
	}
	public void setErrorInfo(int code, String message){
		error = new Error();
		this.error.setCode(code);
		this.error.setMessage(message);
	}

}