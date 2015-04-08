package cn.jpush.webim.socketio.bean;

public class EventSyncRespBean {
	private String appKey;
	private long uid;
	private long juid;
	private int sid;
	private long rid;
	private long eventId;
	private int eventType;
	private long from_uid;
	private long gid;
	public String getAppKey() {
		return appKey;
	}
	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	
	public long getFrom_uid() {
		return from_uid;
	}
	public void setFrom_uid(long from_uid) {
		this.from_uid = from_uid;
	}
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	public long getJuid() {
		return juid;
	}
	public void setJuid(long juid) {
		this.juid = juid;
	}
	public int getSid() {
		return sid;
	}
	public void setSid(int sid) {
		this.sid = sid;
	}
	
	public long getRid() {
		return rid;
	}
	public void setRid(long rid) {
		this.rid = rid;
	}
	public long getEventId() {
		return eventId;
	}
	public void setEventId(long eventId) {
		this.eventId = eventId;
	}
	public int getEventType() {
		return eventType;
	}
	public void setEventType(int eventType) {
		this.eventType = eventType;
	}
	
}
