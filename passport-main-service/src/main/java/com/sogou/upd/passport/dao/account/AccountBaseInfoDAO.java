package com.sogou.upd.passport.dao.account;

import com.sogou.upd.passport.model.account.Account;
import com.sogou.upd.passport.model.account.AccountBaseInfo;
import net.paoding.rose.jade.annotation.DAO;
import net.paoding.rose.jade.annotation.SQL;
import net.paoding.rose.jade.annotation.SQLParam;
import org.springframework.dao.DataAccessException;

/**
 * User: mayan
 * Date: 13-11-27
 * Time: 下午2:57
 * To change this template use File | Settings | File Templates.
 */
@DAO
public interface AccountBaseInfoDAO {
    /**
     * 对应数据库表名称
     */
    String TABLE_NAME = " account_base_info ";

    /**
     * 所有字段列表
     */
    String
            ALL_FIELD =
            " id,passport_id,uniqname,avatar ";

    /**
     * 根据passportId获取Account
     */
    @SQL("select" +
            ALL_FIELD +
            "from" +
            TABLE_NAME +
            " where passport_id=:passport_id")
    public AccountBaseInfo getAccountBaseInfoByPassportId(@SQLParam("passport_id") String passport_id) throws DataAccessException;

    /**
     * 修改用户信息
     */
    @SQL("update " +
            TABLE_NAME +
            " set avatar=:avatar where passport_id=:passport_id")
    public int updateAvatarByPassportId(@SQLParam("avatar") String avatar,
                              @SQLParam("passport_id") String passport_id) throws
            DataAccessException;

}