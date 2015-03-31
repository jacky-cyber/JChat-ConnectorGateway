package cn.jpush.webim.socketio.bean;

import java.util.List;

public class UdpRespBean {
	private List<String> ips;
	private List<String> ssl_ips;
	private List<String> op_conns;
	private List<String> ssl_op_conns;
	private List<String> udp_report;
	private String user;
	public List<String> getIps() {
		return ips;
	}
	public void setIps(List<String> ips) {
		this.ips = ips;
	}
	public List<String> getSsl_ips() {
		return ssl_ips;
	}
	public void setSsl_ips(List<String> ssl_ips) {
		this.ssl_ips = ssl_ips;
	}
	public List<String> getOp_conns() {
		return op_conns;
	}
	public void setOp_conns(List<String> op_conns) {
		this.op_conns = op_conns;
	}
	public List<String> getSsl_op_conns() {
		return ssl_op_conns;
	}
	public void setSsl_op_conns(List<String> ssl_op_conns) {
		this.ssl_op_conns = ssl_op_conns;
	}
	public List<String> getUdp_report() {
		return udp_report;
	}
	public void setUdp_report(List<String> udp_report) {
		this.udp_report = udp_report;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	
}
