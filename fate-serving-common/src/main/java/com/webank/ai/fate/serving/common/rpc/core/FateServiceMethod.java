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

package com.webank.ai.fate.serving.common.rpc.core;

import java.lang.annotation.*;

@Target({ElementType.METHOD})   //说明该注解是一个方法
@Retention(RetentionPolicy.RUNTIME) //在运行时有效（即运行时保留，可以通过此级别获取注解信息）
@Inherited
public @interface FateServiceMethod {   //注解类
    String[] name();
}
