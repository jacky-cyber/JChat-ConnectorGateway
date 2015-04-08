package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import cn.jpush.protocal.im.bean.UpdateGroupInfoRequestBean;

import com.google.protobuf.ByteString;

public class ImUpdateGroupInfoRequestProto extends BaseProtobufRequest {
	private long rid;
	public ImUpdateGroupInfoRequestProto(int cmd, int version, long uid,
			String appkey, long rid, int sid, long juid, List cookie, Object bean) {
		super(cmd, version, uid, appkey, sid, juid, cookie, bean);
		this.rid = rid;
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		UpdateGroupInfoRequestBean bean = (UpdateGroupInfoRequestBean) obj;
		JpushimSdk2B.UpdateGroupInfo.Builder updateGroupInfoMemberBuilder = JpushimSdk2B.UpdateGroupInfo.newBuilder();
		updateGroupInfoMemberBuilder.setGid(bean.getGid());
		updateGroupInfoMemberBuilder.setName(ByteString.copyFromUtf8(bean.getName()));
		updateGroupInfoMemberBuilder.setInfo(ByteString.copyFromUtf8(bean.getContent()));
		bodyBuilder.setUpdateGroupInfo(updateGroupInfoMemberBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

	public long getRid() {
		return rid;
	}

	public void setRid(long rid) {
		this.rid = rid;
	}

}
