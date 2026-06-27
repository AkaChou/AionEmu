package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameEngineServices implements DisposableBean {

    private static volatile ObjectProvider<QuestEngine> questEngineProvider;
    private static volatile ObjectProvider<SkillEngine> skillEngineProvider;
    private static volatile ObjectProvider<InstanceEngine> instanceEngineProvider;
    private static volatile ObjectProvider<AI2Engine> ai2EngineProvider;
    private static volatile ObjectProvider<ChatProcessor> chatProcessorProvider;

    public GameEngineServices(ObjectProvider<QuestEngine> questEngineProvider,
            ObjectProvider<SkillEngine> skillEngineProvider, ObjectProvider<InstanceEngine> instanceEngineProvider,
            ObjectProvider<AI2Engine> ai2EngineProvider, ObjectProvider<ChatProcessor> chatProcessorProvider) {
        GameEngineServices.questEngineProvider = questEngineProvider;
        GameEngineServices.skillEngineProvider = skillEngineProvider;
        GameEngineServices.instanceEngineProvider = instanceEngineProvider;
        GameEngineServices.ai2EngineProvider = ai2EngineProvider;
        GameEngineServices.chatProcessorProvider = chatProcessorProvider;
        QuestEngine.setInstanceProvider(questEngineProvider);
        SkillEngine.setInstanceProvider(skillEngineProvider);
        InstanceEngine.setInstanceProvider(instanceEngineProvider);
        AI2Engine.setInstanceProvider(ai2EngineProvider);
        ChatProcessor.setInstanceProvider(chatProcessorProvider);
    }

    public static QuestEngine questEngine() {
        ObjectProvider<QuestEngine> provider = questEngineProvider;
        if (provider == null) {
            return GameEngineServiceFallbacks.questEngine();
        }
        return provider.getIfAvailable(GameEngineServiceFallbacks::questEngine);
    }

    public static SkillEngine skillEngine() {
        ObjectProvider<SkillEngine> provider = skillEngineProvider;
        if (provider == null) {
            return GameEngineServiceFallbacks.skillEngine();
        }
        return provider.getIfAvailable(GameEngineServiceFallbacks::skillEngine);
    }

    public static InstanceEngine instanceEngine() {
        ObjectProvider<InstanceEngine> provider = instanceEngineProvider;
        if (provider == null) {
            return InstanceEngine.getInstance();
        }
        return provider.getIfAvailable(InstanceEngine::getInstance);
    }

    public static AI2Engine ai2Engine() {
        ObjectProvider<AI2Engine> provider = ai2EngineProvider;
        if (provider == null) {
            return AI2Engine.getInstance();
        }
        return provider.getIfAvailable(AI2Engine::getInstance);
    }

    public static ChatProcessor chatProcessor() {
        ObjectProvider<ChatProcessor> provider = chatProcessorProvider;
        if (provider == null) {
            return ChatProcessor.getInstance();
        }
        return provider.getIfAvailable(ChatProcessor::getInstance);
    }

    @Override
    public void destroy() {
        questEngineProvider = null;
        skillEngineProvider = null;
        instanceEngineProvider = null;
        ai2EngineProvider = null;
        chatProcessorProvider = null;
        QuestEngine.setInstanceProvider(null);
        SkillEngine.setInstanceProvider(null);
        InstanceEngine.setInstanceProvider(null);
        AI2Engine.setInstanceProvider(null);
        ChatProcessor.setInstanceProvider(null);
    }
}
