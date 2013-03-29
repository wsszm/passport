package com.sogou.upd.passport.service.app.impl;

import com.google.common.base.Strings;
import com.sogou.upd.passport.dao.app.AppConfigMapper;
import com.sogou.upd.passport.model.app.AppConfig;
import com.sogou.upd.passport.service.app.AppConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * Created with IntelliJ IDEA.
 * User: shipengzhi
 * Date: 13-3-26
 * Time: 下午8:22
 * To change this template use File | Settings | File Templates.
 */
@Service
public class AppConfigServiceImpl implements AppConfigService {

    private Logger logger = LoggerFactory.getLogger(AppConfigService.class);
    private static final String CACHE_PREFIX_CLIENTID = "PASSPORT:ACCOUNT_CLIENTID_";     //clientid与appConfig映射

    @Inject
    private AppConfigMapper appConfigMapper;
    @Inject
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean verifyClientVaild(int clientId, String clientSecret) {
        try {
            AppConfig appConfig = getAppConfigByClientIdFromCache(clientId);
            // TODO 如果不存在返回的是null还是new AppConfig？
            if (appConfig == null) {
                return false;
            } else if (!clientSecret.equals(appConfig.getClientSecret())) {
                return false;
            }
        } catch (NumberFormatException e) {
            logger.error("{} is not Number", clientId);
            return false;
        }
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AppConfig getAppConfigByClientIdFromCache(final int clientId) {
        AppConfig appConfig = null;
        try {
            String cacheKey = CACHE_PREFIX_CLIENTID + clientId;
            //缓存根据clientId读取AppConfig
//            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
//            String valAppConfig = valueOperations.get(cacheKey);
//            if (!Strings.isNullOrEmpty(valAppConfig)) {
//                appConfig = JSONUtils.jsonToObject(valAppConfig, AppConfig.class);
//            }
            if (appConfig == null) {
                //读取数据库
                appConfig = appConfigMapper.getAppConfigByClientId(clientId);
//                if (appConfig != null) {
//                    addClientIdMapAppConfigToCache(clientId, appConfig);
//                }
            }
        } catch (Exception e) {
            logger.error("[SMS] service method addClientIdMapAppConfig error.{}", e);
        }
        return appConfig;
    }

    @Override
    public boolean addClientIdMapAppConfigToCache(final int clientId, final AppConfig appConfig) {
        boolean flag = true;
//        try {
//            String cacheKey = CACHE_PREFIX_CLIENTID + clientId;
//
//            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
//            valueOperations.setIfAbsent(String.valueOf(cacheKey), JSONUtils.objectToJson(appConfig));
//        } catch (Exception e) {
//            flag = false;
//            logger.error("[SMS] service method addClientIdMapAppConfig error.{}", e);
//        }
        return flag;
    }

    @Override
    public long getMaxClientId() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AppConfig regApp(AppConfig app) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getAccessTokenExpiresIn(int clientId) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getRefreshTokenExpiresIn(int clientId) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
