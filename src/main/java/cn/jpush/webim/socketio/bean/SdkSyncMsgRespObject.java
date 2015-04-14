package cn.jpush.webim.socketio.bean;

public class SdkSyncMsgRespObject {
		private long messageId;
		private int iMsgType;
		private long from_uid;
		private long from_gid;
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
