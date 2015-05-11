package cn.jpush.webim.socketio.bean;

import java.util.ArrayList;

public class SdkSyncEventObject {
	private long eventId;
	private String eventType;
	private int iEventType;
	private long fromUid;
   private String fromUsername;
   private long gid;
   private ArrayList<String> toUsernameList;
   private String description;
	public long getEventId() {
		return eventId;
	}
	public void setEventId(long eventId) {
		this.eventId = eventId;
	}
	public String getEventType() {
		return eventType;
	}
	public void setEventType(String eventType) {
		this.eventType = eventType;
	}
	public int getiEventType() {
		return iEventType;
	}
	public void setiEventType(int iEventType) {
		this.iEventType = iEventType;
	}
	public long getFromUid() {
		return fromUid;
	}
	public void setFromUid(long fromUid) {
		this.fromUid = fromUid;
	}
	public String getFromUsername() {
		return fromUsername;
	}
	public void setFromUsername(String fromUsername) {
		this.fromUsername = fromUsername;
	}
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	public ArrayList<String> getToUsernameList() {
		return toUsernameList;
	}
	public void setToUsernameList(ArrayList<String> toUsernameList) {
		this.toUsernameList = toUsernameList;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
}
