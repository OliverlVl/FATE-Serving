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

package com.webank.ai.fate.serving.core.bean;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.webank.ai.fate.serving.core.rpc.router.RouterInfo;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// gRPC 连接 池
public class GrpcConnectionPool {

    private static final Logger logger = LoggerFactory.getLogger(GrpcConnectionPool.class);

    static private GrpcConnectionPool pool = new GrpcConnectionPool();

    public ConcurrentHashMap<String, ChannelResource> poolMap = new ConcurrentHashMap<String, ChannelResource>();
    Random r = new Random();
    // 可用计算资源，cpu核心线程数
    private int maxTotalPerAddress = Runtime.getRuntime().availableProcessors();
    private long defaultLoadFactor = 10;
    /**
     * 计时器线程池
     * 1个线程可以重复使用，不创建新的线程
     */
    private ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

    private GrpcConnectionPool() {


        /**
         * scheduleAtFixedRate:以固定的频率来执行某个任务
         * 用处：定时任务
         * 四个参数
         *	1:任务----run()方法
         *	2:第一个任务多久执行---1000：1000毫秒后执行
         *	3:每隔多长时间执行这个任务---10000--每隔10000毫秒，任务重复执行
         *	4:时间单位是多少--TimeUnit.MILLISECONDS--指定时间单位毫秒
         */
        scheduledExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {

                            poolMap.forEach((k, v) -> {
                                try {
                                    //logger.info("grpc pool {} channel size {} req count {}", k, v.getChannels().size(), v.getRequestCount().get() - v.getPreCheckCount());

                                    if (needAddChannel(v)) {
                                        // 添加channel
                                        String[] ipPort = k.split(":");
                                        String ip = ipPort[0];
                                        int port = Integer.parseInt(ipPort[1]);
                                        // 创建managed channel
                                        ManagedChannel managedChannel = createManagedChannel(ip, port, v.getNettyServerInfo());
                                        v.getChannels().add(managedChannel);
                                    }
                                    v.getChannels().forEach(e -> {
                                        // 检查状态
                                        try {
                                            ConnectivityState state = e.getState(true);
                                            if (state.equals(ConnectivityState.TRANSIENT_FAILURE) || state.equals(ConnectivityState.SHUTDOWN)) {
                                                fireChannelError(k, state);
                                            }
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            logger.error("channel {} check status error", k);
                                        }

                                    });
                                } catch (Exception e) {
                                    logger.error("channel {} check status error", k);
                                }
                            });
                        }
                    },
                    1000,
                    10000,
                    TimeUnit.MILLISECONDS);


    }

    static public GrpcConnectionPool getPool() {
        return pool;
    }

    /**
     * 通道故障报错
     * @param k
     * @param status
     */
    private void fireChannelError(String k, ConnectivityState status) {
        logger.error("grpc channel {} status is {}", k, status);
    }

    /**
     * 判断是否需要添加channel
     * @param channelResource
     * @return
     */
    private boolean needAddChannel(ChannelResource channelResource) {
        long requestCount = channelResource.getRequestCount().longValue();
        long preCount = channelResource.getPreCheckCount();
        long latestTimestamp = channelResource.getLatestChecktimestamp();

        int channelSize = channelResource.getChannels().size();
        long now = System.currentTimeMillis();
        // 请求数量-之前的请求数量  除以 通道大小*时间 = 每个通道每秒需要多处理的请求数
        long loadFactor = ((requestCount - preCount) * 1000) / (channelSize * (now - latestTimestamp));
        channelResource.setLatestChecktimestamp(now);
        channelResource.setPreCheckCount(requestCount);
        if (channelSize > maxTotalPerAddress) {
            // channel大小>可用计算资源
            return false;
        }
        if (latestTimestamp == 0) {
            return false;
        }
        if (channelSize > 0) {

            if (loadFactor > defaultLoadFactor) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    /**
     * 获取managed channel
     * @param routerInfo
     * @return
     */
    public  ManagedChannel getManagedChannel(RouterInfo  routerInfo){
        NettyServerInfo nettyServerInfo = null;
        if (routerInfo.isUseSSL()) {
            nettyServerInfo = new NettyServerInfo(NegotiationType.TLS.toString(),
                    MetaInfo.PROPERTY_PROXY_GRPC_INTER_CLIENT_CERTCHAIN_FILE,
                    MetaInfo.PROPERTY_PROXY_GRPC_INTER_CLIENT_PRIVATEKEY_FILE,
                    MetaInfo.PROPERTY_PROXY_GRPC_INTER_CA_FILE);
        } else {
            nettyServerInfo = new NettyServerInfo();
        }
        String key = new StringBuilder().append(routerInfo.getHost()).append(":").append(routerInfo.getPort()).toString();
        return getAManagedChannel(key,nettyServerInfo);
    }

    public ManagedChannel getManagedChannel(String key) {
        return getAManagedChannel(key, new NettyServerInfo());
    }

    private ManagedChannel getAManagedChannel(String key, NettyServerInfo nettyServerInfo) {
        ChannelResource channelResource = poolMap.get(key);
        if (channelResource == null) {
            return createInner(key, nettyServerInfo);
        } else {
            return getRandomManagedChannel(channelResource);
        }
    }

    public ManagedChannel getManagedChannel(String ip,int port, NettyServerInfo nettyServerInfo) {
        String key = new StringBuilder().append(ip).append(":").append(port).toString();
        return this.getAManagedChannel(key, nettyServerInfo);
    }

    public ManagedChannel getManagedChannel(String ip, int port) {
        String key = new StringBuilder().append(ip).append(":").append(port).toString();
        return this.getManagedChannel(key);
    }


    /**
     * 随机获取 managed channel
     * @param channelResource
     * @return
     */
    private ManagedChannel getRandomManagedChannel(ChannelResource channelResource) {
        List<ManagedChannel> list = channelResource.getChannels();
        Preconditions.checkArgument(list != null && list.size() > 0);
        // 生成一个随机int值，范围[0,list.size())
        int index = r.nextInt(list.size());
        ManagedChannel result = list.get(index);
        // 请求数加1并返回值
        channelResource.getRequestCount().addAndGet(1);
        return result;

    }

    /**
     * 创建key的channel资源
     * @param key
     * @param nettyServerInfo
     * @return
     */
    private synchronized ManagedChannel createInner(String key, NettyServerInfo nettyServerInfo) {
        // 获取key的channel资源
        ChannelResource channelResource = poolMap.get(key);
        if (channelResource == null) {
            // 资源为null
            String[] ipPort = key.split(":");
            String ip = ipPort[0];
            int port = Integer.parseInt(ipPort[1]);
            // 创建通道
            ManagedChannel managedChannel = createManagedChannel(ip, port, nettyServerInfo);
            List<ManagedChannel> managedChannelList = new ArrayList<ManagedChannel>();
            managedChannelList.add(managedChannel);
            // 新建channel资源
            channelResource = new ChannelResource(key, nettyServerInfo);
            // 讲managedChannel列表 放入channel资源中
            channelResource.setChannels(managedChannelList);
            // 请求数加1并返回值
            channelResource.getRequestCount().addAndGet(1);
            // 将key 的 channel资源放入poolMap中
            poolMap.put(key, channelResource);
            return managedChannel;
        } else {
            return getRandomManagedChannel(channelResource);
        }

    }

    /**
     * 创建gRPC channel
     * @param ip
     * @param port
     * @param nettyServerInfo
     * @return
     */
    public synchronized ManagedChannel createManagedChannel(String ip, int port, NettyServerInfo nettyServerInfo) {
        try {
            logger.info("create channel ip {} port {} server info {}",ip,port,nettyServerInfo);

            NettyChannelBuilder channelBuilder = NettyChannelBuilder
                    .forAddress(ip, port)
                    .keepAliveTime(60, TimeUnit.SECONDS)
                    .keepAliveTimeout(60, TimeUnit.SECONDS)
                    .keepAliveWithoutCalls(true)
                    .idleTimeout(60, TimeUnit.SECONDS)
                    .perRpcBufferLimit(128 << 20)
                    .flowControlWindow(32 << 20)
                    .maxInboundMessageSize(32 << 20)
                    .enableRetry()
                    .retryBufferSize(16 << 20)
                    .maxRetryAttempts(20);

            if (nettyServerInfo != null && nettyServerInfo.getNegotiationType() == NegotiationType.TLS
                    && StringUtils.isNotBlank(nettyServerInfo.getCertChainFilePath())
                    && StringUtils.isNotBlank(nettyServerInfo.getPrivateKeyFilePath())
                    && StringUtils.isNotBlank(nettyServerInfo.getTrustCertCollectionFilePath())) {
                SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient()
                        .keyManager(new File(nettyServerInfo.getCertChainFilePath()), new File(nettyServerInfo.getPrivateKeyFilePath()))
                        .trustManager(new File(nettyServerInfo.getTrustCertCollectionFilePath()))
                        .sessionTimeout(3600 << 4)
                        .sessionCacheSize(65536);
                channelBuilder.sslContext(sslContextBuilder.build()).useTransportSecurity();

                logger.info("running in secure mode for endpoint {}:{}, client crt path: {}, client key path: {}, ca crt path: {}.",
                        ip, port, nettyServerInfo.getCertChainFilePath(), nettyServerInfo.getPrivateKeyFilePath(),
                        nettyServerInfo.getTrustCertCollectionFilePath());
            } else {
                channelBuilder.usePlaintext();
            }
            return channelBuilder.build();
        }
        catch (Exception e) {
            logger.error("create channel error : " ,e);
        }
        return null;
    }

    /**
     * channel 资源池
     */
    class ChannelResource {
        // ip + port
        String address;
        // channel 列表
        List<ManagedChannel> channels = Lists.newArrayList();
        // 请求数量
        // atomiclong 可以理解是加了synchronized的long
        AtomicLong requestCount = new AtomicLong(0);
        long latestChecktimestamp = 0;
        long preCheckCount = 0;
        NettyServerInfo nettyServerInfo;

        public ChannelResource(String address) {
            this(address, null);
        }

        public NettyServerInfo getNettyServerInfo() {
            return nettyServerInfo;
        }

        public  ChannelResource(String address, NettyServerInfo nettyServerInfo) {
            this.address = address;
            this.nettyServerInfo = nettyServerInfo;
        }

        public List<ManagedChannel> getChannels() {
            return channels;
        }

        public void setChannels(List<ManagedChannel> channels) {
            this.channels = channels;
        }

        public AtomicLong getRequestCount() {
            return requestCount;
        }

        public void setRequestCount(AtomicLong requestCount) {
            this.requestCount = requestCount;
        }

        public long getLatestChecktimestamp() {
            return latestChecktimestamp;
        }

        public void setLatestChecktimestamp(long latestChecktimestamp) {
            this.latestChecktimestamp = latestChecktimestamp;
        }

        public long getPreCheckCount() {
            return preCheckCount;
        }

        public void setPreCheckCount(long preCheckCount) {
            this.preCheckCount = preCheckCount;
        }
    }

}