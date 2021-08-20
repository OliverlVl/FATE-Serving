package com.webank.ai.fate.serving.adaptor.dataaccess;

import com.webank.ai.fate.serving.core.bean.Context;
import com.webank.ai.fate.serving.core.bean.ReturnResult;
import com.webank.ai.fate.serving.core.constant.StatusCode;
import com.webank.ai.fate.serving.core.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DatabaseRecordAdapter extends AbstractSingleFeatureDataAdaptor{

    private static final Logger logger = LoggerFactory.getLogger(DatabaseRecordAdapter.class);

    @Override
    public void init() {

    }

    @Override
    public ReturnResult getData(Context context, Map<String, Object> featureIds) {
        ReturnResult returnResult = new ReturnResult();
        Map<String, Object> data = new HashMap<>();
        try{
            // 获取数据库记录

            // 存储到data中
            
            // 返回数据
            returnResult.setData(data);
            returnResult.setRetcode(StatusCode.SUCCESS);

        }
        catch (Exception ex){
            logger.error(ex.getMessage());
            returnResult.setRetcode(StatusCode.SYSTEM_ERROR);
        }

        return null;
    }
}
