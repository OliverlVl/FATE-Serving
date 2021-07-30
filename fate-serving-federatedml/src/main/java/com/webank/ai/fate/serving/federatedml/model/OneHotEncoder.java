/*
 * Copyright 2019 The FATE Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.ai.fate.serving.federatedml.model;

import com.webank.ai.fate.core.mlmodel.buffer.OneHotMetaProto.OneHotMeta;
import com.webank.ai.fate.core.mlmodel.buffer.OneHotParamProto.ColsMap;
import com.webank.ai.fate.core.mlmodel.buffer.OneHotParamProto.OneHotParam;
import com.webank.ai.fate.serving.common.model.LocalInferenceAware;
import com.webank.ai.fate.serving.core.bean.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class OneHotEncoder extends BaseComponent implements LocalInferenceAware {
    private static final Logger logger = LoggerFactory.getLogger(OneHotEncoder.class);
    Pattern doublePattern = Pattern.compile("^-?([1-9]\\\\d*\\\\.\\\\d*|0\\\\.\\\\d*[1-9]\\\\d*|0?\\\\.0+|0)$");
    private List<String> cols;
    private Map<String, ColsMap> colsMapMap;
    private boolean needRun;

    //将模型的参数和结果，也就是Meta和param文件，反序列化为对象从而对Serving模型初始化
    @Override
    public int initModel(byte[] protoMeta, byte[] protoParam) {
        logger.info("start init OneHot Encoder class");
        try {
            OneHotMeta oneHotMeta = this.parseModel(OneHotMeta.parser(), protoMeta);
            OneHotParam oneHotParam = this.parseModel(OneHotParam.parser(), protoParam);
            this.needRun = oneHotMeta.getNeedRun(); //是否需要执行，如果为否，这个组件在后续预测时将被跳过
            this.cols = oneHotMeta.getTransformColNamesList();  //需要做转化的列名
            this.colsMapMap = oneHotParam.getColMapMap();   //每个需要转化的列，各种可能的值对应的新列名
        } catch (Exception ex) {
            logger.error("OneHotEncoder initModel error", ex);
            return ILLEGALDATA;
        }
        logger.info("Finish init OneHot Encoder class");
        return OK;
    }

    private boolean isDouble(String str) {
        if (null == str || "".equals(str)) {
            return false;
        }
        return this.doublePattern.matcher(str).matches();
    }

    @Override
    public Map<String, Object> localInference(Context context, List<Map<String, Object>> inputData) {
        HashMap<String, Object> outputData = new HashMap<>();
        Map<String, Object> firstData = inputData.get(0);
        if (!this.needRun) {
            return firstData;
        }
        for (String colName : firstData.keySet()) {
            try {
                if (!this.cols.contains(colName)) {
                    outputData.put(colName, firstData.get(colName));
                    continue;
                }
                ColsMap colsMap = this.colsMapMap.get(colName);
                List<String> values = colsMap.getValuesList();
                List<String> encodedVariables = colsMap.getTransformedHeadersList();
                Integer inputValue = 0;
                try {
                    String thisInputValue = firstData.get(colName).toString();
                    if (this.isDouble(thisInputValue)) {
                        double d = Double.valueOf(thisInputValue);
                        inputValue = (int) Math.ceil(d);
                    } else {
                        inputValue = Integer.valueOf(thisInputValue);
                    }
                } catch (Throwable e) {
                    logger.error("Onehot component accept number input value only");
                }
                //进行转化功能，对每个需要转化的输入数据，和colsMapMap中的key做对比，
                //当相等时，将对应的新列名的值设定为1，其余值均设定为0，如果其中没有值与输入数据相等，则所有新列名对应的值均为0
                for (int i = 0; i < values.size(); i++) {
                    Integer possibleValue = Integer.parseInt(values.get(i));
                    String newColName = encodedVariables.get(i);
                    if (inputValue.equals(possibleValue)) { //只支持整形数的输入
                        outputData.put(newColName, 1.0);
                    } else {
                        outputData.put(newColName, 0.0);
                    }
                }
            } catch (Throwable e) {
                logger.error("HeteroFeatureBinning error", e);
            }
        }
        return outputData;
    }

}
