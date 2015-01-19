package com.jpush.protocal.encoder;

import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.google.gson.Gson;
import com.jpush.protobuf.Im;
import com.jpush.protobuf.Im.Protocol;
import com.jpush.protocal.decoder.ImProtocalServerEncoder;
import com.jpush.protocal.push.JHead;
import com.jpush.protocal.push.PushLoginResponseBean;
import com.jpush.protocal.push.PushLogoutResponseBean;
import com.jpush.protocal.push.PushMessageRequestBean;
import com.jpush.protocal.push.PushRegResponseBean;
import com.jpush.protocal.utils.Command;
import com.jpush.protocal.utils.ProtocolUtil;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class ImProtocalClientDecoder extends ByteToMessageDecoder {
	private static Logger log = (Logger) LoggerFactory.getLogger(ImProtocalClientDecoder.class);
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		log.info("客户端接收数据开始decode......");
		int length = in.readableBytes();
		if(length<2){
			return;
		} else {
			byte[] len = in.readBytes(2).array();
			in.resetReaderIndex();  // reset readIndex 
			int pkg_len = ProtocolUtil.byteArrayToInt(len);
			log.info("客户端接收数据包大小为： "+pkg_len);
			if(length<pkg_len){
				return;
			} else {
				int command = new JHead(in).getCommandInResponse();
				switch (command) {
					case Command.KKPUSH_REG.COMMAND:
						log.info("receive push reg response");
						PushRegResponseBean Regbean = ProtocolUtil.getPushRegResponseBean(in);
						out.add(Regbean);
						break;
					
					case Command.KKPUSH_LOGIN.COMMAND:
						log.info("receive push login response");
						PushLoginResponseBean Loginbean = ProtocolUtil.getPushLoginResponseBean(in);
						out.add(Loginbean);
						break;
						
					case Command.KKPUSH_LOGOUT.COMMAND:
						log.info("receive push logout response");
						PushLogoutResponseBean bean = ProtocolUtil.getPushLogoutResponseBean(in);
						out.add(bean);
						break;
					
					case Command.KKPUSH_HEARTBEAT.COMMAND:
						log.info("receive push heartbeat request");
						break;
					
					case Command.KKPUSH_MESSAGE.COMMAND:
						log.info("receive im message(througn jpush message) request");
						PushMessageRequestBean Messagebean = ProtocolUtil.getPushMessageRequestBean(in);
						out.add(Messagebean);
						break;
						
					case Command.JPUSH_IM.COMMAND:
						log.info("im 业务响应....");
						Protocol protocol = Im.Protocol.parseFrom(in.readBytes(pkg_len-20).array());
						out.add(protocol);
						break;
						
					default:
						log.info("未定义的消息类型.");
						break;
				}
			
			}
		}
	}

}
