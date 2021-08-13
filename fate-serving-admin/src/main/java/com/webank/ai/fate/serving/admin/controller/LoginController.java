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

package com.webank.ai.fate.serving.admin.controller;

import com.google.common.base.Preconditions;
import com.webank.ai.fate.serving.common.cache.Cache;
import com.webank.ai.fate.serving.core.bean.*;
import com.webank.ai.fate.serving.core.constant.StatusCode;
import com.webank.ai.fate.serving.core.utils.EncryptUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description User management 用户登入和退出
 * @Date: 2020/3/25 11:13
 * @Author: v_dylanxu
 */
@RequestMapping("/api")
@RestController
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Value("${admin.username}")
    private String username;

    @Value("${admin.password}")
    private String password;

    @Autowired
    private Cache cache;

    /**
     * @@Description 用户登入：默认用户：admin，默认密码：admin，用户可在application.properties中修改预设用户
     * serving-admin仅实现简单的用户登录，用户可业务需求，自行实现登录逻辑，或接入第三方平台
     * @param requestParams
     * @return
     */
    @PostMapping("/admin/login")
    public ReturnResult login(@RequestBody RequestParamWrapper requestParams) {
        String username = requestParams.getUsername();
        String password = requestParams.getPassword();

        // Preconditions类:可以简洁的完成参数检验，在进行业务逻辑代码前进行前置判断。
        // 并且避免了冗长的if语句。guava将所有检验的API都放置于Preconditions类中。
        Preconditions.checkArgument(StringUtils.isNotBlank(username), "parameter username is blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(password), "parameter password is blank");

        ReturnResult result = new ReturnResult();
        if (username.equals(this.username) && password.equals(this.password)) {
            // 将数组转化成List集合,用“-”拼接成字符串
            String userInfo = StringUtils.join(Arrays.asList(username, password), "_");
            // MD5加密令牌
            String token = EncryptUtils.encrypt(Dict.USER_CACHE_KEY_PREFIX + userInfo, EncryptMethod.MD5);
            // token，用户信息，缓存过期时间添加到cache中
            // token 作为key ，userInfo作为value
            cache.put(token, userInfo, MetaInfo.PROPERTY_CACHE_TYPE.equalsIgnoreCase("local") ? MetaInfo.PROPERTY_LOCAL_CACHE_EXPIRE : MetaInfo.PROPERTY_REDIS_EXPIRE);
            logger.info("user {} login success.", username);

            Map data = new HashMap<>();
            data.put("timestamp", System.currentTimeMillis());
            data.put(Dict.SESSION_TOKEN, token);
            //返回code
            result.setRetcode(StatusCode.SUCCESS);
            result.setData(data);
        } else {
            logger.info("user {} login failure, username or password is wrong.", username);
            result.setRetcode(StatusCode.PARAM_ERROR);
            result.setRetmsg("username or password is wrong");
        }
        return result;
    }

    /**
     * @Description 用户退出
     * @param request
     * @return
     */
    @PostMapping("/admin/logout")
    public ReturnResult logout(HttpServletRequest request) {
        ReturnResult result = new ReturnResult();

        // 获取会话令牌
        String sessionToken = request.getHeader(Dict.SESSION_TOKEN);
        // 根据key：sessionToken 获取 value：userInfo
        String userInfo = (String) cache.get(sessionToken);
        // 判断userInfo是否为空
        if (StringUtils.isNotBlank(userInfo)) {
            // sessionToken存在，删除token
            cache.delete(sessionToken);
            result.setRetcode(StatusCode.SUCCESS);
        } else {
            logger.info("Session token unavailable");
            result.setRetcode(StatusCode.PARAM_ERROR);
            result.setRetmsg("Session token unavailable");
        }
        return result;
    }

}
