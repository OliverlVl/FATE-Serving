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

public class ProtobufUtils {
    // 创建日志实例
    private static final Logger logger = LoggerFactory.getLogger(ProtobufUtils.class);

    /*
    Xml、Json是目前常用的数据交换格式，它们直接使用字段名称维护序列化后类实例中字段与数据之间的映射关系，
    一般用字符串的形式保存在序列化后的字节流中。消息和消息的定义相对独立，可读性较好。但序列化后的数据字节很大，
    序列化和反序列化的时间较长，数据传输效率不高。

    Protobuf和Xml、Json序列化的方式不同，采用了二进制字节的序列化方式，用字段索引和字段类型通过算法计算得到字段之前的关系映射，
    从而达到更高的时间效率和空间效率，特别适合对数据大小和传输速率比较敏感的场合使用。
     */
    public static <T> T parseProtoObject(com.google.protobuf.Parser<T> protoParser, byte[] protoString) throws com.google.protobuf.InvalidProtocolBufferException {
        T messageV3;
        try {
            messageV3 = protoParser.parseFrom(protoString);
            if (logger.isDebugEnabled()) {
                logger.debug("parse {} proto object normal", messageV3.getClass().getSimpleName());
            }
            return messageV3;
        } catch (Exception ex1) {
            try {
                messageV3 = protoParser.parseFrom(new byte[0]);
                if (logger.isDebugEnabled()) {
                    logger.debug("parse {} proto object with default values", messageV3.getClass().getSimpleName());
                }
                return messageV3;
            } catch (Exception ex2) {
                throw ex1;
            }
        }
    }
}
