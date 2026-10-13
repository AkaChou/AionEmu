package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 引擎服务静态绑定：把 Spring ObjectProvider 喂入引擎域服务自持的 fail-fast 单例通道。
 * Engine static bindings: feeds the Spring ObjectProviders into the fail-fast singleton accessors
 * held by the Engine services.
 * <p>本组件取代已退役的引擎静态定位器（R5 批次）：不再暴露任何静态访问方法，
 * 消费方一律走域服务自持的 {@code getInstance()} 通道。
 * Replaces the retired Engine static locator: no static accessor is exposed anymore;
 * consumers use each domain service's fail-fast {@code getInstance()} channel.</p>
 */
@Component
public final class EngineServiceBindings implements DisposableBean {

    /**
     * 构造并注册各引擎服务的实例提供者。
     * Construct and register instance providers for Engine services.
     * @param questEngineProvider QuestEngine 提供者 / QuestEngine provider
     * @param skillEngineProvider SkillEngine 提供者 / SkillEngine provider
     * @param instanceEngineProvider InstanceEngine 提供者 / InstanceEngine provider
     * @param ai2EngineProvider AI2Engine 提供者 / AI2Engine provider
     * @param chatProcessorProvider ChatProcessor 提供者 / ChatProcessor provider
     */
    public EngineServiceBindings(ObjectProvider<QuestEngine> questEngineProvider,
            ObjectProvider<SkillEngine> skillEngineProvider, ObjectProvider<InstanceEngine> instanceEngineProvider,
            ObjectProvider<AI2Engine> ai2EngineProvider, ObjectProvider<ChatProcessor> chatProcessorProvider) {
        QuestEngine.setInstanceProvider(questEngineProvider);
        SkillEngine.setInstanceProvider(skillEngineProvider);
        InstanceEngine.setInstanceProvider(instanceEngineProvider);
        AI2Engine.setInstanceProvider(ai2EngineProvider);
        ChatProcessor.setInstanceProvider(chatProcessorProvider);
    }

    /**
     * 销毁时清空各域服务的静态提供者通道。
     * Clears each domain service's static provider channel on destroy.
     */
    @Override
    public void destroy() {
        QuestEngine.setInstanceProvider(null);
        SkillEngine.setInstanceProvider(null);
        InstanceEngine.setInstanceProvider(null);
        AI2Engine.setInstanceProvider(null);
        ChatProcessor.setInstanceProvider(null);
    }
}
