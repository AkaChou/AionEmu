package com.aionemu.gameserver.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

class GameEnginesRuntimeBridgeTest {

    private final ObjenesisStd objenesis = new ObjenesisStd();

    @Test
    void usesSpringProvidersBeforeLegacySingletonFallbacks() {
        QuestEngine questEngine = instance(QuestEngine.class);
        SkillEngine skillEngine = instance(SkillEngine.class);
        InstanceEngine instanceEngine = instance(InstanceEngine.class);
        AI2Engine ai2Engine = instance(AI2Engine.class);
        ChatProcessor chatProcessor = instance(ChatProcessor.class);
        ThreadPoolManager threadPoolManager = instance(ThreadPoolManager.class);
        GameEnginesRuntimeBridge runtimeBridge = new GameEnginesRuntimeBridge();

        runtimeBridge.setQuestEngineProvider(provider(QuestEngine.class, questEngine));
        runtimeBridge.setSkillEngineProvider(provider(SkillEngine.class, skillEngine));
        runtimeBridge.setInstanceEngineProvider(provider(InstanceEngine.class, instanceEngine));
        runtimeBridge.setAi2EngineProvider(provider(AI2Engine.class, ai2Engine));
        runtimeBridge.setChatProcessorProvider(provider(ChatProcessor.class, chatProcessor));
        runtimeBridge.setThreadPoolManagerProvider(provider(ThreadPoolManager.class, threadPoolManager));

        assertSame(questEngine, runtimeBridge.questEngine());
        assertSame(skillEngine, runtimeBridge.skillEngine());
        assertSame(instanceEngine, runtimeBridge.instanceEngine());
        assertSame(ai2Engine, runtimeBridge.ai2Engine());
        assertSame(chatProcessor, runtimeBridge.chatProcessor());
        assertSame(threadPoolManager, runtimeBridge.threadPoolManager());
    }

    @Test
    void runtimeBridgeDoesNotCallLegacySingletonsDirectly() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/aionemu/gameserver/lifecycle/GameEnginesRuntimeBridge.java"));

        assertFalse(source.contains("QuestEngine.getInstance()"));
        assertFalse(source.contains("SkillEngine.getInstance()"));
        assertFalse(source.contains("InstanceEngine.getInstance()"));
        assertFalse(source.contains("AI2Engine.getInstance()"));
        assertFalse(source.contains("ChatProcessor.getInstance()"));
        assertFalse(source.contains("ThreadPoolManager.getInstance()"));
    }

    @Test
    void gameEngineServicesQuestEngineAccessorUsesSpringProviderBeforeLegacyFallback() {
        QuestEngine questEngine = instance(QuestEngine.class);
        GameEngineServices gameEngineServices = new GameEngineServices(
            provider(QuestEngine.class, questEngine),
            provider(SkillEngine.class, instance(SkillEngine.class)),
            provider(InstanceEngine.class, instance(InstanceEngine.class)),
            provider(AI2Engine.class, instance(AI2Engine.class)),
            provider(ChatProcessor.class, instance(ChatProcessor.class))
        );

        try {
            assertSame(questEngine, GameEngineServices.questEngine());
        } finally {
            gameEngineServices.destroy();
        }
    }

    @Test
    void gameEngineServicesSkillEngineAccessorUsesSpringProviderBeforeLegacyFallback() {
        SkillEngine skillEngine = instance(SkillEngine.class);
        GameEngineServices gameEngineServices = new GameEngineServices(
            provider(QuestEngine.class, instance(QuestEngine.class)),
            provider(SkillEngine.class, skillEngine),
            provider(InstanceEngine.class, instance(InstanceEngine.class)),
            provider(AI2Engine.class, instance(AI2Engine.class)),
            provider(ChatProcessor.class, instance(ChatProcessor.class))
        );

        try {
            assertSame(skillEngine, GameEngineServices.skillEngine());
        } finally {
            gameEngineServices.destroy();
        }
    }

    @Test
    void gameServerCodeUsesEngineQuestBridgeInsteadOfDirectSingleton() throws IOException {
        List<Path> sources;
        try (var stream = Files.walk(Path.of("src/main/java/com/aionemu/gameserver"))) {
            sources = stream
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.endsWith(Path.of("lifecycle/GameEngineServiceFallbacks.java")))
                .toList();
        }

        for (Path source : sources) {
            String content = Files.readString(source);

            assertFalse(content.contains("QuestEngine.getInstance()"), source.toString());
        }
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
