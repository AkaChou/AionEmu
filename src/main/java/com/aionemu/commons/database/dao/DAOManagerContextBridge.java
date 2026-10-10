package com.aionemu.commons.database.dao;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 将 Spring ApplicationContext 桥接至 DAOManager，使 DAOManager 优先解析 Spring 容器托管的 Bean。
 * Bridges Spring ApplicationContext to DAOManager so DAOManager resolves Spring-managed Beans first.
 */
@Component
public class DAOManagerContextBridge implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        DAOManager.setApplicationContext(applicationContext);
    }
}
