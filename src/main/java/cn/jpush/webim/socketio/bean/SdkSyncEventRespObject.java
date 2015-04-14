package cn.jpush.webim.socketio.bean;

public class SdkSyncEventRespObject {
		private long eventId;
		private int eventType;
		private long from_uid;
		private long gid;
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
		public long getFrom_uid() {
			return from_uid;
		}
		public void setFrom_uid(long from_uid) {
			this.from_uid = from_uid;
		}
		public long getGid() {
			return gid;
		}
		public void setGid(long gid) {
			this.gid = gid;
		}
		
}
