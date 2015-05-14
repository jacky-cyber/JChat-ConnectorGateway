package cn.jpush.webim.common;

import cn.jpush.protocal.utils.SystemConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;
public class RedisClient {
  private final  String BASE_URL = SystemConfig.getProperty("redis.server.host");
  private final  int PORT = SystemConfig.getIntProperty("redis.server.port");;
  public  int MAXTOTAL = 10000;
  public  int MAXIDL = 200;
  public  int MAXWAITMILLIS = 2000;

  public  JedisPool pool;
  public  ShardedJedisPool shardedJedisPool;

  public RedisClient() {
    initPool();
  }

  private void initPool() {
    if (pool == null) {
      JedisPoolConfig config = new JedisPoolConfig();
      config.setMaxTotal(MAXTOTAL);
      config.setMaxIdle(MAXIDL);
      config.setMaxWaitMillis(MAXWAITMILLIS);
      config.setTestOnBorrow(false);
      pool = new JedisPool(config, BASE_URL, PORT);
    }
  }

  /*
   * private void initSharedPool(){ // 池基本配置 JedisPoolConfig config = new JedisPoolConfig();
   * config.setMaxTotal(20); config.setMaxIdle(5); config.setMaxWaitMillis(1000l);
   * config.setTestOnBorrow(false); // slave链接 List<JedisShardInfo> shards = new
   * ArrayList<JedisShardInfo>(); shards.add(new JedisShardInfo(BASE_URL, PORT, "master")); // 构造池
   * shardedJedisPool = new ShardedJedisPool(config, shards); }
   */
  public  Jedis getJeids() {
    Jedis jedis = null;
    try {
      jedis = pool.getResource();
    } catch (JedisConnectionException e) {
      if (jedis != null) pool.returnBrokenResource(jedis);
      throw e;
    }
    return jedis;
  }

  public  ShardedJedis getShardedJedis() {
    ShardedJedis shardedJedis = null;
    try {
      shardedJedis = shardedJedisPool.getResource();
    } catch (JedisConnectionException e) {
      if (shardedJedis != null) shardedJedisPool.returnBrokenResource(shardedJedis);
    }
    shardedJedis = shardedJedisPool.getResource();
    return shardedJedis;
  }

  public  void returnResource(Jedis jedis) {
    if (jedis != null) {
      pool.returnResource(jedis);
    }
  }

  public  void returnBrokenResource(Jedis jedis) {
    if (jedis != null) {
      pool.returnBrokenResource(jedis);
    }
  }
  
  public  Long rpush(Jedis jedis, String key, String... strings) {
    Long count = 0L;
    try {
      count = jedis.rpush(key, strings);
    } catch (Exception e) {
      pool.returnBrokenResource(jedis);
      throw new JedisConnectionException(e);
    }
    return count;
  }
  
  public  Long del(Jedis jedis, String... keys) {
    Long count = 0L;
    try {
      count = jedis.del(keys);
    } catch (Exception e) {
      pool.returnBrokenResource(jedis);
      throw new JedisConnectionException(e);
    }
    return count;
  }
  
  public  void hset(Jedis jedis, String key, String field, String value ) {
    try {
      jedis.hset(key, field, value);
    } catch (Exception e) {
      pool.returnBrokenResource(jedis);
      throw new JedisConnectionException(e);
    }
  }
  
}
