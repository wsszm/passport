package com.sogou.upd.passport.web.account.action.wap;

import com.google.common.base.Strings;
import com.sogou.upd.passport.common.WapConstant;
import com.sogou.upd.passport.common.math.Coder;
import com.sogou.upd.passport.common.model.useroperationlog.UserOperationLog;
import com.sogou.upd.passport.common.result.APIResultSupport;
import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.common.utils.ErrorUtil;
import com.sogou.upd.passport.manager.account.LoginManager;
import com.sogou.upd.passport.manager.account.WapLoginManager;
import com.sogou.upd.passport.manager.form.WapLoginParams;
import com.sogou.upd.passport.web.BaseController;
import com.sogou.upd.passport.web.ControllerHelper;
import com.sogou.upd.passport.web.UserOperationLogUtil;
import com.sogou.upd.passport.web.account.form.WapIndexParams;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.net.URLEncoder;

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
            response.setHeader("Content-Type","text/vnd.wap.wml;charset=utf-8");
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
    public String login(HttpServletRequest request, HttpServletResponse response, Model model, WapLoginParams loginParams)
            throws Exception {
        Result result = new APIResultSupport(false);
        String ip = getIp(request);
        //参数验证
        String validateResult = ControllerHelper.validateParams(loginParams);
        if (!Strings.isNullOrEmpty(validateResult)) {
            result.setCode(ErrorUtil.ERR_CODE_COM_REQURIE);
            result.setMessage(validateResult);
            return getErrorReturnStr(loginParams,validateResult, 0);
        }

        result = wapLoginManager.accountLogin(loginParams, ip);
        //用户登录log
        UserOperationLog userOperationLog = new UserOperationLog(loginParams.getUsername(), request.getRequestURI(), loginParams.getClient_id(), result.getCode(), getIp(request));
        String referer = request.getHeader("referer");
        userOperationLog.putOtherMessage("ref", referer);
        UserOperationLogUtil.log(userOperationLog);

        if (result.isSuccess()) {
            String userId = result.getModels().get("userid").toString();
            String token = result.getModels().get("token").toString();
            wapLoginManager.doAfterLoginSuccess(loginParams.getUsername(), ip, userId, Integer.parseInt(loginParams.getClient_id()));
            response.sendRedirect(getSuccessReturnStr(loginParams.getRu(),token));
            return "empty";
        } else {
            int isNeedCaptcha = 0;
            loginManager.doAfterLoginFailed(loginParams.getUsername(), ip);
            //校验是否需要验证码
            boolean needCaptcha = wapLoginManager.needCaptchaCheck(loginParams.getClient_id(), loginParams.getUsername(), getIp(request));
            if (needCaptcha) {
                isNeedCaptcha = 1;
            }
            if (result.getCode().equals(ErrorUtil.ERR_CODE_ACCOUNT_USERNAME_IP_INBLACKLIST)) {
                result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_USERNAME_PWD_ERROR);
                result.setMessage("密码错误");
            }
            return getErrorReturnStr(loginParams,"用户名或者密码错误", isNeedCaptcha);

        }
    }

    private String getSuccessReturnStr(String ru, String token) {
        String deRu = Coder.decodeUTF8(ru);
        if (deRu.contains("?")) {
            return deRu + "&token=" + token;
        }
        return deRu + "?token=" + token;
    }

    private String getErrorReturnStr(WapLoginParams loginParams,String errorMsg, int isNeedCaptcha) {
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