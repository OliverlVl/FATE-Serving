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

package com.webank.ai.fate.serving.common.bean;

import com.webank.ai.fate.serving.common.model.Model;
import com.webank.ai.fate.serving.core.bean.Dict;

//模型和数据
public class ServingServerContext extends BaseContext {

    String tableName;

    String namespace;

    public Model getModel() {
        return (Model) this.dataMap.get(Dict.MODEL);
    }

    public void setModel(Model model) {
        this.dataMap.put(Dict.MODEL, model);
    }

    public String getModelTableName() {
        return tableName;
    }

    public void setModelTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getModelNamesapce() {
        return namespace;
    }

    public void setModelNamesapce(String modelNamesapce) {
        this.namespace = modelNamesapce;
    }

}
