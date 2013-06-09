package com.sogou.upd.passport.service.account;

import com.sogou.upd.passport.common.result.Result;
import com.sogou.upd.passport.exception.ServiceException;
import com.sogou.upd.passport.model.account.AccountInfo;

/**
 * Created with IntelliJ IDEA. User: hujunfei Date: 13-4-25 Time: 下午5:38 To change this template use
 * File | Settings | File Templates.
 */
public interface AccountInfoService {

    /**
     * 根据passportId获取AccountInfo
     */
    public AccountInfo queryAccountInfoByPassportId(String passportId) throws ServiceException;

    /**
     * 修改绑定邮箱
     */
    public AccountInfo modifyEmailByPassportId(String passportId, String email) throws ServiceException;

    /**
     * 修改密保问题和答案
     */
    public AccountInfo modifyQuesByPassportId(String passportId, String question, String answer) throws ServiceException;

}
