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
		public static final String MESSAGE_RECEIVED = "message.received";
		public static final String EVENT_RECEIVED = "event.received";
		public static final String MESSAGE_PUSH = "message.push";
		public static final String EVENT_PUSH = "event.push";
		public static final String GROUP_CREATE = "group.create";
		public static final String GROUPMEMBERS_ADD = "groupMembers.add";
		public static final String GROUPMEMBERS_REMOVE = "groupMembers.remove";
		public static final String GROUPINFO_GET = "groupInfo.get";
		public static final String GROUPINFO_UPDATE = "groupInfo.update";
		public static final String GROUP_EXIT = "group.exit";
		public static final String GROUPLIST_GET = "groupList.get";
	}
	
	public static class Error {
		public static final int SERVER_ERROR = 872000; // 服务端错误
		public static final int USER_NOT_LOGIN = 872001; // 用户未登陆
		public static final int ARGUMENTS_EXCEPTION = 872002; // 用户传入参数异常
		public static final int USER_LOGIN_EXCEPTION = 872003; // 登陆异常
		public static final int CONFIG_EXCEPTION = 872004; // 配置校验异常
		public static final int SIGNATURE_INVALID = 872005; // 签名失效
		public static final int REQUEST_TIMEOUT = 872006; // 请求超时
		public static final int CONNECTION_DISCONNECT = 872007; // 与 IM Server连接断开
		
		
		public static String getErrorMessage(int code){
			String msg = "server error";
			switch (code) {
				case Error.USER_NOT_LOGIN:
					msg = "user not login";
					break;
				case Error.ARGUMENTS_EXCEPTION:
					msg = "arguments exception";
					break;
				case Error.USER_LOGIN_EXCEPTION:
					msg = "user login exception";
					break;
				case Error.CONFIG_EXCEPTION:
					msg = "config exception";
					break;
				case Error.SIGNATURE_INVALID:
					msg = "signature invalid";
					break;
				case Error.REQUEST_TIMEOUT:
					msg = "request timeout";
					break;
				case Error.CONNECTION_DISCONNECT:
					msg = "connection disconnect";
					break;
				default:
					msg = "unkown error";
					break;
			}
			return msg;
		}
	}
	
}
