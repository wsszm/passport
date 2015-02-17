package com.sogou.upd.passport.service.account.impl;

import com.google.common.base.Strings;
import com.sogou.upd.passport.common.CacheConstant;
import com.sogou.upd.passport.common.CommonConstant;
import com.sogou.upd.passport.common.DateAndNumTimesConstant;
import com.sogou.upd.passport.common.math.AES;
import com.sogou.upd.passport.common.math.Coder;
import com.sogou.upd.passport.common.utils.RedisUtils;
import com.sogou.upd.passport.exception.ServiceException;
import com.sogou.upd.passport.model.app.PackageNameSign;
import com.sogou.upd.passport.service.account.MappSSOService;
import com.sogou.upd.passport.service.app.AppConfigService;
import com.sogou.upd.passport.service.app.PackageNameSignService;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA.
 * User: nahongxu
 * Date: 15-2-16
 * Time: 上午11:06
 * To change this template use File | Settings | File Templates.
 */
@Service
public class MappSSOServiceImpl implements MappSSOService {
    private static final Logger logger = LoggerFactory.getLogger(MappSSOServiceImpl.class);
    private static final String SEPARATOR_1 = CommonConstant.SEPARATOR_1;

    @Autowired
    private PackageNameSignService packageNameSignService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private AppConfigService appConfigService;

    //校验解密后的应用基本信息：时间戳，clientId
    @Override
    public PackageNameSign baseSSOAppInfoCheck(int clientId, long ct, String decryptAppInfo) {
        if (Strings.isNullOrEmpty(decryptAppInfo)) {
            logger.warn("baseSSOAppInfoCheck failed,decryptAppInfo is empty");
            return null;
        }

        String[] stringArray = decryptAppInfo.split("\\" + SEPARATOR_1);
        if (null == stringArray || stringArray.length < 4) {
            logger.warn("baseSSOAppInfoCheck failed,decryptAppInfo can't be parsed");
            return null;
        }

        String clientIdParam = stringArray[0];
        String packageName = stringArray[1];
        String packageSign = stringArray[2];
        String ctParam = stringArray[3];

        //校验时间戳及clientId
        try {
            if ((clientId != Integer.parseInt(clientIdParam)) || (ct != Long.parseLong(ctParam))) {
                logger.warn("baseSSOAppInfoCheck failed,clientId or ct is invalid");
                return null;
            }
        } catch (Exception e) {
            logger.warn("baseSSOAppInfoCheck failed", e);
            return null;
        }

        //基本appInfo验证成功
        PackageNameSign packageNameSign = new PackageNameSign(packageName, packageSign);
        return packageNameSign;
    }

    //校验应用请求中的包签名与passport server端存储的是否相同
    @Override
    public boolean checkSSOPackageSign(PackageNameSign packageNameSign) {

        String packageName = packageNameSign.getPackageName();
        String packageSign = packageNameSign.getPackageSign();
        if (Strings.isNullOrEmpty(packageName) || Strings.isNullOrEmpty(packageSign)) {
            return false;
        }

        PackageNameSign packageInfoStored = packageNameSignService.queryPackageInfoByName(packageName);
        if (null == packageInfoStored || !packageSign.equals(packageInfoStored.getPackageSign())) {
            return false;
        }
        return true;
    }

    @Override

//    //生成token，存储token，并用clientSecret加密后返回
//    public String produceSSOToken(String packageName, int clientId,String udid,long ct) throws ServiceException{
//        String ssoToken=generateSSOToken(ct,packageName,udid);
//        saveSSOTokenToCache(ssoToken);
//
//
//
//        }

    //SSO token格式：MD5(packageName|udid|ct|随机数)
    public String generateSSOToken(long ct, String packageName, String udid) throws ServiceException {
        // 8位随机数
        String random = RandomStringUtils.randomAlphanumeric(8);
        String tokenContent = packageName + SEPARATOR_1 + udid + SEPARATOR_1 + ct + SEPARATOR_1 + random;
        String ssotoken;
        try {
            ssotoken = Coder.encryptMD5(tokenContent);
        } catch (Exception e) {
            logger.error("produceSSOToken fail, udid:" + udid + ",package:" + packageName);
            throw new ServiceException(e);
        }
        return ssotoken;
    }

    public void saveSSOTokenToCache(String ssoToken) {
        String cacheSSOTokenKey = buildCacheSSOTokenKey(ssoToken);
        try {
            redisUtils.setWithinSeconds(cacheSSOTokenKey, ssoToken, DateAndNumTimesConstant.TIME_FIVEMINUTES);
        } catch (Exception e) {
            logger.error("produceSSOToken fail, key:" + cacheSSOTokenKey);
            throw new ServiceException(e);
        }
    }

    public String encryptSSOToken(String ssoToken, String clientSecret) throws ServiceException {

        String ssoTokenEncryped;
        try {
            ssoTokenEncryped = AES.encryptURLSafeString(ssoToken, clientSecret);
        } catch (Exception e) {
            logger.error("encryptSSOToken fail, ssoToken:" + ssoToken);
            throw new ServiceException(e);
        }

        return ssoTokenEncryped;
    }

    @Override
    //验证包签名后，生成ticket，格式为：AES(clientId|udid|ssoToken|)，秘钥为serverSecret
    public String generateTicket(int clientId, String udid, String ssoToken, String serverSecret) {
        String ticketContent = clientId + SEPARATOR_1 + udid + SEPARATOR_1 + ssoToken;
        String ssoTicket;
        try {
            ssoTicket = AES.encryptURLSafeString(ticketContent, serverSecret);
        } catch (Exception e) {
            logger.error("generateTicket fail, clientId:" + clientId + ",udid:" + udid);
            throw new ServiceException(e);
        }

        return ssoTicket;
    }


    public String buildCacheSSOTokenKey(String ssoToken) {
        String cacheSSOTokenKey = CacheConstant.CACHE_PREFIX_SSO_TOKEN_KEY + ssoToken;
        return cacheSSOTokenKey;
    }

}
