package cn.jpush.protocal.im.requestproto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import cn.jpush.protocal.im.bean.AddGroupMemberRequestBean;

public class ImAddGroupMemberRequestProto extends BaseProtobufRequest {


	public ImAddGroupMemberRequestProto(int cmd, int version, long uid,
			String appkey, List cookie, Object bean) {
		super(cmd, version, uid, appkey, cookie, bean);
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

}
