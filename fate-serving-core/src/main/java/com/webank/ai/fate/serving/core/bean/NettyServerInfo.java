package com.webank.ai.fate.serving.core.bean;

import com.webank.ai.fate.serving.core.utils.JsonUtil;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;

// 网络服务信息
public class NettyServerInfo {


    public NettyServerInfo() {
        this.negotiationType = NegotiationType.PLAINTEXT;
    }

    public NettyServerInfo(String negotiationType, String certChainFilePath, String privateKeyFilePath, String trustCertCollectionFilePath) {
        this.negotiationType = NegotiationType.valueOf(negotiationType);
        this.certChainFilePath = certChainFilePath;
        this.privateKeyFilePath = privateKeyFilePath;
        this.trustCertCollectionFilePath = trustCertCollectionFilePath;
    }

    private NegotiationType negotiationType;
    // 证书链文件路劲
    private String certChainFilePath;
    // 私有密钥文件路径
    private String privateKeyFilePath;
    // 信任证书集合文件路径
    private String trustCertCollectionFilePath;

    public NegotiationType getNegotiationType() {
        return negotiationType;
    }

    public void setNegotiationType(NegotiationType negotiationType) {
        this.negotiationType = negotiationType;
    }

    public String getCertChainFilePath() {
        return certChainFilePath;
    }

    public void setCertChainFilePath(String certChainFilePath) {
        this.certChainFilePath = certChainFilePath;
    }

    public String getPrivateKeyFilePath() {
        return privateKeyFilePath;
    }

    public void setPrivateKeyFilePath(String privateKeyFilePath) {
        this.privateKeyFilePath = privateKeyFilePath;
    }

    public String getTrustCertCollectionFilePath() {
        return trustCertCollectionFilePath;
    }

    public void setTrustCertCollectionFilePath(String trustCertCollectionFilePath) {
        this.trustCertCollectionFilePath = trustCertCollectionFilePath;
    }
    public String  toString(){
        return JsonUtil.object2Json(this);
    }
}
