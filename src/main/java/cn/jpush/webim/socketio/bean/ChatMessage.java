package cn.jpush.webim.socketio.bean;

import com.google.gson.Gson;

public class ChatMessage {
	private String appKey;
	private String version;
	private int sid;
	private long juid;
	private long rid;
	private String show_type;
	private String target_type;
	private String target_id;
	private String target_name;
	private String from_type;
	private long from_id;
	private String from_name;
	private int create_time;
	public Notification notification;
	private long msg_id;
	private String msg_type;
	public MsgBody msg_body;
	
	public String getAppKey() {
		return appKey;
	}
	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}
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
	
	public long getRid() {
		return rid;
	}
	public void setRid(long rid) {
		this.rid = rid;
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
	public int getCreate_time() {
		return create_time;
	}
	public void setCreate_time(int create_time) {
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
		private String text;
		private String media_id; 
		private long media_crc32;
		private int duration;
		private int width;
		private int height;
		private String format;
		private String img_link;
		private String extras;
		
		public int getDuration() {
			return duration;
		}
		public void setDuration(int duration) {
			this.duration = duration;
		}
		public String getImg_link() {
			return img_link;
		}
		public void setImg_link(String img_link) {
			this.img_link = img_link;
		}
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		}
		public String getMedia_id() {
			return media_id;
		}
		public void setMedia_id(String media_id) {
			this.media_id = media_id;
		}
		public long getMedia_crc32() {
			return media_crc32;
		}
		public void setMedia_crc32(long media_crc32) {
			this.media_crc32 = media_crc32;
		}
		public int getWidth() {
			return width;
		}
		public void setWidth(int width) {
			this.width = width;
		}
		public int getHeight() {
			return height;
		}
		public void setHeight(int height) {
			this.height = height;
		}
		public String getFormat() {
			return format;
		}
		public void setFormat(String format) {
			this.format = format;
		}
		
		public String getExtras() {
			return extras;
		}
		public void setExtras(String extras) {
			this.extras = extras;
		}
		public String toString(){
			Gson gson = new Gson();
			return gson.toJson(this);
		}
	}

}

