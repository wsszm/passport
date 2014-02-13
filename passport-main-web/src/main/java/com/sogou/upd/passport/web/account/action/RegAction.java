package com.sogou.upd.passport.web.account.action;

import com.google.common.base.Strings;
import com.sogou.upd.passport.common.CommonConstant;
import com.sogou.upd.passport.common.model.useroperationlog.UserOperationLog;
import com.sogou.upd.passport.common.parameter.AccountDomainEnum;
import com.sogou.upd.passport.common.parameter.AccountModuleEnum;
import com.sogou.upd.passport.common.result.APIResultSupport;
import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.common.utils.ErrorUtil;
import com.sogou.upd.passport.common.utils.PhoneUtil;
import com.sogou.upd.passport.common.utils.ServletUtil;
import com.sogou.upd.passport.manager.ManagerHelper;
import com.sogou.upd.passport.manager.account.CommonManager;
import com.sogou.upd.passport.manager.account.RegManager;
import com.sogou.upd.passport.manager.account.SecureManager;
import com.sogou.upd.passport.manager.api.account.LoginApiManager;
import com.sogou.upd.passport.manager.api.account.RegisterApiManager;
import com.sogou.upd.passport.manager.api.account.form.BaseMoblieApiParams;
import com.sogou.upd.passport.manager.api.account.form.CreateCookieUrlApiParams;
import com.sogou.upd.passport.manager.app.ConfigureManager;
import com.sogou.upd.passport.manager.form.ActiveEmailParams;
import com.sogou.upd.passport.manager.form.WebRegisterParams;
import com.sogou.upd.passport.service.account.OperateTimesService;
import com.sogou.upd.passport.web.BaseController;
import com.sogou.upd.passport.web.ControllerHelper;
import com.sogou.upd.passport.web.UserOperationLogUtil;
import com.sogou.upd.passport.web.account.form.CheckUserNameExistParameters;
import com.sogou.upd.passport.web.account.form.MoblieCodeParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;

/**
 * web注册 User: mayan Date: 13-6-7 Time: 下午5:48
 */
@Controller
@RequestMapping("/web")
public class RegAction extends BaseController {

    //  private static final Logger logger = LoggerFactory.getLogger(RegAction.class);
    private static final Logger logger = LoggerFactory.getLogger("com.sogou.upd.passport.regBlackListFileAppender");
    private static final String LOGIN_INDEX_URL = "https://account.sogou.com";

    @Autowired
    private RegManager regManager;
    @Autowired
    private CommonManager commonManager;
    @Autowired
    private ConfigureManager configureManager;
    @Autowired
    private SecureManager secureManager;
    @Autowired
    private RegisterApiManager proxyRegisterApiManager;
    @Autowired
    private RegisterApiManager sgRegisterApiManager;
    @Autowired
    private LoginApiManager proxyLoginApiManager;

    /**
     * 用户注册检查用户名是否存在
     */
    @RequestMapping(value = "/account/checkusername", method = RequestMethod.GET)
    @ResponseBody
    public String checkusername(CheckUserNameExistParameters checkParam)
            throws Exception {

        Result result = new APIResultSupport(false);
        //参数验证
        String validateResult = ControllerHelper.validateParams(checkParam);
        if (!Strings.isNullOrEmpty(validateResult)) {
            result.setCode(ErrorUtil.ERR_CODE_COM_REQURIE);
            result.setMessage(validateResult);
            return result.toString();
        }
        String username = URLDecoder.decode(checkParam.getUsername(), "utf-8");

        String clientIdStr = checkParam.getClient_id();
        int clientId = 1120;
        if (!Strings.isNullOrEmpty(clientIdStr)) {
            clientId = Integer.valueOf(clientIdStr);
        }
        result = checkAccountNotExists(username, clientId);
        if (PhoneUtil.verifyPhoneNumberFormat(username) && ErrorUtil.ERR_CODE_ACCOUNT_REGED.equals(result.getCode())) {
            result.setMessage("此手机号已注册或已绑定，请直接登录");
        }
        return result.toString();
    }

    /**
     * web页面注册
     *
     * @param regParams 传入的参数
     */
    @RequestMapping(value = "/reguser", method = RequestMethod.POST)
    @ResponseBody
    public Object reguser(HttpServletRequest request, HttpServletResponse response, WebRegisterParams regParams, Model model)
            throws Exception {
        Result result = new APIResultSupport(false);
        String ip = null;
        String uuidName = null;
        String finalCode = null;
        try {
            //参数验证
            String validateResult = ControllerHelper.validateParams(regParams);
            if (!Strings.isNullOrEmpty(validateResult)) {
                result.setCode(ErrorUtil.ERR_CODE_COM_REQURIE);
                result.setMessage(validateResult);
                return result.toString();
            }

            ip = getIp(request);
            //校验用户是否允许注册
            uuidName = ServletUtil.getCookie(request, "uuidName");
            result = regManager.checkRegInBlackList(ip, uuidName);
            if (!result.isSuccess()) {
                if (result.getCode().equals(ErrorUtil.ERR_CODE_ACCOUNT_USERNAME_IP_INBLACKLIST)) {
                    finalCode = ErrorUtil.ERR_CODE_ACCOUNT_USERNAME_IP_INBLACKLIST;
                    result.setCode(ErrorUtil.ERR_CODE_REGISTER_UNUSUAL);
                    result.setMessage("注册失败");
                }

                return result.toString();
            }

            //检验用户名是否存在
            String username = regParams.getUsername();
            int clientId = Integer.valueOf(regParams.getClient_id());
            result = checkAccountNotExists(username, clientId);
            if (!result.isSuccess()) {
                return result.toString();
            }

            result = regManager.webRegister(regParams, ip);
            if (result.isSuccess()) {
                //设置来源
                String ru = regParams.getRu();
                if (Strings.isNullOrEmpty(ru)) {
                    ru = LOGIN_INDEX_URL;
                }

                CreateCookieUrlApiParams createCookieUrlApiParams = new CreateCookieUrlApiParams();

                Object obj = result.getModels().get("username");

                createCookieUrlApiParams.setUserid((String) obj);
                createCookieUrlApiParams.setRu(ru);
                createCookieUrlApiParams.setDomain("sogou.com");

                //TODO sogou域账号迁移后cookie生成问题
                Result getCookieValueResult = proxyLoginApiManager.getCookieValue(createCookieUrlApiParams);
                if (getCookieValueResult.isSuccess()) {
                    String ppinf = (String) getCookieValueResult.getModels().get("ppinf");
                    String pprdig = (String) getCookieValueResult.getModels().get("pprdig");
                    ServletUtil.setCookie(response, "ppinf", ppinf, -1, CommonConstant.SOGOU_ROOT_DOMAIN);
                    ServletUtil.setCookie(response, "pprdig", pprdig, -1, CommonConstant.SOGOU_ROOT_DOMAIN);

                    result.setDefaultModel(CommonConstant.RESPONSE_RU, ru);
                }
            }
        } catch (Exception e) {
            logger.error("reguser:User Register Is Failed,Username is " + regParams.getUsername(), e);
        } finally {
            String logCode = null;
            if (!Strings.isNullOrEmpty(finalCode)) {
                logCode = finalCode;
            } else {
                logCode = result.getCode();
            }
            //用户注册log
            UserOperationLog userOperationLog = new UserOperationLog(regParams.getUsername(), request.getRequestURI(), regParams.getClient_id(), logCode, getIp(request));
            String referer = request.getHeader("referer");
            userOperationLog.putOtherMessage("ref", referer);
            UserOperationLogUtil.log(userOperationLog);
        }


        regManager.incRegTimes(ip, uuidName);
        String userId = (String) result.getModels().get("userid");
        if (!Strings.isNullOrEmpty(userId) && AccountDomainEnum.getAccountDomain(userId) != AccountDomainEnum.OTHER) {
            if (result.isSuccess()) {
                // 非外域邮箱用户不用验证，直接注册成功后记录登录记录
                int clientId = Integer.parseInt(regParams.getClient_id());
                secureManager.logActionRecord(userId, clientId, AccountModuleEnum.LOGIN, ip, null);
            }
        }

        return result.toString();
    }

    /**
     * 邮件激活
     *
     * @param activeParams 传入的参数
     */
    @RequestMapping(value = "/activemail", method = RequestMethod.GET)
    @ResponseBody
    public Object activeEmail(HttpServletRequest request, HttpServletResponse response, ActiveEmailParams activeParams)
            throws Exception {
        Result result = new APIResultSupport(false);
        //参数验证
        String validateResult = ControllerHelper.validateParams(activeParams);
        if (!Strings.isNullOrEmpty(validateResult)) {
            result.setCode(ErrorUtil.ERR_CODE_COM_REQURIE);
            result.setMessage(validateResult);
            return result;
        }
        //验证client_id
        int clientId = Integer.parseInt(activeParams.getClient_id());

        //检查client_id是否存在
        if (!configureManager.checkAppIsExist(clientId)) {
            result.setCode(ErrorUtil.INVALID_CLIENTID);
            return result;
        }
        String ip = getIp(request);
        //邮件激活
        result = regManager.activeEmail(activeParams, ip);
        if (result.isSuccess()) {
            // 种sohu域cookie
            result = commonManager.createCookieUrl(result, activeParams.getPassport_id(), "", 1);

            CreateCookieUrlApiParams createCookieUrlApiParams = new CreateCookieUrlApiParams();
            createCookieUrlApiParams.setUserid(activeParams.getPassport_id());
            createCookieUrlApiParams.setRu(activeParams.getRu());
            createCookieUrlApiParams.setDomain("sogou.com");

            //TODO sogou域账号迁移后cookie生成问题
            Result getCookieValueResult = proxyLoginApiManager.getCookieValue(createCookieUrlApiParams);
            if (getCookieValueResult.isSuccess()) {
                String ppinf = (String) getCookieValueResult.getModels().get("ppinf");
                String pprdig = (String) getCookieValueResult.getModels().get("pprdig");
                ServletUtil.setCookie(response, "ppinf", ppinf, -1, CommonConstant.SOGOU_ROOT_DOMAIN);
                ServletUtil.setCookie(response, "pprdig", pprdig, -1, CommonConstant.SOGOU_ROOT_DOMAIN);

                result.setDefaultModel(CommonConstant.RESPONSE_RU, activeParams.getRu());
            }
        }
        return result;
    }

    /**
     * web页面手机账号注册时发送的验证码
     *
     * @param reqParams 传入的参数
     */
    @RequestMapping(value = {"/sendsms"}, method = RequestMethod.GET)
    @ResponseBody
    public Object sendMobileCode(MoblieCodeParams reqParams, HttpServletRequest request)
            throws Exception {
        Result result = new APIResultSupport(false);
        String uuidName;
        String finalCode = null;
        String ip = null;
        try {
            //参数验证
            String validateResult = ControllerHelper.validateParams(reqParams);
            if (!Strings.isNullOrEmpty(validateResult)) {
                result.setCode(ErrorUtil.ERR_CODE_COM_REQURIE);
                result.setMessage(validateResult);
                return result.toString();
            }
            //验证client_id
            int clientId = Integer.parseInt(reqParams.getClient_id());

            //检查client_id是否存在
            if (!configureManager.checkAppIsExist(clientId)) {
                result.setCode(ErrorUtil.INVALID_CLIENTID);
                return result.toString();
            }
            ip = getIp(request);
            //校验用户ip是否中了黑名单
            result = regManager.checkMobileRegInBlackList(ip);
            if (!result.isSuccess()) {
                if (result.getCode().equals(ErrorUtil.ERR_CODE_ACCOUNT_USERNAME_IP_INBLACKLIST)) {
                    finalCode = ErrorUtil.ERR_CODE_ACCOUNT_USERNAME_IP_INBLACKLIST;
                    result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_SMSCODE_SEND);
                    result.setMessage("发送短信失败");
                }
                return result.toString();
            }

            String mobile = reqParams.getMobile();
            //为了数据迁移三个阶段，这里需要转换下参数类
            BaseMoblieApiParams baseMoblieApiParams = buildProxyApiParams(clientId, mobile);
            if (ManagerHelper.isInvokeProxyApi(mobile)) {
                result = proxyRegisterApiManager.sendMobileRegCaptcha(baseMoblieApiParams);
            } else {
                result = sgRegisterApiManager.sendMobileRegCaptcha(baseMoblieApiParams);
            }
        } catch (Exception e) {
            logger.error("method[sendMobileCode] send mobile sms error.{}", e);
        } finally {
            String logCode;
            if (!Strings.isNullOrEmpty(finalCode)) {
                logCode = finalCode;
            } else {
                logCode = result.getCode();
            }
            //web页面手机注册时，发送手机验证码
            UserOperationLog userOperationLog = new UserOperationLog(reqParams.getMobile(), request.getRequestURI(), reqParams.getClient_id(), logCode, ip);
            String referer = request.getHeader("referer");
            userOperationLog.putOtherMessage("ref", referer);
            UserOperationLogUtil.log(userOperationLog);
        }
        regManager.incSendTimesForMobile(ip);
        return result.toString();
    }

    private BaseMoblieApiParams buildProxyApiParams(int clientId, String mobile) {
        BaseMoblieApiParams baseMoblieApiParams = new BaseMoblieApiParams();
        baseMoblieApiParams.setMobile(mobile);
        baseMoblieApiParams.setClient_id(clientId);
        return baseMoblieApiParams;
    }

    //检查用户是否存在
    private Result checkAccountNotExists(String username, int clientId) throws Exception {
        Result result = new APIResultSupport(false);
        //校验是否是搜狐域内用户

        if (AccountDomainEnum.SOHU.equals(AccountDomainEnum.getAccountDomain(username))) {
            result.setCode(ErrorUtil.ERR_CODE_NOTSUPPORT_SOHU_REGISTER);
            return result;
        }
        //校验是否是搜狗用户
        if (AccountDomainEnum.SOGOU.equals(AccountDomainEnum.getAccountDomain(username))) {
            result.setCode(ErrorUtil.ERR_CODE_NOTSUPPORT_SOGOU_REGISTER);
            return result;
        }

        //判断是否是个性账号
        if (username.indexOf("@") == -1) {
            //判断是否是手机号注册
            if (PhoneUtil.verifyPhoneNumberFormat(username)) {
                result = regManager.isAccountNotExists(username, true, clientId);
            } else {
                username = username + "@sogou.com";
                result = regManager.isAccountNotExists(username, false, clientId);
            }
        } else {
            result = regManager.isAccountNotExists(username, false, clientId);
        }
        return result;
    }

    /*
     外域邮箱用户激活成功的页面
   */
    @RequestMapping(value = "/reg/emailverify", method = RequestMethod.GET)
    public String emailVerifySuccess(HttpServletRequest request) throws Exception {
        //状态码参数
        return "reg/emailsuccess";
    }
}
