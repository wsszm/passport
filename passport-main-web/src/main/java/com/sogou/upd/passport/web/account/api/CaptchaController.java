package com.sogou.upd.passport.web.account.api;

import com.sogou.upd.passport.common.utils.CaptchaUtils;
import com.sogou.upd.passport.common.utils.RedisUtils;
import com.sogou.upd.passport.manager.account.AccountRegManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: mayan Date: 13-5-7 Time: 下午6:22 To change this template use
 * File | Settings | File Templates.
 */
@Controller
public class CaptchaController{

  @Autowired
  private RedisUtils redisUtils;
  @Autowired
  private CaptchaUtils captchaUtils;
  @Autowired
  private AccountRegManager accountRegManager;

  @RequestMapping(value = "/captcha", method = RequestMethod.GET)
  @ResponseBody
  public Object obtainCaptcha(HttpServletRequest request, HttpServletResponse response) throws Exception {

    //生成验证码
    String randCode=captchaUtils.getRandcode(request, response);
    accountRegManager.getCaptchaCode();

    response.setContentType("image/jpeg");//设置相应类型,告诉浏览器输出的内容为图片
    response.setHeader("Pragma", "No-cache");//设置响应头信息，告诉浏览器不要缓存此内容
    response.setHeader("Cache-Control", "no-cache");
    response.setDateHeader("Expire", 0);
    try {
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}

