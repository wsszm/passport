package com.sogou.upd.passport.zk;

import com.google.common.base.Strings;
import com.netflix.curator.framework.recipes.cache.NodeCache;
import com.netflix.curator.framework.recipes.cache.NodeCacheListener;
import com.sogou.upd.passport.common.lang.StringUtil;
import com.sogou.upd.passport.common.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisShardInfo;

import java.util.Map;

/**
 * 用于监控zookeeper上的redis连接地址
 * 如果地址变化则动态切换redis链接
 * User: ligang201716@sogou-inc.com
 * Date: 13-10-31
 * Time: 下午11:14
 */
@Component
public class RedisMonitor {

    private static final Logger log = LoggerFactory.getLogger(RedisMonitor.class);

    private NodeCache cacheNodeCache;

    private NodeCache tokenNodeCache;

    private String cachePath;

    private String tokenPath;

    private Monitor monitor;

    @Autowired
    private JedisConnectionFactory tokenConnectionFactory; //PC端token存储缓存
    @Autowired
    private JedisConnectionFactory jedisConnectionFactory;  //web接口临时信息存储缓存

    public RedisMonitor(Monitor monitor, String cachePath, String tokenPath) {
        this.monitor = monitor;
        this.cachePath = cachePath;
        this.tokenPath = tokenPath;
        this.addListener(cacheNodeCache, cachePath, new CacheListenerImpl());
        this.addListener(tokenNodeCache, tokenPath, new TokenListenerImpl());
    }


    private void addListener(NodeCache nodeCache, String path, NodeCacheListener nodeCacheListener) {
        nodeCache = new NodeCache(monitor.getCuratorFramework(), path, true);
        try {
            nodeCache.start();
            nodeCache.getListenable().addListener(nodeCacheListener);
        } catch (Exception e) {
            log.error("RedisMonitor start error", e);
        }
    }

    private class CacheListenerImpl implements NodeCacheListener {
        @Override
        public void nodeChanged() throws Exception {
            log.warn("cache redis node changed ");
            refresh(tokenNodeCache, jedisConnectionFactory);
        }
    }

    private class TokenListenerImpl implements NodeCacheListener {

        @Override
        public void nodeChanged() throws Exception {
            log.warn("redis node changed ");
            refresh(cacheNodeCache, tokenConnectionFactory);
        }
    }

    /**
     * 动态刷新redis连接
     *
     * @param nodeCache
     * @param jedisConnectionFactory
     */
    private void refresh(NodeCache nodeCache, JedisConnectionFactory jedisConnectionFactory) {
        try {
            if (nodeCache.getCurrentData() != null && nodeCache.getCurrentData().getData() != null) {
                String data = new String(nodeCache.getCurrentData().getData());
                log.warn("cache redis node changed data:" + data);

                Map jsonMap = JsonUtil.jsonToBean(data, Map.class);
                String host = (String) jsonMap.get("host");
                String portStr = (String) jsonMap.get("port");
                if (!Strings.isNullOrEmpty(host) && !Strings.isNullOrEmpty(portStr)) {
                    int port = Integer.valueOf(portStr);
//                JSONObject jsonObject = (JSONObject) JSON.parse(data);
//                if (jsonObject.contains("host") && jsonObject.contains("port")) {
//                    String host = jsonObject.getString("host");
//                    int port = jsonObject.getInt("port", -1);
                    if (StringUtil.isBlank(host) || port <= 0) {
                        log.error("redis refresh error host:" + host + " ,port:" + port);
                        return;
                    }
                    if (host.equals(jedisConnectionFactory.getHostName()) && port == jedisConnectionFactory.getPort()) {
                        log.error("redis not need refresh  host:" + host + " ,port:" + port);
                        return;
                    }

                    JedisShardInfo shardInfo = new JedisShardInfo(host, port);
                    jedisConnectionFactory.setHostName(host);
                    jedisConnectionFactory.setPort(port);
                    jedisConnectionFactory.setShardInfo(shardInfo);
                    jedisConnectionFactory.afterPropertiesSet();
                }

            } else {
                log.warn(" redis node changed data: is null ,path:" + nodeCache.getCurrentData().getPath());
            }
        } catch (NumberFormatException e) {
            log.error("port is not number format", e);
        }
    }

    /**
     * spring销毁Monitor时，关闭对zookeeper的监听
     */
    public void destroy() {
        try {
            if (cacheNodeCache != null) {
                cacheNodeCache.close();
            }
            if (tokenNodeCache != null) {
                tokenNodeCache.close();
            }
        } catch (Exception e) {
            log.error("error when destroy PathChildrenCache in Observer", e);
        }
    }
}
