package cn.jpush.webim.socketio.bean;

public class SdkCommonErrorRespObject {
	private boolean result = false;
	private Error error;
	public Error getError() {
		return error;
	}
	public void setError(Error error) {
		this.error = error;
	}
	public void setErrorInfo(int code, String error_message){
		error = new Error();
		this.error.setError_code(code);
		this.error.setError_message(error_message);
	}

}