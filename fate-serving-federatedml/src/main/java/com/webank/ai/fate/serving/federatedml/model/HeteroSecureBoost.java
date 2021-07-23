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

package com.webank.ai.fate.serving.federatedml.model;

import com.google.common.collect.Maps;
import com.webank.ai.fate.core.mlmodel.buffer.BoostTreeModelMetaProto.BoostingTreeModelMeta;
import com.webank.ai.fate.core.mlmodel.buffer.BoostTreeModelParamProto.BoostingTreeModelParam;
import com.webank.ai.fate.core.mlmodel.buffer.BoostTreeModelParamProto.DecisionTreeModelParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public abstract class HeteroSecureBoost extends BaseComponent {
    public static final Logger logger = LoggerFactory.getLogger(HeteroSecureBoost.class);
    protected List<Map<Integer, Double>> splitMaskdict;
    protected Map<String, Integer> featureNameFidMapping = Maps.newHashMap();
    protected int treeNum;
    protected List<Double> initScore;
    protected List<DecisionTreeModelParam> trees;
    protected int numClasses;
    protected List<String> classes;
    protected int treeDim;
    protected double learningRate;

    @Override
    public int initModel(byte[] protoMeta, byte[] protoParam) {
        logger.info("start init HeteroLR class");
        try {
            //对输入的Meta和Param两个序列化的模型文件进行反序列化
            BoostingTreeModelParam param = this.parseModel(BoostingTreeModelParam.parser(), protoParam);
            BoostingTreeModelMeta meta = this.parseModel(BoostingTreeModelMeta.parser(), protoMeta);
            Map<Integer, String> featureNameMapping = param.getFeatureNameFidMapping();
            featureNameMapping.forEach((k, v) -> {
                featureNameFidMapping.put(v, k);
            });
            //初始化
            this.treeNum = param.getTreeNum();
            this.initScore = param.getInitScoreList();  //boost的初始化得分，具体可参考FATE离线建模文档
            this.trees = param.getTreesList();  //具体的树信息列表，可参考对应的DecisionTreeModelParam
            this.numClasses = param.getNumClasses();    //多少类，二分类问题为2，多分类问题则为具体分类数，回归问题为0，通过该字段可以判断具体建模任务类型
            this.classes = param.getClassesList();  //类别标签
            this.treeDim = param.getTreeDim();  //boost的每轮树的数量，对于回归和二分类等于1，对于多分类，是类别数量，每轮每个分类都有一个对应的树
            this.learningRate = meta.getLearningRate(); //学习率和权重放缩因子，推理时每个树得到的权重都会乘以learning_rate

        } catch (Exception ex) {
            ex.printStackTrace();
            return ILLEGALDATA;
        }
        logger.info("Finish init HeteroSecureBoost class");
        return OK;
    }

    //离线的时候，每个树节点的域信息是partyid，如host:10000，通过该函数获取$role
    protected String getSite(int treeId, int treeNodeId) {
        return this.trees.get(treeId).getTree(treeNodeId).getSitename().split(":", -1)[0];
    }

    //用来存储和读取每轮使用的数据
    protected String generateTag(String caseId, String modelId, int communicationRound) {
        return caseId + "_" + modelId + "_" + String.valueOf(communicationRound);
    }

    protected String[] parseTag(String tag) {
        return tag.split("_");
    }

    //输入当前的树、节点编号，特征值，输出树的下一层节点编号
    protected int gotoNextLevel(int treeId, int treeNodeId, Map<String, Object> input) {
        int nextTreeNodeId;
        int fid = this.trees.get(treeId).getTree(treeNodeId).getFid();
        double splitValue = this.trees.get(treeId).getSplitMaskdict().get(treeNodeId);
        String fidStr = String.valueOf(fid);
        if (input.containsKey(fidStr)) {
            if (Double.parseDouble(input.get(fidStr).toString()) <= splitValue + 1e-20) {
                nextTreeNodeId = this.trees.get(treeId).getTree(treeNodeId).getLeftNodeid();
            } else {
                nextTreeNodeId = this.trees.get(treeId).getTree(treeNodeId).getRightNodeid();
            }
        } else {
            if (this.trees.get(treeId).getMissingDirMaskdict().containsKey(treeNodeId)) {
                int missingDir = this.trees.get(treeId).getMissingDirMaskdict().get(treeNodeId);
                if (missingDir == 1) {
                    nextTreeNodeId = this.trees.get(treeId).getTree(treeNodeId).getRightNodeid();
                } else {
                    nextTreeNodeId = this.trees.get(treeId).getTree(treeNodeId).getLeftNodeid();
                }
            } else {
                nextTreeNodeId = this.trees.get(treeId).getTree(treeNodeId).getRightNodeid();
            }
        }
        return nextTreeNodeId;
    }

}

