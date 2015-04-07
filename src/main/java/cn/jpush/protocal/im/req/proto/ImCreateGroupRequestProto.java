package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import cn.jpush.protocal.im.bean.CreateGroupRequestBean;

import com.google.protobuf.ByteString;

public class ImCreateGroupRequestProto extends BaseProtobufRequest {

	public ImCreateGroupRequestProto(int cmd, int version, long uid,
			String appkey, int sid, long juid, List cookie, Object bean) {
		super(cmd, version, uid, appkey, sid, juid, cookie, bean);
	}

	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		CreateGroupRequestBean bean = (CreateGroupRequestBean) obj;
		JpushimSdk2B.CreateGroup.Builder createGroupBuilder = JpushimSdk2B.CreateGroup.newBuilder();
		createGroupBuilder.setGroupName(ByteString.copyFromUtf8(bean.getGroup_name()));
		createGroupBuilder.setGroupDesc(ByteString.copyFromUtf8(bean.getGroup_desc()));
		createGroupBuilder.setGroupLevel(bean.getGroup_level());
		createGroupBuilder.setFlag(bean.getFlag());
		bodyBuilder.setCreateGroup(createGroupBuilder);
		protocalBuilder.setBody(bodyBuilder);
	}

}
