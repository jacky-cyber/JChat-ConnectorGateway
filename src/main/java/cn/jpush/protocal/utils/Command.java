package cn.jpush.protocal.utils;


public interface Command {

	public static final String ENCODING_UTF_8 = "UTF-8";

	public interface DEVICE_TYPE{
		public static final int ANDROID = 1;
		public static final int IOS = 2;
		public static final int WINPHONE = 4;
	}

	public interface KKPUSH_REG {
		public static final int COMMAND = 0;  //注册
	}
	
	public interface KKPUSH_LOGIN {
		public static final int COMMAND = 1;  //登陆
	}

	public interface KKPUSH_HEARTBEAT {
		public static final int COMMAND = 2;  //心跳
	}
	
	public interface KKPUSH_MESSAGE {
		public static final int COMMAND = 3;  //P2P消息
	}
	
	public interface KKPUSH_MESSAGE_RESP {
		public static final int COMMAND = 4;  //P2P消息应答
	}

	public interface KKPUSH_LOGOUT {
		public static final int COMMAND = 5;  //登出
	}
	
	public interface KKPUSH_SET_TAG {
		public static final int COMMAND = 10;  //设置tag、alias
	}

	public interface KKPUSH_DEVICETOKEN_REPORT {
		public static final int COMMAND = 13;   // 上报device token
	}
	
	public interface KKPUSH_GET_MSG {
		public static final int COMMAND = 18;  //获取离线消息（立即）
	}
	
	public interface JPUSH_ACK_RESP {
		public static final int COMMAND = 19;  //协议消息响应
	}

	public interface JPUSH_IM {
		public static final int COMMAND = 100;  //IM业务相关消息
		
		public static final int LOGIN = 1;  //登陆 
		public static final int LOGOUT = 2; //登出 
		public static final int SENDMSG_SINGAL = 3; //发消息 单聊
		public static final int SENDMSG_GROUP = 4; //发消息 群聊
		public static final int ADD_FRIEND = 5; // 添加好友
		public static final int DEL_FRIEND = 6; //删除好友
		public static final int UPDATE_MEMO = 7; //修改好友备注
		public static final int CREATE_GROUP = 8; //创建群
		public static final int EXIT_GROUP = 9;  //退出群
		public static final int ADD_GROUP_MEMBER = 10; //添加群组成员
		public static final int DEL_GROUP_MEMBER = 11; //删除群组成员
		public static final int UPDATE_GROUP_INFO = 12; //修改群组详情
		public static final int SYNC = 13; //IM业务同步信息
		
	}
	
}