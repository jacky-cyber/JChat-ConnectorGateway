package cn.jpush.webim.socketio.bean;

import com.google.gson.Gson;

public class MsgContentBean {
	private int version;
	private String target_type;
	private String target_id;
	private String target_name;
	private String from_type;
	private String from_id;
	private String from_name;
	private String from_platform;
	private int create_time;
	private String msg_type;
	private MsgBody msg_body;
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public String getTarget_type() {
		return target_type;
	}
	public void setTarget_type(String target_type) {
		this.target_type = target_type;
	}
	public String getTarget_id() {
		return target_id;
	}
	public void setTarget_id(String target_id) {
		this.target_id = target_id;
	}
	public String getTarget_name() {
		return target_name;
	}
	public void setTarget_name(String target_name) {
		this.target_name = target_name;
	}
	public String getFrom_type() {
		return from_type;
	}
	public void setFrom_type(String from_type) {
		this.from_type = from_type;
	}
	public String getFrom_id() {
		return from_id;
	}
	public void setFrom_id(String from_id) {
		this.from_id = from_id;
	}
	public String getFrom_name() {
		return from_name;
	}
	public void setFrom_name(String from_name) {
		this.from_name = from_name;
	}
	public String getFrom_platform() {
		return from_platform;
	}
	public void setFrom_platform(String from_platform) {
		this.from_platform = from_platform;
	}
	public int getCreate_time() {
		return create_time;
	}
	public void setCreate_time(int create_time) {
		this.create_time = create_time;
	}
	public String getMsg_type() {
		return msg_type;
	}
	public void setMsg_type(String msg_type) {
		this.msg_type = msg_type;
	}
	
	public MsgBody getMsg_body() {
		return msg_body;
	}
	public void setMsg_body(MsgBody msg_body) {
		this.msg_body = msg_body;
	}
	
}
