package com.jpush.protocal.decoder;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import jpushim.s2b.JpushimSdk2B.Packet;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

import com.jpush.protocal.push.JHead;
import com.jpush.protocal.push.PushLoginRequestBean;
import com.jpush.protocal.push.PushRegRequestBean;
import com.jpush.protocal.utils.Command;
import com.jpush.protocal.utils.ProtocolUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * IM 协议解码器 从字节流到对象
 */
public class ImProtocalServerDecoder extends ByteToMessageDecoder {
	private static Logger log = (Logger) LoggerFactory.getLogger(ImProtocalServerDecoder.class);
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		log.info("服务端开始decode......");
		while(in.isReadable()){
			int length = in.readableBytes();
			log.info("可读数据量为："+length);
			if(length<2){
				return;
			} else {
				byte[] len = in.readBytes(2).array();
				in.resetReaderIndex();  // reset readIndex 
				int pkg_len = ProtocolUtil.byteArrayToInt(len);
				log.info("数据包大小为： "+pkg_len);
				if(length<pkg_len){
					return;
				} else {
					int command = new JHead(in).getCommandInRequest();
					switch (command) {
						case Command.KKPUSH_REG.COMMAND:
							log.info("jpush reg request decoder...");
							PushRegRequestBean Regbean = ProtocolUtil.getPushRegRequestBean(in);
							out.add(Regbean);
							break;
							
						case Command.KKPUSH_LOGIN.COMMAND:
							log.info("jpush login request decoder...");
							PushLoginRequestBean Loginbean = ProtocolUtil.getPushLoginRequestBean(in);
							out.add(Loginbean);
							break;
							
						case Command.KKPUSH_LOGOUT.COMMAND:
							log.info("jpush logout request decoder...");
							int logoutCmd = Command.KKPUSH_LOGOUT.COMMAND;
							out.add(logoutCmd);
							break;
							
						case Command.KKPUSH_HEARTBEAT.COMMAND:
							log.info("jpush heart beat request decoder...");
							int heartBeatCmd = Command.KKPUSH_HEARTBEAT.COMMAND;
							out.add(heartBeatCmd);
							break;
							
						case Command.JPUSH_IM.COMMAND:
							log.info("im 业务请求....");
							Packet protocol = JpushimSdk2B.Packet.parseFrom(in.readBytes(pkg_len-24).array());
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

}
