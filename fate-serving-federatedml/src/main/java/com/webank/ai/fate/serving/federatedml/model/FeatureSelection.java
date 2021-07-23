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

import com.webank.ai.fate.core.mlmodel.buffer.FeatureSelectionMetaProto.FeatureSelectionMeta;
import com.webank.ai.fate.core.mlmodel.buffer.FeatureSelectionParamProto.FeatureSelectionParam;
import com.webank.ai.fate.core.mlmodel.buffer.FeatureSelectionParamProto.LeftCols;
import com.webank.ai.fate.serving.core.bean.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeatureSelection extends BaseComponent {
    private static final Logger logger = LoggerFactory.getLogger(FeatureSelection.class);
    private FeatureSelectionParam featureSelectionParam;
    private FeatureSelectionMeta featureSelectionMeta;
    private LeftCols finalLeftCols;
    private boolean needRun;

    //将模型的参数和结果，也就是Meta和param文件，反序列化为对象从而对Serving模型初始化
    @Override
    public int initModel(byte[] protoMeta, byte[] protoParam) {
        logger.info("start init Feature Selection class");
        this.needRun = false;
        try {
            this.featureSelectionMeta = this.parseModel(FeatureSelectionMeta.parser(), protoMeta);
            this.needRun = this.featureSelectionMeta.getNeedRun();  //是否需要执行，如果为否，这个组件在后续预测时将被跳过
            this.featureSelectionParam = this.parseModel(FeatureSelectionParam.parser(), protoParam);
            this.finalLeftCols = featureSelectionParam.getFinalLeftCols();  //经过特征选择后，需要保留的列名
        } catch (Exception ex) {
            ex.printStackTrace();
            return ILLEGALDATA;
        }
        logger.info("Finish init Feature Selection class");
        return OK;
    }

    @Override
    public Map<String, Object> localInference(Context context, List<Map<String, Object>> inputData) {
        HashMap<String, Object> outputData = new HashMap<>(8);
        Map<String, Object> firstData = inputData.get(0);
        if (!this.needRun) {
            return firstData;
        }
        //进行转化功能，将输入数据中，属于最终需要保留的变量留下，其余变量被过滤掉
        for (String key : firstData.keySet()) {
            if (this.finalLeftCols.getLeftCols().containsKey(key)) {
                Boolean isLeft = this.finalLeftCols.getLeftCols().get(key);
                if (isLeft) {
                    outputData.put(key, firstData.get(key));
                }
            }
        }
        return outputData;
    }
}
