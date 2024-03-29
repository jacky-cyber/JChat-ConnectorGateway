package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import cn.jpush.protocal.im.bean.LogoutRequestBean;

import com.google.protobuf.ByteString;
/**
 * IM 用户登出请求 protobuf 封装
 * protobuf 定义请参考wiki文档
 */
public class ImLogoutRequestProto extends BaseProtobufRequest {
	private long rid;
	public ImLogoutRequestProto(int cmd, int version, long uid, String appkey,
			int sid, long juid, long rid, List cookie, Object bean) {
		super(cmd, version, uid, appkey, sid, juid, cookie, bean);
		this.rid = rid;
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		LogoutRequestBean bean = (LogoutRequestBean) obj;
		JpushimSdk2B.Logout.Builder logoutBuilder = JpushimSdk2B.Logout.newBuilder();
		logoutBuilder.setUsername(ByteString.copyFromUtf8(bean.getUsername()));
		bodyBuilder.setLogout(logoutBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

	public long getRid() {
		return rid;
	}

	public void setRid(long rid) {
		this.rid = rid;
	}

}
