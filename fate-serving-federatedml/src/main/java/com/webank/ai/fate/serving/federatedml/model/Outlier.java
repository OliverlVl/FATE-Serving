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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Outlier {
    private static final Logger logger = LoggerFactory.getLogger(Outlier.class);
    public HashSet<String> outlierValueSet;
    public Map<String, String> outlierReplaceValues;

    //初始化模型参数
    public Outlier(List<String> outlierValues, Map<String, String> outlierReplaceValue) {
        //离线训练时，包含异常值的每一列的列名，即变量名
        this.outlierValueSet = new HashSet<String>(outlierValues);
        //key-value格式，离线训练时，每一列的异常值和对应的替换值
        this.outlierReplaceValues = outlierReplaceValue;
    }

    //异常值替换功能
    public Map<String, Object> transform(Map<String, Object> inputData) {
        if (inputData != null) {
            for (String key : inputData.keySet()) {
                if (inputData.get(key) != null) {
                    String value = inputData.get(key).toString();
                    //搜索变量是否在离线训练时候进行过异常值处理
                    if (this.outlierValueSet.contains(value.toLowerCase())) {
                        try {
                            //对进行过异常值处理的变量对应的值，搜索是否在outlierReplaceValues中，若在，用替换值替代
                            inputData.put(key, outlierReplaceValues.get(key));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            inputData.put(key, 0.);
                        }
                    }
                }
            }
        }

        return inputData;
    }
}
