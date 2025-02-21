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

package com.webank.ai.fate.serving.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

public class ObjectTransform {

    public ObjectTransform() {
    }
    // javaBean: 一种类的规格编写规范; 含有setXxx()或者getXxx()方法的类都可以称之为javaBean
    public static String bean2Json(Object object) {
        if (object == null) {
            return "";
        } else {
            try {
                return (new ObjectMapper()).writeValueAsString(object);
            } catch (JsonProcessingException var2) {
                return "";
            }
        }
    }

    public static Object json2Bean(String json, Class objectType) {
        if (StringUtils.isEmpty(json)) {
            return null;
        } else {
            try {
                return (new ObjectMapper()).readValue(json, objectType);
            } catch (Exception var3) {
                return null;
            }
        }
    }
}
