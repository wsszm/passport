package com.sogou.upd.passport.web.connect;

import com.google.common.base.Strings;

import com.sogou.upd.passport.common.parameter.AccountTypeEnum;
import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.common.utils.ErrorUtil;
import com.sogou.upd.passport.manager.connect.ConnectAuthManager;
import com.sogou.upd.passport.manager.connect.params.ConnectParams;
import com.sogou.upd.passport.oauth2.openresource.response.OAuthSinaSSOTokenRequest;
import com.sogou.upd.passport.web.BaseConnectController;
import com.sogou.upd.passport.web.ControllerHelper;
import com.sogou.upd.passport.web.form.ConnectObtainParams;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * SSO-SDK第三方登录授权回调接口 User: shipengzhi Date: 13-3-24 Time: 下午12:07 To change this template use File
 * | Settings | File Templates.
 */
@Controller
@RequestMapping("/v2/connect")
public class ConnectSSOLoginController extends BaseConnectController {

  @Autowired
  private ConnectAuthManager connectAuthManager;


  @RequestMapping(value = "/ssologin/sina", method = RequestMethod.POST)
  @ResponseBody
  public Object handleSSOLogin(HttpServletRequest req, HttpServletResponse res) throws Exception {

    OAuthSinaSSOTokenRequest oauthRequest = new OAuthSinaSSOTokenRequest(req);

    int accountType = AccountTypeEnum.SINA.getValue();
    int clientId = oauthRequest.getClientId();
    String instanceId = oauthRequest.getInstanceId();
    String connectUid = oauthRequest.getOpenid();
    String ip = getIp(req);
    ConnectParams
        connectParams =
        new ConnectParams(accountType, clientId, connectUid, null, instanceId, ip);
    Result result = connectAuthManager.connectAuthLogin(oauthRequest, connectParams);
    return result;
  }

  @RequestMapping(value = "/users/getopenid", method = RequestMethod.GET)
  @ResponseBody
  public Object getopenid(ConnectObtainParams reqParams) throws Exception {

    //参数验证
    String validateResult = ControllerHelper.validateParams(reqParams);
    if (!Strings.isNullOrEmpty(validateResult)) {
      return ErrorUtil.buildError(ErrorUtil.ERR_CODE_COM_REQURIE, validateResult);
    }

    int clientId = reqParams.getClient_id();
    String passportId = reqParams.getPassport_id();
    int provider = reqParams.getProvider();

    Result result = connectAuthManager.getOpenIdByPassportId(passportId, clientId, provider);

    return result;
  }

    /*
    该账号是否在当前应用登录过
     */
//    private AccountConnect getAppointClientIdAccountConnect(List<AccountConnect> accountConnectList, int clientId) {
//        AccountConnect accountConnect = null;
//        for (AccountConnect connect : accountConnectList) {
//            if (clientId == connect.getClientId()) {
//                accountConnect = connect;
//                break;
//            }
//        }
//        return accountConnect;
//    }

}
