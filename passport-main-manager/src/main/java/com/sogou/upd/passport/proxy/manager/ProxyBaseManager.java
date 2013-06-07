package com.sogou.upd.passport.proxy.manager;

import com.sogou.upd.passport.common.lang.StringUtil;
import com.sogou.upd.passport.common.math.Coder;
import com.sogou.upd.passport.common.model.httpclient.RequestModel;
import com.sogou.upd.passport.common.parameter.HttpTransformat;
import com.sogou.upd.passport.common.utils.SGHttpClient;
import com.sogou.upd.passport.exception.ServiceException;
import com.sogou.upd.passport.model.app.AppConfig;
import com.sogou.upd.passport.service.app.AppConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * User: ligang201716@sogou-inc.com
 * Date: 13-6-6
 * Time: 下午1:36
 */
@Component
public class ProxyBaseManager {

    @Autowired
    private AppConfigService appConfigService;


    protected Map<String,Object> execute(final RequestModel requestModel){
        if (requestModel == null) {
            throw new IllegalArgumentException("requestModel may not be null");
        }

        //由于SGPP对一些参数的命名和SHPP不一致，在这里做相应的调整
        this.paramNameAdapter(requestModel);

        //计算参数的签名
        this.calculateDefaultCode(requestModel);

        return SGHttpClient.executeBean(requestModel, HttpTransformat.xml,Map.class);
    }

    /**
     * 用于判断和计算默认的code
     * 如果requestModel中已经存在code则不再生成
     * @param requestModel
     */
    private void calculateDefaultCode(final RequestModel requestModel){
        //计算默认的codeserverSecret
        Object codeObject=requestModel.getParam("code");
        if (codeObject==null|| StringUtil.isBlank(codeObject.toString())) {
            //获取app的
            String client_id = requestModel.getParam("appid").toString();

            Integer clientId=Integer.valueOf(client_id);

            AppConfig appConfig= appConfigService.queryAppConfigByClientId(clientId);

            String serverSecret=appConfig.getServerSecret();

            //系统当前时间
            long ct = System.currentTimeMillis();
            String passport_id = requestModel.getParam("userid").toString();
            //计算默认的code
            String code = passport_id + client_id + serverSecret + ct;
            try {
                code = Coder.encryptMD5(code);
            } catch (Exception e) {
                throw new ServiceException("calculate default code error",e);
            }
            requestModel.addParam("code", code);
            requestModel.addParam("ct", ct);
        }
    }

    /**
     * SGPP对一些参数的命名月SHPP有一些区别，在这里使我们的参数与他们的一致
     * @param requestModel
     */
    private void  paramNameAdapter(final RequestModel requestModel){
        this.paramNameAdapter(requestModel,"clientId","appid");
        this.paramNameAdapter(requestModel,"passport_id","userid");
    }

    /**
     * 用于修改参数名称
     * @param requestModel
     * @param oldName
     * @param newName
     */
    private void paramNameAdapter(final RequestModel requestModel,String oldName,String newName){
        if(requestModel.containsKey(oldName)){
            Object param = requestModel.getParam(oldName);
            requestModel.deleteParams(oldName);
            requestModel.addParam(newName, param);
        }
    }
}