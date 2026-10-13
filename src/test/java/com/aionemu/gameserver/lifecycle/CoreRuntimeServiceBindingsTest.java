package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aionemu.gameserver.services.AccountService;
import com.aionemu.gameserver.services.ArmsfusionService;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.services.LimitedQuestService;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.services.RenameService;
import com.aionemu.gameserver.services.SocialService;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * CoreRuntimeServiceBindings 接线审计：7 个核心运行时服务（CoreRuntimeServiceBeans 装配）的
 * fail-fast 单例通道必须由该绑定组件喂入，并在销毁时清空。
 * Wiring audit for CoreRuntimeServiceBindings: the fail-fast singleton channels of the seven core
 * runtime services (wired by CoreRuntimeServiceBeans) must be fed by this bindings component and
 * cleared on destroy.
 */
class CoreRuntimeServiceBindingsTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void bindingsComponentFeedsAndClearsEveryCoreRuntimeChannel() {
        AccountService accountService = objenesis.newInstance(AccountService.class);
        HTMLService htmlService = objenesis.newInstance(HTMLService.class);
        ArmsfusionService armsfusionService = objenesis.newInstance(ArmsfusionService.class);
        PunishmentService punishmentService = objenesis.newInstance(PunishmentService.class);
        RenameService renameService = objenesis.newInstance(RenameService.class);
        SocialService socialService = objenesis.newInstance(SocialService.class);
        LimitedQuestService limitedQuestService = objenesis.newInstance(LimitedQuestService.class);

        CoreRuntimeServiceBindings bindings = new CoreRuntimeServiceBindings(
            provider(AccountService.class, accountService),
            provider(HTMLService.class, htmlService),
            provider(ArmsfusionService.class, armsfusionService),
            provider(PunishmentService.class, punishmentService),
            provider(RenameService.class, renameService),
            provider(SocialService.class, socialService),
            provider(LimitedQuestService.class, limitedQuestService));
        try {
            assertSame(accountService, AccountService.getInstance());
            assertSame(htmlService, HTMLService.getInstance());
            assertSame(armsfusionService, ArmsfusionService.getInstance());
            assertSame(punishmentService, PunishmentService.getInstance());
            assertSame(renameService, RenameService.getInstance());
            assertSame(socialService, SocialService.getInstance());
            assertSame(limitedQuestService, LimitedQuestService.getInstance());
        } finally {
            bindings.destroy();
        }

        assertThrows(IllegalStateException.class, AccountService::getInstance);
        assertThrows(IllegalStateException.class, HTMLService::getInstance);
        assertThrows(IllegalStateException.class, ArmsfusionService::getInstance);
        assertThrows(IllegalStateException.class, PunishmentService::getInstance);
        assertThrows(IllegalStateException.class, RenameService::getInstance);
        assertThrows(IllegalStateException.class, SocialService::getInstance);
        assertThrows(IllegalStateException.class, LimitedQuestService::getInstance);
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
