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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class InferenceUtils {

    // 创建日志实例
    private static final Logger logger = LoggerFactory.getLogger(InferenceUtils.class);

    // UUID(Universally Unique Identifier)全局唯一标识符,,是指在一台机器上生成的数字，它保证对在同一时空中的所有机器都是唯一的，是由一个十六位的数字组成,表现出来的形式。
    // UUID.randomUUID().toString() 可以用来生成数据库的主键id
    // UUID是由一个十六位的数字组成,表现出来的形式例如:550E8400-E29B-11D4-A716-446655440000
    public static String generateCaseid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generateSeqno() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 反射
    // 我们利用反射和配置文件，可以使：应用程序更新时，对源码无需进行任何修改
    // 我们只需要将新类发送给客户端，并修改配置文件即可
    public static Object getClassByName(String classPath) {
        try {
            Class thisClass = Class.forName(classPath); // 加载Class对象
            return thisClass.getConstructor().newInstance(); //创建实例
        } catch (ClassNotFoundException ex) {
            logger.error("Can not found this class: {}.", classPath);
        } catch (NoSuchMethodException ex) {
            logger.error("Can not get this class({}) constructor.", classPath);
        } catch (Exception ex) {
            logger.error(ex.getMessage());
            logger.error("Can not create class({}) instance.", classPath);
        }
        return null;
    }
}
