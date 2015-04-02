package cn.jpush.webim.socketio.bean;

import jpushim.s2b.JpushimSdk2B.Packet;

public class IMPacket {
	private long rid;
	private Packet packet;
	public IMPacket(long rid, Packet packet){
		this.rid = rid;
		this.packet = packet;
	}
	public long getRid() {
		return rid;
	}
	public void setRid(long rid) {
		this.rid = rid;
	}
	public Packet getPacket() {
		return packet;
	}
	public void setPacket(Packet packet) {
		this.packet = packet;
	}
	
}
