package com.sogou.upd.passport.manager.account;

import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.manager.form.PCAuthTokenParams;
import com.sogou.upd.passport.manager.form.PCPairTokenParams;
import com.sogou.upd.passport.manager.form.PCRefreshTokenParams;

/**
 * 桌面端登录流程Manager
 * User: chenjiameng
 * Date: 13-7-28
 * Time: 上午11:48
 * To change this template use File | Settings | File Templates.
 */
public interface PCAccountManager {

    /**
     * 此接口处理两种情况下的生成pairToken：
     * 1.验证用户名和密码；
     * 2.验证由refreshtoken生成的sig；
     *
     * @param pcTokenParams
     * @return
     */
    public Result createPairToken(PCPairTokenParams pcTokenParams);

    /**
     * 根据refreshtoken换一个token用来延长登陆
     * @param pcRefreshTokenParams
     * @return
     */
    public Result authRefreshToken(PCRefreshTokenParams pcRefreshTokenParams);

    /**
     * 验证token并根据token换取cookie
     *
     * @param authPcTokenParams
     * @return
     */
    public Result authToken(PCAuthTokenParams authPcTokenParams);
}