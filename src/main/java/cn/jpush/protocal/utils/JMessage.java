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
	
	public static class Error {
		public static final int SERVER_ERROR = 872000; //服务端错误
		public static final int USER_NOT_LOGIN = 872001; //用户未登陆
		public static final int ARGUMENTS_EXCEPTION = 872002; //用户传入参数异常
		
		public static String getErrorMessage(int code){
			String msg = "server error";
			switch (code) {
				case Error.USER_NOT_LOGIN:
					msg = "user not login";
					break;
				case Error.ARGUMENTS_EXCEPTION:
					msg = "arguments exception";
					break;
				default:
					msg = "unkown error";
					break;
			}
			return msg;
		}
	}
	
}
