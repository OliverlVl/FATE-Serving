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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.webank.ai.fate.api.mlmodel.manager.ModelServiceGrpc;
import com.webank.ai.fate.api.mlmodel.manager.ModelServiceProto;
import com.webank.ai.fate.serving.admin.services.ComponentService;
import com.webank.ai.fate.serving.core.bean.GrpcConnectionPool;
import com.webank.ai.fate.serving.core.bean.MetaInfo;
import com.webank.ai.fate.serving.core.bean.RequestParamWrapper;
import com.webank.ai.fate.serving.core.bean.ReturnResult;
import com.webank.ai.fate.serving.core.exceptions.RemoteRpcException;
import com.webank.ai.fate.serving.core.exceptions.SysException;
import com.webank.ai.fate.serving.core.utils.JsonUtil;
import com.webank.ai.fate.serving.core.utils.NetUtils;
import io.grpc.ManagedChannel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Description Model management
 * @Date: 2020/3/25 11:13
 * @Author: v_dylanxu
 */
@RequestMapping("/api")
@RestController
public class ModelController {

    private static final Logger logger = LoggerFactory.getLogger(ModelController.class);
    GrpcConnectionPool grpcConnectionPool = GrpcConnectionPool.getPool();

    @Autowired
    private ComponentService componentService;

    /**
     * 模型查询
     * @param host host ip地址
     * @param port 端口号
     * @param serviceId 服务ID
     * @param page 页数
     * @param pageSize 每页大小
     * @return
     * @throws Exception
     */
    @GetMapping("/model/query")
    public ReturnResult queryModel(String host, int port, String serviceId, Integer page, Integer pageSize) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotBlank(host), "parameter host is blank");
        Preconditions.checkArgument(port != 0, "parameter port is blank");

        if (page == null || page < 0) {
            page = 1;
        }

        if (pageSize == null) {
            pageSize = 10;
        }

        // 调试时候使用debug输出信息
        if (logger.isDebugEnabled()) {
            logger.debug("query model, host: {}, port: {}, serviceId: {}", host, port, serviceId);
        }

        // 创建阻塞式存根 Stub
        ModelServiceGrpc.ModelServiceBlockingStub blockingStub = getModelServiceBlockingStub(host, port);

        // 构造请求对象
        ModelServiceProto.QueryModelRequest.Builder queryModelRequestBuilder = ModelServiceProto.QueryModelRequest.newBuilder();

        if (StringUtils.isNotBlank(serviceId)) {
            // by service id
            queryModelRequestBuilder.setQueryType(1);
//            queryModelRequestBuilder.setTableName(tableName);
//            queryModelRequestBuilder.setNamespace(namespace);
            queryModelRequestBuilder.setServiceId(serviceId);
        } else {
            // list all
            queryModelRequestBuilder.setQueryType(0);
        }

        // 查询模型响应
        ModelServiceProto.QueryModelResponse response = blockingStub.queryModel(queryModelRequestBuilder.build());

        if (logger.isDebugEnabled()) {
            logger.debug("response: {}", response);
        }

        Map data = Maps.newHashMap();
        List rows = Lists.newArrayList();
        // 模型列表
        List<ModelServiceProto.ModelInfoEx> modelInfosList = response.getModelInfosList();
        int totalSize = 0;
        if (modelInfosList != null) {
            // 总个数
            totalSize = modelInfosList.size();
            // 排序
            modelInfosList = modelInfosList.stream().sorted(Comparator.comparingInt(ModelServiceProto.ModelInfoEx::getIndex)).collect(Collectors.toList());

            // Pagination
            int totalPage = (modelInfosList.size() + pageSize - 1) / pageSize;
            if (page <= totalPage) {
                modelInfosList = modelInfosList.subList((page - 1) * pageSize, Math.min(page * pageSize, modelInfosList.size()));
            }

            // json转成map存储
            for (ModelServiceProto.ModelInfoEx modelInfoEx : modelInfosList) {
                rows.add(JsonUtil.json2Object(modelInfoEx.getContent(), Map.class));
            }
        }

        data.put("total", totalSize);
        data.put("rows", rows);
        return ReturnResult.build(response.getRetcode(), response.getMessage(), data);
    }

    /**
     * 模型卸载:卸载实例已载入的模型，模型卸载会同时解绑服务ID，并注销注册的服务接口
     * @param requestParams
     * @return
     * @throws Exception
     */
    @PostMapping("/model/unload")
    public Callable<ReturnResult> unload(@RequestBody RequestParamWrapper requestParams) throws Exception {
        return () -> {
            String host = requestParams.getHost();
            Integer port = requestParams.getPort();
            String tableName = requestParams.getTableName();
            String namespace = requestParams.getNamespace();

            Preconditions.checkArgument(StringUtils.isNotBlank(tableName), "parameter tableName is blank");
            Preconditions.checkArgument(StringUtils.isNotBlank(namespace), "parameter namespace is blank");

            ReturnResult result = new ReturnResult();

            if (logger.isDebugEnabled()) {
                logger.debug("unload model by tableName and namespace, host: {}, port: {}, tableName: {}, namespace: {}", host, port, tableName, namespace);
            }

            // 创建 存根 stub
            ModelServiceGrpc.ModelServiceFutureStub futureStub = getModelServiceFutureStub(host, port);

            // 构造请求对象
            ModelServiceProto.UnloadRequest unloadRequest = ModelServiceProto.UnloadRequest.newBuilder()
                    .setTableName(tableName)
                    .setNamespace(namespace)
                    .build();

            // 获取模型卸载 响应数据
            ListenableFuture<ModelServiceProto.UnloadResponse> future = futureStub.unload(unloadRequest);
            ModelServiceProto.UnloadResponse response = future.get(MetaInfo.PROPERTY_GRPC_TIMEOUT, TimeUnit.MILLISECONDS);

            if (logger.isDebugEnabled()) {
                logger.debug("response: {}", response);
            }

            result.setRetcode(response.getStatusCode());
//            result.setData(JSONObject.parseObject(response.getData().toStringUtf8()));
            result.setRetmsg(response.getMessage());
            return result;
        };
    }

    /**
     * 模型解绑:可以对模型绑定的服务ID进行解绑，并注销对应服务注册的服务接口
     * @param requestParams
     * @return
     * @throws Exception
     */
    @PostMapping("/model/unbind")
    public Callable<ReturnResult> unbind(@RequestBody RequestParamWrapper requestParams) throws Exception {
        return () -> {
            String host = requestParams.getHost();
            Integer port = requestParams.getPort();
            String tableName = requestParams.getTableName();
            String namespace = requestParams.getNamespace();
            List<String> serviceIds = requestParams.getServiceIds();

            Preconditions.checkArgument(StringUtils.isNotBlank(tableName), "parameter tableName is blank");
            Preconditions.checkArgument(StringUtils.isNotBlank(namespace), "parameter namespace is blank");
            Preconditions.checkArgument(serviceIds != null && serviceIds.size() != 0, "parameter serviceId is blank");

            ReturnResult result = new ReturnResult();

            if (logger.isDebugEnabled()) {
                logger.debug("unload model by tableName and namespace, host: {}, port: {}, tableName: {}, namespace: {}", host, port, tableName, namespace);
            }

            // 创建 存根 stub
            ModelServiceGrpc.ModelServiceFutureStub futureStub = getModelServiceFutureStub(host, port);

            // 构造请求对象
            ModelServiceProto.UnbindRequest unbindRequest = ModelServiceProto.UnbindRequest.newBuilder()
                    .setTableName(tableName)
                    .setNamespace(namespace)
                    .addAllServiceIds(serviceIds)
                    .build();

            // 获取模型解绑 响应数据
            ListenableFuture<ModelServiceProto.UnbindResponse> future = futureStub.unbind(unbindRequest);
            ModelServiceProto.UnbindResponse response = future.get(MetaInfo.PROPERTY_GRPC_TIMEOUT, TimeUnit.MILLISECONDS);

            if (logger.isDebugEnabled()) {
                logger.debug("response: {}", response);
            }

            result.setRetcode(response.getStatusCode());
            result.setRetmsg(response.getMessage());
            return result;
        };
    }

    /**
     * 获取阻塞式Stub存根：
     * 为屏蔽客户调用远程主机上的对象，必须提供某种方式来模拟本地对象,这种本地对象称为存根(stub),存根负责接收本地方法调用,并将它们委派给各自的具体实现对象。
     * @param host
     * @param port
     * @return
     * @throws Exception
     */
    private ModelServiceGrpc.ModelServiceBlockingStub getModelServiceBlockingStub(String host, Integer port) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotBlank(host), "parameter host is blank");
        Preconditions.checkArgument(port != null && port.intValue() != 0, "parameter port was wrong");

        // 判断地址是否有效
        if (!NetUtils.isValidAddress(host + ":" + port)) {
            throw new SysException("invalid address");
        }

        // 判断是否允许访问
        if (!componentService.isAllowAccess(host, port)) {
            throw new RemoteRpcException("no allow access, target: " + host + ":" + port);
        }

        // 获取一个 gRPC 频道
        ManagedChannel managedChannel = grpcConnectionPool.getManagedChannel(host, port);
        // 创建存根，blockingStub为阻塞式，需要阻塞等待服务端的回应。
        ModelServiceGrpc.ModelServiceBlockingStub blockingStub = ModelServiceGrpc.newBlockingStub(managedChannel);
        // gRPC 设置超时时间
        blockingStub = blockingStub.withDeadlineAfter(MetaInfo.PROPERTY_GRPC_TIMEOUT, TimeUnit.MILLISECONDS);
        return blockingStub;
    }

    /**
     *  获取future 存根 stub
     * @param host
     * @param port
     * @return
     * @throws Exception
     */
    private ModelServiceGrpc.ModelServiceFutureStub getModelServiceFutureStub(String host, Integer port) throws Exception {
        Preconditions.checkArgument(StringUtils.isNotBlank(host), "parameter host is blank");
        Preconditions.checkArgument(port != null && port.intValue() != 0, "parameter port was wrong");

        // 判断地址是否有效
        if (!NetUtils.isValidAddress(host + ":" + port)) {
            throw new SysException("invalid address");
        }

        // 判断是否允许访问
        if (!componentService.isAllowAccess(host, port)) {
            throw new RemoteRpcException("no allow access, target: " + host + ":" + port);
        }

        // 获取一个 gRPC 频道
        ManagedChannel managedChannel = grpcConnectionPool.getManagedChannel(host, port);
        // 创建存根
        ModelServiceGrpc.ModelServiceFutureStub futureStub = ModelServiceGrpc.newFutureStub(managedChannel);
        return futureStub;
    }

}
