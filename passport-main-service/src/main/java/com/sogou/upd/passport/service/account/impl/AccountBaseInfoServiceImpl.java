package com.sogou.upd.passport.service.account.impl;

import com.google.common.base.Strings;
import com.sogou.upd.passport.common.CacheConstant;
import com.sogou.upd.passport.common.utils.PhotoUtils;
import com.sogou.upd.passport.common.utils.RedisUtils;
import com.sogou.upd.passport.dao.account.AccountBaseInfoDAO;
import com.sogou.upd.passport.exception.ServiceException;
import com.sogou.upd.passport.model.account.AccountBaseInfo;
import com.sogou.upd.passport.oauth2.openresource.vo.ConnectUserInfoVO;
import com.sogou.upd.passport.service.account.AccountBaseInfoService;
import com.sogou.upd.passport.service.account.UniqNamePassportMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: shipengzhi
 * Date: 13-11-28
 * Time: 上午1:39
 * To change this template use File | Settings | File Templates.
 */
@Service
public class AccountBaseInfoServiceImpl implements AccountBaseInfoService {

    @Autowired
    private PhotoUtils photoUtils;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private UniqNamePassportMappingService uniqNamePassportMappingService;
    @Autowired
    private AccountBaseInfoDAO accountBaseInfoDAO;

    private static final long ONE_MONTH = 30;
    private static final Logger logger = LoggerFactory.getLogger(AccountBaseInfoService.class);
    private static final String CACHE_PREFIX_PASSPORTID_ACCOUNT_BASE_INFO = CacheConstant.CACHE_PREFIX_PASSPORTID_ACCOUNT_BASE_INFO;

    @Override
    public boolean initConnectAccountBaseInfo(String passportId, ConnectUserInfoVO connectUserInfoVO) {
        String uniqname = connectUserInfoVO.getNickname();
        String avatar = connectUserInfoVO.getImageURL();
        if (Strings.isNullOrEmpty(uniqname) && Strings.isNullOrEmpty(avatar)) {
            return true;
        }
        if (!Strings.isNullOrEmpty(avatar) && !avatar.matches("%s/app/[a-z]+/%s/[a-zA-Z0-9]+_\\d+")) {
            avatar = photoUtils.uploadWebImg(avatar);
        }
        try {
            AccountBaseInfo accountBaseInfo = queryAccountBaseInfo(passportId);
            if (accountBaseInfo == null) { // 原来该用户无昵称头像
                return insertAccountBaseInfo(passportId, uniqname, avatar);
            } else {
                String oldUniqname = accountBaseInfo.getUniqname();
                String oldAvatar = accountBaseInfo.getAvatar();
                if (!Strings.isNullOrEmpty(oldUniqname) && !Strings.isNullOrEmpty(oldAvatar)) {
                    return true;
                } else if (Strings.isNullOrEmpty(oldUniqname) && !Strings.isNullOrEmpty(uniqname)) {
                    return updateUniqname(accountBaseInfo, uniqname);
                } else if (Strings.isNullOrEmpty(oldAvatar) && !Strings.isNullOrEmpty(avatar)) {
                    return updateAvatar(accountBaseInfo, avatar);
                } else {
                    return insertAccountBaseInfo(passportId, uniqname, avatar);
                }
            }
        } catch (Exception e) {
            logger.error("insertOrUpdateAccountBaseInfo fail", e);
            return false;
        }
    }

    @Override
    public AccountBaseInfo queryAccountBaseInfo(String passportId) throws ServiceException {

        String cacheKey = buildAccountBaseInfoKey(passportId);
        AccountBaseInfo accountBaseInfo;
        try {
            accountBaseInfo = redisUtils.getObject(cacheKey, AccountBaseInfo.class);
            if (accountBaseInfo == null) {
                accountBaseInfo = accountBaseInfoDAO.getAccountBaseInfoByPassportId(passportId);
                if (accountBaseInfo != null) {
                    redisUtils.set(cacheKey, accountBaseInfo);
                }
            }
        } catch (Exception e) {
            logger.error("queryAccountBaseInfo fail!", e);
            throw new ServiceException();
        }
        return accountBaseInfo;
    }

    @Override
    public boolean updateUniqname(AccountBaseInfo oldBaseInfo, String uniqname) {
        String oldUniqName = oldBaseInfo.getUniqname();
        String passportId = oldBaseInfo.getPassportId();
        try {
            if (!oldUniqName.equals(uniqname)) {
                //检查昵称是否存在
                if (isUniqNameExist(uniqname)) {
                    return false;
                }
                //更新数据库
                int row = accountBaseInfoDAO.updateUniqnameByPassportId(uniqname, passportId);
                if (row > 0) {
                    String cacheKey = buildAccountBaseInfoKey(passportId);
                    oldBaseInfo.setUniqname(uniqname);
                    redisUtils.set(cacheKey, oldBaseInfo, ONE_MONTH, TimeUnit.DAYS);
                    //移除原来映射表
                    if (uniqNamePassportMappingService.removeUniqName(oldUniqName)) {
                        boolean isInsert = uniqNamePassportMappingService.insertUniqName(passportId, uniqname);
                        return isInsert;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("insertOrUpdateAccountBaseInfo fail", e);
            return false;
        }
    }

    @Override
    public boolean updateAvatar(AccountBaseInfo oldBaseInfo, String avatar) {
        String passportId = oldBaseInfo.getPassportId();
        try {
            //更新数据库
            if (!avatar.equals(oldBaseInfo.getAvatar())) {
                int row = accountBaseInfoDAO.updateAvatarByPassportId(avatar, passportId);
                if (row > 0) {
                    String cacheKey = buildAccountBaseInfoKey(passportId);
                    oldBaseInfo.setAvatar(avatar);
                    redisUtils.set(cacheKey, oldBaseInfo, ONE_MONTH, TimeUnit.DAYS);
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("insertOrUpdateAccountBaseInfo fail", e);
            return false;
        }
    }

    @Override
    public boolean insertAccountBaseInfo(String passportId, String uniqname, String avatar) throws ServiceException {
        try {
            boolean isInertUniqnameMapping = true;  // 是否插入UniqnameMapping表
            if (Strings.isNullOrEmpty(uniqname) && Strings.isNullOrEmpty(avatar)) {
                return true;
            }
            if (!Strings.isNullOrEmpty(uniqname)) {
                if (isUniqNameExist(uniqname)) {
                    uniqname = "";      // 如果昵称重复则置为空
                } else {
                    isInertUniqnameMapping = uniqNamePassportMappingService.insertUniqName(passportId, uniqname);
                }
            }
            if (Strings.isNullOrEmpty(uniqname) && Strings.isNullOrEmpty(avatar)) {
                return true;
            }
            if (isInertUniqnameMapping) {
                AccountBaseInfo accountBaseInfo = newAccountBaseInfo(passportId, uniqname, avatar);
                int accountBaseInfoRow = accountBaseInfoDAO.saveAccountBaseInfo(passportId, accountBaseInfo);
                if (accountBaseInfoRow > 0) {
                    String cacheKey = CacheConstant.CACHE_PREFIX_PASSPORTID_ACCOUNT_BASE_INFO + passportId;
                    redisUtils.set(cacheKey, accountBaseInfo, ONE_MONTH, TimeUnit.DAYS);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("insertOrUpdateAccountBaseInfo fail", e);
            return false;
        }
    }

    @Override
    public boolean simpleSaveAccountBaseInfo(AccountBaseInfo accountBaseInfo) {
        String passportId = accountBaseInfo.getPassportId();
        try {
            int accountBaseInfoRow = accountBaseInfoDAO.saveAccountBaseInfo(passportId, accountBaseInfo);
            if (accountBaseInfoRow > 0) {
                String cacheKey = CacheConstant.CACHE_PREFIX_PASSPORTID_ACCOUNT_BASE_INFO + passportId;
                redisUtils.set(cacheKey, accountBaseInfo, ONE_MONTH, TimeUnit.DAYS);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("simpleSaveAccountBaseInfo fail", e);
            return false;
        }
    }

    public boolean isUniqNameExist(String uniqname) {
        if (!Strings.isNullOrEmpty(uniqname)) {
            String existPassportId = uniqNamePassportMappingService.checkUniqName(uniqname);
            if (Strings.isNullOrEmpty(existPassportId)) {
                return false;
            }
        }
        return true;
    }

    private AccountBaseInfo newAccountBaseInfo(String passportId, String uniqname, String avatar) {
        AccountBaseInfo accountBaseInfo = new AccountBaseInfo();
        accountBaseInfo.setPassportId(passportId);
        accountBaseInfo.setUniqname(uniqname);
        accountBaseInfo.setAvatar(avatar);
        return accountBaseInfo;
    }

    private String buildAccountBaseInfoKey(String passportId) {
        return CACHE_PREFIX_PASSPORTID_ACCOUNT_BASE_INFO + passportId;
    }
}