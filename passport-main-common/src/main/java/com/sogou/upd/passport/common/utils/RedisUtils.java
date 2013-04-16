package com.sogou.upd.passport.common.utils;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import net.sf.json.util.JSONUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis工具类
 * User: mayan
 * Date: 13-3-27
 * Time: 上午11:19
 * To change this template use File | Settings | File Templates.
 */
public class RedisUtils {

    private static RedisTemplate redisTemplate;

    /*
    * 设置缓存内容
    */
    public static void set(String key, String value) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key, value);
    }
    /*
    * 设置缓存内容
    */
    public static void set(String key, Object obj) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(key, new Gson().toJson(obj));
    }

    /*
   * 根据key取缓存内容
   */
    public static String get(String key) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        return valueOperations.get(key);
    }


    /*
   * 判断key是否存在
   */
    public static boolean checkKeyIsExist(String key) {
        if (redisTemplate.hasKey(key)) {
            return true;
        }
        return false;
    }

    /*
     * 字符串转换byte数组
     */
    public static byte[] stringToByteArry(String str) {
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        return stringSerializer.serialize(str);
    }

    /*
   * byte数组转换字符串
   */
    public static String byteArryToString(byte[] bytes) {
        String parseResult = null;
        if (bytes != null && bytes.length > 0) {
            RedisSerializer<String> stringSerializer = new StringRedisSerializer();
            parseResult = stringSerializer.deserialize(bytes);
        }
        return parseResult;
    }

    /*
   * byte数组转换int
   */
    public static int byteArryToInteger(byte[] bytes) {
        String parseResult = null;
        if (bytes != null && bytes.length > 0) {
            RedisSerializer<String> stringSerializer = new StringRedisSerializer();
            parseResult = stringSerializer.deserialize(bytes);
        }
        return Strings.isNullOrEmpty(parseResult) ? 0 : Integer.parseInt(parseResult);
    }

    /*
 * byte数组转换long
 */
    public static long byteArryToLong(byte[] bytes) {
        String parseResult = null;
        if (bytes != null && bytes.length > 0) {
            RedisSerializer<String> stringSerializer = new StringRedisSerializer();
            parseResult = stringSerializer.deserialize(bytes);
        }
        return Strings.isNullOrEmpty(parseResult) ? 0 : Long.parseLong(parseResult);
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
