package cn.jpush.webim.socketio.bean;

public class SdkSyncEventRespObject {
		private long eventId;
		private int eventType;
		private long fromUid;
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
		public long getFromUid() {
			return fromUid;
		}
		public void setFromUid(long fromUid) {
			this.fromUid = fromUid;
		}
		public long getGid() {
			return gid;
		}
		public void setGid(long gid) {
			this.gid = gid;
		}
		
}
