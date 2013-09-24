package com.sogou.upd.passport.manager.account.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sogou.upd.passport.common.lang.StringUtil;
import com.sogou.upd.passport.common.parameter.AccountDomainEnum;
import com.sogou.upd.passport.common.parameter.AccountModuleEnum;
import com.sogou.upd.passport.common.result.APIResultSupport;
import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.common.utils.ErrorUtil;
import com.sogou.upd.passport.common.utils.PhoneUtil;
import com.sogou.upd.passport.exception.ServiceException;
import com.sogou.upd.passport.manager.ManagerHelper;
import com.sogou.upd.passport.manager.account.ResetPwdManager;
import com.sogou.upd.passport.manager.account.vo.AccountSecureInfoVO;
import com.sogou.upd.passport.manager.api.account.SecureApiManager;
import com.sogou.upd.passport.manager.api.account.UserInfoApiManager;
import com.sogou.upd.passport.manager.api.account.form.GetSecureInfoApiParams;
import com.sogou.upd.passport.manager.api.account.form.GetUserInfoApiparams;
import com.sogou.upd.passport.model.account.Account;
import com.sogou.upd.passport.model.account.AccountInfo;
import com.sogou.upd.passport.service.account.*;
import com.sogou.upd.passport.service.app.AppConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: hujunfei Date: 13-6-8 Time: 上午10:46 To change this template use
 * File | Settings | File Templates.
 */
@Component
public class ResetPwdManagerImpl implements ResetPwdManager {
    private static Logger logger = LoggerFactory.getLogger(ResetPwdManagerImpl.class);

    private static String SECURE_FIELDS = "sec_email,sec_mobile,sec_ques";

    @Autowired
    private MobileCodeSenderService mobileCodeSenderService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AccountInfoService accountInfoService;
    @Autowired
    private AccountTokenService accountTokenService;
    @Autowired
    private AppConfigService appConfigService;
    @Autowired
    private MobilePassportMappingService mobilePassportMappingService;
    @Autowired
    private EmailSenderService emailSenderService;
    @Autowired
    private AccountSecureService accountSecureService;
    @Autowired
    private OperateTimesService operateTimesService;

    // Manager
    @Autowired
    private SecureApiManager sgSecureApiManager;
    @Autowired
    private SecureApiManager proxySecureApiManager;
    @Autowired
    private UserInfoApiManager proxyUserInfoApiManager;

    @Override
    public Result queryAccountSecureInfo(String username, int clientId, boolean doProcess) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            String userId = username;
            if (PhoneUtil.verifyPhoneNumberFormat(username)) {
                userId = mobilePassportMappingService.queryPassportIdByMobile(username);
                if (Strings.isNullOrEmpty(userId)) {
                    userId += "@sohu.com";
                }
            } else {
                // 不查询account表
                if (username.indexOf("@") == -1) {
                    userId += "@sogou.com";
                }
            }

            // 判断是否超过修改密码次数
            if (operateTimesService.checkLimitResetPwd(username, clientId)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_RESETPASSWORD_LIMITED);
                return result;
            }

            Account account = accountService.queryNormalAccount(userId);
            if (account == null) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_NOTHASACCOUNT);
                return result;
            }

            AccountSecureInfoVO accountSecureInfoVO = new AccountSecureInfoVO();

            if (ManagerHelper.isInvokeProxyApi(userId)) {
                // 代理接口
                GetUserInfoApiparams getUserInfoApiparams = new GetUserInfoApiparams();
                getUserInfoApiparams.setUserid(userId);
                getUserInfoApiparams.setClient_id(clientId);
                getUserInfoApiparams.setFields(SECURE_FIELDS);
                result = proxyUserInfoApiManager.getUserInfo(getUserInfoApiparams);
            } else {
                GetSecureInfoApiParams params = new GetSecureInfoApiParams();
                params.setUserid(userId);
                params.setClient_id(clientId);
                result = sgSecureApiManager.getUserSecureInfo(params);
            }

            Map<String, String> map = result.getModels();
            result.setModels(Maps.newHashMap());

            if (!result.isSuccess()) {
                return result;
            }

            String mobile = map.get("sec_mobile");
            String emailBind = map.get("sec_email");
            String question = map.get("sec_ques");

            if (doProcess) {
                if (!Strings.isNullOrEmpty(emailBind)) {
                    String emailProcessed = StringUtil.processEmail(emailBind);
                    accountSecureInfoVO.setSec_email(emailProcessed);
                }
                if (!Strings.isNullOrEmpty(mobile)) {
                    String mobileProcessed = StringUtil.processMobile(mobile);
                    accountSecureInfoVO.setSec_mobile(mobileProcessed);
                }
                if (!Strings.isNullOrEmpty(question)) {
                    accountSecureInfoVO.setSec_ques(question);
                }
                if (AccountDomainEnum.getAccountDomain(userId) == AccountDomainEnum.OTHER) {
                    String emailRegProcessed = StringUtil.processEmail(userId);
                    accountSecureInfoVO.setReg_email(emailRegProcessed);
                }
            } else {
                if (!Strings.isNullOrEmpty(emailBind)) {
                    accountSecureInfoVO.setSec_email(emailBind);
                }
                if (!Strings.isNullOrEmpty(mobile)) {
                    accountSecureInfoVO.setSec_mobile(mobile);
                }
                if (!Strings.isNullOrEmpty(question)) {
                    accountSecureInfoVO.setSec_ques(question);
                }
                if (AccountDomainEnum.getAccountDomain(userId) == AccountDomainEnum.OTHER) {
                    accountSecureInfoVO.setReg_email(userId);
                }
            }
            /*
            String secMobile = accountSecureInfoVO.getSec_mobile();
            String secEmail = accountSecureInfoVO.getSec_email();
            String secQues = accountSecureInfoVO.getSec_ques();
            String regEmail = accountSecureInfoVO.getReg_email();
            */
            result.setSuccess(true);
            result.setMessage("查询成功");
            result.setDefaultModel(accountSecureInfoVO);
            return result;
        } catch (ServiceException e) {
            logger.error("query account_secure_info Fail:", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
            return result;
        }
    }

    private Result sendEmailResetPwd(String passportId, int clientId, AccountModuleEnum module,
                                     String email) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            if (!emailSenderService.checkLimitForSendEmail(passportId, clientId, module, email)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_SENDEMAIL_LIMITED);
                return result;
            }
            if (!emailSenderService.sendEmail(passportId, clientId, module, email, false)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNTSECURE_SENDEMAIL_FAILED);
                return result;
            }
            result.setSuccess(true);
            result.setMessage("重置密码申请邮件发送成功");
            return result;
        } catch (ServiceException e) {
            logger.error("send email for reset pwd fail:", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
            return result;
        }
    }

    @Override
    public Result resetPasswordByQues(String passportId, int clientId, String password,
                                      String answer) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            Account account = accountService.queryNormalAccount(passportId);
            if (account == null) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_NOTHASACCOUNT);
                return result;
            }
            AccountInfo accountInfo = accountInfoService.queryAccountInfoByPassportId(passportId);
            if (accountInfo == null || Strings.isNullOrEmpty(accountInfo.getAnswer())) {
                result.setCode(ErrorUtil.NOTHAS_BINDINGQUESTION);
                return result;
            }
            String answerBind = accountInfo.getAnswer();
            if (!answer.equals(answerBind)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNTSECURE_CHECKANSWER_FAILED);
                return result;
            }
            if (!accountService.resetPassword(account, password, false)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_RESETPASSWORD_FAILED);
                return result;
            }
            operateTimesService.incLimitResetPwd(passportId, clientId);
            result.setSuccess(true);
            result.setMessage("重置密码成功！");
            return result;
        } catch (ServiceException e) {
            logger.error("reset password by ques fail:", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
            return result;
        }
    }

    @Override
    public Result resetPasswordByMobile(String passportId, int clientId, String password,
                                        String smsCode) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            Account account = accountService.queryAccountByPassportId(passportId);
            if (account == null) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_NOTHASACCOUNT);
                return result;
            }
            String mobile = account.getMobile();
            if (Strings.isNullOrEmpty(mobile)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_PHONE_OBTAIN_FIELDS);
                return result;
            }

            // 验证错误次数是否小于限制次数
            boolean checkFailLimited =
                    mobileCodeSenderService.checkLimitForSmsFail(mobile, clientId,
                                                                 AccountModuleEnum.RESETPWD);
            if (!checkFailLimited) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_CHECKSMSCODE_LIMIT);
                return result;
            }

            // 验证手机号码与验证码是否匹配
            if (!mobileCodeSenderService.checkSmsInfoFromCache(mobile, clientId, AccountModuleEnum.RESETPWD, smsCode)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_PHONE_NOT_MATCH_SMSCODE);
                return result;
            }

            if (!accountService.resetPassword(account, password, false)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_RESETPASSWORD_FAILED);
                return result;
            }
            //清除验证码的缓存
            mobileCodeSenderService.deleteSmsCache(mobile, clientId);
            operateTimesService.incLimitResetPwd(passportId, clientId);
            result.setSuccess(true);
            result.setMessage("重置密码成功！");
            return result;
        } catch (ServiceException e) {
            logger.error("reset password Fail:", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
            return result;
        }
    }


    /* ------------------------------------重置密码Begin------------------------------------ */

    /*
     * 重置密码（邮件方式）——1.发送重置密码申请验证邮件
     */
    @Override
    public Result sendEmailResetPwdByPassportId(String passportId, int clientId, boolean useRegEmail)
            throws Exception {
        Result result = new APIResultSupport(false);
        try {
            AccountModuleEnum module = AccountModuleEnum.RESETPWD;
            if (useRegEmail) {
                // 使用注册邮箱
                boolean isOtherDomain = (AccountDomainEnum.getAccountDomain(passportId) ==
                                         AccountDomainEnum.OTHER);
                if (isOtherDomain) {
                    // 外域用户无绑定邮箱
                    return sendEmailResetPwd(passportId, clientId, module, passportId);
                } else {
                    result.setCode(ErrorUtil.ERR_CODE_ACCOUNTSECURE_RESETPWD_EMAIL_FAILED);
                    return result;
                }
            } else {
                // 使用绑定邮箱
                AccountInfo accountInfo = accountInfoService.queryAccountInfoByPassportId(passportId);
                if (accountInfo == null || Strings.isNullOrEmpty(accountInfo.getEmail())) {
                    result.setCode(ErrorUtil.NOTHAS_BINDINGEMAIL);
                    return result;
                } else {
                    String emailBind = accountInfo.getEmail();
                    return sendEmailResetPwd(passportId, clientId, module, emailBind);
                }
            }
        } catch (ServiceException e) {
            logger.error("send email for reset pwd by passportId fail:", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
            return result;
        }
    }

    /*
     * 重置密码（邮件方式）——2.验证重置密码申请链接
     */
    @Override
    public Result checkEmailResetPwd(String passportId, int clientId, String scode) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            boolean saveEmail = false;
            AccountModuleEnum module = AccountModuleEnum.RESETPWD;
            String resultStr = emailSenderService.checkScodeForEmail(passportId, clientId, module, scode, saveEmail);
            if (Strings.isNullOrEmpty(resultStr)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNTSECURE_RESETPWD_URL_FAILED);
                return result;
            }
            result.setSuccess(true);
            result.setMessage("重置密码申请链接验证成功");
            return result;
        } catch (ServiceException e) {
            logger.error("check email fail:", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
            return result;
        }
    }

    /*
     * 重置密码（邮件方式）——3.再一次验证token，并修改密码。目前passportId与邮件申请链接中的uid一样
     */
    @Override
    public Result resetPasswordByEmail(String passportId, int clientId, String password,
                                       String scode) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            boolean saveEmail = false;
            AccountModuleEnum module = AccountModuleEnum.RESETPWD;
            Account account = accountService.queryNormalAccount(passportId);
            if (account == null) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_NOTHASACCOUNT);
                return result;
            }
            String resultStr = emailSenderService.checkScodeForEmail(passportId, clientId, module, scode, saveEmail);
            if (Strings.isNullOrEmpty(resultStr)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNTSECURE_RESETPWD_URL_FAILED);
                return result;
            }
            if (!accountService.resetPassword(account, password, false)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_RESETPASSWORD_FAILED);
                return result;
            }
            // 删除邮件链接token缓存
            emailSenderService.deleteScodeCacheForEmail(passportId, clientId, module);
            operateTimesService.incLimitResetPwd(passportId, clientId);
            result.setSuccess(true);
            result.setMessage("重置密码成功！");
            return result;
        } catch (ServiceException e) {
            logger.error("reset password Fail:", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
            return result;
        }
    }

    /*
     * 重置密码（手机方式）——2.检查手机短信码，成功则返回secureCode记录成功标志
     *                      （1.发送见sendMobileCode***）
     */
    @Override
    public Result checkMobileCodeResetPwd(String passportId, int clientId, String smsCode)
            throws Exception {
        Result result = new APIResultSupport(false);
        // TODO:与checkMobileCodeOldForBinding整合
        try {
            // result = checkMobileCodeByPassportId(passportId, clientId, smsCode);
            if (result.isSuccess()) {
                result.setDefaultModel("scode", accountSecureService.getSecureCodeResetPwd(passportId, clientId));
            }
            return result;
        } catch (ServiceException e) {
            logger.error("check mobile code reset pwd Fail:", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
            return result;
        }
    }

    /*
     * 重置密码（密保方式）——1.验证密保答案及captcha，成功则返回secureCode记录成功标志。(可用于其他功能模块)
     */
    @Override
    public Result checkAnswerByPassportId(String passportId, int clientId, String answer,
                                          String token, String captcha) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            if (!accountService.checkCaptchaCodeIsVaild(token, captcha)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_CAPTCHA_CODE_FAILED);
                return result;
            }
            // 不需要检测Account是否存在，在修改密码时检测，避免二次查询缓存/数据库
            AccountInfo accountInfo = accountInfoService.queryAccountInfoByPassportId(passportId);
            if (accountInfo == null || Strings.isNullOrEmpty(accountInfo.getAnswer())) {
                result.setCode(ErrorUtil.NOTHAS_BINDINGQUESTION);
                return result;
            }
            String answerBind = accountInfo.getAnswer();
            if (!answer.equals(answerBind)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNTSECURE_CHECKANSWER_FAILED);
                return result;
            }
            result.setSuccess(true);
            result.setMessage("验证密保答案成功！");
            result.setDefaultModel("scode", accountSecureService.getSecureCodeResetPwd(passportId, clientId));
            return result;
        } catch (ServiceException e) {
            logger.error("check secure answer Fail:", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
            return result;
        }
    }

    /*
     * 重置密码（手机和密保方式）——2.根据secureCode修改密码（secureCode由上一步验证手机或密保问题成功获取）
     */
    @Override
    public Result resetPasswordByScode(String passportId, int clientId, String password,
                                       String scode) throws Exception {
        Result result = new APIResultSupport(false);
        // TODO:启用后，删除ByMobile和ByQues
        try {
            if (!accountSecureService.checkSecureCodeResetPwd(passportId, clientId, scode)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNTSECURE_RESETPWD_URL_FAILED);
                return result;
            }
            Account account = accountService.queryAccountByPassportId(passportId);
            if (account == null) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_NOTHASACCOUNT);
                return result;
            }
            if (!accountService.resetPassword(account, password, false)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_RESETPASSWORD_FAILED);
                return result;
            }
            operateTimesService.incLimitResetPwd(passportId, clientId);
            result.setSuccess(true);
            result.setMessage("重置密码成功！");
            return result;
            // TODO:检验checkCode，是否区分密保还是手机验证码——未区分
            // TODO:在修改绑定手机时，能否重用checkCode代码——未重用
            // TODO:能否将邮件产生token的代码提取出来统一产生checkCode?——暂时未用，可考虑
        } catch (ServiceException e) {
            logger.error("reset password Fail:", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
            return result;
        }
    }

    /*
     * 只修改密码加检测限制
     */
    @Override
    public Result resetPassword(String passportId, int clientId, String password) throws Exception {
        Result result = new APIResultSupport(false);
        try {
            Account account = accountService.queryNormalAccount(passportId);
            if (account == null) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_NOTHASACCOUNT);
                return result;
            }
            if (!accountService.resetPassword(account, password, false)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_RESETPASSWORD_FAILED);
                return result;
            }
            operateTimesService.incLimitResetPwd(passportId, clientId);
            result.setSuccess(true);
            result.setMessage("重置密码成功！");
            return result;
        } catch (ServiceException e) {
            logger.error("reset password Fail:", e);
            result.setCode(ErrorUtil.SYSTEM_UNKNOWN_EXCEPTION);
            return result;
        }
    }

    /* ------------------------------------重置密码End------------------------------------ */

}
