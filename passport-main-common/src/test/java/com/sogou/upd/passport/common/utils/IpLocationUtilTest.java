package com.sogou.upd.passport.common.utils;

import com.google.common.collect.Maps;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: hujunfei Date: 13-6-19 Time: 下午4:21 To change this template use
 * File | Settings | File Templates.
 */
public class IpLocationUtilTest extends TestCase {
    private static String IP = "202.106.180.10";

    @Test
    public void testGetCity() {
        System.out.println(IpLocationUtil.getCity(IP));
    }
}
