package com.aionemu.gameserver.services;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


/**
 * 遗留服务 Bean 装配聚合入口：按域拆分到多个配置类。
 * Aggregator for legacy service beans; delegates to the per-domain configuration classes.
 */
@Configuration(proxyBeanMethods = false)
@Import({
    CoreRuntimeServiceBeans.class,
    EngineBeans.class,
    NetworkBeans.class,
    EventBeans.class,
    SiegeBattlefieldBeans.class,
    TaskAndStatBeans.class
})
public class GameLegacyServiceBridgeConfiguration {
}
