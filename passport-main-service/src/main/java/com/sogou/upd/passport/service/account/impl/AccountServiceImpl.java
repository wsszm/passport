package com.sogou.upd.passport.service.account.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sogou.upd.passport.common.exception.SystemException;
import com.sogou.upd.passport.common.parameter.AccountStatusEnum;
import com.sogou.upd.passport.common.utils.ErrorUtil;
import com.sogou.upd.passport.common.utils.JSONUtils;
import com.sogou.upd.passport.common.utils.RedisUtils;
import com.sogou.upd.passport.common.utils.SMSUtil;
import com.sogou.upd.passport.dao.account.AccountAuthMapper;
import com.sogou.upd.passport.dao.account.AccountMapper;
import com.sogou.upd.passport.dao.app.AppConfigMapper;
import com.sogou.upd.passport.model.account.Account;
import com.sogou.upd.passport.model.account.AccountAuth;
import com.sogou.upd.passport.model.account.PostUserProfile;
import com.sogou.upd.passport.model.app.AppConfig;
import com.sogou.upd.passport.service.account.AccountService;
import com.sogou.upd.passport.service.account.generator.PassportIDGenerator;
import com.sogou.upd.passport.service.account.generator.PwdGenerator;
import com.sogou.upd.passport.service.account.generator.TokenGenerator;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Date;
import java.util.Map;

/**
 * User: mayan
 * Date: 13-3-22
 * Time: 下午3:38
 * To change this template use File | Settings | File Templates.
 */
@Service
public class AccountServiceImpl implements AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountServiceImpl.class);
    private static final String CACHE_PREFIX_ACCOUNT_SMSCODE = "PASSPORT:ACCOUNT_SMSCODE_";   //account与smscode映射
    private static final String CACHE_PREFIX_ACCOUNT_SENDNUM = "PASSPORT:ACCOUNT_SENDNUM_";
    private static final String CACHE_PREFIX_PASSPORTID = "PASSPORT:ACCOUNT_PASSPORTID_";     //passport_id与userID映射
    private static final String CACHE_PREFIX_USERID = "PASSPORT:ACCOUNT_USERID_";     //userID与passport_id映射
    private static final String CACHE_PREFIX_CLIENTID = "PASSPORT:ACCOUNT_CLIENTID_";     //clientid与appConfig映射
    @Inject
    private AccountMapper accountMapper;
    @Inject
    private AccountAuthMapper accountAuthMapper;

    @Inject
    private AppConfigMapper appConfigMapper;

    @Inject
    private TaskExecutor taskExecutor;
    @Inject
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean checkIsRegisterAccount(Account account) {
        Account accountReturn = accountMapper.checkIsRegisterAccount(account);
        return accountReturn == null ? true : false;
    }

    @Override
    public Map<String, Object> handleSendSms(final String account, final int clientId) {
        final Map<String, Object> mapResult = Maps.newHashMap();
        Object obj = null;
        try {
            obj = redisTemplate.execute(new RedisCallback() {
                boolean isSend = true;

                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    //设置每日最多发送短信验证码条数
                    byte[] keySendNumCache = RedisUtils.stringToByteArry(CACHE_PREFIX_ACCOUNT_SENDNUM + account);
                    byte[] keySendNum = RedisUtils.stringToByteArry("sendNum");
                    if (!connection.exists(keySendNumCache)) {
                        boolean flag = connection.hSetNX(keySendNumCache,
                                keySendNum,
                                RedisUtils.stringToByteArry("1"));
                        if (flag) {
                            connection.expire(keySendNumCache, SMSUtil.SMS_ONEDAY);
                        }
                    } else {
                        //如果存在，判断是否已经超出日发送最高限额   (比如30分钟后失效了，再次获取验证码 需要和此用户当天发送的总的条数对比)
                        Map<byte[], byte[]> mapCacheSendNumResult = connection.hGetAll(keySendNumCache);
                        if (MapUtils.isNotEmpty(mapCacheSendNumResult)) {
                            int sendNum = RedisUtils.byteArryToInteger(mapCacheSendNumResult.get(keySendNum));
                            if (sendNum < SMSUtil.MAX_SMS_COUNT_ONEDAY) {     //每日最多发送短信验证码条数
                                connection.hIncrBy(keySendNumCache, keySendNum, 1);
                            } else {
                                return ErrorUtil.buildError(ErrorUtil.ERR_CODE_ACCOUNT_CANTSENTSMS, "短信发送已达今天的最高上限" + SMSUtil.MAX_SMS_COUNT_ONEDAY + "条");
                            }
                        }
                    }
                    //生成随机数
                    String randomCode = RandomStringUtils.randomNumeric(5);
                    //写入缓存
                    String keyCache = CACHE_PREFIX_ACCOUNT_SMSCODE + account + "_" + clientId;
                    BoundHashOperations<String, String, String> boundHashOperations = redisTemplate.boundHashOps(keyCache);
                    Map<String, String> mapData = Maps.newHashMap();
                    mapData.put("smsCode", randomCode);    //初始化验证码
                    mapData.put("mobile", account);        //发送手机号
                    mapData.put("sendTime", Long.toString(System.currentTimeMillis()));   //发送时间
                    boundHashOperations.putAll(mapData);

                    //设置失效时间 30分钟  ，1800秒
                    connection.expire(RedisUtils.stringToByteArry(keyCache), SMSUtil.SMS_VALID);
                    //读取短信内容
                    String smsText = getSmsText(clientId, randomCode);
                    if (!Strings.isNullOrEmpty(smsText)) {
                        isSend = SMSUtil.sendSMS(account, smsText);
                        if (isSend) {
                            mapResult.put("smscode", randomCode);
                            return ErrorUtil.buildSuccess("获取注册验证码成功", mapResult);
                        }
                    } else {
                        return ErrorUtil.buildError(ErrorUtil.ERR_CODE_ACCOUNT_SMSCODE_SEND);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            logger.error("[SMS] service method handleSendSms error.{}", e);
        }
        return obj != null ? (Map<String, Object>) obj : null;
    }

    /*
     * 获取sms信息
     */
    public String getSmsText(final int clientId, String smsCode) {
        //缓存中根据clientId获取AppConfig
        AppConfig appConfig = getAppConfigByClientIdFromCache(clientId);
        if (appConfig != null) {
            return String.format(appConfig.getSmsText(), smsCode);
        }
        return null;
    }


    @Override
    public boolean checkKeyIsExistFromCache(final String cacheKey) {
        Object obj = null;
        try {
            obj = redisTemplate.execute(new RedisCallback() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    boolean flag = connection.exists(RedisUtils.stringToByteArry(CACHE_PREFIX_ACCOUNT_SMSCODE + cacheKey));
                    return flag;
                }
            });
        } catch (Exception e) {
            logger.error("[SMS] service method checkIsExistFromCache error.{}", e);
        }
        return obj != null ? (Boolean) obj : false;
    }

    @Override
    public Map<String, Object> updateSmsInfoByAccountFromCache(final String cacheKey, final int clientId) {
        Object obj = null;
        try {
            obj = redisTemplate.execute(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    byte[] cacheKeyByteArr = RedisUtils.stringToByteArry(CACHE_PREFIX_ACCOUNT_SMSCODE + cacheKey);
                    Map<String, Object> mapResult = Maps.newHashMap();
                    Map<byte[], byte[]> mapCacheResult = connection.hGetAll(cacheKeyByteArr);

                    //初始化缓存元素
                    byte[] sendTimeByte = RedisUtils.stringToByteArry("sendTime");
                    byte[] smsCodeByte = RedisUtils.stringToByteArry("smsCode");
                    byte[] mobileByte = RedisUtils.stringToByteArry("mobile");
                    byte[] sendNumByte = RedisUtils.stringToByteArry("sendNum");
                    if (MapUtils.isNotEmpty(mapCacheResult)) {

                        //获取缓存数据
                        long sendTime = RedisUtils.byteArryToLong(mapCacheResult.get(sendTimeByte));
                        String smsCode = RedisUtils.byteArryToString(mapCacheResult.get(smsCodeByte));
                        String account = RedisUtils.byteArryToString(mapCacheResult.get(mobileByte));

                        byte[] keySendNumCache = RedisUtils.stringToByteArry(CACHE_PREFIX_ACCOUNT_SENDNUM + account);
                        Map<byte[], byte[]> mapCacheSendNumResult = connection.hGetAll(keySendNumCache);
                        if (MapUtils.isNotEmpty(mapCacheSendNumResult)) {
                            int sendNum = RedisUtils.byteArryToInteger(mapCacheSendNumResult.get(sendNumByte));
                            long curtime = System.currentTimeMillis();
                            boolean valid = curtime >= (sendTime + SMSUtil.SEND_SMS_INTERVAL); // 1分钟只能发1条短信
                            if (valid) {
                                if (sendNum < SMSUtil.MAX_SMS_COUNT_ONEDAY) {     //每日最多发送短信验证码条数
                                    connection.hIncrBy(keySendNumCache, sendNumByte, 1);
                                    connection.hSet(cacheKeyByteArr,
                                            sendTimeByte,
                                            RedisUtils.stringToByteArry(String.valueOf(System.currentTimeMillis())));
                                    //读取短信内容
                                    String smsText = getSmsText(clientId, smsCode);
                                    if (!Strings.isNullOrEmpty(smsText)) {
                                        boolean isSend = SMSUtil.sendSMS(smsText, smsCode);
                                        if (isSend) {
                                            //30分钟之内返回原先验证码
                                            mapResult.put("smscode", smsCode);
                                            return ErrorUtil.buildSuccess("获取注册验证码成功", mapResult);
                                        }
                                    } else {
                                        return ErrorUtil.buildError(ErrorUtil.ERR_CODE_ACCOUNT_SMSCODE_SEND);
                                    }
                                } else {
                                    return ErrorUtil.buildError(ErrorUtil.ERR_CODE_ACCOUNT_CANTSENTSMS, "短信发送已达今天的最高上限" + SMSUtil.MAX_SMS_COUNT_ONEDAY + "条");
                                }
                            } else {
                                return ErrorUtil.buildError(ErrorUtil.ERR_CODE_ACCOUNT_MINUTELIMIT, "1分钟只能发送一条短信");
                            }
                        }
                    }
                    return ErrorUtil.buildError(ErrorUtil.ERR_CODE_ACCOUNT_SMSCODE_SEND, "手机验证码发送失败");
                }
            });
        } catch (Exception e) {
            logger.error("[SMS] service method updateCacheStatusByAccount error.{}", e);
        }
        return obj != null ? (Map<String, Object>) obj : null;
    }

    @Override
    public long userRegister(Account account) {
        return accountMapper.userRegister(account);
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, Object> handleLogin(String mobile, String passwd, int clientId, PostUserProfile postData) throws SystemException {
        Account userAccount = null;
        //判断用户是否存在
        try {
            userAccount = getUserAccount(mobile, passwd);
        } catch (Exception e) {
            return ErrorUtil.buildError(ErrorUtil.ERR_CODE_ACCOUNT_LOGINERROR);
        }
        if (userAccount == null) {
            return ErrorUtil.buildError(ErrorUtil.ERR_CODE_ACCOUNT_LOGINERROR);
        }
        //判读access_token有效性，是否在有效的范围内
        AccountAuth accountAuth = accountAuthMapper.getUserAuthByUserId(userAccount.getId());

        long curtime = System.currentTimeMillis();
        boolean valid = curtime < accountAuth.getAccessValidTime();

//        AccountAuth accountAuth=newAccountAuth(userAccount.getId(),userAccount.getPassportId(),appkey);
//        int updateNum=accountAuthMapper.updateAccountAuth(accountAuth);
//
//        if (accountAuth == null) {
//            return ErrorUtil.buildError(ErrorUtil.ERR_CODE_ACCOUNT_ACCESSTOKEN_FAILED);
//        }

        if (valid) {
            return ErrorUtil.buildSuccess("登录成功", null);
        }

        return ErrorUtil.buildError(ErrorUtil.ERR_CODE_ACCOUNT_LOGINERROR);
    }

    /**
     * 根据用户名密码获取用户Account
     *
     * @param
     * @return
     */
    public Account getUserAccount(String mobile, String passwd) throws SystemException {
        Map<String, String> mapResult = Maps.newHashMap();
        mapResult.put("mobile", mobile);
        mapResult.put("passwd", Strings.isNullOrEmpty(passwd) ? "" : PwdGenerator.generatorPwdSign(passwd));
        Account accountResult = accountMapper.getUserAccount(mapResult);
        return accountResult != null ? accountResult : null;
    }


    @Override
    public boolean checkSmsInfoFromCache(final String account, final String smsCode, final String clientId) {
        Object obj = null;
        try {
            obj = redisTemplate.execute(new RedisCallback() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    String keyCache = CACHE_PREFIX_ACCOUNT_SMSCODE + account + "_" + clientId;
                    String strValue = null;
                    Map<byte[], byte[]> mapResult = connection.hGetAll(RedisUtils.stringToByteArry(keyCache));
                    if (MapUtils.isNotEmpty(mapResult)) {
                        byte[] value = mapResult.get(RedisUtils.stringToByteArry("smsCode"));
                        strValue = RedisUtils.byteArryToString(value);
                        if (StringUtils.isNotBlank(strValue)) {
                            if (strValue.equals(smsCode)) {
                                return true;
                            } else {
                                return false;
                            }
                        }
                    } else {
                        return false;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            logger.error("[SMS] service method checkSmsInfoFromCache error.{}", e);
        }
        return obj != null ? (Boolean) obj : false;
    }

    @Override
    public Account initialAccount(String account, String pwd, String ip, int provider) throws SystemException {
        Account accountReturn = new Account();
        accountReturn.setPassportId(PassportIDGenerator.generator(account, provider));
        String passwdSign = null;
        if (!Strings.isNullOrEmpty(pwd)) {
            passwdSign = PwdGenerator.generatorPwdSign(pwd);
        }
        accountReturn.setPasswd(passwdSign);
        accountReturn.setRegTime(new Date());
        accountReturn.setRegIp(ip);
        accountReturn.setAccountType(provider);
        accountReturn.setStatus(AccountStatusEnum.REGULAR.getValue());
        accountReturn.setVersion(Account.NEW_ACCOUNT_VERSION);
        accountReturn.setMobile(account);
        long id = accountMapper.userRegister(accountReturn);
        if (id != 0) {
            accountReturn.setId(id);
            return accountReturn;
        }
        return null;
    }

    @Override
    public Account initialConnectAccount(String account, String ip, int provider) throws SystemException {
        return initialAccount(account, null, ip, provider);
    }

    @Override
    public AccountAuth initialAccountAuth(long userId, String passportId, int clientId) throws SystemException {
        AccountAuth accountAuth = newAccountAuth(userId, passportId, clientId);
        long id = accountAuthMapper.saveAccountAuth(accountAuth);
        if (id != 0) {
            accountAuth.setId(id);
            return accountAuth;
        }
        return null;
    }

    @Override
    public AccountAuth updateAccountAuth(long userId, String passportId, int clientId) throws Exception {
        AccountAuth accountAuth = newAccountAuth(userId, passportId, clientId);
        if (accountAuth != null) {
            int accountRow = accountAuthMapper.updateAccountAuth(accountAuth);
            return accountRow == 0 ? null : accountAuth;
        }
        return null;
    }


    @Override
    public boolean addClientIdMapAppConfigToCache(final int clientId, final AppConfig appConfig) {
        boolean flag = true;
        try {
            String cacheKey = CACHE_PREFIX_CLIENTID + clientId;

            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            valueOperations.setIfAbsent(String.valueOf(cacheKey), JSONUtils.objectToJson(appConfig));
        } catch (Exception e) {
            flag = false;
            logger.error("[SMS] service method addClientIdMapAppConfig error.{}", e);
        }
        return flag;
    }

    @Override
    public long getUserIdByPassportIdFromCache(final String passportId) {
        long userIdResult = 0;
        try {
            String cacheKey = CACHE_PREFIX_PASSPORTID + passportId;
            String userId = getFromCache(cacheKey);
            if (Strings.isNullOrEmpty(userId)) {
                //读取数据库
                userIdResult = getUserIdByPassportId(passportId);
                if (userIdResult != 0) {
                    addPassportIdMapUserIdToCache(passportId, userId);
                }
            } else {
                userIdResult = Long.parseLong(userId);
            }
        } catch (Exception e) {
            logger.error("[SMS] service method getPassportIdByUserId error.{}", e);
        }
        return userIdResult;
    }

    @Override
    public boolean addPassportIdMapUserIdToCache(final String passportId, final String userId) {
        boolean flag = true;
        try {
            String cacheKey = CACHE_PREFIX_PASSPORTID + passportId;

            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            valueOperations.setIfAbsent(cacheKey, userId);

        } catch (Exception e) {
            flag = false;
            logger.error("[SMS] service method addPassportIdMapUserIdToCache error.{}", e);
        }
        return flag;
    }

    @Override
    public boolean addUserIdMapPassportIdToCache(final String userId, final String passportId) {
        boolean flag = true;
        try {
            String cacheKey = CACHE_PREFIX_USERID + userId;

            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            valueOperations.setIfAbsent(cacheKey, passportId);

        } catch (Exception e) {
            flag = false;
            logger.error("[SMS] service method addUserIdMapPassportId error.{}", e);
        }
        return flag;
    }

    @Override
    public String getPassportIdByUserIdFromCache(final long userId) {
        String passportId = null;
        try {
            String cacheKey = CACHE_PREFIX_USERID + userId;
            passportId = getFromCache(cacheKey);
            if (Strings.isNullOrEmpty(passportId)) {
                //读取数据库
                passportId = getPassportIdByUserId(userId);
                if (!Strings.isNullOrEmpty(passportId)) {
                    addUserIdMapPassportIdToCache(String.valueOf(userId), passportId);
                }
            }

        } catch (Exception e) {
            logger.error("[SMS] service method getPassportIdByUserId error.{}", e);
        }
        return passportId;
    }

    @Override
    public AppConfig getAppConfigByClientIdFromCache(final int clientId) {
        AppConfig appConfig = null;
        try {
            String cacheKey = CACHE_PREFIX_CLIENTID + clientId;
            //缓存根据clientId读取AppConfig
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            String valAppConfig = valueOperations.get(cacheKey);
            if (!Strings.isNullOrEmpty(valAppConfig)) {
                appConfig = JSONUtils.jsonToObject(valAppConfig, AppConfig.class);
            }
            if (appConfig == null) {
                //读取数据库
                appConfig = appConfigMapper.getAppConfigByClientId(clientId);
                if (appConfig != null) {
                    addClientIdMapAppConfigToCache(clientId, appConfig);
                }
            }
        } catch (Exception e) {
            logger.error("[SMS] service method addClientIdMapAppConfig error.{}", e);
        }
        return appConfig;
    }

    @Override
    public boolean deleteSmsCache(final String mobile, final String clientId) {
        Object obj = null;
        try {
            obj = redisTemplate.execute(new RedisCallback<Object>() {
                @Override
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    byte[] keyCache = RedisUtils.stringToByteArry(CACHE_PREFIX_ACCOUNT_SMSCODE + mobile + "_" + clientId);
                    byte[] keySendNumCache = RedisUtils.stringToByteArry(CACHE_PREFIX_ACCOUNT_SENDNUM + mobile);
                    connection.del(keyCache);
                    connection.del(keySendNumCache);
                    return true;
                }
            });
        } catch (Exception e) {
            logger.error("[SMS] service method deleteSmsCache error.{}", e);
        }
        return obj != null ? (Boolean) obj : false;
    }

    /**
     * 修改用户状态表
     *
     * @param accountAuth
     * @return
     */
    @Override
    public int updateAccountAuth(AccountAuth accountAuth) {
        if (accountAuth != null) {
            int accountRow = accountAuthMapper.updateAccountAuth(accountAuth);
            return accountRow == 0 ? 0 : accountRow;
        }
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }


    /**
     * 根据主键ID获取passportId
     *
     * @param userId
     * @return
     */
    @Override
    public String getPassportIdByUserId(long userId) {
        String passportId = null;
        if (userId != 0) {
            passportId = getPassportIdByUserId(userId);
            return passportId == null ? null : passportId;
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * 根据主键ID获取passportId
     *
     * @param passportId
     * @return
     */
    @Override
    public long getUserIdByPassportId(String passportId) {
        long userId = 0;
        if (passportId != null) {
            userId = accountMapper.getUserIdByPassportId(passportId);
            return userId == 0 ? 0 : userId;
        }
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /*
     * 根据key从缓存中获取value
     */
    public String getFromCache(final String key) throws Exception {
        String valResult = null;
        try {
            ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
            valResult = valueOperations.get(key);
        } catch (Exception e) {
            logger.error("[SMS] service method getFromCache error.{}", e);
        }
        return valResult;
    }

    /**
     * 构造一个新的AccountAuth
     *
     * @param userId
     * @param passportID
     * @param clientId
     * @return
     */
    private AccountAuth newAccountAuth(long userId, String passportID, int clientId) throws SystemException {

        AppConfig appConfig = getAppConfigByClientIdFromCache(clientId);
        AccountAuth accountAuth = new AccountAuth();
        if (appConfig != null) {
            int accessTokenExpiresIn = appConfig.getAccessTokenExpiresIn();
            int refreshTokenExpiresIn = appConfig.getRefreshTokenExpiresIn();

            String accessToken;
            String refreshToken;
            try {
                accessToken = TokenGenerator.generatorAccessToken(passportID, clientId, accessTokenExpiresIn);
                refreshToken = TokenGenerator.generatorRefreshToken(passportID, clientId);
            } catch (Exception e) {
                throw new SystemException(e);
            }
            accountAuth.setUserId(userId);
            accountAuth.setClientId(clientId);
            accountAuth.setAccessToken(accessToken);
            accountAuth.setAccessValidTime(TokenGenerator.generatorVaildTime(accessTokenExpiresIn));
            accountAuth.setRefreshToken(refreshToken);
            accountAuth.setRefreshValidTime(TokenGenerator.generatorVaildTime(refreshTokenExpiresIn));
        }

        return accountAuth;  //To change body of implemented methods use File | Settings | File Templates.
    }


}
