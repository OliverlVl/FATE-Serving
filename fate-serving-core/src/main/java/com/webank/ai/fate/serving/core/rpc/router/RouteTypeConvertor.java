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

package com.webank.ai.fate.serving.core.rpc.router;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description TODO
 * 实现路由类型转换，输入字符串，转换成RouterType类型变量
 * @Author
 **/
public class RouteTypeConvertor {
    /*
    被final修饰的类不可以被继承
    被final修饰的方法不可以被重写
    被final修饰的变量不可变
     */
    private static final String ROUTE_TYPE_RANDOM = "random";
    private static final String ROUTE_TYPE_CONSISTENT_HASH = "consistent";

    // 创建日志实例，在控制台日志输出的时候，可以打印出日志信息所在的类
    // 定义成static final,logger变量不可变，读取速度快
    // static 修饰的变量是不管创建了new了多少个实例，也只创建一次，节省空间，如果每次都创建Logger的话比较浪费内存；final修饰表示不可更改，常量
    // 将域定义为static,每个类中只有一个这样的域(成员变量).而每一个对象对于所有的实例域却都有自己的一份拷贝，用static修饰既节约空间，效率也好。final 是本 logger 不能再指向其他 Logger 对象
    private static final Logger logger = LoggerFactory.getLogger(RouteTypeConvertor.class);

    public static RouteType string2RouteType(String routeTypeString) {
        // 初始化对象
        RouteType routeType = RouteType.RANDOM_ROUTE;
        // 判断字符串是否为空
        if (StringUtils.isNotEmpty(routeTypeString)) {
            // equals() 会判断大小写区别，equalsIgnoreCase() 不会判断大小写区别
            if (routeTypeString.equalsIgnoreCase(ROUTE_TYPE_RANDOM)) {
                routeType = RouteType.RANDOM_ROUTE;
            } else if (routeTypeString.equalsIgnoreCase(ROUTE_TYPE_CONSISTENT_HASH)) {
                routeType = RouteType.CONSISTENT_HASH_ROUTE;
            } else {
                routeType = RouteType.RANDOM_ROUTE;
                logger.error("unknown routeType{}, will use {} instead.", routeTypeString, ROUTE_TYPE_RANDOM);
            }
        }
        return routeType;
    }
}
