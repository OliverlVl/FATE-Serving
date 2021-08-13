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

package com.webank.ai.fate.serving.grpc.service;

import com.google.protobuf.ByteString;
import com.webank.ai.fate.api.mlmodel.manager.ModelServiceGrpc;
import com.webank.ai.fate.api.mlmodel.manager.ModelServiceProto;
import com.webank.ai.fate.api.mlmodel.manager.ModelServiceProto.PublishRequest;
import com.webank.ai.fate.api.mlmodel.manager.ModelServiceProto.PublishResponse;
import com.webank.ai.fate.register.annotions.RegisterService;
import com.webank.ai.fate.serving.common.bean.ServingServerContext;
import com.webank.ai.fate.serving.common.provider.ModelServiceProvider;
import com.webank.ai.fate.serving.common.rpc.core.InboundPackage;
import com.webank.ai.fate.serving.common.rpc.core.OutboundPackage;
import com.webank.ai.fate.serving.core.bean.Context;
import com.webank.ai.fate.serving.core.bean.ModelActionType;
import com.webank.ai.fate.serving.core.bean.ReturnResult;
import com.webank.ai.fate.serving.core.utils.JsonUtil;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ModelService extends ModelServiceGrpc.ModelServiceImplBase {

    @Autowired
    ModelServiceProvider modelServiceProvider;

    //服务端：实现类根据req请求取到参数，然后生成返回值，调用StreamObserver回调接口，来通知Grpc框架层发送返回值给客户端。
    @Override
    @RegisterService(serviceName = "publishLoad")
    //StreamObserver：回调接口，responseObserver：回调函数，回调函数由Grpc框架提供，可以理解为Grpc框架层在关注PublishResponse类型的返回值的生成，然后使用协议层及io层做数据发送
    public synchronized void publishLoad(PublishRequest req, StreamObserver<PublishResponse> responseObserver) {
        Context context = prepareContext(ModelActionType.MODEL_LOAD.name());
        InboundPackage<PublishRequest> inboundPackage = new InboundPackage();
        inboundPackage.setBody(req);
        OutboundPackage outboundPackage = modelServiceProvider.service(context, inboundPackage);    //提供服务
        ReturnResult returnResult = (ReturnResult) outboundPackage.getData();
        PublishResponse.Builder builder = PublishResponse.newBuilder();
        builder.setStatusCode(Integer.valueOf(returnResult.getRetcode()));
        builder.setMessage(returnResult.getRetmsg() != null ? returnResult.getRetmsg() : "");
        builder.setData(ByteString.copyFrom(JsonUtil.object2Json(returnResult.getData()).getBytes()));
        //onNext和onComplete方法均会调用内部的ServerCall实例发送消息
        responseObserver.onNext(builder.build());   //每一次对onNext的调用都代表一条消息的发送
        responseObserver.onCompleted(); //如果全部发送完了或者发送出错，那么就需要调用onError或者onComplete来告知对方本次stream已经结束
    }

    @Override
    @RegisterService(serviceName = "publishOnline")
    public synchronized void publishOnline(PublishRequest req, StreamObserver<PublishResponse> responseObserver) {
        Context context = prepareContext(ModelActionType.MODEL_PUBLISH_ONLINE.name());
        InboundPackage<ModelServiceProto.PublishRequest> inboundPackage = new InboundPackage();
        inboundPackage.setBody(req);
        OutboundPackage outboundPackage = modelServiceProvider.service(context, inboundPackage);
        ReturnResult returnResult = (ReturnResult) outboundPackage.getData();
        PublishResponse.Builder builder = PublishResponse.newBuilder();
        builder.setStatusCode(Integer.valueOf(returnResult.getRetcode()));
        builder.setMessage(returnResult.getRetmsg() != null ? returnResult.getRetmsg() : "");
        builder.setData(ByteString.copyFrom(JsonUtil.object2Json(returnResult.getData()).getBytes()));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    @RegisterService(serviceName = "publishBind")
    public synchronized void publishBind(PublishRequest req, StreamObserver<PublishResponse> responseObserver) {
        Context context = prepareContext(ModelActionType.MODEL_PUBLISH_ONLINE.name());
        InboundPackage<ModelServiceProto.PublishRequest> inboundPackage = new InboundPackage();
        inboundPackage.setBody(req);
        OutboundPackage outboundPackage = modelServiceProvider.service(context, inboundPackage);
        ReturnResult returnResult = (ReturnResult) outboundPackage.getData();
        PublishResponse.Builder builder = PublishResponse.newBuilder();
        builder.setStatusCode(Integer.valueOf(returnResult.getRetcode()));
        builder.setMessage(returnResult.getRetmsg() != null ? returnResult.getRetmsg() : "");
        builder.setData(ByteString.copyFrom(JsonUtil.object2Json(returnResult.getData()).getBytes()));
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    @RegisterService(serviceName = "unload")
    public synchronized void unload(ModelServiceProto.UnloadRequest request, StreamObserver<ModelServiceProto.UnloadResponse> responseObserver) {
        Context context = prepareContext(ModelActionType.UNLOAD.name());
        InboundPackage<ModelServiceProto.UnloadRequest> inboundPackage = new InboundPackage();
        inboundPackage.setBody(request);
        OutboundPackage outboundPackage = modelServiceProvider.service(context, inboundPackage);
        ModelServiceProto.UnloadResponse unloadResponse = (ModelServiceProto.UnloadResponse) outboundPackage.getData();
        responseObserver.onNext(unloadResponse);
        responseObserver.onCompleted();
    }

    @Override
    @RegisterService(serviceName = "unbind")
    public synchronized void unbind(ModelServiceProto.UnbindRequest request, StreamObserver<ModelServiceProto.UnbindResponse> responseObserver) {
        Context context = prepareContext(ModelActionType.UNBIND.name());
        InboundPackage<ModelServiceProto.UnbindRequest> inboundPackage = new InboundPackage();
        inboundPackage.setBody(request);
        OutboundPackage outboundPackage = modelServiceProvider.service(context, inboundPackage);
        ModelServiceProto.UnbindResponse unbindResponse = (ModelServiceProto.UnbindResponse) outboundPackage.getData();
        responseObserver.onNext(unbindResponse);
        responseObserver.onCompleted();
    }

    @Override
    public void queryModel(ModelServiceProto.QueryModelRequest request, StreamObserver<ModelServiceProto.QueryModelResponse> responseObserver) {
        Context context = prepareContext(ModelActionType.QUERY_MODEL.name());
        InboundPackage<ModelServiceProto.QueryModelRequest> inboundPackage = new InboundPackage();
        inboundPackage.setBody(request);
        OutboundPackage outboundPackage = modelServiceProvider.service(context, inboundPackage);
        ModelServiceProto.QueryModelResponse queryModelResponse = (ModelServiceProto.QueryModelResponse) outboundPackage.getData();
        responseObserver.onNext(queryModelResponse);
        responseObserver.onCompleted();
    }

    private Context prepareContext(String actionType) {
        ServingServerContext context = new ServingServerContext();
        context.setActionType(actionType);
        context.setCaseId(UUID.randomUUID().toString().replaceAll("-", ""));
        return context;
    }

}
