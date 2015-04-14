package cn.jpush.webim.socketio.bean;

public class SdkSyncEventRespObject {
		private long event_id;
		private int event_type;
		private long from_uid;
		private long gid;
		
		public long getEvent_id() {
			return event_id;
		}
		public void setEvent_id(long event_id) {
			this.event_id = event_id;
		}
		public int getEvent_type() {
			return event_type;
		}
		public void setEvent_type(int event_type) {
			this.event_type = event_type;
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
