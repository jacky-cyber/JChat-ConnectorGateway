package cn.jpush.webim.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import io.netty.channel.Channel;

import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import ch.qos.logback.classic.Logger;
import cn.jpush.protocal.common.JPushTcpClient;
import cn.jpush.protocal.push.PushRegRequestBean;
import cn.jpush.protocal.utils.StringUtils;

/*
 * Uid 资源池
 */

public class UidResourcesPool {
	private static Logger log = (Logger) LoggerFactory.getLogger(UidResourcesPool.class);
	public static final int DEFAULT_CAPACITY = 50;  //  池容量
	private static final float DEFAULR_LOAD_FACTOR = 0.7f;  //  负载因子
	private static RedisClient redisClient = new RedisClient();
	public static Semaphore produceResourceSemaphore = new Semaphore(1);  // 互斥产生资源线程
	private static Semaphore getUidSemaphore = new Semaphore(1);  // 互斥获取数据线程
	public static CountDownLatch capacityCountDown;  //  生成数量计数
	private static ExecutorService executor;
	
	/*
	 * 从池中获取uid
	 */
	public static Map<String, String> getUidAndPassword() throws InterruptedException{
		Jedis jedis = null;
		String uid = "";
		Map<String, String> data = new HashMap<String, String>();
		try{
			jedis = redisClient.getJeids();
			getUidSemaphore.acquire();  //  只允许一个线程进入
			Long currentSize = jedis.llen("im_uid_pool");
			if(currentSize>0){  // 有资源可供使用
				uid = jedis.lpop("im_uid_pool");
				List<String> list = jedis.hmget("im_uid_"+uid, "password");
				String password = list.get(0);
				data.put("uid", uid);
				data.put("password", password);
				jedis.del("im_uid_"+uid);
				log.info("get data from pool, uid: "+uid+", pwd: "+password);
			}
			if(currentSize <(DEFAULT_CAPACITY-DEFAULT_CAPACITY*DEFAULR_LOAD_FACTOR)){  // 资源有限，需要生成
				log.error("limit resource, now produce uid.");
				if(produceResourceSemaphore.availablePermits()>0){
					produceResource();  //  开线程申请资源
				} else {
					log.error("已有线程在产生资源");
				}
			}
			if(currentSize==0){  // 资源已消耗完
				if(produceResourceSemaphore.availablePermits()>0){
					produceResource();  //  开线程申请资源
				} else {
					while(true){
						if(capacityCountDown.getCount()<DEFAULT_CAPACITY){
							uid = jedis.lpop("im_uid_pool");
							List<String> list = jedis.hmget("im_uid_"+uid, "password");
							String password = list.get(0);
							data.put("uid", String.valueOf(uid));
							data.put("password", password);
							jedis.del("im_uid_"+uid);
							break;
						}
					}
				}
			}
		} catch (JedisConnectionException e) {
			log.error(e.getMessage());
			redisClient.returnBrokenResource(jedis);
			throw new JedisConnectionException(e);
		} finally {
			redisClient.returnResource(jedis);
		}
		getUidSemaphore.release();  //  释放锁，允许其他线程获取资源
		return data;
	}
	
	public static void addUidToPool(long uid, String password){
		Jedis jedis = null;
		try{
			jedis = redisClient.getJeids();
			log.info("add data to uid pool, uid: "+uid);
			jedis.lpush("im_uid_pool", String.valueOf(uid));
			Map<String, String> data = new HashMap<String, String>();
			data.put("password", password);
			jedis.hmset("im_uid_"+uid, data);
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
		try {
			executor = Executors.newSingleThreadExecutor();
			capacityCountDown = new CountDownLatch(DEFAULT_CAPACITY);
			produceResourceSemaphore.acquire();
		} catch (InterruptedException e) {
			log.info(e.getMessage());
		}
		executor.submit(new ProduceUidResourcesThread()); 
	}
	
}

class ProduceUidResourcesThread implements Runnable{
	private static Logger log = (Logger) LoggerFactory.getLogger(ProduceUidResourcesThread.class);
	private JPushTcpClient jpushClient;
	private Channel channel;
	@Override
	public void run() {
		log.info("produce uid pool resources.");
		jpushClient = new JPushTcpClient("ebbd49c14a649e0fa4f01f3f");
		channel = jpushClient.getChannel();
		for(int i=0; i<UidResourcesPool.DEFAULT_CAPACITY; i++){
			String imei = StringUtils.getIntRandom(15);
			String imsi = StringUtils.getIntRandom(15);
			String deviceId = StringUtils.getStringRandom(32);
			String arg2 = imei+"$$"+imsi+"$$com.android.mypushdemo180src$$ebbd49c14a649e0fa4f01f3f";
			String arg3 = "1$$"+deviceId+"$$00000000$$b095c7a18792bd8b$$CC:3A:61:BD:CB:3D";
			PushRegRequestBean request = new PushRegRequestBean(arg2,
					"1.8.0", "4.4.2,19$$SCH-I959$$I959KEUHND6$$ja3gduosctc$$developer-default$$1.8.0$$0$$1080*1920", 
					"", 0, 0, 0, arg3);
			channel.writeAndFlush(request);
		}
	}
	
}

