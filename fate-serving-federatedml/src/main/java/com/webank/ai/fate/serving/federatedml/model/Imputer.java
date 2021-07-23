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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Imputer {
    private static final Logger logger = LoggerFactory.getLogger(Imputer.class);
    public HashSet<String> missingValueSet;
    public Map<String, String> missingReplaceValues;

    //初始化模型参数
    public Imputer(List<String> missingValues, Map<String, String> missingReplaceValue) {
        //离线训练时，包含缺失值的每一列的列名，即变量名
        this.missingValueSet = new HashSet<String>(missingValues);
        //key-value格式，离线训练时，每一列的缺失值和对应的替换值
        this.missingReplaceValues = missingReplaceValue;
    }

    //缺失值替换功能
    public Map<String, Object> transform(Map<String, Object> inputData) {
        Map<String, Object> output = new HashMap<>();
        for (String col : this.missingReplaceValues.keySet()) {
            if (inputData.containsKey(col)) {
                String value = inputData.get(col).toString();
                //搜索变量是否在离线训练时候进行过缺失值处理
                if (this.missingValueSet.contains(value.toLowerCase())) {
                    //对进行过异常值处理的变量对应的值，搜索是否在missingReplaceValues中，若在，用替换值替代
                    output.put(col, this.missingReplaceValues.get(col));
                } else {
                    output.put(col, inputData.get(col));
                }
            } else {
                output.put(col, this.missingReplaceValues.get(col));
            }
        }
        return output;
    }
}
