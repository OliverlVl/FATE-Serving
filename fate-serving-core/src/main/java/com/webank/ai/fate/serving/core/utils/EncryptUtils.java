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

import com.webank.ai.fate.serving.core.bean.EncryptMethod;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

public class EncryptUtils {

    public static final String UTF8 = "UTF-8";
    private static final String HMAC_SHA1 = "HmacSHA1";

    /**
     *
     * @param originString 需要加密的字符串
     * @param encryptMethod 加密方法（md5和sha256）
     * @return
     */
    public static String encrypt(String originString, EncryptMethod encryptMethod) {
        try {
            // 生成实现指定摘要算法的 MessageDigest 对象
            MessageDigest m = MessageDigest.getInstance(getEncryptMethodString(encryptMethod));
            // 使用指定的字节更新摘要
            m.update(originString.getBytes("UTF8"));
            //  通过运行诸如填充之类的终于操作完毕哈希计算
            byte[] s = m.digest();
            String result = "";
            for (int i = 0; i < s.length; i++) {
                result += Integer.toHexString((0x000000FF & s[i]) | 0xFFFFFF00).substring(6);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * 使用 HMAC-SHA1 签名方法对对encryptText进行签名
     * @param encryptText 被签名的字符串
     * @param encryptKey 密钥
     * @return 返回被加密后的字符串
     * @throws Exception
     */
    public static byte[] hmacSha1Encrypt(String encryptText, String encryptKey) throws Exception {
        byte[] data = encryptKey.getBytes(UTF8);
        // 根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
        SecretKey secretKey = new SecretKeySpec(data, HMAC_SHA1);
        // 生成一个指定 Mac 算法 的 Mac 对象
        Mac mac = Mac.getInstance(HMAC_SHA1);
        // 用给定密钥初始化 Mac 对象
        mac.init(secretKey);

        byte[] text = encryptText.getBytes(UTF8);
        // 完成 Mac 操作
        return mac.doFinal(text);
    }

    private static String getEncryptMethodString(EncryptMethod encryptMethod) {
        String methodString = "";
        switch (encryptMethod) {
            case MD5:
                methodString = "MD5";
                break;
            case SHA256:
                methodString = "SHA-256";
                break;
            default:
                break;
        }
        return methodString;
    }

}
