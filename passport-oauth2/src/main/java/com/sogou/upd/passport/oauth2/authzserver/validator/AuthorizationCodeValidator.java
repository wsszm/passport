package com.sogou.upd.passport.oauth2.authzserver.validator;

import com.sogou.upd.passport.oauth2.common.OAuth;
import com.sogou.upd.passport.oauth2.common.validators.AbstractValidator;

import javax.servlet.http.HttpServletRequest;

/**
 * GrantType=authorization_code的授权请求验证器
 * OAuth2.0协议中的Authorization Code Grant
 */
public class AuthorizationCodeValidator extends AbstractValidator<HttpServletRequest> {

    public AuthorizationCodeValidator() {
        requiredParams.add(OAuth.OAUTH_GRANT_TYPE);
        requiredParams.add(OAuth.OAUTH_CLIENT_ID);
        requiredParams.add(OAuth.OAUTH_CODE);
        requiredParams.add(OAuth.OAUTH_REDIRECT_URI);
        requiredParams.add(OAuth.OAUTH_CLIENT_SECRET);
    }


}
