package cn.jpush.webim.socketio.bean;

public class SdkSyncMsgObject {
	private long messageId;
	private int iMsgType;
	private long fromUid;
	private long fromGid;
	private int version;
	private String targetType;
	private String targetId;
	private String targetName;
	private String fromType;
	private String fromId;
	private String fromName;
	private int createTime;
	private String msgType;
	private String msgBody;
	public long getMessageId() {
		return messageId;
	}
	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}
	public int getiMsgType() {
		return iMsgType;
	}
	public void setiMsgType(int iMsgType) {
		this.iMsgType = iMsgType;
	}
	public long getFromUid() {
		return fromUid;
	}
	public void setFromUid(long fromUid) {
		this.fromUid = fromUid;
	}
	public long getFromGid() {
		return fromGid;
	}
	public void setFromGid(long fromGid) {
		this.fromGid = fromGid;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public String getTargetType() {
		return targetType;
	}
	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}
	public String getTargetId() {
		return targetId;
	}
	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}
	public String getTargetName() {
		return targetName;
	}
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}
	public String getFromType() {
		return fromType;
	}
	public void setFromType(String fromType) {
		this.fromType = fromType;
	}
	public String getFromId() {
		return fromId;
	}
	public void setFromId(String fromId) {
		this.fromId = fromId;
	}
	public String getFromName() {
		return fromName;
	}
	public void setFromName(String fromName) {
		this.fromName = fromName;
	}
	public int getCreateTime() {
		return createTime;
	}
	public void setCreateTime(int createTime) {
		this.createTime = createTime;
	}
	public String getMsgType() {
		return msgType;
	}
	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}
	public String getMsgBody() {
		return msgBody;
	}
	public void setMsgBody(String msgBody) {
		this.msgBody = msgBody;
	}
	
	
}
