package com.xxl.job.admin.core.scheduler;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.thread.*;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.client.ExecutorBizClient;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xuxueli 2018-10-28 00:18:17
 */

public class XxlJobScheduler  {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobScheduler.class);


    public void init() throws Exception {
        // init i18n
        initI18n();

        /**
         * 初始化fastTriggerPool（默认大小200）、slowTriggerPool（默认大小100）线程池。
         * JobTriggerPoolHelper作用：
         * 1、执行器的触发均是提交到该两个线程池中执行；
         * 2、内部还维护了一个map缓存，key是jobId，value是次数；
         * 3、每次job的时候同时会统计执行时间，若执行时间超过500ms，则记录到map缓存中，且value值+1；
         * 4、下次提交任务的时候，如果该job在map缓存中的值超过10次，则提交给慢线程池，否则提交给快线程池；
         * 5、map缓存每60s清空一次；
         */
        // admin trigger pool start
        JobTriggerPoolHelper.toStart();

        /**
         * 1、初始化registryOrRemoveThreadPool线程池。
         * 2、启动定时任务，每30s执行一次，剔除90S未更新的执行器节点，并将存活的执行器节点地址更新到Group中；
         */
        // admin registry monitor run
        JobRegistryHelper.getInstance().start();

        /**
         * 启动定时任务，每10s执行一次：
         * 1、扫描出【执行失败 && 告警状态为0（初始化）】的log；
         *  2、将告警状态更新为-1（锁定），防止多节点并发操作；
         *  3、如果还有重试次数，则进行重试，重试次数配置在任务页面；
         *  4、触发告警，根据告警结果更新log的告警状态字段：2-告警成功/3-告警失败；
         */
        // admin fail-monitor run
        JobFailMonitorHelper.getInstance().start();

        // admin lose-monitor run ( depend on JobTriggerPoolHelper )
        JobCompleteHelper.getInstance().start();

        // admin log report start
        JobLogReportHelper.getInstance().start();

        // start-schedule  ( depend on JobTriggerPoolHelper )
        JobScheduleHelper.getInstance().start();

        logger.info(">>>>>>>>> init xxl-job admin success.");
    }

    
    public void destroy() throws Exception {

        // stop-schedule
        JobScheduleHelper.getInstance().toStop();

        // admin log report stop
        JobLogReportHelper.getInstance().toStop();

        // admin lose-monitor stop
        JobCompleteHelper.getInstance().toStop();

        // admin fail-monitor stop
        JobFailMonitorHelper.getInstance().toStop();

        // admin registry stop
        JobRegistryHelper.getInstance().toStop();

        // admin trigger pool stop
        JobTriggerPoolHelper.toStop();

    }

    // ---------------------- I18n ----------------------

    private void initI18n(){
        for (ExecutorBlockStrategyEnum item:ExecutorBlockStrategyEnum.values()) {
            item.setTitle(I18nUtil.getString("jobconf_block_".concat(item.name())));
        }
    }

    // ---------------------- executor-client ----------------------
    private static ConcurrentMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<String, ExecutorBiz>();
    public static ExecutorBiz getExecutorBiz(String address) throws Exception {
        // valid
        if (address==null || address.trim().length()==0) {
            return null;
        }

        // load-cache
        address = address.trim();
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if (executorBiz != null) {
            return executorBiz;
        }

        // set-cache
        executorBiz = new ExecutorBizClient(address, XxlJobAdminConfig.getAdminConfig().getAccessToken());

        executorBizRepository.put(address, executorBiz);
        return executorBiz;
    }

}
