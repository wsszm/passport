package com.sogou.upd.passport.service.dataimport.fulldatacheck;

import com.google.common.collect.Lists;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.sogou.upd.passport.common.math.Coder;
import com.sogou.upd.passport.common.model.httpclient.RequestModelXml;
import com.sogou.upd.passport.common.model.httpclient.RequestModelXmlGBK;
import com.sogou.upd.passport.common.parameter.HttpTransformat;
import com.sogou.upd.passport.common.utils.SGHttpClient;
import com.sogou.upd.passport.dao.account.AccountDAO;
import com.sogou.upd.passport.dao.account.AccountInfoDAO;
import com.sogou.upd.passport.model.account.Account;
import com.sogou.upd.passport.model.account.AccountInfo;
import com.sogou.upd.passport.service.dataimport.util.FileUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveTask;

/**
 * Created with IntelliJ IDEA.
 * User: chengang
 * Date: 14-5-13
 * Time: 下午2:50
 */
//public class FullDataCheckApp extends RecursiveTask<List<String>> {
public class FullDataCheckApp extends RecursiveTask<Map<String, String>> {


    private static final Logger LOGGER = LoggerFactory.getLogger(FullDataCheckApp.class);

    private static final String appId = "1100";

    private static final String key = "yRWHIkB$2.9Esk>7mBNIFEcr:8\\[Cv";


    private static final String REQUEST_URL = "http://internal.passport.sohu.com/interface/getuserinfo";

    private static final String REQUEST_INFO = "info";

    private static final long serialVersionUID = 506833609075209712L;

    //记录对比不同的结果
    private List<String> differenceList = Lists.newArrayList();


    //记录对不不同的结果，记录不同的内容
    private Map<String, String> differenceMap = Maps.newConcurrentMap();


    private Map<String, String> userFlagMap = Maps.newConcurrentMap();


    //从搜狐获取数据失败记录
    private List<String> failedList = Lists.newArrayList();


    //检查用户flag信息 存储文件
    private static final String CHECK_USER_FLAG_FILE_PATCH = "D:\\项目\\非第三方账号迁移\\check_full_data\\check_user_flag.txt";


    private AccountDAO accountDAO;

    private AccountInfoDAO accountInfoDAO;

    private String filePath;

    public FullDataCheckApp(AccountDAO accountDAO, AccountInfoDAO accountInfoDAO, String filePath) {
        this.accountDAO = accountDAO;
        this.accountInfoDAO = accountInfoDAO;
        this.filePath = filePath;
    }


    @Override
//    protected List<String> compute() {
    protected Map<String, String> compute() {

        LOGGER.info("start check full data ");

        StopWatch watch = new StopWatch();
        watch.start();

        Path checkFilePath = Paths.get(filePath);

//        Path saveFlagLocalPath = Paths.get(CHECK_USER_FLAG_FILE_PATCH);

        try (BufferedReader reader = Files.newBufferedReader(checkFilePath, Charset.defaultCharset())) {

//            BufferedWriter writer = Files.newBufferedWriter(saveFlagLocalPath, Charset.defaultCharset());

            String passportId;
            while ((passportId = reader.readLine()) != null) {
                RequestModelXml requestModelXml = bulidRequestModelXml(passportId);

                Map<String, Object> mapB = null;
                try {
                    mapB = SGHttpClient.executeBean(requestModelXml, HttpTransformat.xml, Map.class);
                } catch (Exception e) {
                    failedList.add(passportId);
                    LOGGER.error("FullDataCheckApp get account from SOHU error.", e);
                    continue;
                }

                if (StringUtils.isNotEmpty(passportId)) {
                    Account account = accountDAO.getAccountByPassportId(passportId);

                    //不验证 birthday 采用 getAccountInfoByPid4DataCheck 方法  email,gender, province, city,fullname,personalid
                    AccountInfo accountInfo = accountInfoDAO.getAccountInfoByPid4DataCheck(passportId);
                    if (account != null && accountInfo != null) {
                        //Test 库数据
                        Map<String, Object> mapA = Maps.newHashMap();

                        mapA.put("createip", account.getRegIp() == null || account.getRegIp() == "" ? StringUtils.EMPTY : account.getRegIp());
                        mapA.put("userid", passportId);
                        mapA.put("personalid", accountInfo.getPersonalid() == null ? StringUtils.EMPTY : accountInfo.getPersonalid());
                        mapA.put("city", accountInfo.getCity() == null ? StringUtils.EMPTY : accountInfo.getCity());

                        String createTime = String.valueOf(account.getRegTime());
                        if (StringUtils.isNotEmpty(createTime)) {
                            if (createTime.length() >= 19) {
                                mapA.put("createtime", StringUtils.substring(createTime, 0, 19));
                            }
                        } else {
                            mapA.put("createtime", StringUtils.EMPTY);
                        }

                        mapA.put("username", accountInfo.getFullname() == null ? StringUtils.EMPTY : accountInfo.getFullname());
                        mapA.put("email", accountInfo.getEmail() == null ? StringUtils.EMPTY : accountInfo.getEmail());
                        mapA.put("province", accountInfo.getProvince() == null ? StringUtils.EMPTY : accountInfo.getProvince());
                        mapA.put("gender", accountInfo.getGender() == null ? StringUtils.EMPTY : accountInfo.getGender());
                        mapA.put("mobile", account.getMobile() == null ? StringUtils.EMPTY : account.getMobile());

                        //比较文件
                        if (mapA != null && mapB != null) {
                            mapA.put("flag", mapB.get("flag"));
                            mapA.put("status", mapB.get("status"));

                            //记录调用搜狐接口获取用户信息对应的Flag
                            userFlagMap.put(passportId, mapB.get("flag").toString());

                            MapDifference difference = Maps.difference(mapA, mapB);
                            if (!difference.areEqual()) {
                                if (!difference.entriesDiffering().isEmpty()) {
                                    differenceMap.put(passportId, difference.entriesDiffering().toString());
//                                    LOGGER.info("mapA and mapB entriesDiffering {}", difference.entriesDiffering().toString());
                                } else if (!difference.entriesOnlyOnLeft().isEmpty()) {
                                    differenceMap.put(passportId, difference.entriesOnlyOnLeft().toString());
//                                    LOGGER.info("mapA and mapB entriesOnlyOnLeft {}", difference.entriesOnlyOnLeft().toString());
                                } else if (!difference.entriesOnlyOnRight().isEmpty()) {
                                    differenceMap.put(passportId, difference.entriesOnlyOnRight().toString());
//                                    LOGGER.info("mapA and mapB entriesOnlyOnRight {}", difference.entriesOnlyOnRight().toString());
                                }
                            }
                        }

                        //记录用户flag信息到本地文件
//                        {
//                            writer.write(passportId + " | " + mapB.get("flag"));
//                            writer.newLine();
//                        }
                    }
                }
            }
//            writer.flush();
//            writer.close();
            if (userFlagMap != null && !userFlagMap.isEmpty()) {
                FileUtil.storeFileMap2Local("D:\\项目\\非第三方账号迁移\\check_full_data\\check_full_data_flag.txt", userFlagMap);
            }

            if (CollectionUtils.isNotEmpty(failedList)) {
                //记录调用搜狐接口，获取失败的数据，在对失败的数据进行验证
                FileUtil.storeFile("D:\\项目\\非第三方账号迁移\\check_full_data\\check_full_data_fail.txt", failedList);
            }

            LOGGER.info("FullDataCheckApp finish check full data use time {} s", watch.stop());
        } catch (Exception e) {
            LOGGER.error(" FullDataCheckApp check full data error.", e);
        }
//        return differenceList;
        return differenceMap;
    }

    /**
     * 构建请求参数
     *
     * @return
     */
    public static RequestModelXml bulidRequestModelXml(String passportId) {

        RequestModelXml requestModelXml = new RequestModelXml(REQUEST_URL, REQUEST_INFO);
        try {
            long ct = System.currentTimeMillis();
            String code = passportId + appId + key + ct;
            code = Coder.encryptMD5(code);

            requestModelXml.addParam("question", "");
            requestModelXml.addParam("mobile", "");
            requestModelXml.addParam("createtime", "");
            requestModelXml.addParam("createip", "");
            requestModelXml.addParam("email", "");
//            requestModelXml.addParam("birthday", ""); //数据验证,暂先不取生日
            requestModelXml.addParam("gender", "");
            requestModelXml.addParam("province", "");
            requestModelXml.addParam("city", "");
            requestModelXml.addParam("username", "");
            requestModelXml.addParam("personalid", "");
            requestModelXml.addParam("userid", passportId);
            requestModelXml.addParam("appid", appId);
            requestModelXml.addParam("ct", ct);
            requestModelXml.addParam("code", code);
        } catch (Exception e) {
            LOGGER.error("build RequestModelXml error.", e);
            e.printStackTrace();
        }
        return requestModelXml;
    }

    public static RequestModelXmlGBK bulidRequestModelXmlGBK(String passportId) {

        RequestModelXmlGBK requestModelXml = new RequestModelXmlGBK(REQUEST_URL, REQUEST_INFO);
        try {
            long ct = System.currentTimeMillis();
            String code = passportId + appId + key + ct;
            code = Coder.encryptMD5(code);

            requestModelXml.addParam("question", "");
            requestModelXml.addParam("mobile", "");
            requestModelXml.addParam("createtime", "");
            requestModelXml.addParam("createip", "");
            requestModelXml.addParam("email", "");
//            requestModelXml.addParam("birthday", ""); //数据验证,暂先不取生日
            requestModelXml.addParam("gender", "");
            requestModelXml.addParam("province", "");
            requestModelXml.addParam("city", "");
            requestModelXml.addParam("username", "");
            requestModelXml.addParam("personalid", "");
            requestModelXml.addParam("userid", passportId);
            requestModelXml.addParam("appid", appId);
            requestModelXml.addParam("ct", ct);
            requestModelXml.addParam("code", code);
        } catch (Exception e) {
            LOGGER.error("build RequestModelXml error.", e);
            e.printStackTrace();
        }
        return requestModelXml;
    }
}
