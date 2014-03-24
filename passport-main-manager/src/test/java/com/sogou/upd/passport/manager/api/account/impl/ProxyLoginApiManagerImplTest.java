package com.sogou.upd.passport.manager.api.account.impl;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.sogou.upd.passport.BaseTest;
import com.sogou.upd.passport.common.CommonConstant;
import com.sogou.upd.passport.common.math.Coder;
import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.manager.api.account.LoginApiManager;
import com.sogou.upd.passport.manager.api.account.form.*;
import junit.framework.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * User: ligang201716@sogou-inc.com
 * Date: 13-6-6
 * Time: 下午2:28
 */

public class ProxyLoginApiManagerImplTest extends BaseTest {

    private static final int clientId = 1100;

    @Autowired
    private LoginApiManager proxyLoginApiManager;

    @Test
    public void testAuthUser() {
        try {
            AuthUserApiParams authUserParameters = new AuthUserApiParams();
            authUserParameters.setUserid("tinkame302@sohu.com");
            authUserParameters.setClient_id(clientId);
            authUserParameters.setPassword(Coder.encryptMD5("123456"));
            Result result = proxyLoginApiManager.webAuthUser(authUserParameters);
            Assert.assertEquals("0", result.getCode());
            System.out.println(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetCookieInfo() throws Exception {
        //已有账号的密保手机的手机账号
        CookieApiParams cookieApiParams = getCookieApiParams("15210832767@sohu.com");
        Result result = proxyLoginApiManager.getCookieInfo(cookieApiParams);
        System.out.println("result:" + result.toString());
        String ppinf = (String)result.getModels().get("ppinf");
        String pprdig = (String)result.getModels().get("pprdig");
        Assert.assertTrue(!StringUtils.isBlank(ppinf));
        Assert.assertTrue(!StringUtils.isBlank(pprdig));



        CookieApiParams cookieApiParams_1 = getCookieApiParams("13621009174@sohu.com");
        Result result_1 = proxyLoginApiManager.getCookieInfo(cookieApiParams_1);
        System.out.println("result_1:" + result_1.toString());
        String ppinf_1 = (String)result_1.getModels().get("ppinf");
        String pprdig_1 = (String)result_1.getModels().get("pprdig");
        Assert.assertTrue(!StringUtils.isBlank(ppinf_1));
        Assert.assertTrue(!StringUtils.isBlank(pprdig_1));

        CookieApiParams cookieApiParams_2 = getCookieApiParams("tinkame001@126.com");
        Result result_2 = proxyLoginApiManager.getCookieInfo(cookieApiParams_2);
        System.out.println("result_2:" + result_2.toString());
        String ppinf_2 = (String)result_2.getModels().get("ppinf");
        String pprdig_2 = (String)result_2.getModels().get("pprdig");
        Assert.assertTrue(!StringUtils.isBlank(ppinf_2));
        Assert.assertTrue(!StringUtils.isBlank(pprdig_2));
    }

    private CookieApiParams getCookieApiParams(String userId) {
        CookieApiParams cookieApiParams = new CookieApiParams();
        cookieApiParams.setUserid(userId);
        cookieApiParams.setIp("200.0.98.23");
        cookieApiParams.setClient_id(1120);
        cookieApiParams.setRu(ru);
        cookieApiParams.setTrust(CookieApiParams.IS_ACTIVE);
        cookieApiParams.setPersistentcookie(String.valueOf(1));

        return cookieApiParams;
    }

    @Test
    public void testAppAuth() throws Exception {
        AppAuthTokenApiParams params = new AppAuthTokenApiParams();
        params.setClient_id(1120);
        params.setToken("54b4c49bfdb3321a5ffea8358c7ec08b");
        params.setCode("23b442b3c93c059b5510b6230d85f070");
        params.setType(2);
        params.setCt(1160703204);
        Result result = proxyLoginApiManager.appAuthToken(params);
        System.out.println("result:" + result);
    }

    @Test
    public void testgetSHCookieValue() {
        try {
            String userid = "大大大31231@focus.cn";
//           String utfUserId = new String(userid.getBytes(),"gbk");
            CookieApiParams cookieApiParams = new CookieApiParams();
            cookieApiParams.setUserid(userid);
            cookieApiParams.setClient_id(1044);
            cookieApiParams.setRu("https://account.sogou.com/");
            cookieApiParams.setTrust(CookieApiParams.IS_ACTIVE);
            cookieApiParams.setPersistentcookie(String.valueOf(1));

            //TODO sogou域账号迁移后cookie生成问题
            Result getCookieValueResult = proxyLoginApiManager.getCookieInfo(cookieApiParams);
            System.out.println(getCookieValueResult.toString());
        } catch (Exception ex) {

        }

    }

    @Test
    public void testGetCookieValue() {
        CreateCookieUrlApiParams createCookieUrlApiParams = new CreateCookieUrlApiParams();
        createCookieUrlApiParams.setUserid("shipengzhi1986@sogou.com");
        createCookieUrlApiParams.setRu(CommonConstant.DEFAULT_CONNECT_REDIRECT_URL);
        createCookieUrlApiParams.setPersistentcookie(1);
        createCookieUrlApiParams.setDomain("sogou.com");
        Result result = proxyLoginApiManager.getCookieInfoWithRedirectUrl(createCookieUrlApiParams);
        System.out.println(result);
    }

    @Test
    public void testBuildCreateCookieUrl() {
        CreateCookieUrlApiParams createCookieUrlApiParams = new CreateCookieUrlApiParams();
        createCookieUrlApiParams.setUserid(userid);
        createCookieUrlApiParams.setRu("https://account.sogou.com/login/success");
        createCookieUrlApiParams.setPersistentcookie(1);
        Result result = proxyLoginApiManager.buildCreateCookieUrl(createCookieUrlApiParams, false, true);
        System.out.println(result);
    }
}
