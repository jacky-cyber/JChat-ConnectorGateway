package cn.jpush.webim.socketio.bean;

import java.util.List;

public class SdkRequesrParamsObject {
	private String appKey;
	private String timestamp;
	private String randomStr;
	private String signature;

	private String username;
	private String password;
	
	private String targetId;
	private String targetType;
	private String text;
	private String fileId;
	
	private String groupName;
	private String groupDescription;
	
	private long groupId;
	private List<String> memberUsernames;
	
	private long messageId;
  	private int msgType;
  	private long fromUid;
  	private long fromGid;
  	
  	private long eventId;
  	private int eventType;
  	private long gid;
	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getRandomStr() {
		return randomStr;
	}

	public void setRandomStr(String randomStr) {
		this.randomStr = randomStr;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getGroupDescription() {
		return groupDescription;
	}

	public void setGroupDescription(String groupDescription) {
		this.groupDescription = groupDescription;
	}

	public long getGroupId() {
		return groupId;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}

	public List<String> getMemberUsernames() {
		return memberUsernames;
	}

	public void setMemberUsernames(List<String> memberUsernames) {
		this.memberUsernames = memberUsernames;
	}

	public long getMessageId() {
		return messageId;
	}

	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}

	public int getMsgType() {
		return msgType;
	}

	public void setMsgType(int msgType) {
		this.msgType = msgType;
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

	public long getGid() {
		return gid;
	}

	public void setGid(long gid) {
		this.gid = gid;
	}

}
