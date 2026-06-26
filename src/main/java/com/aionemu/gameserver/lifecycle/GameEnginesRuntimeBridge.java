package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameEnginesRuntimeBridge {

    private ObjectProvider<QuestEngine> questEngineProvider;
    private ObjectProvider<InstanceEngine> instanceEngineProvider;
    private ObjectProvider<AI2Engine> ai2EngineProvider;
    private ObjectProvider<ChatProcessor> chatProcessorProvider;
    private ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;

    @Autowired(required = false)
    void setQuestEngineProvider(ObjectProvider<QuestEngine> questEngineProvider) {
        this.questEngineProvider = questEngineProvider;
    }

    @Autowired(required = false)
    void setInstanceEngineProvider(ObjectProvider<InstanceEngine> instanceEngineProvider) {
        this.instanceEngineProvider = instanceEngineProvider;
    }

    @Autowired(required = false)
    void setAi2EngineProvider(ObjectProvider<AI2Engine> ai2EngineProvider) {
        this.ai2EngineProvider = ai2EngineProvider;
    }

    @Autowired(required = false)
    void setChatProcessorProvider(ObjectProvider<ChatProcessor> chatProcessorProvider) {
        this.chatProcessorProvider = chatProcessorProvider;
    }

    @Autowired(required = false)
    void setThreadPoolManagerProvider(ObjectProvider<ThreadPoolManager> threadPoolManagerProvider) {
        this.threadPoolManagerProvider = threadPoolManagerProvider;
    }

    public QuestEngine questEngine() {
        if (questEngineProvider == null) {
            return GameEngineServiceFallbacks.questEngine();
        }
        return questEngineProvider.getIfAvailable(GameEngineServiceFallbacks::questEngine);
    }

    public InstanceEngine instanceEngine() {
        if (instanceEngineProvider == null) {
            return GameEngineServiceFallbacks.instanceEngine();
        }
        return instanceEngineProvider.getIfAvailable(GameEngineServiceFallbacks::instanceEngine);
    }

    public AI2Engine ai2Engine() {
        if (ai2EngineProvider == null) {
            return GameEngineServiceFallbacks.ai2Engine();
        }
        return ai2EngineProvider.getIfAvailable(GameEngineServiceFallbacks::ai2Engine);
    }

    public ChatProcessor chatProcessor() {
        if (chatProcessorProvider == null) {
            return GameEngineServiceFallbacks.chatProcessor();
        }
        return chatProcessorProvider.getIfAvailable(GameEngineServiceFallbacks::chatProcessor);
    }

    public ThreadPoolManager threadPoolManager() {
        if (threadPoolManagerProvider == null) {
            return GameEngineServiceFallbacks.threadPoolManager();
        }
        return threadPoolManagerProvider.getIfAvailable(GameEngineServiceFallbacks::threadPoolManager);
    }
}
