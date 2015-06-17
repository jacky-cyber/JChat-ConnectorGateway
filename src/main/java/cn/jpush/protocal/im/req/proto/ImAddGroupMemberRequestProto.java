package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import cn.jpush.protocal.im.bean.AddGroupMemberRequestBean;

/**
 * IM 添加群成员请求 protobuf 封装
 * protobuf 定义请参考wiki文档
 */
public class ImAddGroupMemberRequestProto extends BaseProtobufRequest {
	private long rid;
	public ImAddGroupMemberRequestProto(int cmd, int version, long uid,
			String appkey, long rid, int sid, long juid, List cookie, Object bean) {
		super(cmd, version, uid, appkey, sid, juid, cookie, bean);
		this.rid = rid;
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

	public long getRid() {
		return rid;
	}

	public void setRid(long rid) {
		this.rid = rid;
	}

}
