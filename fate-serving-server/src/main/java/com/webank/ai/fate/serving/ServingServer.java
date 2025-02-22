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

package com.webank.ai.fate.serving;

import com.webank.ai.fate.register.common.NamedThreadFactory;
import com.webank.ai.fate.register.provider.FateServer;
import com.webank.ai.fate.register.provider.FateServerBuilder;
import com.webank.ai.fate.register.zookeeper.ZookeeperRegistry;
import com.webank.ai.fate.serving.common.bean.BaseContext;
import com.webank.ai.fate.serving.core.bean.Dict;
import com.webank.ai.fate.serving.core.bean.MetaInfo;
import com.webank.ai.fate.serving.grpc.service.*;
import com.webank.ai.fate.serving.model.ModelManager;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
public class ServingServer implements InitializingBean {

    Logger logger = LoggerFactory.getLogger(ServingServer.class);
    @Autowired
    GuestInferenceService guestInferenceService;
    @Autowired
    CommonService commonService;
    @Autowired
    ModelManager modelManager;
    @Autowired
    ModelService modelService;
    @Autowired
    HostInferenceService hostInferenceService;
    @Autowired(required = false)
    ZookeeperRegistry zookeeperRegistry;
    private Server server;

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("try to star server ,meta info {}", MetaInfo.toMap());
        Executor executor = new ThreadPoolExecutor(MetaInfo.PROPERTY_SERVING_CORE_POOL_SIZE, MetaInfo.PROPERTY_SERVING_MAX_POOL_SIZE, MetaInfo.PROPERTY_SERVING_POOL_ALIVE_TIME, TimeUnit.MILLISECONDS,
                MetaInfo.PROPERTY_SERVING_POOL_QUEUE_SIZE == 0 ? new SynchronousQueue<Runnable>() :
                        (MetaInfo.PROPERTY_SERVING_POOL_QUEUE_SIZE < 0 ? new LinkedBlockingQueue<Runnable>()
                                : new LinkedBlockingQueue<Runnable>(MetaInfo.PROPERTY_SERVING_POOL_QUEUE_SIZE)), new NamedThreadFactory("ServingServer", true));
        //创建ServerBuilder，设置服务端口，经过forPort(port)方法返回NettyServerBuilder对象，并绑定指定端口（8000）
        FateServerBuilder serverBuilder = (FateServerBuilder) ServerBuilder.forPort(MetaInfo.PROPERTY_PORT);
        serverBuilder.keepAliveTime(100, TimeUnit.MILLISECONDS);
        serverBuilder.executor(executor);
        //为 ServerBuilder 添加方法和拦截器（拦截器：主要完成请求参数的解析、将页面表单参数赋给值栈中相应属性、执行功能检验、程序异常调试等工作）
        //ServerInterceptor：服务端拦截器，在方法调用之前会被调用
        //绑定服务，将指定的服务实现类添加到方法注册器中（xxx.class）
        serverBuilder.addService(ServerInterceptors.intercept(guestInferenceService, new ServiceExceptionHandler(), new ServiceOverloadProtectionHandle()), GuestInferenceService.class);
        serverBuilder.addService(ServerInterceptors.intercept(modelService, new ServiceExceptionHandler(), new ServiceOverloadProtectionHandle()), ModelService.class);
        serverBuilder.addService(ServerInterceptors.intercept(hostInferenceService, new ServiceExceptionHandler(), new ServiceOverloadProtectionHandle()), HostInferenceService.class);
        serverBuilder.addService(ServerInterceptors.intercept(commonService, new ServiceExceptionHandler(), new ServiceOverloadProtectionHandle()), CommonService.class);
        //构建server
        server = serverBuilder.build();
        //创建对象，开始启动流程
        server.start();
        boolean useRegister = MetaInfo.PROPERTY_USE_REGISTER;   //使用注册中心，开启后会将serving-server中的接口注册至zookeeper
        if (useRegister) {
            logger.info("serving-server is using register center");
            zookeeperRegistry.subProject(Dict.PROPERTY_PROXY_ADDRESS);
            zookeeperRegistry.subProject(Dict.PROPERTY_FLOW_ADDRESS);
            zookeeperRegistry.register(FateServer.serviceSets);
        } else {
            logger.warn("serving-server not use register center");
        }
        modelManager.restore(new BaseContext());
        logger.warn("serving-server start over");
    }
}
