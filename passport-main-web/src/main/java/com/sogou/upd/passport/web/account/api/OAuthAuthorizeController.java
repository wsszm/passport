package com.sogou.upd.passport.web.account.api;

import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.common.utils.ErrorUtil;
import com.sogou.upd.passport.manager.account.AccountLoginManager;
import com.sogou.upd.passport.manager.app.ConfigureManager;
import com.sogou.upd.passport.oauth2.authzserver.request.OAuthTokenASRequest;
import com.sogou.upd.passport.oauth2.common.exception.OAuthProblemException;
import com.sogou.upd.passport.web.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Created with IntelliJ IDEA.
 * User: shipengzhi
 * Date: 13-3-28
 * Time: 下午8:33
 * To change this template use File | Settings | File Templates.
 */
@Controller
@RequestMapping("/oauth2")
public class OAuthAuthorizeController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthAuthorizeController.class);

    @Autowired
    private AccountLoginManager accountLoginManager;
    @Autowired
    private ConfigureManager configureManager;

    @RequestMapping(value = "/token", method = RequestMethod.POST)
    @ResponseBody
    public Object authorize(HttpServletRequest request) throws Exception {
        OAuthTokenASRequest oauthRequest;
        try {
            oauthRequest = new OAuthTokenASRequest(request);
        } catch (OAuthProblemException e) {
            return Result.buildError(e.getError(), e.getDescription());
        }

        int clientId = oauthRequest.getClientId();

        // 检查client_id和client_secret是否有效
        if (!configureManager.verifyClientVaild(clientId, oauthRequest.getClientSecret())) {
            return Result.buildError(ErrorUtil.INVALID_CLIENT);
        }
        Result result = accountLoginManager.authorize(oauthRequest);
        return result;
    }

}