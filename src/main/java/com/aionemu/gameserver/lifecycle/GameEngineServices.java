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

    public GameEngineServices(ObjectProvider<QuestEngine> questEngineProvider,
            ObjectProvider<SkillEngine> skillEngineProvider, ObjectProvider<InstanceEngine> instanceEngineProvider,
            ObjectProvider<AI2Engine> ai2EngineProvider, ObjectProvider<ChatProcessor> chatProcessorProvider) {
        GameEngineServices.questEngineProvider = questEngineProvider;
        GameEngineServices.skillEngineProvider = skillEngineProvider;
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

    @Override
    public void destroy() {
        questEngineProvider = null;
        skillEngineProvider = null;
        QuestEngine.setInstanceProvider(null);
        SkillEngine.setInstanceProvider(null);
        InstanceEngine.setInstanceProvider(null);
        AI2Engine.setInstanceProvider(null);
        ChatProcessor.setInstanceProvider(null);
    }
}
