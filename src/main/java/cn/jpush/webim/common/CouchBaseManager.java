package cn.jpush.webim.common;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.ConnectionFactoryBuilder.Protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.jpush.protocal.utils.SystemConfig;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;

public class CouchBaseManager {
	private static Logger LOG = LoggerFactory.getLogger(CouchBaseManager.class);
	private static CouchbaseClient client = null;
	private static Map<String, CouchbaseClient> couchbaseClientMap = new HashMap<String, CouchbaseClient>();

	public static CouchbaseClient initCouchbaseClient(String couchbaseName) {
		String serverAddress = SystemConfig.getProperty(couchbaseName
				+ ".couchbase.host");
		String bucket = SystemConfig.getProperty(couchbaseName
				+ ".couchbase.bucket");
		String pwd = SystemConfig
				.getProperty(couchbaseName + ".couchbase.pass");

		String[] serverNames = serverAddress.split(",");
		ArrayList<URI> serverList = new ArrayList<URI>();
		for (String serverName : serverNames) {
			URI base = null;
			base = URI.create(String.format("http://%s/pools", serverName));
			serverList.add(base);
		}

		try {
			CouchbaseConnectionFactoryBuilder ccfb = new CouchbaseConnectionFactoryBuilder();
			ccfb.setProtocol(Protocol.BINARY);
			ccfb.setOpTimeout(10000); 
			ccfb.setOpQueueMaxBlockTime(5000); 
			ccfb.setMaxReconnectDelay(1500);
			CouchbaseConnectionFactory cf = ccfb.buildCouchbaseConnection(
					serverList, bucket, pwd);
			client = new CouchbaseClient(cf);
		} catch (IOException e) {
			LOG.error(String
					.format("get CouchbaseClient Exception,host[%s],bucket[%s],pwd[%s].",
							serverAddress, bucket, pwd));
			return null;
		} catch (Exception e) {
			LOG.error(
					String.format(
							"get CouchbaseClient Exception,host[%s],bucket[%s],pwd[%s].",
							serverAddress, bucket, pwd), e);
			return null;
		}

		couchbaseClientMap.put(couchbaseName, client);
		return client;
	}

	public synchronized static CouchbaseClient getCouchbaseClientInstance(
			String couchbaseName) {
		if (couchbaseClientMap.containsKey(couchbaseName))
			return couchbaseClientMap.get(couchbaseName);
		return initCouchbaseClient(couchbaseName);
	}

	public static void shutdown(String couchbaseName) {
		couchbaseClientMap.get(couchbaseName).shutdown(3, TimeUnit.MINUTES);
	}

}
