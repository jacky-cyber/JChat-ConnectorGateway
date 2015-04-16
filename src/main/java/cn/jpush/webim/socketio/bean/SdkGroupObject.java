package cn.jpush.webim.socketio.bean;

public class SdkGroupObject {
	private String groupName;
	private String groupDescription;
	private int groupLevel;
	private int flag;
	private long gid;
	
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public String getGroupDescription() {
		return groupDescription;
	}
	public void setGroupDescription(String groupDescription) {
		this.groupDescription = groupDescription;
	}
	public int getGroupLevel() {
		return groupLevel;
	}
	public void setGroupLevel(int groupLevel) {
		this.groupLevel = groupLevel;
	}
	public int getFlag() {
		return flag;
	}
	public void setFlag(int flag) {
		this.flag = flag;
	}
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	
}
