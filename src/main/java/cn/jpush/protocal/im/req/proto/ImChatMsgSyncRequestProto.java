package cn.jpush.protocal.im.req.proto;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import jpushim.s2b.JpushimSdk2B.ChatMsg;
import jpushim.s2b.JpushimSdk2B.ChatMsgSync;
import cn.jpush.protocal.im.bean.SendSingleMsgRequestBean;

import com.google.protobuf.ByteString;

public class ImChatMsgSyncRequestProto extends BaseProtobufRequest {

	public ImChatMsgSyncRequestProto(int cmd, int version, long uid,
			String appkey, int sid, long juid, List cookie, Object bean) {
		super(cmd, version, uid, appkey, sid, juid, cookie, bean);
	}
	
	@Override
	protected void buildBody(Object obj) {
		JpushimSdk2B.ProtocolBody.Builder bodyBuilder = JpushimSdk2B.ProtocolBody.newBuilder();
		ChatMsg bean = (ChatMsg) obj;
		ChatMsgSync.Builder chatMsgSync = ChatMsgSync.newBuilder();
		chatMsgSync.addChatMsg(bean);
		bodyBuilder.setChatMsg(chatMsgSync);
		protocalBuilder.setBody(bodyBuilder);	
	}

}
