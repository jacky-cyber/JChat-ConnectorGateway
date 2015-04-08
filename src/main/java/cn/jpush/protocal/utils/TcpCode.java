package cn.jpush.protocal.utils;

public interface TcpCode {
	
	public interface PUSH {
		public static final int SUCCESS = 0;
	}
	
	public interface IM {
		public static final int SUCCESS = 0;
		public static final int USER_UNEXIT = 801003;
		public static final int LOGIN_UNLEAGAL_PASSWORD = 801004;
		public static final int USERNAME_WRONG = 802002;
		public static final int ADDGROUP_USER_UNEXIST = 810005;
		public static final int ADDGROUP_USER_REPEATADD = 810007;
		public static final int GROUP_NAME_NULL = 808001;
		public static final int NO_PERMISSION_CREATE_GROUP = 808002;
		public static final int USER_CREATE_GROUP_TOOMUCH = 808003;
		public static final int GROUP_NAME_TOO_LONG = 808004;
		public static final int GROUP_DESC_TOO_LONG = 808005;
	}
}
	
