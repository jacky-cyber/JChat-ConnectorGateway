package com.jpush.webim.common;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

import io.netty.channel.Channel;

import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import ch.qos.logback.classic.Logger;

import com.jpush.protocal.common.JPushTcpClient;
import com.jpush.protocal.push.PushRegRequestBean;

/*
 * Uid pool
 */

public class UidPool {
	private static Logger log = (Logger) LoggerFactory.getLogger(UidPool.class);
	private static final int DEFAULT_CAPACITY = 10;
	private static final float DEFAULR_LOAD_FACTOR = 0.8f;
	private static RedisClient redisClient = new RedisClient();
	private static JPushTcpClient jpushClient;
	private static Channel channel;
	public static Semaphore semaphore = new Semaphore(1);  // 处理异步通信的顺序
	
	public static long getUid() throws InterruptedException{
		Jedis jedis = null;
		String uid = "";
		try{
			jedis = redisClient.getJeids();
			Long currentSize = jedis.llen("im_uid_pool");
			if(currentSize <(DEFAULT_CAPACITY-DEFAULT_CAPACITY*DEFAULR_LOAD_FACTOR)||currentSize==0){
				log.info("limit resource, now produce again.");
				semaphore.acquire();
				produceResource();
				log.info("wait the pool is added data now.");
				semaphore.acquire();
				channel.close();  // 关闭获取 uid 的channel的连接
			}
			uid = jedis.lpop("im_uid_pool");
			log.info("get data from pool, uid: "+uid);
		} catch (JedisConnectionException e) {
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		return Long.parseLong(uid);
	}
	
	public static void addUidToPool(long uid){
		Jedis jedis = null;
		try{
			jedis = redisClient.getJeids();
			log.info("add data to uid pool, uid: "+uid);
			jedis.lpush("im_uid_pool", String.valueOf(uid));
		} catch (JedisConnectionException e) {
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
	}
	
	/*
	 * 申请 uid 资源
	 */
	private static void produceResource(){
		log.info("produce uid pool resources.");
		jpushClient = new JPushTcpClient();
		channel = jpushClient.getChannel();
		// send jpush reg quest batch
		for(int i=0; i<DEFAULT_CAPACITY; i++){
			PushRegRequestBean request = new PushRegRequestBean("ssfe", "1.0.1", "web im", "service token232", 1, 2, 5, "null ext");
			channel.writeAndFlush(request);
		}
		/*try {
			channel.close().sync();  // 直接关闭会收不到返回包
		} catch (InterruptedException e) {
			log.info("close channel exception: "+e.getMessage());
		}*/
	}
	
	
}
