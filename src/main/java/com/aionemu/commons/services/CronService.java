package com.aionemu.commons.services;

import com.aionemu.commons.services.cron.CronServiceException;
import com.aionemu.commons.services.cron.RunnableRunner;
import com.aionemu.commons.utils.GenericValidator;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定时任务调度服务类
 * Cron Task Scheduling Service Class
 *
 * 该类基于Quartz框架实现定时任务的调度管理，采用单例模式。
 * This class implements scheduled task management based on Quartz framework using Singleton pattern.
 *
 * 主要功能:
 * Main features:
 * 1. 支持基于cron表达式的任务调度
 *    Supports cron expression based task scheduling
 * 2. 支持长短任务的区分执行
 *    Supports differentiated execution of long and short running tasks
 * 3. 提供任务的添加、删除、查询等管理功能
 *    Provides task management functions including add, delete and query
 */
public final class CronService {
    /** 日志记录器 Logger instance */
    private static final Logger log = LoggerFactory.getLogger(CronService.class);
    
    /** 单例实例 Singleton instance */
    private static CronService instance;
    
    /** Quartz调度器 Quartz scheduler */
    private Scheduler scheduler;
    
    /** 可运行任务执行器类 Runnable task executor class */
    private Class<? extends RunnableRunner> runnableRunner;

    /**
     * 获取CronService实例
     * Get CronService instance
     *
     * @return CronService单例实例 CronService singleton instance
     */
    public static CronService getInstance() {
        return instance;
    }

    /**
     * 初始化CronService单例
     * Initialize CronService singleton
     *
     * @param runableRunner 任务执行器类 Task executor class
     * @throws CronServiceException 如果服务已初始化 if service is already initialized
     */
    public static synchronized void initSingleton(Class<? extends RunnableRunner> runableRunner) {
      if (instance != null) {
         throw new CronServiceException("CronService is already initialized");
      } else {
         CronService cs = new CronService();
         cs.init(runableRunner);
         instance = cs;
      }
   }

   private CronService() {
   }

    /**
     * 初始化调度器
     * Initialize scheduler
     *
     * @param runnableRunner 任务执行器类 Task executor class
     * @throws CronServiceException 初始化失败时 when initialization fails
     */
    public synchronized void init(Class<? extends RunnableRunner> runnableRunner) {
      if (this.scheduler == null) {
         if (runnableRunner == null) {
            throw new CronServiceException("RunnableRunner class must be defined");
         } else {
            this.runnableRunner = runnableRunner;
            Properties properties = new Properties();
            properties.setProperty("org.quartz.threadPool.threadCount", "1");
            ch.qos.logback.classic.Logger quartzLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.quartz");
            ch.qos.logback.classic.Level oldLevel = quartzLogger.getLevel();
            quartzLogger.setLevel(ch.qos.logback.classic.Level.WARN);

            try {
               this.scheduler = (new StdSchedulerFactory(properties)).getScheduler();
               this.scheduler.start();
            } catch (SchedulerException var4) {
               throw new CronServiceException("Failed to initialize CronService", var4);
            } finally {
                quartzLogger.setLevel(oldLevel);
            }
         }
      }
   }

    /**
     * 关闭调度器服务
     * Shutdown scheduler service
     */
    public void shutdown() {
      Scheduler localScheduler;
      synchronized(this) {
         if (this.scheduler == null) {
            return;
         }

         localScheduler = this.scheduler;
         this.scheduler = null;
         this.runnableRunner = null;
      }

      try {
         localScheduler.shutdown(false);
      } catch (SchedulerException var4) {
         log.error("Failed to shutdown CronService correctly", var4);
      }

   }

    /**
     * 调度一个定时任务
     * Schedule a timed task
     *
     * @param r 要执行的任务 Task to execute
     * @param cronExpression cron表达式 Cron expression
     */
    public void schedule(Runnable r, String cronExpression) {
      this.schedule(r, cronExpression, false);
   }

    /**
     * 调度一个定时任务，可指定是否为长时任务
     * Schedule a timed task with long-running option
     *
     * @param r 要执行的任务 Task to execute
     * @param cronExpression cron表达式 Cron expression
     * @param longRunning 是否为长时任务 Whether it's a long-running task
     * @throws CronServiceException 调度失败时 when scheduling fails
     */
    public void schedule(Runnable r, String cronExpression, boolean longRunning) {
      try {
         JobDataMap jdm = new JobDataMap();
         jdm.put("cronservice.scheduled.runnable.instance", r);
         jdm.put("cronservice.scheduled.runnable.islognrunning", longRunning);
         jdm.put("cronservice.scheduled.runnable.cronexpression", cronExpression);
         String jobId = "Started at ms" + System.currentTimeMillis() + "; ns" + System.nanoTime();
         JobKey jobKey = new JobKey("JobKey:" + jobId);
         JobDetail jobDetail = JobBuilder.newJob(this.runnableRunner).usingJobData(jdm).withIdentity(jobKey).build();
         CronScheduleBuilder csb = CronScheduleBuilder.cronSchedule(cronExpression);
         CronTrigger trigger = (CronTrigger)TriggerBuilder.newTrigger().withSchedule(csb).build();
         this.scheduler.scheduleJob(jobDetail, trigger);
      } catch (Exception var10) {
         throw new CronServiceException("Failed to start job", var10);
      }
   }

    /**
     * 取消一个已调度的任务
     * Cancel a scheduled task
     *
     * @param r 要取消的任务 Task to cancel
     */
    public void cancel(Runnable r) {
      Map<Runnable, JobDetail> map = this.getRunnables();
      JobDetail jd = (JobDetail)map.get(r);
      this.cancel(jd);
   }

    /**
     * 取消一个任务详情对应的调度任务
     * Cancel a scheduled task by job detail
     *
     * @param jd 任务详情 Job detail
     * @throws CronServiceException 取消失败时 when cancellation fails
     */
    public void cancel(JobDetail jd) {
      if (jd != null) {
         if (jd.getKey() == null) {
            throw new CronServiceException("JobDetail should have JobKey");
         } else {
            try {
               this.scheduler.deleteJob(jd.getKey());
            } catch (SchedulerException var3) {
               throw new CronServiceException("Failed to delete Job", var3);
            }
         }
      }
   }

    /**
     * 获取所有任务详情
     * Get all job details
     *
     * @return 任务详情集合 Collection of job details
     * @throws CronServiceException 获取失败时 when retrieval fails
     */
    protected Collection<JobDetail> getJobDetails() {
      if (this.scheduler == null) {
         return Collections.emptySet();
      } else {
         try {
            Set<JobKey> keys = this.scheduler.getJobKeys((GroupMatcher)null);
            if (GenericValidator.isBlankOrNull((Collection)keys)) {
               return Collections.emptySet();
            } else {
               Set<JobDetail> result = Sets.newHashSetWithExpectedSize(keys.size());
               Iterator i$ = keys.iterator();

               while(i$.hasNext()) {
                  JobKey jk = (JobKey)i$.next();
                  result.add(this.scheduler.getJobDetail(jk));
               }

               return result;
            }
         } catch (Exception var5) {
            throw new CronServiceException("Can't get all active job details", var5);
         }
      }
   }

    /**
     * 获取所有运行中的任务映射
     * Get all running tasks mapping
     *
     * @return 任务与详情的映射 Map of runnable to job detail
     */
    public Map<Runnable, JobDetail> getRunnables() {
      Collection<JobDetail> jobDetails = this.getJobDetails();
      if (GenericValidator.isBlankOrNull(jobDetails)) {
         return Collections.emptyMap();
      } else {
         Map<Runnable, JobDetail> result = Maps.newHashMap();
         Iterator i$ = jobDetails.iterator();

         while(i$.hasNext()) {
            JobDetail jd = (JobDetail)i$.next();
            if (!GenericValidator.isBlankOrNull((Map)jd.getJobDataMap()) && jd.getJobDataMap().containsKey("cronservice.scheduled.runnable.instance")) {
               result.put((Runnable)jd.getJobDataMap().get("cronservice.scheduled.runnable.instance"), jd);
            }
         }

         return Collections.unmodifiableMap(result);
      }
   }

    /**
     * 获取任务的触发器列表
     * Get triggers for a job
     *
     * @param jd 任务详情 Job detail
     * @return 触发器列表 List of triggers
     */
    public List<? extends Trigger> getJobTriggers(JobDetail jd) {
      return this.getJobTriggers(jd.getKey());
   }

    /**
     * 获取任务的触发器列表
     * Get triggers for a job
     *
     * @param jk 任务键 Job key
     * @return 触发器列表 List of triggers
     * @throws CronServiceException 获取失败时 when retrieval fails
     */
    public List<? extends Trigger> getJobTriggers(JobKey jk) {
      if (this.scheduler == null) {
         return Collections.emptyList();
      } else {
         try {
            return this.scheduler.getTriggersOfJob(jk);
         } catch (SchedulerException var3) {
            throw new CronServiceException("Can't get triggers for JobKey " + jk, var3);
         }
      }
   }
}
