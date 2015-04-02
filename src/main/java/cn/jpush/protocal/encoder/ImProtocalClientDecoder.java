package cn.jpush.protocal.encoder;

import java.util.List;

import jpushim.s2b.JpushimSdk2B;
import jpushim.s2b.JpushimSdk2B.Packet;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.decoder.ImProtocalServerEncoder;
import cn.jpush.protocal.push.JHead;
import cn.jpush.protocal.push.PushLoginResponseBean;
import cn.jpush.protocal.push.PushLogoutResponseBean;
import cn.jpush.protocal.push.PushMessageRequestBean;
import cn.jpush.protocal.push.PushRegResponseBean;
import cn.jpush.protocal.utils.Command;
import cn.jpush.protocal.utils.ProtocolUtil;
import cn.jpush.webim.socketio.bean.IMPacket;

import com.google.gson.Gson;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class ImProtocalClientDecoder extends ByteToMessageDecoder {
	private static Logger log = (Logger) LoggerFactory.getLogger(ImProtocalClientDecoder.class);
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		log.info("client decode data from jpush server");
		int length = in.readableBytes();
		log.info(String.format("client decode readable bytes length is %d", length));
		if(length<2){
			return;
		} else {
			in.markReaderIndex();
			byte[] len = in.readBytes(2).array();
			in.resetReaderIndex();
			int pkg_len = ProtocolUtil.byteArrayToInt(len);
			log.info(String.format("client decode the data package length: %d", pkg_len));
//			in.markReaderIndex();
//			byte[] content = in.readBytes(pkg_len).array();
//			log.info("数据包内容： "+ProtocolUtil.byteToHexString(content));
//			in.resetReaderIndex();
			if(pkg_len<0||pkg_len==0){
				in.readBytes(2);
				return;
			}
			if(length<pkg_len){
				in.resetReaderIndex(); 
				return;
			} else {
				JHead head = new JHead(in);
				int command = head.getCommandInResponse();
				long rid = head.getRid();
				switch (command) {
					case Command.KKPUSH_REG.COMMAND:
						log.info("client decode recv JPUSH reg from jpush server");
						PushRegResponseBean Regbean = ProtocolUtil.getPushRegResponseBean(in);
						out.add(Regbean);
						break;
					
					case Command.KKPUSH_LOGIN.COMMAND:
						log.info("client decode recv JPUSH login from jpush server");
						PushLoginResponseBean Loginbean = ProtocolUtil.getPushLoginResponseBean(in);
						out.add(Loginbean);
						break;
						
					case Command.KKPUSH_LOGOUT.COMMAND:
						log.info("client decode recv JPUSH logout from jpush server");
						PushLogoutResponseBean bean = ProtocolUtil.getPushLogoutResponseBean(in);
						out.add(bean);
						break;
					
//					case Command.KKPUSH_HEARTBEAT.COMMAND:
//						log.info("client decode recv JPUSH heartbeat from jpush server");
//						break;
//					
//					case Command.KKPUSH_MESSAGE.COMMAND:
//						log.info("receive im message(througn jpush message) request");
//						PushMessageRequestBean Messagebean = ProtocolUtil.getPushMessageRequestBean(in);
//						out.add(Messagebean);
//						break;
						
					case Command.JPUSH_IM.COMMAND:
						log.info("client decode recv IM command from jpush server");
						in.readBytes(2);  // 有2B的body的长度
						Packet protocol = JpushimSdk2B.Packet.parseFrom(in.readBytes(pkg_len-22).array());
						IMPacket imPacket = new IMPacket(rid, protocol);  //  rid 用于标示消息
						out.add(imPacket);
						break;
						
					case Command.JPUSH_ACK_RESP.COMMAND:
						log.info("client decode recv JPUSH heartbeat ack resp from jpush server");
						in.readBytes(pkg_len-20);
						//String message = new String(in.readBytes(pkg_len-20).array(),"utf-8");
						break;
					default:
						log.warn("未定义的消息类型.");
						break;
				}
			
			}
		}
	}

}
