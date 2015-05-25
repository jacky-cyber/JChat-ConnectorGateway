package cn.jpush.protocal.common;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.im.request.SISRequest;
import cn.jpush.protocal.push.PushLoginResponseBean;
import cn.jpush.protocal.utils.SystemConfig;
import cn.jpush.webim.socketio.bean.UdpRespBean;

public class JPushUdpClient {
	private static Logger log = (Logger) LoggerFactory.getLogger(JPushUdpClient.class);
	
	public static UdpRespBean sendSISRequest(String appkey, String host, int port) throws Exception{
		DatagramSocket client = new DatagramSocket();   
		client.setSoTimeout(10000);
		SISRequest request = new SISRequest("1.8.0", "WIFI", 1, appkey, 0);
	   byte[] sendBuf = request.getRequestPackage();
	   InetAddress addr = InetAddress.getByName(host);
	   DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, addr, port);
	   log.info("begin send sis request");
	   client.send(sendPacket);
	   byte[] recvBuf = new byte[1024];
	   DatagramPacket recvPacket = new DatagramPacket(recvBuf , recvBuf.length);
	   client.receive(recvPacket);
	   String recvStr = new String(recvPacket.getData() , 0 ,recvPacket.getLength());
	   log.info(String.format("get sis response data: %s", recvStr));
	   Gson gson = new Gson();
	   UdpRespBean bean = gson.fromJson(recvStr, UdpRespBean.class);
	   client.close();
		return bean;
	}
	
	public static UdpRespBean getSISRespBean(String appkey){
		UdpRespBean bean = null;
		for(int i=1; i<3; i++){
			String portProperty = "sis.http.port"+i;
			for(int j=1; j<5; j++){
				String hostProperty = "sis.http.url"+j;
				String host = SystemConfig.getProperty(hostProperty);
				int port = SystemConfig.getIntProperty(portProperty);
				log.info(String.format("SIS pull data to: %s -- %s",  host, port));
				try {
					bean = JPushUdpClient.sendSISRequest(appkey, host, port);
					return bean;
				} catch (Exception e) {
					log.warn(String.format("SIS Request to: %s -- %s Exception: %s", host, port, e.getMessage()));
				}
			}
		}
		return bean;
	}
	
	public static void main(String[] args){
		UdpRespBean bean = JPushUdpClient.getSISRespBean("4f7aef34fb361292c566a1cd");
		List<String> list = bean.getIps();
		for(String str: list){
			log.info("ips: "+str);
		}
	}
	
}
