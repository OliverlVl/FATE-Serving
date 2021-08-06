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

import com.webank.ai.fate.core.mlmodel.buffer.FeatureBinningMetaProto.FeatureBinningMeta;
import com.webank.ai.fate.core.mlmodel.buffer.FeatureBinningMetaProto.TransformMeta;
import com.webank.ai.fate.core.mlmodel.buffer.FeatureBinningParamProto.FeatureBinningParam;
import com.webank.ai.fate.core.mlmodel.buffer.FeatureBinningParamProto.FeatureBinningResult;
import com.webank.ai.fate.core.mlmodel.buffer.FeatureBinningParamProto.IVParam;
import com.webank.ai.fate.serving.core.bean.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeteroFeatureBinning extends BaseComponent {
    private static final Logger logger = LoggerFactory.getLogger(HeteroFeatureBinning.class);
    private Map<String, List<Double>> splitPoints;
    private List<Long> transformCols;
    private List<String> header;
    private boolean needRun;

    @Override
    public int initModel(byte[] protoMeta, byte[] protoParam) {
        logger.info("start init Feature Binning class");
        this.needRun = false;
        this.splitPoints = new HashMap<>(8);
        try {
            //对输入的Meta和Param两个序列化的模型文件进行反序列化
            FeatureBinningMeta featureBinningMeta = this.parseModel(FeatureBinningMeta.parser(), protoMeta);
            //从离线模型中继承参数
            this.needRun = featureBinningMeta.getNeedRun(); //是否需要执行，如果为否，这个组件在后续预测时将被跳过
            TransformMeta transformMeta = featureBinningMeta.getTransformParam();
            this.transformCols = transformMeta.getTransformColsList();  //需要对哪些列做转化
            FeatureBinningParam featureBinningParam = this.parseModel(FeatureBinningParam.parser(), protoParam);
            this.header = featureBinningParam.getHeaderList();
            FeatureBinningResult featureBinningResult = featureBinningParam.getBinningResult(); //特征分箱后的结果。其中，包含每个特征的iv(?)，分箱点，woe等
            Map<String, IVParam> binningResult = featureBinningResult.getBinningResultMap();
            for (String key : binningResult.keySet()) {
                IVParam oneColResult = binningResult.get(key);
                List<Double> splitPoints = oneColResult.getSplitPointsList();
                this.splitPoints.put(key, splitPoints);
            }
        } catch (Exception ex) {
            logger.error("init model error:", ex);
            return ILLEGALDATA;
        }
        logger.info("Finish init Feature Binning class");
        return OK;
    }

    //在本地进行转化功能，将数据和模型结果中的splitPoint比较，确定属于哪个分箱后，用分箱的index代替原值
    @Override
    public Map<String, Object> localInference(Context context, List<Map<String, Object>> inputData) {
        HashMap<String, Object> outputData = new HashMap<>(8);
        HashMap<String, Long> headerMap = new HashMap<>(8);
        Map<String, Object> firstData = inputData.get(0);
        if (!this.needRun) {
            return firstData;
        }
        for (int i = 0; i < this.header.size(); i++) {
            headerMap.put(this.header.get(i), (long) i);  //特征列
        }
        for (String colName : firstData.keySet()) {
            try {
                if (!this.splitPoints.containsKey(colName)) {   //该列没有分裂点，直接输出
                    outputData.put(colName, firstData.get(colName));
                    continue;
                }
                Long thisColIndex = headerMap.get(colName); //当前列的序号
                if (!this.transformCols.contains(thisColIndex)) {   //该列不分箱，直接输出
                    outputData.put(colName, firstData.get(colName));
                    continue;
                }
                List<Double> splitPoint = this.splitPoints.get(colName);
                Double colValue = Double.valueOf(firstData.get(colName).toString());
                int colIndex = Collections.binarySearch(splitPoint, colValue);  //分裂点序号
                if (colIndex < 0) {
                    colIndex = Math.min((- colIndex - 1), splitPoint.size() - 1);
                }
                outputData.put(colName, colIndex);
            } catch (Throwable e) {
                logger.error("HeteroFeatureBinning error", e);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("DEBUG: HeteroFeatureBinning output {}", outputData);
        }
        return outputData;
    }

}
