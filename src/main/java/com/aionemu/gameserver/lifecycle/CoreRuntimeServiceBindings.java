package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.AccountService;
import com.aionemu.gameserver.services.ArmsfusionService;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.services.LimitedQuestService;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.services.RenameService;
import com.aionemu.gameserver.services.SocialService;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 核心运行时服务绑定：把 Spring ObjectProvider 喂入 {@code CoreRuntimeServiceBeans} 装配的
 * 7 个服务自持的 fail-fast 单例通道。
 * Core runtime service bindings: feeds the Spring ObjectProviders into the fail-fast singleton
 * channels held by the seven services wired in {@code CoreRuntimeServiceBeans}.
 * <p>这些服务的 fail-fast 通道在 Flex 迁移批次引入时遗漏了接线（实机登录认证链路曾以
 * 「AccountService 未由 Spring 提供」失败），本组件补齐；消费方一律走各服务自持的
 * {@code getInstance()} 通道。
 * These services missed their wiring when the fail-fast channels were introduced by the
 * Flex-migration batch (the live login-auth chain failed with "AccountService not provided by
 * Spring"); this component fills that gap and consumers use each service's fail-fast
 * {@code getInstance()} channel.</p>
 */
@Component
public final class CoreRuntimeServiceBindings implements DisposableBean {

    /**
     * 构造并注册各核心运行时服务的实例提供者。
     * Construct and register instance providers for the core runtime services.
     * @param accountServiceProvider 账号服务提供者 / Account service provider
     * @param htmlServiceProvider HTML 指引服务提供者 / HTML-guide service provider
     * @param armsfusionServiceProvider 武器融合服务提供者 / Armsfusion service provider
     * @param punishmentServiceProvider 惩罚服务提供者 / Punishment service provider
     * @param renameServiceProvider 改名服务提供者 / Rename service provider
     * @param socialServiceProvider 社交服务提供者 / Social service provider
     * @param limitedQuestServiceProvider 限量任务服务提供者 / Limited-quest service provider
     */
    public CoreRuntimeServiceBindings(ObjectProvider<AccountService> accountServiceProvider,
            ObjectProvider<HTMLService> htmlServiceProvider,
            ObjectProvider<ArmsfusionService> armsfusionServiceProvider,
            ObjectProvider<PunishmentService> punishmentServiceProvider,
            ObjectProvider<RenameService> renameServiceProvider,
            ObjectProvider<SocialService> socialServiceProvider,
            ObjectProvider<LimitedQuestService> limitedQuestServiceProvider) {
        AccountService.setInstanceProvider(accountServiceProvider);
        HTMLService.setInstanceProvider(htmlServiceProvider);
        ArmsfusionService.setInstanceProvider(armsfusionServiceProvider);
        PunishmentService.setInstanceProvider(punishmentServiceProvider);
        RenameService.setInstanceProvider(renameServiceProvider);
        SocialService.setInstanceProvider(socialServiceProvider);
        LimitedQuestService.setInstanceProvider(limitedQuestServiceProvider);
    }

    /**
     * 销毁时清空各服务的静态提供者通道。
     * Clears each service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        AccountService.setInstanceProvider(null);
        HTMLService.setInstanceProvider(null);
        ArmsfusionService.setInstanceProvider(null);
        PunishmentService.setInstanceProvider(null);
        RenameService.setInstanceProvider(null);
        SocialService.setInstanceProvider(null);
        LimitedQuestService.setInstanceProvider(null);
    }
}
