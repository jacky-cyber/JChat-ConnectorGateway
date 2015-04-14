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
	public void setErrorInfo(int code, String errorMessage){
		error.setErrorCode(code);
		error.setErrorMessage(errorMessage);
	}

	public class Error{
		private int errorCode;
		private String errorMessage;
		public int getErrorCode() {
			return errorCode;
		}
		public void setErrorCode(int errorCode) {
			this.errorCode = errorCode;
		}
		public String getErrorMessage() {
			return errorMessage;
		}
		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}
	}
}