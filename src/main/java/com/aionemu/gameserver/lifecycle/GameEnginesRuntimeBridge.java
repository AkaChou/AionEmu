package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class GameEnginesRuntimeBridge {

    public QuestEngine questEngine() {
        return QuestEngine.getInstance();
    }

    public InstanceEngine instanceEngine() {
        return InstanceEngine.getInstance();
    }

    public AI2Engine ai2Engine() {
        return AI2Engine.getInstance();
    }

    public ChatProcessor chatProcessor() {
        return ChatProcessor.getInstance();
    }

    public ThreadPoolManager threadPoolManager() {
        return ThreadPoolManager.getInstance();
    }
}
