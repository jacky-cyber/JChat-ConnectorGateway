package cn.jpush.webim.socketio.bean;

import java.util.ArrayList;

public class SdkSyncEventObject {
	private long event_id;
	private String event_type;
	private int i_event_type;
	private long from_uid;
   private String from_username;
   private long gid;
   private ArrayList<String> to_username_list;
   private String description;
	public String getEvent_type() {
		return event_type;
	}
	public void setEvent_type(String event_type) {
		this.event_type = event_type;
	}
	public String getFrom_username() {
		return from_username;
	}
	public void setFrom_username(String from_username) {
		this.from_username = from_username;
	}
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	public ArrayList<String> getTo_username_list() {
		return to_username_list;
	}
	public void setTo_username_list(ArrayList<String> to_username_list) {
		this.to_username_list = to_username_list;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public long getEvent_id() {
		return event_id;
	}
	public void setEvent_id(long event_id) {
		this.event_id = event_id;
	}
	public long getFrom_uid() {
		return from_uid;
	}
	public void setFrom_uid(long from_uid) {
		this.from_uid = from_uid;
	}
	public int getI_event_type() {
		return i_event_type;
	}
	public void setI_event_type(int i_event_type) {
		this.i_event_type = i_event_type;
	}
   
}
