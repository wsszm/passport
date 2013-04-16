package com.sogou.upd.passport.manager.account.impl;

import com.google.common.collect.Maps;
import com.sogou.upd.passport.common.parameter.AccountTypeEnum;
import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.common.utils.ErrorUtil;
import com.sogou.upd.passport.manager.account.AccountRegManager;
import com.sogou.upd.passport.manager.account.parameters.RegisterParameters;
import com.sogou.upd.passport.model.account.Account;
import com.sogou.upd.passport.model.account.AccountAuth;
import com.sogou.upd.passport.service.account.AccountAuthService;
import com.sogou.upd.passport.service.account.AccountService;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

/**
 * User: mayan
 * Date: 13-4-15
 * Time: 下午4:43
 * To change this template use File | Settings | File Templates.
 */
@Component
public class AccountRegManagerImpl implements AccountRegManager {

    @Inject
    private AccountService accountService;

    @Inject
    private AccountAuthService accountAuthService;

    @Override
    public Result mobileRegister(RegisterParameters regParams) throws Exception {
        //TODO 入口参数regParams验证
        String mobile = regParams.getMobile();
        String smsCode = regParams.getSmscode();
        String password = regParams.getPassword();
        String ip = regParams.getIp();
        int clientId = regParams.getClientId();
        String instanceId = regParams.getInstanceId();
        //直接查询Account的mobile字段
        Account existAccount = accountService.getAccountByUserName(mobile);
        if (existAccount != null) {
            return returnResult(ErrorUtil.ERR_CODE_ACCOUNT_REGED, null);
        }
        //验证手机号码与验证码是否匹配
        boolean checkSmsInfo = accountService.checkSmsInfoFromCache(mobile, smsCode, String.valueOf(clientId));
        if (!checkSmsInfo) {
            return returnResult(ErrorUtil.ERR_CODE_ACCOUNT_PHONE_NOT_MATCH_SMSCODE, null);
        }
        Account account = accountService.initialAccount(mobile, password, ip, AccountTypeEnum.PHONE.getValue());
        if (account != null) {  //     如果插入account表成功，则插入用户授权信息表
            //生成token并向account_auth表里插一条用户状态记录
            AccountAuth accountAuth = accountAuthService.initialAccountAuth(account.getId(), account.getPassportId(), clientId, instanceId);
            if (accountAuth != null) {   //如果用户授权信息表插入也成功，则说明注册成功
                accountService.addPassportIdMapUserIdToCache(account.getPassportId(), Long.toString(account.getId()));
                //清除验证码的缓存
                accountService.deleteSmsCache(mobile, String.valueOf(clientId));
                String accessToken = accountAuth.getAccessToken();
                long accessValidTime = accountAuth.getAccessValidTime();
                String refreshToken = accountAuth.getRefreshToken();
                Map<String, Object> mapResult = Maps.newHashMap();
                mapResult.put("access_token", accessToken);
                mapResult.put("expires_time", accessValidTime);
                mapResult.put("refresh_token", refreshToken);
                return returnResult("0", mapResult);
            } else {
                return returnResult(ErrorUtil.ERR_CODE_ACCOUNT_REGISTER_FAILED, null);
            }
        } else {
            return returnResult(ErrorUtil.ERR_CODE_ACCOUNT_REGISTER_FAILED, null);
        }
    }

//    @Override
//    public Result findPassword(String mobile, int clientId) {
//        Account account = accountService.getAccountByUserName(mobile);
//        if (account == null) {
//            return returnResult(ErrorUtil.ERR_CODE_COM_NOUSER, null);
//        }
//        //判断账号是否被缓存
//        String cacheKey = mobile + "_" + clientId;
//        boolean isExistFromCache = accountService.checkCacheKeyIsExist(cacheKey);
//        Map<String, Object> mapResult;
//        if (isExistFromCache) {
//            //更新缓存状态
//            mapResult = accountService.updateSmsCacheInfoByKeyAndClientId(cacheKey, clientId);
//            return returnResult("0", mapResult);
//        } else {
//            mapResult = accountService.handleSendSms(mobile, clientId);
//        }
//        return MapUtils.isNotEmpty(mapResult) ? returnResult("0", mapResult) : returnResult(ErrorUtil.ERR_CODE_ACCOUNT_SMSCODE_SEND, null);
//    }
//
//    @Override
//    public Result resetPassword(RegisterParameters regParams) throws Exception {
//        //TODO 入口参数验证
//        String mobile = regParams.getMobile();
//        String smsCode = regParams.getSmscode();
//        String password = regParams.getPassword();
//        String ip = regParams.getIp();
//        int clientId = regParams.getClientId();
//        String instanceId = regParams.getInstanceId();
//        //验证手机号码与验证码是否匹配
//        boolean checkSmsInfo = accountService.checkSmsInfoFromCache(mobile, smsCode, String.valueOf(clientId));
//        if (!checkSmsInfo) {
//            return returnResult(ErrorUtil.ERR_CODE_ACCOUNT_PHONE_NOT_MATCH_SMSCODE, null);
//        }
//
//        //重置密码
//        Account account = accountService.resetPassword(mobile, password);
//        //先更新当前客户端实例对应的access_token和refresh_token，再异步更新该用户其它客户端的两个token
//        AccountAuth accountAuthResult = null;
//        if (account != null) {
//            accountAuthResult = accountAuthService.updateAccountAuth(account.getId(), account.getPassportId(), clientId, instanceId);
//            //TODO 存在分库分表问题
//            accountAuthService.asynUpdateAccountAuthBySql(mobile, clientId, instanceId);
//        }
//        if (accountAuthResult != null) {
//            //清除验证码的缓存
//            accountService.deleteSmsCache(mobile, String.valueOf(clientId));
//            return returnResult("0", null);
//        } else {
//            return returnResult(ErrorUtil.ERR_CODE_ACCOUNT_RESETPASSWORD_FAILED, null);
//        }
//    }


    /**
     * 公共方法提取，根据错误码返回result对象
     *
     * @param errorCode 错误码
     * @param object    封装的数据
     * @return 含错误码及相应的提示信息
     */
    private Result returnResult(String errorCode, Object object) {
        Result result = new Result();
        if (object != null) {
            result.addDefaultModel("data", object);
        }
        result.setStatus(errorCode);
        result.setStatusText(ErrorUtil.getERR_CODE_MSG(errorCode));
        return result;
    }
}
