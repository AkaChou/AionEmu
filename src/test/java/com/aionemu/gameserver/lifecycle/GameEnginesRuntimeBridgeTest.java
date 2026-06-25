package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class GameEnginesRuntimeBridgeTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        QuestEngine questEngine = instance(QuestEngine.class);
        InstanceEngine instanceEngine = instance(InstanceEngine.class);
        AI2Engine ai2Engine = instance(AI2Engine.class);
        ChatProcessor chatProcessor = instance(ChatProcessor.class);
        ThreadPoolManager threadPoolManager = instance(ThreadPoolManager.class);
        GameEnginesRuntimeBridge runtimeBridge = new GameEnginesRuntimeBridge();

        runtimeBridge.setQuestEngineProvider(provider(QuestEngine.class, questEngine));
        runtimeBridge.setInstanceEngineProvider(provider(InstanceEngine.class, instanceEngine));
        runtimeBridge.setAi2EngineProvider(provider(AI2Engine.class, ai2Engine));
        runtimeBridge.setChatProcessorProvider(provider(ChatProcessor.class, chatProcessor));
        runtimeBridge.setThreadPoolManagerProvider(provider(ThreadPoolManager.class, threadPoolManager));

        assertSame(questEngine, runtimeBridge.questEngine());
        assertSame(instanceEngine, runtimeBridge.instanceEngine());
        assertSame(ai2Engine, runtimeBridge.ai2Engine());
        assertSame(chatProcessor, runtimeBridge.chatProcessor());
        assertSame(threadPoolManager, runtimeBridge.threadPoolManager());
    }

    private <T> T instance(Class<T> type) {
        return objenesis.newInstance(type);
    }

    private static <T> ObjectProvider<T> provider(Class<T> type, T instance) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton(type.getName(), instance);
        return beanFactory.getBeanProvider(type);
    }
}
