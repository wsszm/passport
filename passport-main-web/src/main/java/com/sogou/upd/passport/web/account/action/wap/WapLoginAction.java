package com.sogou.upd.passport.web.account.action.wap;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sogou.upd.passport.common.CommonConstant;
import com.sogou.upd.passport.common.DateAndNumTimesConstant;
import com.sogou.upd.passport.common.LoginConstant;
import com.sogou.upd.passport.common.WapConstant;
import com.sogou.upd.passport.common.math.AES;
import com.sogou.upd.passport.common.math.Coder;
import com.sogou.upd.passport.common.model.useroperationlog.UserOperationLog;
import com.sogou.upd.passport.common.parameter.AccountDomainEnum;
import com.sogou.upd.passport.common.result.APIResultSupport;
import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.common.utils.ErrorUtil;
import com.sogou.upd.passport.common.utils.JacksonJsonMapperUtil;
import com.sogou.upd.passport.common.utils.ServletUtil;
import com.sogou.upd.passport.manager.account.LoginManager;
import com.sogou.upd.passport.manager.account.SecureManager;
import com.sogou.upd.passport.manager.account.WapLoginManager;
import com.sogou.upd.passport.manager.api.account.UserInfoApiManager;
import com.sogou.upd.passport.manager.api.account.form.GetUserInfoApiparams;
import com.sogou.upd.passport.manager.form.WapLoginParams;
import com.sogou.upd.passport.manager.form.WapLogoutParams;
import com.sogou.upd.passport.manager.form.WapPassThroughParams;
import com.sogou.upd.passport.oauth2.common.parameters.QueryParameterApplier;
import com.sogou.upd.passport.web.BaseController;
import com.sogou.upd.passport.web.ControllerHelper;
import com.sogou.upd.passport.web.UserOperationLogUtil;
import com.sogou.upd.passport.web.account.form.WapIndexParams;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: chenjiameng
 * Date: 13-11-12
 * Time: 下午12:17
 * To change this template use File | Settings | File Templates.
 */
@Controller
public class WapLoginAction extends BaseController {

    @Autowired
    private LoginManager loginManager;
    @Autowired
    private WapLoginManager wapLoginManager;

    @Autowired
    private SecureManager secureManager;


    @Autowired
    private UserInfoApiManager proxyUserInfoApiManager;
    @Autowired
    private UserInfoApiManager sgUserInfoApiManager;


    private static final Logger logger = LoggerFactory.getLogger(WapLoginAction.class);
    private static final String SECRETKEY="afE0WZf345@werdm";


    @RequestMapping(value = {"/wap/index"})
    public String index(HttpServletRequest request, HttpServletResponse response, Model model, WapIndexParams wapIndexParams)
            throws Exception {
        Result result = new APIResultSupport(false);
        //参数验证
        String validateResult = ControllerHelper.validateParams(wapIndexParams);
        if (!Strings.isNullOrEmpty(validateResult)) {
            result.setCode(ErrorUtil.ERR_CODE_COM_REQURIE);
            result.setMessage(validateResult);
            response.sendRedirect(getIndexErrorReturnStr(wapIndexParams.getRu(), result.getMessage()));
            return "empty";
        }

        model.addAttribute("v", wapIndexParams.getV());
        model.addAttribute("ru", wapIndexParams.getRu());
        model.addAttribute("client_id", wapIndexParams.getClient_id());
        model.addAttribute("errorMsg", wapIndexParams.getErrorMsg());
        model.addAttribute("isNeedCaptcha", wapIndexParams.getNeedCaptcha());
        //生成token
        String token = RandomStringUtils.randomAlphanumeric(48);
        model.addAttribute("token", token);

        if (WapConstant.WAP_SIMPLE.equals(wapIndexParams.getV())) {
            response.setHeader("Content-Type", "text/vnd.wap.wml;charset=utf-8");
            return "wap/index_simple";
        } else if (WapConstant.WAP_TOUCH.equals(wapIndexParams.getV())) {
            return "wap/index_touch";
        } else {
            return "wap/index_color";
        }
    }

    private String getIndexErrorReturnStr(String ru, String errorMsg) {
        if (!Strings.isNullOrEmpty(ru)) {
            return (ru + "?errorMsg=" + errorMsg);
        }
        return WapConstant.WAP_INDEX + "?errorMsg=" + errorMsg;
    }


    @RequestMapping(value = "/wap/login", method = RequestMethod.POST)
    public String login(HttpServletRequest request, HttpServletResponse response, WapLoginParams loginParams)
            throws Exception {
        Result result = new APIResultSupport(false);
        String ip = getIp(request);
        //参数验证
        String validateResult = ControllerHelper.validateParams(loginParams);
        if (!Strings.isNullOrEmpty(validateResult)) {
            result.setCode(ErrorUtil.ERR_CODE_COM_REQURIE);
            result.setMessage(validateResult);
            return getErrorReturnStr(loginParams, validateResult, 0);
        }

        result = wapLoginManager.accountLogin(loginParams, ip);  //wap端ip做安全限制
        //用户登录log
        UserOperationLog userOperationLog = new UserOperationLog(loginParams.getUsername(), request.getRequestURI(), loginParams.getClient_id(), result.getCode(), ip);
        String referer = request.getHeader("referer");
        userOperationLog.putOtherMessage("ref", referer);
        UserOperationLogUtil.log(userOperationLog);

        if (result.isSuccess()) {
            String userId = result.getModels().get("userid").toString();
            String sgid = result.getModels().get("sgid").toString();

            ServletUtil.setCookie(response, "sgid", sgid, (int) DateAndNumTimesConstant.SIX_MONTH, CommonConstant.SOGOU_ROOT_DOMAIN);

            if (WapConstant.WAP_JSON.equals(loginParams.getV())) {
                //在返回的数据中导入 json格式，用来给客户端用。
                //第三方获取个人资料
                AccountDomainEnum domain = AccountDomainEnum.getAccountDomain(result.getModels().get("userid").toString());
                // 调用内部接口
                GetUserInfoApiparams userInfoApiparams=new GetUserInfoApiparams(result.getModels().get("userid").toString(),"uniqname,avatarurl,gender");
                if (domain == AccountDomainEnum.THIRD) {
                    result = sgUserInfoApiManager.getUserInfo(userInfoApiparams);
                }else {
                    result = proxyUserInfoApiManager.getUserInfo(userInfoApiparams);
                }

                //result = secureManager.queryAccountSecureInfo(userId, Integer.parseInt(loginParams.getClient_id()), true);
                result.getModels().put("sgid",sgid);
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(result.toString());
                wapLoginManager.doAfterLoginSuccess(loginParams.getUsername(), ip, userId, Integer.parseInt(loginParams.getClient_id()));

                return "empty";

            }

            wapLoginManager.doAfterLoginSuccess(loginParams.getUsername(), ip, userId, Integer.parseInt(loginParams.getClient_id()));
            response.sendRedirect(getSuccessReturnStr(loginParams.getRu(), sgid));
            return "empty";
        } else {
            int isNeedCaptcha = 0;
            loginManager.doAfterLoginFailed(loginParams.getUsername(), ip,result.getCode());
            //校验是否需要验证码
            boolean needCaptcha = wapLoginManager.needCaptchaCheck(loginParams.getClient_id(), loginParams.getUsername(), getIp(request));
            if (needCaptcha) {
                isNeedCaptcha = 1;
            }
            if (result.getCode().equals(ErrorUtil.ERR_CODE_ACCOUNT_USERNAME_IP_INBLACKLIST)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_USERNAME_PWD_ERROR);
                result.setMessage("密码错误");
            }

            if (WapConstant.WAP_JSON.equals(loginParams.getV())) {

                if(needCaptcha){
                    if(result.getCode()!=ErrorUtil.ERR_CODE_ACCOUNT_USERNAME_PWD_ERROR) {
                        result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_PHONE_NOT_MATCH_SMSCODE);
                        result.setMessage("验证码错误或已过期");
                    }
                }else{
                    result.setCode(ErrorUtil.ERR_CODE_COM_REQURIE);
                    result.setMessage("用户名或者密码错误");
                }
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(result.toString());
                return "empty";
            }

            return getErrorReturnStr(loginParams, "用户名或者密码错误", isNeedCaptcha);

        }
    }

    /**
     * wap sgid透传
     */
    @RequestMapping(value = "/wap/passthrough_qq", method = RequestMethod.GET)
    public String qqPassThrough(HttpServletRequest req,
                                HttpServletResponse res,
                                WapPassThroughParams params) {
        String openId = null;
        String accessToken = null;
        String ru = null;
        String ip = null;
        String expires_in = null;
        String data=null;
        try {
            // 校验参数
            ru = req.getParameter(CommonConstant.RESPONSE_RU);
            try {
                ru = URLDecoder.decode(ru, CommonConstant.DEFAULT_CONTENT_CHARSET);
                String validateResult = ControllerHelper.validateParams(params);
                if (!Strings.isNullOrEmpty(validateResult)) {
                    ru = buildErrorRu(ru, ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
                    res.sendRedirect(ru);
                    return "empty";
                }
            } catch (UnsupportedEncodingException e) {
                logger.error("Url decode Exception! ru:" + ru);
                ru = CommonConstant.DEFAULT_CONNECT_REDIRECT_URL;
            }

            data = params.getData();
            //获取QQ回跳参数
            byte[] buf= Hex.decodeHex(data.toCharArray());
            String decryptedValue = new String(AES.decrypt(buf, SECRETKEY), CommonConstant.DEFAULT_CONTENT_CHARSET);

            QQPassthroughParam param= JacksonJsonMapperUtil.getMapper().readValue(decryptedValue,QQPassthroughParam.class);
            accessToken = param.getAccess_token();
            openId = param.getOpenid();
            expires_in = param.getExpires_in();

            ip = getIp(req);

            //获取sgid
            String sgid = ServletUtil.getCookie(req, LoginConstant.COOKIE_SGID);

            //暂时只是sogou小说调用 client_id为 1115
            int clientId=CommonConstant.XIAOSHUO_CLIENTID;
            Result result = wapLoginManager.passThroughQQ(clientId,sgid, accessToken, openId, ip, expires_in);
            if (result.isSuccess()) {
                sgid = (String) result.getModels().get("sgid");
                ServletUtil.setCookie(res, "sgid", sgid, (int) DateAndNumTimesConstant.SIX_MONTH, CommonConstant.SOGOU_ROOT_DOMAIN);

                ru = buildSuccessRu(ru, sgid);
                res.sendRedirect(ru);
                return "empty";
            } else {
                ru = buildErrorRu(ru, ErrorUtil.ERR_CODE_CONNECT_PASSTHROUGH, null);
                res.sendRedirect(ru);
                return "empty";
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("/wap/passthrough_qq " + "openId:" + openId + ",accessToken:" + accessToken);
            }
        } finally {
//            用于记录log
            UserOperationLog userOperationLog = new UserOperationLog(openId, accessToken, "0", ip);
            String referer = req.getHeader("referer");
            userOperationLog.putOtherMessage("ref", referer);
            userOperationLog.putOtherMessage(CommonConstant.RESPONSE_RU, ru);
            UserOperationLogUtil.log(userOperationLog);
        }
        return "empty";
    }

    private String buildSuccessRu(String ru, String sgid) {
        Map params = Maps.newHashMap();
        try {
            ru = URLDecoder.decode(ru, CommonConstant.DEFAULT_CONTENT_CHARSET);
        } catch (Exception e) {
            logger.error("Url decode Exception! ru:" + ru);
            ru = CommonConstant.DEFAULT_WAP_URL;
        }
        //ru后缀一个sgid
        params.put("sgid", sgid);
        ru = QueryParameterApplier.applyOAuthParametersString(ru, params);
        return ru;
    }

    /**
     * wap页面退出
     * 页面直接跳转，回跳到之前的地址
     */
    @RequestMapping(value = "/wap/logout_redirect", method = RequestMethod.GET)
    public void logoutWithRu(HttpServletRequest request,
                               HttpServletResponse response,
                               WapLogoutParams params) {
        // 校验参数
        String sgid = null;
        String client_id = null;
        String ru = null;
        try {
            ru = params.getRu();
            String validateResult = ControllerHelper.validateParams(params);
            if (!Strings.isNullOrEmpty(validateResult)) {
                ru = buildErrorRu(CommonConstant.DEFAULT_WAP_URL, ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
                response.sendRedirect(ru);
                return ;
            }

            sgid = ServletUtil.getCookie(request, LoginConstant.COOKIE_SGID);
            sgid = Strings.isNullOrEmpty(sgid)?params.getSgid():sgid;
            client_id = params.getClient_id();

            //处理ru
            if (Strings.isNullOrEmpty(ru)) {
                ru = CommonConstant.DEFAULT_WAP_URL;
            }
            //session server中清除cookie
            Result result = wapLoginManager.removeSession(sgid);
            if (result.isSuccess()) {
                //清除cookie
                ServletUtil.clearCookie(response, LoginConstant.COOKIE_SGID);
                response.sendRedirect(ru);
                return;
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("logout_redirect " + "sgid:" + sgid + ",client_id:" + client_id);
            }
        } finally {
            //用于记录log
            UserOperationLog userOperationLog = new UserOperationLog(sgid, client_id, "0", getIp(request));
            String referer = request.getHeader("referer");
            userOperationLog.putOtherMessage("ref", referer);
            userOperationLog.putOtherMessage(CommonConstant.RESPONSE_RU, ru);
            UserOperationLogUtil.log(userOperationLog);
        }
        ru = buildErrorRu(ru, ErrorUtil.ERR_CODE_REMOVE_COOKIE_FAILED, "error");
        try {
            response.sendRedirect(ru);
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("logout_redirect " + "sgid:" + sgid + ",client_id:" + client_id);
            }
        }
        return;
    }

    /**
     * 第三方登录接口错误返回结果的跳转url
     *
     * @param ru        回调url
     * @param errorCode 错误码
     * @param errorText 错误文案
     * @return
     */
    protected String buildErrorRu(String ru, String errorCode, String errorText) {
        try {
            if (Strings.isNullOrEmpty(ru)) {
                ru = CommonConstant.DEFAULT_WAP_CONNECT_REDIRECT_URL;
            }
            if (!Strings.isNullOrEmpty(errorCode)) {
                Map params = Maps.newHashMap();
                params.put(CommonConstant.RESPONSE_STATUS, errorCode);
                if (Strings.isNullOrEmpty(errorText)) {
                    errorText = ErrorUtil.ERR_CODE_MSG_MAP.get(errorCode);
                }
                params.put(CommonConstant.RESPONSE_STATUS_TEXT, errorText);
                ru = QueryParameterApplier.applyOAuthParametersString(ru, params);
            }
        } catch (Exception e) {
            logger.error("buildErrorRu! ru:" + ru);
        }
        return ru;
    }

    private String getSuccessReturnStr(String ru, String token) {
        String deRu = Coder.decodeUTF8(ru);
        if (deRu.contains("?")) {
            return deRu + "&sgid=" + token;
        }
        return deRu + "?sgid=" + token;
    }

    private String getErrorReturnStr(WapLoginParams loginParams, String errorMsg, int isNeedCaptcha) {
        StringBuilder returnStr = new StringBuilder();
        returnStr.append("redirect:/wap/index?");
        if (!Strings.isNullOrEmpty(loginParams.getV())) {
            returnStr.append("v=" + loginParams.getV());
        }
        if (!Strings.isNullOrEmpty(loginParams.getRu())) {
            returnStr.append("&ru=" + loginParams.getRu());
        }
        if (!Strings.isNullOrEmpty(loginParams.getClient_id())) {
            returnStr.append("&client_id=" + loginParams.getClient_id());
        }
        if (!Strings.isNullOrEmpty(errorMsg)) {
            returnStr.append("&errorMsg=" + Coder.encodeUTF8(errorMsg));
        }
        returnStr.append("&needCaptcha=" + isNeedCaptcha);
        return returnStr.toString();
    }
}
class QQPassthroughParam{
    private String access_token;
    private String openid;
    private String expires_in;

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(String expires_in) {
        this.expires_in = expires_in;
    }
}
