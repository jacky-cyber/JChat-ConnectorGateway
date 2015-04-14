package cn.jpush.webim.socketio.bean;

public class SdkSyncMsgRespObject {
		private long message_id;
		private int msg_type;
		private long from_uid;
		private long from_gid;
		
		public long getMessage_id() {
			return message_id;
		}
		public void setMessage_id(long message_id) {
			this.message_id = message_id;
		}
		public int getMsg_type() {
			return msg_type;
		}
		public void setMsg_type(int msg_type) {
			this.msg_type = msg_type;
		}
		public long getFrom_uid() {
			return from_uid;
		}
		public void setFrom_uid(long from_uid) {
			this.from_uid = from_uid;
		}
		public long getFrom_gid() {
			return from_gid;
		}
		public void setFrom_gid(long from_gid) {
			this.from_gid = from_gid;
		}
		
}
