package com.sogou.upd.passport.web.account.action;

import com.google.common.base.Strings;

import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.common.utils.ErrorUtil;
import com.sogou.upd.passport.common.utils.PhoneUtil;
import com.sogou.upd.passport.manager.account.AccountManager;
import com.sogou.upd.passport.manager.account.AccountRegManager;
import com.sogou.upd.passport.manager.app.ConfigureManager;
import com.sogou.upd.passport.manager.form.ActiveEmailParameters;
import com.sogou.upd.passport.manager.form.WebRegisterParameters;
import com.sogou.upd.passport.web.BaseController;
import com.sogou.upd.passport.web.ControllerHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: mayan Date: 13-4-28 Time: 下午4:07 To change this template use File | Settings | File
 * Templates.
 */
@Controller
@RequestMapping("/web")
public class AccountAction extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(AccountAction.class);

  @Autowired
  private AccountRegManager accountRegManager;
  @Autowired
  private AccountManager accountManager;
  @Autowired
  private ConfigureManager configureManager;

  /*
     web注册页跳转
   */
  @RequestMapping(value = "/register", method = RequestMethod.GET)
  public String register(HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    return "reg";
  }

  /**
   * web页面注册
   *
   * @param regParams 传入的参数
   */
  @RequestMapping(value = "/reguser", method = RequestMethod.POST)
  @ResponseBody
  public Object reguser(HttpServletRequest request, WebRegisterParameters regParams)
      throws Exception {

    //参数验证
    String validateResult = ControllerHelper.validateParams(regParams);
    if (!Strings.isNullOrEmpty(validateResult)) {
      return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
    }
    Result result = null;
    //验证码校验
    String captchaCode = regParams.getVcode();
    String token=regParams.getToken();
    result=accountRegManager.checkCaptchaCodeIsVaild(token,captchaCode);
    if(result!=null){
      return result;
    }
    //验证client_id
    int clientId;
    try {
      clientId = Integer.parseInt(regParams.getClient_id());
    } catch (NumberFormatException e) {
      return Result.buildError(ErrorUtil.ERR_FORMAT_CLIENTID);
    }
    //检查client_id格式以及client_id是否存在
    if (!configureManager.checkAppIsExist(clientId)) {
      return Result.buildError(ErrorUtil.INVALID_CLIENTID);
    }

    String username = regParams.getUsername();

    //验证用户是否注册过
    if (!accountManager.isAccountExists(username)) {
      String ip = getIp(request);
      result = accountRegManager.webRegister(regParams, ip);
    } else {
      result = result.buildError(ErrorUtil.ERR_CODE_ACCOUNT_REGED);
    }
    return result;
  }

  /**
   * 邮件激活
   *
   * @param activeParams 传入的参数
   */
  @RequestMapping(value = "/activemail", method = RequestMethod.GET)
  @ResponseBody
  public Object activeEmail(HttpServletRequest request, ActiveEmailParameters activeParams)
      throws Exception {

    //参数验证
    String validateResult = ControllerHelper.validateParams(activeParams);
    if (!Strings.isNullOrEmpty(validateResult)) {
      return Result.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
    }
    //验证client_id
    int clientId;
    try {
      clientId = Integer.parseInt(activeParams.getClient_id());
    } catch (NumberFormatException e) {
      return Result.buildError(ErrorUtil.ERR_FORMAT_CLIENTID);
    }
    //检查client_id是否存在
    if (!configureManager.checkAppIsExist(clientId)) {
      return Result.buildError(ErrorUtil.INVALID_CLIENTID);
    }

    //邮件激活
    Result result = accountRegManager.activeEmail(activeParams);

    return result;
  }
}
