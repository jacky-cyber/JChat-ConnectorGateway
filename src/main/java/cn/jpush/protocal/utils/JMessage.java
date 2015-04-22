package cn.jpush.protocal.utils;

public class JMessage {
	public interface Method {
		public static final String CONNECT = "connect";
		public static final String DISCONNECT = "disconnect";
		public static final String CONFIG = "config";
		public static final String LOGIN = "login";
		public static final String LOGOUT = "logout";
		public static final String USERINFO_GET = "userinfo.get";
		public static final String TEXTMESSAGE_SEND = "textMessage.send";
		public static final String IMAGEMESSAGE_SEND = "imageMessage.send";
		public static final String MESSAGE_FEEDBACK = "message.feedback";
		public static final String EVENT_FEEDBACK = "event.feedback";
		public static final String MESSAGE_RECEIVE = "message.receive";
		public static final String EVENT_RECEIVE = "event.receive";
		public static final String GROUP_CREATE = "group.create";
		public static final String GROUPMEMBERS_ADD = "groupMembers.add";
		public static final String GROUPMEMBERS_REMOVE = "groupMembers.remove";
		public static final String GROUPINFO_GET = "groupInfo.get";
		public static final String GROUPINFO_UPDATE = "groupInfo.update";
		public static final String GROUP_EXIT = "group.exit";
		public static final String GROUPLIST_GET = "groupList.get";
	}
}
