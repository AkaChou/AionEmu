package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameEngineServices implements DisposableBean {

    public GameEngineServices(ObjectProvider<QuestEngine> questEngineProvider,
            ObjectProvider<InstanceEngine> instanceEngineProvider, ObjectProvider<AI2Engine> ai2EngineProvider,
            ObjectProvider<ChatProcessor> chatProcessorProvider) {
        QuestEngine.setInstanceProvider(questEngineProvider);
        InstanceEngine.setInstanceProvider(instanceEngineProvider);
        AI2Engine.setInstanceProvider(ai2EngineProvider);
        ChatProcessor.setInstanceProvider(chatProcessorProvider);
    }

    @Override
    public void destroy() {
        QuestEngine.setInstanceProvider(null);
        InstanceEngine.setInstanceProvider(null);
        AI2Engine.setInstanceProvider(null);
        ChatProcessor.setInstanceProvider(null);
    }
}
