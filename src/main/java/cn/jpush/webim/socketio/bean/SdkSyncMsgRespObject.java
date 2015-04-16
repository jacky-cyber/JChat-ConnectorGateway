package cn.jpush.webim.socketio.bean;

public class SdkSyncMsgRespObject {
		private long messageId;
		private int msgType;
		private long fromUid;
		private long fromGid;
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
		
}
