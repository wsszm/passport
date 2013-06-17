package com.sogou.upd.passport.manager;

import com.sogou.upd.passport.BaseTest;
import com.sogou.upd.passport.common.math.Coder;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: shipengzhi
 * Date: 13-6-17
 * Time: 下午3:37
 * To change this template use File | Settings | File Templates.
 */
public class ManagerHelperTest extends BaseTest {

    @Test
    public void testGeneratorCode() {
        long ct = System.currentTimeMillis();
        System.out.println("ct:" + ct);
        String code = ManagerHelper.generatorCode(userid, clientId, serverSecret, ct);
        System.out.println("code:" + code);

        try {
            String pwdMD5 = Coder.encryptMD5(password);
            System.out.println("pwdMD5:" + pwdMD5);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
