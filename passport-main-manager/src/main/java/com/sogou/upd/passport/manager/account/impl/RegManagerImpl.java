package com.sogou.upd.passport.manager.account.impl;

import com.google.common.base.Strings;
import com.sogou.upd.passport.common.CommonHelper;
import com.sogou.upd.passport.common.parameter.AccountDomainEnum;
import com.sogou.upd.passport.common.parameter.AccountModuleEnum;
import com.sogou.upd.passport.common.parameter.AccountStatusEnum;
import com.sogou.upd.passport.common.result.APIResultSupport;
import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.common.utils.ErrorUtil;
import com.sogou.upd.passport.common.utils.PhoneUtil;
import com.sogou.upd.passport.exception.ServiceException;
import com.sogou.upd.passport.manager.ManagerHelper;
import com.sogou.upd.passport.manager.account.RegManager;
import com.sogou.upd.passport.manager.api.account.BindApiManager;
import com.sogou.upd.passport.manager.api.account.RegisterApiManager;
import com.sogou.upd.passport.manager.api.account.form.*;
import com.sogou.upd.passport.manager.form.ActiveEmailParams;
import com.sogou.upd.passport.manager.form.WebRegisterParams;
import com.sogou.upd.passport.model.account.Account;
import com.sogou.upd.passport.service.account.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 注册管理
 * User: mayan
 * Date: 13-4-15 Time: 下午4:43
 */
@Component
public class RegManagerImpl implements RegManager {

    @Autowired
    private AccountService accountService;
    @Autowired
    private RegisterApiManager sgRegisterApiManager;
    @Autowired
    private RegisterApiManager proxyRegisterApiManager;
    @Autowired
    private BindApiManager proxyBindApiManager;
    @Autowired
    private BindApiManager sgBindApiManager;
    @Autowired
    private MobileCodeSenderService mobileCodeSenderService;
    @Autowired
    private OperateTimesService operateTimesService;
    @Autowired
    private SnamePassportMappingService snamePassportMappingService;
    @Autowired
    private AccountSecureService accountSecureService;

    private static final Logger logger = LoggerFactory.getLogger(RegManagerImpl.class);

    private static final String EMAIL_REG_VERIFY_URL = "https://account.sogou.com/web/reg/emailverify";
    private static final String LOGIN_INDEX_URL = "https://account.sogou.com";


    @Override
    public Result webRegister(WebRegisterParams regParams, String ip) throws Exception {

        Result result = new APIResultSupport(false);
        String username = null;
        try {
            int clientId = Integer.parseInt(regParams.getClient_id());
            username = regParams.getUsername().trim().toLowerCase();
            String password = regParams.getPassword();
            String captcha = regParams.getCaptcha();
            String ru = regParams.getRu();

            boolean isSogou = false;//外域还是个性账号
            //判断是否是个性账号
            if (username.indexOf("@") == -1) {
                //判断是否是手机号注册
                if (!PhoneUtil.verifyPhoneNumberFormat(username)) {
                    username = username + "@sogou.com";
                    isSogou = true;
                }
            } else {
                int index = username.indexOf("@");
                username = username.substring(0, index) + username.substring(index, username.length()).toLowerCase();
            }
            //判断注册账号类型，sogou用户还是手机用户
            AccountDomainEnum emailType = AccountDomainEnum.getAccountDomain(username);

            switch (emailType) {
                case SOGOU://个性账号直接注册
                case OTHER://外域邮件注册
                case INDIVID:
                    String token = regParams.getToken();
                    //判断验证码
                    if (!accountService.checkCaptchaCode(token, captcha)) {
                        logger.debug("[webRegister captchaCode wrong warn]:username=" + username + ", ip=" + ip + ", token=" + token + ", captchaCode=" + captcha);
                        result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_CAPTCHA_CODE_FAILED);
                        return result;
                    }
                    //发出激活信以后跳转页面，ru为空跳到sogou激活成功页面
                    if (Strings.isNullOrEmpty(ru)) {
                        if (isSogou) {
                            ru = LOGIN_INDEX_URL;
                        } else {
                            ru = EMAIL_REG_VERIFY_URL;
                        }
                    }
                    RegEmailApiParams regEmailApiParams = buildRegMailProxyApiParams(username, password, ip,
                            clientId, ru);
                    //调用SOHU API注册时，未激活的外域邮箱先临时注册到SG缓存,有效期一天,此判断一直保留到自有账号只读写SG库上线
                    if (AccountDomainEnum.OTHER.equals(AccountDomainEnum.getAccountDomain(username))) {
                        accountService.initialMailToCache(username, password, ip);
                    }
                    if (ManagerHelper.isInvokeProxyApi(username)) {
                        result = proxyRegisterApiManager.regMailUser(regEmailApiParams);
                    } else {
                        result = sgRegisterApiManager.regMailUser(regEmailApiParams);
                    }
                    break;
                case PHONE://手机号
                    RegMobileCaptchaApiParams regMobileCaptchaApiParams = buildProxyApiParams(username, password, captcha, clientId, ip);
                    if (ManagerHelper.isInvokeProxyApi(username)) {
                        result = registerMobile(username, password, clientId, captcha);
                        if (result.isSuccess()) {
                            username = (String) result.getModels().get("userid");
                        }
                    } else {
                        result = sgRegisterApiManager.regMobileCaptchaUser(regMobileCaptchaApiParams);
                    }
                    break;
            }
        } catch (ServiceException e) {
            logger.error("webRegister fail,passportId:" + regParams.getUsername(), e);
            result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_REGISTER_FAILED);
            return result;
        }
        if (result.isSuccess()) {
            result.getModels().put("username", username);            //判断是否是外域邮箱注册 外域邮箱激活以后种cookie
        } else {
            result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_REGISTER_FAILED);
        }
        return result;
    }

    @Override
    public Result registerMobile(String username, String password, int clientId, String captcha) throws Exception {
        Result result = new APIResultSupport(false);
        result = mobileCodeSenderService.checkSmsCode(username, clientId, AccountModuleEnum.REGISTER, captcha);
        if (!result.isSuccess()) {
            result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_PHONE_NOT_MATCH_SMSCODE);
            return result;
        }
        RegMobileApiParams regApiParams = new RegMobileApiParams(username, password, clientId);
        result = proxyRegisterApiManager.regMobileUser(regApiParams);
        return result;
    }

    private RegEmailApiParams buildRegMailProxyApiParams(String username, String password, String ip, int clientId, String ru) {
        return new RegEmailApiParams(username, password, ip, clientId, ru);
    }


    private RegMobileCaptchaApiParams buildProxyApiParams(String mobile, String password, String captcha, int clientId, String ip) {
        RegMobileCaptchaApiParams regMobileCaptchaApiParams = new RegMobileCaptchaApiParams();
        regMobileCaptchaApiParams.setMobile(mobile);
        regMobileCaptchaApiParams.setPassword(password);
        regMobileCaptchaApiParams.setCaptcha(captcha);
        regMobileCaptchaApiParams.setClient_id(clientId);
        regMobileCaptchaApiParams.setIp(ip);
        return regMobileCaptchaApiParams;
    }

    @Override
    public Result activeEmail(ActiveEmailParams activeParams, String ip) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            String username = activeParams.getPassport_id();
            String token = activeParams.getToken();
            int clientId = Integer.parseInt(activeParams.getClient_id());
            //激活邮件
            boolean isSuccessActive = accountService.activeEmail(username, token, clientId);

            if (isSuccessActive) {
                //激活成功
                Account account = accountService.initialWebAccount(username, ip);
                if (account != null) {
                    //更新缓存
                    result.setDefaultModel(account);
                    result.setDefaultModel("userid", account.getPassportId());
                    result.setSuccess(true);
                    result.setMessage("激活成功！");
                    return result;
                } else {
                    result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_NOTHASACCOUNT);
                    return result;
                }
            } else {
                //激活失败
                Account account = accountService.queryAccountByPassportId(username);
                if (account != null) {
                    if (account.getFlag() == AccountStatusEnum.REGULAR.getValue()) {
                        //已经激活，无需再次激活
                        result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_ALREADY_ACTIVED_FAILED);
                        return result;
                    } else {
                        result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_ACTIVED_URL_FAILED);
                        return result;
                    }
                } else {
                    //无此账号
                    result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_NOTHASACCOUNT);
                    return result;
                }
            }
        } catch (ServiceException e) {
            logger.error("activeEmail fail, passportId:" + activeParams.getPassport_id() + " clientId:" + activeParams.getClient_id(), e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
            return result;
        }
    }

    @Override
    public Map<String, Object> getCaptchaCode(String code) {
        return accountService.getCaptchaCode(code);
    }

    private Result checkUserFromSohu(String username, int clientId) throws Exception {
        Result result;
        try {
            if (username.indexOf("@") == -1) {
                //判断是否是手机号注册
                if (!PhoneUtil.verifyPhoneNumberFormat(username)) {
                    username = username + "@sogou.com";
                }
            }
            if (PhoneUtil.verifyPhoneNumberFormat(username)) {
                BaseMoblieApiParams params = new BaseMoblieApiParams();
                params.setMobile(username);
                //手机号 判断绑定账户
                result = proxyBindApiManager.getPassportIdByMobile(params);
                if (result.isSuccess()) {
                    result.setSuccess(false);
                    result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_REGED);
                    result.setMessage("账号已注册");
                    return result;
                } else if (CommonHelper.isExplorerToken(clientId)) {
                    result = isSohuplusUser(username, clientId);
                } else {
                    result.setSuccess(true);
                    result.setMessage("账户未被占用");
                }
            } else {
                CheckUserApiParams checkUserApiParams = buildProxyApiParams(username, clientId);
                result = proxyRegisterApiManager.checkUser(checkUserApiParams);
                if (result.isSuccess() && CommonHelper.isExplorerToken(clientId)) {
                    result = isSohuplusUser(username, clientId);
                }
            }
        } catch (Exception e) {
            logger.error("check user from sohu error, username:" + username + "clientid:" + clientId, e);
            throw new Exception(e);
        }
        return result;
    }

    @Override
    public Result isAccountNotExists(String username, int clientId) throws Exception {
        Result result;
        try {
            if (ManagerHelper.readSohuSwitcher()) {
                //回滚流程
                result = checkUserFromSohu(username, clientId);
            } else {
                //正常流程
                result = bothCheck(username, clientId);
            }
        } catch (ServiceException e) {
            logger.error("Check account is exists Exception, username:" + username, e);
            throw new Exception(e);
        }
        return result;
    }

    private Result bothCheck(String username, int clientId) throws Exception {
        Result result;
        if (PhoneUtil.verifyPhoneNumberFormat(username) && accountSecureService.getUpdateSuccessFlag(username)) {
            //主账号有更新绑定手机的操作时，调用sohu api检查账号是否可用
            result = checkUserFromSohu(username, clientId);
        } else {
            ////没有更新绑定手机时，走正常的双读检查账号是否可用流程
            CheckUserApiParams checkUserApiParams = buildProxyApiParams(username, clientId);
            result = sgRegisterApiManager.checkUser(checkUserApiParams);
            if (result.isSuccess()) {  //SG没有，查询SH
                result = checkUserFromSohu(username, clientId);
            }
        }
        return result;
    }


    @Override
    public Result checkRegInBlackListByIpForInternal(String ip, int clientId) throws Exception {
        Result result = new APIResultSupport(false);
        //如果在黑名单，也在白名单，允许注册；如果在黑名单不在白名单，不允许注册
        if (operateTimesService.checkRegInBlackListForInternal(ip, clientId)) {
            if (!operateTimesService.checkRegInWhiteList(ip)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_USERNAME_IP_INBLACKLIST);
                return result;
            }
        }
        result.setSuccess(true);
        return result;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Result checkRegInBlackList(String ip, String cookieStr) throws Exception {

        Result result = new APIResultSupport(false);
        try {
            //检查账号是否在黑名单中
            if (operateTimesService.checkRegInBlackList(ip, cookieStr)) {
                //检查账号是否在白名单中
                if (!operateTimesService.checkRegInWhiteList(ip)) {
                    result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_USERNAME_IP_INBLACKLIST);
                    return result;
                }
            }
        } catch (ServiceException e) {
            logger.error("register checkRegInBlackList Exception", e);
            throw new Exception(e);
        }
        result.setSuccess(true);
        return result;
    }

    @Override
    public void incRegTimes(String ip, String cookieStr) throws Exception {
        try {
            operateTimesService.incRegTimes(ip, cookieStr);
        } catch (ServiceException e) {
            logger.error("register incRegTimes Exception", e);
            throw new Exception(e);
        }
    }

    @Override
    public Result checkMobileSendSMSInBlackList(String ip) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            //检查ip是否在黑名单中
            if (operateTimesService.isMobileSendSMSInBlackList(ip)) {
                //检查ip是否在白名单中
                if (!operateTimesService.checkRegInWhiteList(ip)) {
                    result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_USERNAME_IP_INBLACKLIST);
                    return result;
                }
            }
        } catch (Exception e) {
            logger.error("[manager]method isMobileSendSMSInBlackList error", e);
            throw new Exception(e);
        }
        result.setSuccess(true);
        return result;
    }

    @Override
    public void incSendTimesForMobile(String ip) throws Exception {
        try {
            operateTimesService.incSendTimesForMobile(ip);
        } catch (ServiceException e) {
            logger.error("register incSendTimesForMobile Exception", e);
            throw new Exception(e);
        }
    }

    /*
     * client=1044的username为个性域名或手机号
     * 都有可能是sohuplus的账号，需要判断sohuplus映射表
     * 如果username包含@，则取@前面的
     */

    private Result isSohuplusUser(String username, int clientId) {
        Result result = new APIResultSupport(false);
        if (username.contains("@")) {
            username = username.substring(0, username.indexOf("@"));
        }
        String sohuplus_passportId = snamePassportMappingService.queryPassportIdBySnameOrPhone(username);
        if (!Strings.isNullOrEmpty(sohuplus_passportId)) {
            result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_REGED);
            return result;
        } else {
            result.setSuccess(true);
            result.setMessage("操作成功");
        }
        return result;
    }

    private CheckUserApiParams buildProxyApiParams(String username, int clientId) {
        CheckUserApiParams checkUserApiParams = new CheckUserApiParams();
        checkUserApiParams.setUserid(username);
        checkUserApiParams.setClient_id(clientId);
        return checkUserApiParams;
    }

    @Override
    public boolean isUserInExistBlackList(final String username, final String ip) {
        //校验username是否在账户黑名单中
        if (operateTimesService.isUserInExistBlackList(username, ip)) {
            //是否在白名单中
            if (!operateTimesService.checkLoginUserInWhiteList(username, ip)) {
                return true;
            }
        }
        //次数累加
        operateTimesService.incExistTimes(username, ip);
        return false;
    }
}
