package cn.jpush.webim.socketio.bean;

public class ChatMessage {
	private String version;
	private int sid;
	private long juid;
	private String show_type;
	private String target_type;
	private String target_id;
	private String target_name;
	private String from_type;
	private long from_id;
	private String from_name;
	private String create_time;
	public Notification notification;
	private long msg_id;
	private String msg_type;
	public MsgBody msg_body;
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getShow_type() {
		return show_type;
	}
	public void setShow_type(String show_type) {
		this.show_type = show_type;
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
	public long getFrom_id() {
		return from_id;
	}
	public void setFrom_id(long from_id) {
		this.from_id = from_id;
	}
	public String getFrom_name() {
		return from_name;
	}
	public void setFrom_name(String from_name) {
		this.from_name = from_name;
	}
	public String getCreate_time() {
		return create_time;
	}
	public void setCreate_time(String create_time) {
		this.create_time = create_time;
	}
	public Notification getNotification() {
		return notification;
	}
	public void setNotification(Notification notification) {
		this.notification = notification;
	}
	public long getMsg_id() {
		return msg_id;
	}
	public void setMsg_id(long msg_id) {
		this.msg_id = msg_id;
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
	
	public int getSid() {
		return sid;
	}
	public void setSid(int sid) {
		this.sid = sid;
	}
	public long getJuid() {
		return juid;
	}
	public void setJuid(long juid) {
		this.juid = juid;
	}

	public class Notification{
		private String alert;

		public String getAlert() {
			return alert;
		}

		public void setAlert(String alert) {
			this.alert = alert;
		}
		
	}

	public class MsgBody{
		private String content;

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}
		
	}
	
}

