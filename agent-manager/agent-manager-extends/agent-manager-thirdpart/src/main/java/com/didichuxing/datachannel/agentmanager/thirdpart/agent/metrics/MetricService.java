package com.didichuxing.datachannel.agentmanager.thirdpart.agent.metrics;

import com.alibaba.fastjson.JSON;
import com.didichuxing.datachannel.agentmanager.common.bean.domain.agent.AgentDO;
import com.didichuxing.datachannel.agentmanager.common.bean.domain.receiver.ReceiverTopicDO;
import com.didichuxing.datachannel.agentmanager.common.bean.po.agent.AgentPO;
import com.didichuxing.datachannel.agentmanager.common.bean.po.receiver.KafkaClusterPO;
import com.didichuxing.datachannel.agentmanager.common.util.ConvertUtil;
import com.didichuxing.datachannel.agentmanager.persistence.mysql.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class MetricService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricService.class);

    @Autowired
    private KafkaClusterMapper kafkaClusterMapper;

    @Autowired
    private AgentMetricsDAO agentMetricsDAO;

    private volatile boolean errorLogsWriteStopTrigger = false;
    private volatile boolean metricsWriteStopTrigger = false;
    private volatile boolean errorLogsWriteStopped = true;
    private volatile boolean metricsWriteStopped = true;

    private static final Long RECEIVER_CLOSE_TIME_OUT_MS = 1 * 60 * 1000l;

    private static final String CONSUMER_GROUP_ID = "g1";

    private static final long RETENTION_TIME = 7 * 24 * 3600 * 1000;

    private static Set<ReceiverTopicDO> metricSet = new HashSet<>();
    private static Set<ReceiverTopicDO> errorSet = new HashSet<>();

    private KafkaClusterPO lastAgentErrorLogsKafkaClusterPO = null;
    private KafkaClusterPO lastAgentMetricsKafkaClusterPO = null;

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 2, 2, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));

    public void writeMetrics(String agentMetricsTopic, String kafkaClusterBrokerConfiguration) {
        try {
            LOGGER.info("Thread: {}, cluster: {}, topic: {}", Thread.currentThread().getName(), kafkaClusterBrokerConfiguration, agentMetricsTopic);
            Properties properties = getProducerProps(kafkaClusterBrokerConfiguration);
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
            consumer.subscribe(Arrays.asList(agentMetricsTopic));
            while (true) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                    agentMetricsDAO.writeMetrics(records);
                    if (metricsWriteStopTrigger) {
                        consumer.close();
                        break;
                    }
                } catch (Throwable ex) {
                    LOGGER.error(
                            String.format("writeMetrics error: %s", ex.getMessage()),
                            ex
                    );
                    consumer.close();
                    break;
                }
            }
        } catch (Throwable ex) {
            LOGGER.error(
                    String.format("writeMetrics error: %s", ex.getMessage()),
                    ex
            );
        } finally {
            metricsWriteStopped = true;
        }
    }

    public void writeErrorLogs(String agentErrorLogsTopic, String kafkaClusterBrokerConfiguration) {
        try {
            LOGGER.info("Thread: {}, cluster: {}, topic: {}", Thread.currentThread().getName(), kafkaClusterBrokerConfiguration, agentErrorLogsTopic);
            Properties properties = getProducerProps(kafkaClusterBrokerConfiguration);
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
            consumer.subscribe(Arrays.asList(agentErrorLogsTopic));
            while (true) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
                    agentMetricsDAO.writeErrors(records);
                    if (errorLogsWriteStopTrigger) {
                        consumer.close();
                        break;
                    }
                } catch (Throwable ex) {
                    LOGGER.error(
                            String.format("writeErrorLogs error: %s", ex.getMessage()),
                            ex
                    );
                    consumer.close();
                }
            }
        } catch (Throwable ex) {
            LOGGER.error(
                    String.format("writeErrorLogs error: %s", ex.getMessage()),
                    ex
            );
        } finally {
            errorLogsWriteStopped = true;
        }
    }

    private Properties getProducerProps(String bootstrapServers) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServers);
        props.put("group.id", CONSUMER_GROUP_ID);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }

    @PostConstruct
    public void resetMetricConsumers() {
        /*
         * 1.）获取 agent metrics & error logs 对应接收端信息、topic
         */
        KafkaClusterPO agentErrorLogsKafkaClusterPO = kafkaClusterMapper.getAgentErrorLogsTopicExistsKafkaCluster();
        KafkaClusterPO agentMetricsKafkaClusterPO = kafkaClusterMapper.getAgentMetricsTopicExistsKafkaCluster();
        /*
         * 2.）校验较上一次获取是否相同，如不同，则立即进行对应变更处理
         */
        if(errorLogsReceiverChanged(lastAgentErrorLogsKafkaClusterPO, agentErrorLogsKafkaClusterPO)) {
            LOGGER.info(
                    String.format("ErrorLogs receiver changed, before is %s, after is %s", JSON.toJSONString(lastAgentErrorLogsKafkaClusterPO), JSON.toJSONString(agentErrorLogsKafkaClusterPO))
            );
            restartWriteErrorLogs(agentErrorLogsKafkaClusterPO);
            lastAgentErrorLogsKafkaClusterPO = agentErrorLogsKafkaClusterPO;
        }
        if(metricsReceiverChanged(lastAgentMetricsKafkaClusterPO, agentMetricsKafkaClusterPO)) {
            LOGGER.info(
                    String.format("Metrics receiver changed, before is %s, after is %s", JSON.toJSONString(lastAgentMetricsKafkaClusterPO), JSON.toJSONString(agentMetricsKafkaClusterPO))
            );
            restartWriteMetrics(agentMetricsKafkaClusterPO);
            lastAgentMetricsKafkaClusterPO = agentMetricsKafkaClusterPO;
        }
    }

    private boolean errorLogsReceiverChanged(KafkaClusterPO lastAgentErrorLogsKafkaClusterPO, KafkaClusterPO agentErrorLogsKafkaClusterPO) {
        if(null == lastAgentErrorLogsKafkaClusterPO && null == agentErrorLogsKafkaClusterPO) {
            return false;
        }
        if(null == agentErrorLogsKafkaClusterPO) {
            return false;
        }
        if(null == lastAgentErrorLogsKafkaClusterPO && null != agentErrorLogsKafkaClusterPO) {
            return true;
        }
        if(
                !lastAgentErrorLogsKafkaClusterPO.getAgentErrorLogsTopic().equals(agentErrorLogsKafkaClusterPO.getAgentErrorLogsTopic()) ||
                        !lastAgentErrorLogsKafkaClusterPO.getKafkaClusterBrokerConfiguration().equals(agentErrorLogsKafkaClusterPO.getKafkaClusterBrokerConfiguration())
        ) {
            return true;
        }
        return false;
    }

    private boolean metricsReceiverChanged(KafkaClusterPO lastAgentMetricsKafkaClusterPO, KafkaClusterPO agentMetricsKafkaClusterPO) {
        if(null == lastAgentMetricsKafkaClusterPO && null == agentMetricsKafkaClusterPO) {
            return false;
        }
        if(null == agentMetricsKafkaClusterPO) {
            return false;
        }
        if(null == lastAgentMetricsKafkaClusterPO && null != agentMetricsKafkaClusterPO) {
            return true;
        }
        if(
                !lastAgentMetricsKafkaClusterPO.getAgentMetricsTopic().equals(agentMetricsKafkaClusterPO.getAgentMetricsTopic()) ||
                        !lastAgentMetricsKafkaClusterPO.getKafkaClusterBrokerConfiguration().equals(agentMetricsKafkaClusterPO.getKafkaClusterBrokerConfiguration())
        ) {
            return true;
        }
        return false;
    }

    private void restartWriteMetrics(KafkaClusterPO agentMetricsKafkaClusterPO) {
        LOGGER.info(
                String.format("restartWriteMetrics: Is going to stop receiver %s", JSON.toJSONString(lastAgentMetricsKafkaClusterPO))
        );
        /*
         * stop
         */
        metricsWriteStopTrigger = true;
        Long currentTime = System.currentTimeMillis();
        while (
                !metricsWriteStopped &&
                        (System.currentTimeMillis() - currentTime) <= RECEIVER_CLOSE_TIME_OUT_MS
        ) {
            try {
                // 等待现有的kafka consumer线程全部关闭
                Thread.sleep(1 * 1000);
            } catch (InterruptedException e) {
                LOGGER.error("thread interrupted", e);
            }
        }
        LOGGER.info(
                String.format("restartWriteErrorLogs: Stop receiver %s successful", JSON.toJSONString(lastAgentMetricsKafkaClusterPO))
        );
        LOGGER.info(
                String.format("restartWriteErrorLogs: Is going to start receiver %s", JSON.toJSONString(agentMetricsKafkaClusterPO))
        );
        /*
         * start
         */
        metricsWriteStopped = false;
        metricsWriteStopTrigger = false;
        executor.execute(() -> writeMetrics(agentMetricsKafkaClusterPO.getAgentMetricsTopic(), agentMetricsKafkaClusterPO.getKafkaClusterBrokerConfiguration()));
        LOGGER.info(
                String.format("restartWriteErrorLogs: Start receiver %s successful", JSON.toJSONString(agentMetricsKafkaClusterPO))
        );
    }

    private void restartWriteErrorLogs(KafkaClusterPO agentErrorLogsKafkaClusterPO) {
        LOGGER.info(
                String.format("restartWriteErrorLogs: Is going to stop receiver %s", JSON.toJSONString(lastAgentErrorLogsKafkaClusterPO))
        );
        /*
         * stop
         */
        errorLogsWriteStopTrigger = true;
        Long currentTime = System.currentTimeMillis();
        while (
                !errorLogsWriteStopped &&
                        (System.currentTimeMillis() - currentTime) <= RECEIVER_CLOSE_TIME_OUT_MS
        ) {
            try {
                // 等待现有的kafka consumer线程全部关闭
                Thread.sleep(1 * 1000);
            } catch (InterruptedException e) {
                LOGGER.error("thread interrupted", e);
            }
        }
        LOGGER.info(
                String.format("restartWriteErrorLogs: Stop receiver %s successful", JSON.toJSONString(lastAgentErrorLogsKafkaClusterPO))
        );
        LOGGER.info(
                String.format("restartWriteErrorLogs: Is going to start receiver %s", JSON.toJSONString(agentErrorLogsKafkaClusterPO))
        );
        /*
         * start
         */
        errorLogsWriteStopped = false;
        errorLogsWriteStopTrigger = false;
        executor.execute(() -> writeErrorLogs(agentErrorLogsKafkaClusterPO.getAgentErrorLogsTopic(), agentErrorLogsKafkaClusterPO.getKafkaClusterBrokerConfiguration()));
        LOGGER.info(
                String.format("restartWriteErrorLogs: Start receiver %s successful", JSON.toJSONString(agentErrorLogsKafkaClusterPO))
        );
    }

}
