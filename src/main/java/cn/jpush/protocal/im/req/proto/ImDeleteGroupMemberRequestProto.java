package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import cn.jpush.protocal.im.bean.DeleteGroupMemberRequestBean;

public class ImDeleteGroupMemberRequestProto extends BaseProtobufRequest {
	private long rid;
	public ImDeleteGroupMemberRequestProto(int cmd, int version, long uid,
			String appkey, long rid, int sid, long juid, List cookie, Object bean) {
		super(cmd, version, uid, appkey, sid, juid, cookie, bean);
		this.rid = rid;
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		DeleteGroupMemberRequestBean bean = (DeleteGroupMemberRequestBean) obj;
		JpushimSdk2B.DelGroupMember.Builder delGroupMemberBuilder = JpushimSdk2B.DelGroupMember.newBuilder();
		delGroupMemberBuilder.setGid(bean.getGid());
		delGroupMemberBuilder.setMemberCount(bean.getMember_count());
		List memList = bean.getMember_uid_list();
		for(int i=0; i<memList.size(); i++){
			delGroupMemberBuilder.addMemberUidlist((Long)memList.get(i));
		}
		bodyBuilder.setDelGroupMember(delGroupMemberBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

	public long getRid() {
		return rid;
	}

	public void setRid(long rid) {
		this.rid = rid;
	}

}
