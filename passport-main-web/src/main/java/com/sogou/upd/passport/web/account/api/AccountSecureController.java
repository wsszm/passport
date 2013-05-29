package com.sogou.upd.passport.web.account.api;

import com.google.common.base.Strings;

import com.sogou.upd.passport.common.lang.StringUtil;
import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.common.utils.ErrorUtil;
import com.sogou.upd.passport.common.utils.PhoneUtil;
import com.sogou.upd.passport.manager.account.AccountLoginManager;
import com.sogou.upd.passport.manager.account.AccountManager;
import com.sogou.upd.passport.manager.account.AccountSecureManager;
import com.sogou.upd.passport.manager.form.AccountAnswerCaptParams;
import com.sogou.upd.passport.manager.form.AccountBindEmailParams;
import com.sogou.upd.passport.manager.form.AccountPwdParams;
import com.sogou.upd.passport.manager.form.AccountPwdScodeParams;
import com.sogou.upd.passport.manager.form.AccountSecureInfoParams;
import com.sogou.upd.passport.manager.form.AccountSmsNewScodeParams;
import com.sogou.upd.passport.manager.form.AccountSmsScodeParams;
import com.sogou.upd.passport.manager.form.BaseAccountParams;
import com.sogou.upd.passport.manager.form.UserModuleTypeParams;
import com.sogou.upd.passport.web.ControllerHelper;
import com.sogou.upd.passport.web.account.action.AccountSecureAction;
import com.sogou.upd.passport.web.annotation.LoginRequired;
import com.sogou.upd.passport.web.inteceptor.HostHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created with IntelliJ IDEA. User: hujunfei Date: 13-5-9 Time: 上午11:50 To change this template use
 * File | Settings | File Templates.
 */
@Controller
@RequestMapping("/api")
public class AccountSecureController {
    private static final Logger logger = LoggerFactory.getLogger(AccountSecureAction.class);

    @Autowired
    private AccountManager accountManager;
    @Autowired
    private AccountSecureManager accountSecureManager;
    @Autowired
    private AccountLoginManager accountLoginManager;
    @Autowired
    private HostHolder hostHolder;

    // TODO:method是POST或GET

    /**
     * 查询密保方式，用于重置密码/修改密保内容
     *
     * @param params 传入的参数
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/secure/query", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    public Object querySecureInfo(AccountSecureInfoParams params) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String username = params.getUsername();
        int clientId = Integer.parseInt(params.getClient_id());
        String passportId = accountManager.getPassportIdByUsername(username);
        if (passportId == null) {
            return Result.buildError(ErrorUtil.ERR_CODE_ACCOUNT_NOTHASACCOUNT);
        }
        params.setUsername(passportId);

        return accountSecureManager.queryAccountSecureInfo(params);
    }

    /**
     * 重置密码（邮件方式）——1.发送重置密码申请验证邮件至注册邮箱
     * @param params
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/findpwd/sendremail", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    public Object sendEmailRegForResetPwd(BaseAccountParams params) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String passportId = params.getPassport_id();
        int clientId = Integer.parseInt(params.getClient_id());
        return accountSecureManager.sendEmailResetPwdByPassportId(passportId, clientId, 1);
    }

    /**
     * 重置密码（邮件方式）——1.发送重置密码申请验证邮件至绑定邮箱
     * @param params
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/findpwd/sendbemail", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    public Object sendEmailBindForResetPwd(BaseAccountParams params) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String passportId = params.getPassport_id();
        int clientId = Integer.parseInt(params.getClient_id());
        return accountSecureManager.sendEmailResetPwdByPassportId(passportId, clientId, 2);
    }

    /*
     * 验证邮件链接的方法在Action里，需要指向某页面，不能作为接口？TODO:Action完成之后删除此注释
     */

    /**
     * 重置密码（邮件方式）——3.再一次验证token，并修改密码
     *
     * @param params
     * @return
     */
    @RequestMapping(value = "/findpwd/email", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    public Object resetPasswordByEmail(AccountPwdScodeParams params) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String passportId = params.getPassport_id();
        int clientId = Integer.parseInt(params.getClient_id());
        String password = params.getPassword();
        String scode = params.getScode();
        return accountSecureManager.resetPasswordByEmail(passportId, clientId, password, scode);
    }

    /**
     * 发送手机验证码——不区分业务，统一接口
     *
     * @param params 传入的参数:
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/sendsms", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    public Object sendSms(UserModuleTypeParams params) throws Exception {
        // TODO:module按业务模块划分
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String username = params.getUsername();
        int clientId = Integer.parseInt(params.getClient_id());
        String mode = params.getMode();
        String module = params.getModule();
        if (mode.equals("1")) {
            // 发送至已绑定手机（验证登录，不传递手机号——适用于修改绑定手机）
            // TODO:验证登录，及登录账号是否=username
            return accountSecureManager.sendMobileCodeByPassportId(username, clientId);
            /*if (PhoneUtil.verifyPhoneNumberFormat(username)) {
                if (!accountManager.isAccountExists(username)) {
                    return Result.buildError(ErrorUtil.ERR_CODE_ACCOUNT_PHONE_OBTAIN_FIELDS);
                }
                return accountSecureManager.sendSmsCodeToMobile(username, clientId);
            } else {
                return accountSecureManager.sendMobileCodeByPassportId(username, clientId);
            }*/
        } else if (mode.equals("2")) {
            // 发送至已绑定手机（不验证登录，不传递手机号——适用于找回密码）
            return accountSecureManager.sendMobileCodeByPassportId(username, clientId);
        } else if (mode.equals("3")) {
            // 发送至未绑定手机（验证登录，传递手机号——适用于修改绑定手机或注册）
            // TODO:验证登录，及登录账号是否=username
            if (PhoneUtil.verifyPhoneNumberFormat(username)) {
                if (accountManager.isAccountExists(username)) {
                    return Result.buildError(ErrorUtil.ERR_CODE_ACCOUNT_PHONE_BINDED);
                }
                return accountSecureManager.sendMobileCode(username, clientId);
            } else {
                return Result.buildError(ErrorUtil.ERR_CODE_ACCOUNT_PHONEERROR);
            }
        } else {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE);
        }
    }

    /**
     * 重置密码（手机方式）——2.检查手机短信码 TODO:可否与其他验证的合并在一起？
     *
     * @param params
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/findpwd/checksms", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    public Object checkSmsResetPwd(AccountSmsScodeParams params) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String passportId = params.getPassport_id();
        int clientId = Integer.parseInt(params.getClient_id());
        String smsCode = params.getSmscode();
        return accountSecureManager.checkMobileCodeResetPwd(passportId, clientId, smsCode);
    }

    /**
     * 重置密码（手机和密保方式）——2.根据scode修改密码
     *
     * @param params
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/findpwd/mobile", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    public Object resetPasswordByMobile(AccountPwdScodeParams params) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String passportId = params.getPassport_id();
        int clientId = Integer.parseInt(params.getClient_id());
        String password = params.getPassword();
        String scode = params.getScode();
        return accountSecureManager.resetPasswordBySecureCode(passportId, clientId, password, scode);
    }

    /**
     * 重置密码（密保方式）——1.验证密保答案及captcha
     *
     * @param params
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/findpwd/checkanswer", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    public Object checkAnswerResetPwd(AccountAnswerCaptParams params) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String passportId = params.getPassport_id();
        int clientId = Integer.parseInt(params.getClient_id());
        String answer = params.getAnswer();
        String captcha = params.getCaptcha();
        String token = params.getToken();
        return accountSecureManager.checkAnswerByPassportId(passportId, clientId, answer, token, captcha);
    }

    /**
     * 重置密码（手机和密保方式）——2.根据scode修改密码 TODO:与手机方式合并，目前代码相同
     *
     * @param params
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/findpwd/ques", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    public Object resetPasswordByQues(AccountPwdScodeParams params) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String passportId = params.getPassport_id();
        int clientId = Integer.parseInt(params.getClient_id());
        String password = params.getPassword();
        String scode = params.getScode();
        return accountSecureManager.resetPasswordBySecureCode(passportId, clientId, password, scode);
    }

    /**
     * 修改密保邮箱——1.验证原绑定邮箱及发送邮件至待绑定邮箱
     *
     * @param params
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/bind/sendemail", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    @LoginRequired
    public Object sendEmailForBind(AccountBindEmailParams params) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String passportId = params.getPassport_id();
        if (!passportId.equals(hostHolder.getPassportId())) {
            return Result.buildError(ErrorUtil.ERR_CODE_ACCOUNT_LOGIN_OPERACCOUNT_MISMATCH);
        }
        int clientId = Integer.parseInt(params.getClient_id());
        String password = params.getPassword();
        String newEmail = params.getNew_email();
        String oldEmail = params.getOld_email();
        return accountSecureManager.sendEmailForBinding(passportId, clientId, password, newEmail, oldEmail);
    }

    /*
     * 验证邮件链接的方法在Action里，需要指向某页面，不能作为接口？TODO:Action完成之后删除此注释
     */

    /**
     * 修改密保手机——1.检查原绑定手机短信码
     *
     * @param params
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/bind/checksms", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    @LoginRequired
    public Object checkSmsBindMobile(AccountSmsScodeParams params) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String passportId = params.getPassport_id();
        if (!passportId.equals(hostHolder.getPassportId())) {
            return Result.buildError(ErrorUtil.ERR_CODE_ACCOUNT_LOGIN_OPERACCOUNT_MISMATCH);
        }
        int clientId = Integer.parseInt(params.getClient_id());
        String smsCode = params.getSmscode();
        return accountSecureManager.checkMobileCodeOldForBinding(passportId, clientId, smsCode);
    }

    /**
     * 修改密保手机——2.验证密码、新绑定手机短信码，绑定新手机号
     *
     * @param params
     * @param scode
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/bind/modifymobile", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    @LoginRequired
    public Object modifyBindMobile(AccountSmsNewScodeParams params, @RequestParam("scode") String scode) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult) || Strings.isNullOrEmpty(scode)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String passportId = params.getPassport_id();
        if (!passportId.equals(hostHolder.getPassportId())) {
            return Result.buildError(ErrorUtil.ERR_CODE_ACCOUNT_LOGIN_OPERACCOUNT_MISMATCH);
        }
        int clientId = Integer.parseInt(params.getClient_id());
        String smsCode = params.getSmscode();
        String newMobile = params.getNew_mobile();
        return accountSecureManager.modifyMobileByPassportId(passportId, clientId, newMobile, smsCode, scode, false);
    }

    /**
     * 修改密保手机——2.验证密码、新绑定手机短信码，绑定新手机号
     *
     * @param params
     * @param password
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/bind/bindmobile", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    @LoginRequired
    public Object bindNewMobile(AccountSmsNewScodeParams params, @RequestParam("password") String password) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult) || Strings.isNullOrEmpty(password)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
        }
        String passportId = params.getPassport_id();
        if (!passportId.equals(hostHolder.getPassportId())) {
            return Result.buildError(ErrorUtil.ERR_CODE_ACCOUNT_LOGIN_OPERACCOUNT_MISMATCH);
        }
        int clientId = Integer.parseInt(params.getClient_id());
        String smsCode = params.getSmscode();
        String newMobile = params.getNew_mobile();
        return accountSecureManager.modifyMobileByPassportId(passportId, clientId, newMobile, smsCode, password, true);
    }

    @RequestMapping(value = "/bind/ques", method = { RequestMethod.POST, RequestMethod.GET })
    @ResponseBody
    @LoginRequired
    public Object bindQues(AccountPwdParams params, @RequestParam("new_ques") String newQues,
            @RequestParam("new_answer") String newAnswer) throws Exception {
        String validateResult = ControllerHelper.validateParams(params);
        if (!Strings.isNullOrEmpty(validateResult) || StringUtil.checkExistNullOrEmpty(newQues, newAnswer)) {
            return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE,
                StringUtil.defaultIfEmpty(validateResult, "必选参数未填"));
        }
        String passportId = params.getPassport_id();
        if (!passportId.equals(hostHolder.getPassportId())) {
            return Result.buildError(ErrorUtil.ERR_CODE_ACCOUNT_LOGIN_OPERACCOUNT_MISMATCH);
        }
        int clientId = Integer.parseInt(params.getClient_id());
        String password = params.getPassword();
        return accountSecureManager.modifyQuesByPassportId(passportId, clientId, password, newQues, newAnswer);
    }
}