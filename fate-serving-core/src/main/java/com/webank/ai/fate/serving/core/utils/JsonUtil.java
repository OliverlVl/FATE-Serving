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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class JsonUtil {

    private static ObjectMapper mapper = new ObjectMapper();

    /*
    (1)Java序列化就是指把Java对象转换为字节序列的过程
       Java反序列化就是指把字节序列恢复为Java对象的过程。
    (2)序列化最重要的作用：在传递和保存对象时.保证对象的完整性和可传递性。对象转换为有序字节流,以便在网络上传输或者保存在本地文件中。
       反序列化的最重要的作用：根据字节流中保存的对象状态及描述信息，通过反序列化重建对象。
     */
    static {
        //属性为空不参与序列化
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        //反序列化 遇到未知属性 不抛出异常
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //序列化 遇到未知属性 不抛出异常
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    public static String object2Json(Object o) {
        if (o == null) {
            return null;
        }
        String s = "";
        try {
            s = mapper.writeValueAsString(o);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    public static <T> T json2Object(String json, Class<T> c) {
        /*
        1.isEmpty 没有忽略空格参数，是以是否为空和是否存在为判断依据。
        2.isBlank 是在 isEmpty 的基础上进行了为空（字符串都为空格、制表符、tab 的情况）的判断。（一般更为常用）
         */
        if (StringUtils.isBlank(json)) {
            return null;
        }
        T t = null;
        try {
            t = mapper.readValue(json, c);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return t;
    }

    public static <T> T json2Object(byte[] json, Class<T> c) {
        T t = null;
        try {
            t = mapper.readValue(json, c);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return t;
    }

    public static <T> T json2List(String json, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        T result = null;
        try {
            result = mapper.readValue(json, typeReference);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> T json2Object(String json, TypeReference<T> tr) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        T t = null;
        try {
            t = (T) mapper.readValue(json, tr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return (T) t;
    }

}
