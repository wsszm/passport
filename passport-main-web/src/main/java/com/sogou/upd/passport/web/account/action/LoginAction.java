package com.sogou.upd.passport.web.account.action;

import com.google.common.base.Strings;

import com.sogou.upd.passport.common.utils.ErrorUtil;
import com.sogou.upd.passport.manager.account.AccountLoginManager;
import com.sogou.upd.passport.manager.account.AccountManager;
import com.sogou.upd.passport.manager.form.WebLoginParameters;
import com.sogou.upd.passport.web.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.sogou.upd.passport.common.result.APIResultSupport;
import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.web.ControllerHelper;

import javax.servlet.http.HttpServletRequest;

/**
 *  User: mayan
 *  Date: 13-6-7 Time: 下午5:48
 *  web登录
 */
public class LoginAction extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(LoginAction.class);

  @Autowired
  private AccountManager accountManager;
  @Autowired
  private AccountLoginManager accountLoginManager;


  /**
   * web页面登录
   *
   * @param loginParams 传入的参数
   */
  @RequestMapping(value = "login", method = RequestMethod.POST)
  @ResponseBody
  public Object login(HttpServletRequest request, WebLoginParameters loginParams)
      throws Exception {
    Result result = new APIResultSupport(false);
    //参数验证
    String validateResult = ControllerHelper.validateParams(loginParams);
    if (!Strings.isNullOrEmpty(validateResult)) {
      result.setCode(ErrorUtil.ERR_CODE_COM_REQURIE);
      result.setMessage(validateResult);
      return result.toString();
    }

    //判断用户是否存在
    String username = loginParams.getUsername();
    if (!accountManager.isAccountExists(username)) {
      result.setCode(ErrorUtil.ERR_CODE_ACCOUNT_NOTHASACCOUNT);
      return result.toString();
    }
    result = accountLoginManager.accountLogin(loginParams, getIp(request));
    return result.toString();
  }

}
