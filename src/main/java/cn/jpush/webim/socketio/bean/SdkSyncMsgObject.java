package cn.jpush.webim.socketio.bean;

public class SdkSyncMsgObject {
	private String username;
	private String to_username;
	private String message;
	private String msg_type;
	private String content_type;
	private int create_time;
	private long message_id;
	private int i_msg_type;
	private long from_uid;
	private long from_gid;
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getTo_username() {
		return to_username;
	}
	public void setTo_username(String to_username) {
		this.to_username = to_username;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getMsg_type() {
		return msg_type;
	}
	public void setMsg_type(String msg_type) {
		this.msg_type = msg_type;
	}
	public String getContent_type() {
		return content_type;
	}
	public void setContent_type(String content_type) {
		this.content_type = content_type;
	}
	public int getCreate_time() {
		return create_time;
	}
	public void setCreate_time(int create_time) {
		this.create_time = create_time;
	}
	public long getMessage_id() {
		return message_id;
	}
	public void setMessage_id(long message_id) {
		this.message_id = message_id;
	}
	public int getI_msg_type() {
		return i_msg_type;
	}
	public void setI_msg_type(int i_msg_type) {
		this.i_msg_type = i_msg_type;
	}
	public long getFrom_uid() {
		return from_uid;
	}
	public void setFrom_uid(long from_uid) {
		this.from_uid = from_uid;
	}
	public long getFrom_gid() {
		return from_gid;
	}
	public void setFrom_gid(long from_gid) {
		this.from_gid = from_gid;
	}
	
}
