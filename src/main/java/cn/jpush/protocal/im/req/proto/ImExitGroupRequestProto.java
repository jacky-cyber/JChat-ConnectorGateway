package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import cn.jpush.protocal.im.bean.ExitGroupRequestBean;

public class ImExitGroupRequestProto extends BaseProtobufRequest {
	private long rid;
	public ImExitGroupRequestProto(int cmd, int version, long uid,
			String appkey, long rid, int sid, long juid, List cookie, Object bean) {
		super(cmd, version, uid, appkey, sid, juid, cookie, bean);
		this.rid = rid;
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		ExitGroupRequestBean bean = (ExitGroupRequestBean) obj;
		JpushimSdk2B.ExitGroup.Builder exitGroupBuilder = JpushimSdk2B.ExitGroup.newBuilder();
		exitGroupBuilder.setGid(bean.getGid()); 
		bodyBuilder.setExitGroup(exitGroupBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

	public long getRid() {
		return rid;
	}

	public void setRid(long rid) {
		this.rid = rid;
	}

}
