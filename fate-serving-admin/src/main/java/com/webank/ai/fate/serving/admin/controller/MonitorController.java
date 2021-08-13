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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.webank.ai.fate.api.networking.common.CommonServiceGrpc;
import com.webank.ai.fate.api.networking.common.CommonServiceProto;
import com.webank.ai.fate.serving.admin.services.ComponentService;
import com.webank.ai.fate.serving.common.flow.JvmInfo;
import com.webank.ai.fate.serving.common.flow.MetricNode;
import com.webank.ai.fate.serving.core.bean.Dict;
import com.webank.ai.fate.serving.core.bean.GrpcConnectionPool;
import com.webank.ai.fate.serving.core.bean.MetaInfo;
import com.webank.ai.fate.serving.core.bean.ReturnResult;
import com.webank.ai.fate.serving.core.constant.StatusCode;
import com.webank.ai.fate.serving.core.exceptions.RemoteRpcException;
import com.webank.ai.fate.serving.core.exceptions.SysException;
import com.webank.ai.fate.serving.core.utils.JsonUtil;
import com.webank.ai.fate.serving.core.utils.NetUtils;
import io.grpc.ManagedChannel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 统计/监控
 */
@RequestMapping("/api")
@RestController
public class MonitorController {

    GrpcConnectionPool grpcConnectionPool = GrpcConnectionPool.getPool();

    @Autowired
    ComponentService componentService;

    /**
     * 查询Jvm 监控信息
     * @param host
     * @param port
     * @return
     */
    @GetMapping("/monitor/queryJvm")
    public ReturnResult queryJvmData(String host, int port) {
        // 创建stub
        CommonServiceGrpc.CommonServiceBlockingStub blockingStub = getMonitorServiceBlockStub(host, port);
        // gRPC 设置超时时间
        blockingStub = blockingStub.withDeadlineAfter(MetaInfo.PROPERTY_GRPC_TIMEOUT, TimeUnit.MILLISECONDS);
        // 构造请求对象
        CommonServiceProto.QueryJvmInfoRequest.Builder builder = CommonServiceProto.QueryJvmInfoRequest.newBuilder();
        // 获取jvm 响应数据
        CommonServiceProto.CommonResponse commonResponse = blockingStub.queryJvmInfo(builder.build());
        List<JvmInfo> resultList = Lists.newArrayList();
        if (commonResponse.getData() != null && !commonResponse.getData().toStringUtf8().equals("null")) {
            // json 转 List
            List<JvmInfo> resultData = JsonUtil.json2List(commonResponse.getData().toStringUtf8(), new TypeReference<List<JvmInfo>>() {
            });

            if (resultData != null) {
                resultList = resultData;
            }

            // 排序 在sorted方法中，o1是最后面的元素，o2是倒数第二个元素，以此类推，流是处理元素是从后面开始取值。
            resultList = resultList.stream()
                    .sorted(((o1, o2) -> o1.getTimestamp() == o2.getTimestamp() ? 0 : ((o1.getTimestamp() - o2.getTimestamp()) > 0 ? 1 : -1)))
                    .collect(Collectors.toList());
        }
        Map map = Maps.newHashMap();
        map.put("total", resultList.size());
        map.put("rows", resultList);
        return ReturnResult.build(StatusCode.SUCCESS, Dict.SUCCESS, map);
    }

    /**
     * 查询监控数据
     * @param host
     * @param port
     * @param source
     * @return
     */
    @GetMapping("/monitor/query")
    public ReturnResult queryMonitorData(String host, int port, String source) {
        // 创建stub
        CommonServiceGrpc.CommonServiceBlockingStub blockingStub = getMonitorServiceBlockStub(host, port);
        // gRPC 设置超时时间
        blockingStub = blockingStub.withDeadlineAfter(MetaInfo.PROPERTY_GRPC_TIMEOUT, TimeUnit.MILLISECONDS);
        // 构造请求对象
        CommonServiceProto.QueryMetricRequest.Builder builder = CommonServiceProto.QueryMetricRequest.newBuilder();
        // 时间
        long now = System.currentTimeMillis();
        builder.setBeginMs(now - 15000);
        builder.setEndMs(now);

        if (StringUtils.isNotBlank(source)) {
            builder.setSource(source);
        }
        // 类型：interface
        builder.setType(CommonServiceProto.MetricType.INTERFACE);
        // 获取响应数据
        CommonServiceProto.CommonResponse commonResponse = blockingStub.queryMetrics(builder.build());
        List<MetricNode> metricNodes = Lists.newArrayList();
        if (commonResponse.getData() != null && !commonResponse.getData().toStringUtf8().equals("null")) {
            // json 转 List
            List<MetricNode> resultData = JsonUtil.json2List(commonResponse.getData().toStringUtf8(), new TypeReference<List<MetricNode>>() {
            });
            if (resultData != null) {
                metricNodes = resultData;
            }
        }
        // 排序
        metricNodes = metricNodes.stream()
                .sorted(((o1, o2) -> o1.getTimestamp() == o2.getTimestamp() ? 0 : ((o1.getTimestamp() - o2.getTimestamp()) > 0 ? 1 : -1)))
                .collect(Collectors.toList());

        Map<String, Object> dataMap = Maps.newHashMap();
        if (metricNodes != null) {
            metricNodes.forEach(metricNode -> {
                List<MetricNode> nodes = (List<MetricNode>) dataMap.get(metricNode.getResource());
                if (nodes == null) {
                    nodes = Lists.newArrayList();
                }
                nodes.add(metricNode);
                dataMap.put(metricNode.getResource(), nodes);
            });
        }
        return ReturnResult.build(StatusCode.SUCCESS, Dict.SUCCESS, dataMap);
    }


    /**
     * 查询模型监控数据
     * @param host
     * @param port
     * @param source
     * @return
     */
    @GetMapping("/monitor/queryModel")
    public ReturnResult queryModelMonitorData(String host, int port, String source) {
        Preconditions.checkArgument(StringUtils.isNotBlank(source), "parameter source is blank");
        // 创建stub
        CommonServiceGrpc.CommonServiceBlockingStub blockingStub = getMonitorServiceBlockStub(host, port);
        // gRPC 设置超时时间
        blockingStub = blockingStub.withDeadlineAfter(MetaInfo.PROPERTY_GRPC_TIMEOUT, TimeUnit.MILLISECONDS);
        // 构造请求对象
        CommonServiceProto.QueryMetricRequest.Builder builder = CommonServiceProto.QueryMetricRequest.newBuilder();
        // 时间
        long now = System.currentTimeMillis();
        builder.setBeginMs(now - 15000);
        builder.setEndMs(now);
        if (StringUtils.isNotBlank(source)) {
            builder.setSource(source);
        }
        // 类型：model
        builder.setType(CommonServiceProto.MetricType.MODEL);
        // 获取响应数据
        CommonServiceProto.CommonResponse commonResponse = blockingStub.queryMetrics(builder.build());
        List<MetricNode> metricNodes = Lists.newArrayList();
        if (commonResponse.getData() != null && !commonResponse.getData().toStringUtf8().equals("null")) {
            // json 转 List
            List<MetricNode> resultData = JsonUtil.json2List(commonResponse.getData().toStringUtf8(), new TypeReference<List<MetricNode>>() {
            });
            if (resultData != null) {
                metricNodes = resultData;
            }
        }
        // 排序
        metricNodes = metricNodes.stream()
                .sorted(((o1, o2) -> o1.getTimestamp() == o2.getTimestamp() ? 0 : ((o1.getTimestamp() - o2.getTimestamp()) > 0 ? 1 : -1)))
                .collect(Collectors.toList());

        Map<String, Object> dataMap = Maps.newHashMap();
        if (metricNodes != null) {
            metricNodes.forEach(metricNode -> {
                List<MetricNode> nodes = (List<MetricNode>) dataMap.get(metricNode.getResource());
                if (nodes == null) {
                    nodes = Lists.newArrayList();
                }
                nodes.add(metricNode);
                dataMap.put(metricNode.getResource(), nodes);
            });
        }

        return ReturnResult.build(StatusCode.SUCCESS, Dict.SUCCESS, dataMap);
    }

    /**
     * 获取Stub存根
     * @param host
     * @param port
     * @return
     */
    private CommonServiceGrpc.CommonServiceBlockingStub getMonitorServiceBlockStub(String host, int port) {
        Preconditions.checkArgument(StringUtils.isNotBlank(host), "parameter host is blank");
        Preconditions.checkArgument(port != 0, "parameter port was wrong");

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
        CommonServiceGrpc.CommonServiceBlockingStub blockingStub = CommonServiceGrpc.newBlockingStub(managedChannel);
        return blockingStub;
    }

}
