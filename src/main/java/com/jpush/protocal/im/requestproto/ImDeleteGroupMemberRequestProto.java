package com.jpush.protocal.im.requestproto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;

import com.jpush.protocal.im.bean.DeleteGroupMemberRequestBean;

public class ImDeleteGroupMemberRequestProto extends BaseProtobufRequest {
	public ImDeleteGroupMemberRequestProto(int cmd, int version, long uid,
			List cookie, Object bean) {
		super(cmd, version, uid, cookie, bean);
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

}
