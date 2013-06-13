package com.sogou.upd.passport.web.account.form;

import org.hibernate.validator.constraints.NotBlank;

/**
 * Created with IntelliJ IDEA. User: hujunfei Date: 13-5-20 Time: 上午11:18 To change this template
 * use File | Settings | File Templates.
 */
public class UserCaptchaParams extends BaseUserParams {
    @NotBlank
    private String token;
    @NotBlank(message = "验证码不允许为空!")
    private String captcha;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }
}