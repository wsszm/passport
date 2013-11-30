package com.sogou.upd.passport.service.account.impl;

import com.google.common.base.Strings;
import com.sogou.upd.passport.common.CacheConstant;
import com.sogou.upd.passport.common.utils.RedisUtils;
import com.sogou.upd.passport.dao.account.UniqNamePassportMappingDAO;
import com.sogou.upd.passport.exception.ServiceException;
import com.sogou.upd.passport.service.account.UniqNamePassportMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created with IntelliJ IDEA.
 * User: shipengzhi
 * Date: 13-11-28
 * Time: 上午2:15
 * To change this template use File | Settings | File Templates.
 */
@Service
public class UniqNamePassportMappingServiceImpl implements UniqNamePassportMappingService {

    private static final String CACHE_PREFIX_NICKNAME_PASSPORTID = CacheConstant.CACHE_PREFIX_NICKNAME_PASSPORTID;

    private static final Logger logger = LoggerFactory.getLogger(UniqNamePassportMappingService.class);

    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private UniqNamePassportMappingDAO uniqNamePassportMappingDAO;

    @Override
    public String checkUniqName(String uniqname) throws ServiceException {
        String passportId = null;
        try {
            String cacheKey = CACHE_PREFIX_NICKNAME_PASSPORTID + uniqname;
            passportId = redisUtils.get(cacheKey);
            if (Strings.isNullOrEmpty(passportId)) {
                passportId = uniqNamePassportMappingDAO.getPassportIdByUniqName(uniqname);
                if (!Strings.isNullOrEmpty(passportId)) {
                    redisUtils.set(cacheKey, passportId);
                }
            }
        } catch (Exception e) {
            logger.error("checkUniqName fail", e);
            throw new ServiceException(e);
        }
        return passportId;
    }

    @Override
    public boolean checkAndInsertUniqName(String passportId, String uniqname) throws ServiceException {
        String existPassportId = checkUniqName(uniqname);
        if (!Strings.isNullOrEmpty(existPassportId)) {
            return false;      // 如果昵称重复则置为空
        } else {
            return insertUniqName(passportId, uniqname);
        }
    }

    @Override
    public boolean insertUniqName(String passportId, String uniqname) throws ServiceException {
        try {
            int row = uniqNamePassportMappingDAO.insertUniqNamePassportMapping(uniqname, passportId);
            if (row > 0) {
                String cacheKey = CACHE_PREFIX_NICKNAME_PASSPORTID + uniqname;
                redisUtils.set(cacheKey, passportId);
                return true;
            }
        } catch (Exception e) {
            logger.error("insertUniqName fail", e);
            throw new ServiceException(e);
        }
        return false;
    }

    @Override
    public boolean updateUniqName(/*Account account,*/String passportId, String oldNickName, String nickname) throws ServiceException {
        try {
            //sogou分支需要修改此逻辑，在account主表中修改昵称
//            String oldNickName = account.getUniqname();
//            String passportId = account.getPassportId();
//            //更新数据库
//            int row = accountDAO.updateNickName(nickname, passportId);
//            if (row > 0) {
//                String cacheKey = buildAccountKey(passportId);
//                account.setUniqname(nickname);
//                redisUtils.set(cacheKey, account);

            //移除原来映射表
            if (removeUniqName(oldNickName)) {
                //更新新的映射表
                int row = uniqNamePassportMappingDAO.insertUniqNamePassportMapping(nickname, passportId);
                if (row > 0) {
                    String cacheKey = CACHE_PREFIX_NICKNAME_PASSPORTID + nickname;
                    redisUtils.set(cacheKey, passportId);
                    return true;
                }
            } else {
                return false;
            }

//            }
        } catch (Exception e) {
            throw new ServiceException(e);
        }
        return false;
    }

    @Override
    public boolean removeUniqName(String uniqname) throws ServiceException {
        try {
            if (!Strings.isNullOrEmpty(uniqname)) {
                //更新映射
                int row = uniqNamePassportMappingDAO.deleteUniqNamePassportMapping(uniqname);
                if (row >= 0) {
                    String cacheKey = CACHE_PREFIX_NICKNAME_PASSPORTID + uniqname;
                    redisUtils.delete(cacheKey);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("removeUniqName fail", e);
            return false;
        }
        return true;
    }
}