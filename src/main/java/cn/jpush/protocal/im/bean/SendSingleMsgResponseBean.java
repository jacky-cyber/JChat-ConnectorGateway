package cn.jpush.protocal.im.bean;


public class SendSingleMsgResponseBean{
	private int code;
	private String message;
	private String msgid;
	public SendSingleMsgResponseBean(){}
	public SendSingleMsgResponseBean(int code, String message, String msgid){
		this.code = code;
		this.message = message;
		this.msgid = msgid;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getMsgid() {
		return msgid;
	}
	public void setMsgid(String msgid) {
		this.msgid = msgid;
	}
	
	
}
