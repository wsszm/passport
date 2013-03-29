package com.sogou.upd.passport.service.app;

import com.sogou.upd.passport.common.exception.ApplicationException;
import com.sogou.upd.passport.model.app.AppConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: shipengzhi
 * Date: 13-3-25
 * Time: 下午11:41
 * To change this template use File | Settings | File Templates.
 */
public interface AppConfigService {

    /**
     * 验证client合法性
     * @param clientId
     * @param clientSecret
     * @return
     */
    public boolean verifyClientVaild(String clientId, String clientSecret);

    /**
     * 根据ClientId 获取AppConfig （缓存读取）
     * @param clientId
     * @return
     */
    public AppConfig getAppConfigByClientIdFromCache(int clientId);

    /**
     * ClientId与AppConfig缓存映射
     * @param clientId
     * @param appConfig
     */
    public boolean addClientIdMapAppConfigToCache(int clientId,AppConfig appConfig);

    public long getMaxClientId();

    public AppConfig regApp(AppConfig app);

    public int getAccessTokenExpiresIn(int clientId);

    public int getRefreshTokenExpiresIn(int clientId);


}
