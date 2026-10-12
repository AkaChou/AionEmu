package com.aionemu.loginserver.service;

import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.commons.services.CronService;
import com.aionemu.loginserver.utils.cron.ThreadPoolManagerRunnableRunner;

import lombok.RequiredArgsConstructor;

/**
 * 登录服 Cron 生命周期：以 Spring 容器创建 Job 实例（构造注入）的方式初始化，并提供关闭入口。
 * Login-server cron lifecycle: initializes CronService so job instances come from the Spring container
 * (constructor injection) and exposes the shutdown entry.
 */
@Service
@Lazy
@RequiredArgsConstructor
public class LoginCronService {

    private final AutowireCapableBeanFactory beanFactory;

    private volatile CronService cronService;

    /**
     * 初始化登录服 Cron 单例，Job 实例交由 Spring 装配。
     * Initializes the login cron singleton; job instances are wired by Spring.
     */
    public void initialize() {
        cronService = CronService.initSingleton(ThreadPoolManagerRunnableRunner.class, springJobFactory());
    }

    /**
     * 若已初始化则关闭持有的 Cron；否则关闭当前上下文中的全局实例。
     * Shuts down the owned cron when initialized; otherwise shuts down the current global instance.
     */
    public void shutdown() {
        CronService initialized = cronService;
        if (initialized != null) {
            cronService = null;
            initialized.shutdown();
            return;
        }
        CronService.shutdownCurrentIfInitialized();
    }

    /**
     * 返回基于 Spring 容器的 Quartz JobFactory：按构造器自动装配 Job 实例。
     * Returns a Quartz JobFactory backed by the Spring container, autowiring job instances by constructor.
     * @return Job 工厂 / job factory
     */
    private JobFactory springJobFactory() {
        return (TriggerFiredBundle bundle, Scheduler scheduler) -> (Job) beanFactory.createBean(
            bundle.getJobDetail().getJobClass(), AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);
    }
}
