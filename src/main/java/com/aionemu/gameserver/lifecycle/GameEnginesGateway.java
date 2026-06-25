package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameEnginesGateway {

    private ObjectProvider<QuestEngine> questEngineProvider;
    private ObjectProvider<InstanceEngine> instanceEngineProvider;
    private ObjectProvider<AI2Engine> ai2EngineProvider;
    private ObjectProvider<ChatProcessor> chatProcessorProvider;
    private ObjectProvider<ThreadPoolManager> threadPoolManagerProvider;
    private ObjectProvider<GameEnginesRuntimeBridge> runtimeBridgeProvider;

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

    @Autowired(required = false)
    void setRuntimeBridgeProvider(ObjectProvider<GameEnginesRuntimeBridge> runtimeBridgeProvider) {
        this.runtimeBridgeProvider = runtimeBridgeProvider;
    }

    public void printSection() {
        Util.printSection(" *** Engines *** ");
    }

    public List<GameEngine> engines() {
        return List.of(
            questEngine(),
            instanceEngine(),
            ai2Engine(),
            chatProcessor()
        );
    }

    public void execute(Runnable runnable) {
        threadPoolManager().execute(runnable);
    }

    private QuestEngine questEngine() {
        if (questEngineProvider == null) {
            return runtimeBridge().questEngine();
        }
        return questEngineProvider.getIfAvailable(() -> runtimeBridge().questEngine());
    }

    private InstanceEngine instanceEngine() {
        if (instanceEngineProvider == null) {
            return runtimeBridge().instanceEngine();
        }
        return instanceEngineProvider.getIfAvailable(() -> runtimeBridge().instanceEngine());
    }

    private AI2Engine ai2Engine() {
        if (ai2EngineProvider == null) {
            return runtimeBridge().ai2Engine();
        }
        return ai2EngineProvider.getIfAvailable(() -> runtimeBridge().ai2Engine());
    }

    private ChatProcessor chatProcessor() {
        if (chatProcessorProvider == null) {
            return runtimeBridge().chatProcessor();
        }
        return chatProcessorProvider.getIfAvailable(() -> runtimeBridge().chatProcessor());
    }

    private ThreadPoolManager threadPoolManager() {
        if (threadPoolManagerProvider == null) {
            return runtimeBridge().threadPoolManager();
        }
        return threadPoolManagerProvider.getIfAvailable(() -> runtimeBridge().threadPoolManager());
    }

    private GameEnginesRuntimeBridge runtimeBridge() {
        if (runtimeBridgeProvider == null) {
            return new GameEnginesRuntimeBridge();
        }
        return runtimeBridgeProvider.getIfAvailable(GameEnginesRuntimeBridge::new);
    }
}
