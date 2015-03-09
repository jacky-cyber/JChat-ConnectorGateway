package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import cn.jpush.protocal.im.bean.AddGroupMemberRequestBean;

public class ImAddGroupMemberRequestProto extends BaseProtobufRequest {
	private int sid;
	private long juid;
	public ImAddGroupMemberRequestProto(int cmd, int version, long uid,
			String appkey, int sid, long juid, List cookie, Object bean) {
		super(cmd, version, uid, appkey, cookie, bean);
		this.sid = sid;
		this.juid = juid;
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		AddGroupMemberRequestBean bean = (AddGroupMemberRequestBean) obj;
		JpushimSdk2B.AddGroupMember.Builder addGroupMemberBuilder = JpushimSdk2B.AddGroupMember.newBuilder();
		addGroupMemberBuilder.setGid(bean.getGid());
		addGroupMemberBuilder.setMemberCount(bean.getMember_count());
		List memList = bean.getMember_uid_list();
		for(int i=0; i<memList.size(); i++){
			addGroupMemberBuilder.addMemberUidlist((Long)memList.get(i));
		}
		bodyBuilder.setAddGroupMember(addGroupMemberBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

	public int getSid() {
		return sid;
	}

	public void setSid(int sid) {
		this.sid = sid;
	}

	public long getJuid() {
		return juid;
	}

	public void setJuid(long juid) {
		this.juid = juid;
	}

}
