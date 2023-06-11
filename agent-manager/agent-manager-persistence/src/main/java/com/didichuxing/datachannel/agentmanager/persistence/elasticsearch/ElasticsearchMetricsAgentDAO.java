package com.didichuxing.datachannel.agentmanager.persistence.elasticsearch;

import com.didichuxing.datachannel.agentmanager.common.bean.po.metrics.MetricsAgentPO;
import com.didichuxing.datachannel.agentmanager.common.bean.po.metrics.MetricsLogCollectTaskTopPO;
import com.didichuxing.datachannel.agentmanager.common.bean.vo.metrics.MetricPoint;
import com.didichuxing.datachannel.agentmanager.persistence.MetricsAgentDAO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository(value = "ElasticsearchMetricsAgentDAO")
public class ElasticsearchMetricsAgentDAO implements MetricsAgentDAO {
    @Override
    public int insertSelective(MetricsAgentPO record) {
        return 0;
    }

    @Override
    public MetricsAgentPO selectByPrimaryKey(Long id) {
        return null;
    }

    @Override
    public Object getLast(Map<String, Object> params) {
        return null;
    }

    @Override
    public List<MetricPoint> getSingleChatNonStatistic(Map<String, Object> params) {
        return null;
    }

    @Override
    public List<MetricPoint> getSingleChatStatistic(Map<String, Object> params) {
        return null;
    }

    @Override
    public Double getSumMetricAllAgents(Map<String, Object> params) {
        return null;
    }

    @Override
    public List<MetricsLogCollectTaskTopPO> getTopNByMetricPerHostName(Map<String, Object> params) {
        return null;
    }

    @Override
    public List<MetricPoint> getSingleChatStatisticByHostName(Map<String, Object> params) {
        return null;
    }

    @Override
    public List<MetricPoint> getSingleChatNonStatisticByHostName(Map<String, Object> params) {
        return null;
    }

    @Override
    public void deleteByLtHeartbeatTime(Long heartBeatTime) {

    }

    @Override
    public Object getAggregationQueryPerHostNameFromMetricsAgent(Map<String, Object> params) {
        return null;
    }

    @Override
    public MetricsAgentPO getLastRecord(Map<String, Object> params) {
        return null;
    }

    @Override
    public List<MetricsAgentPO> getErrorMetrics(Map<String, Object> params) {
        return null;
    }
}
