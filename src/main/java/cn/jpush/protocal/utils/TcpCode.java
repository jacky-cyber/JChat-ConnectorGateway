package cn.jpush.protocal.utils;

public interface TcpCode {
	
	public interface PUSH {
		public static final int SUCCESS = 0;
	}
	
	public interface IM {
		public static final int SUCCESS = 0;
		public static final int LOGIN_UNLEAGAL_PASSWORD = 801004;
		public static final int ADDGROUP_USER_UNEXIST = 810005;
		public static final int ADDGROUP_USER_REPEATADD = 810007;
	}
}
	
