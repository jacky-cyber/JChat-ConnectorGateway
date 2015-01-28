package cn.jpush.protocal.im.bean;


public class LogoutRequestBean{	
	private String username;
	public LogoutRequestBean(){}
	public LogoutRequestBean(String username){
		this.username = username;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

}
