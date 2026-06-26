package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;

final class GameEngineServiceFallbacks {

    private GameEngineServiceFallbacks() {
    }

    static QuestEngine questEngine() {
        return QuestEngineFallback.INSTANCE;
    }

    static InstanceEngine instanceEngine() {
        return InstanceEngineFallback.INSTANCE;
    }

    static AI2Engine ai2Engine() {
        return Ai2EngineFallback.INSTANCE;
    }

    static ChatProcessor chatProcessor() {
        return ChatProcessorFallback.INSTANCE;
    }

    static ThreadPoolManager threadPoolManager() {
        return GameCoreServiceFallbacks.threadPoolManager();
    }

    private static final class QuestEngineFallback {
        private static final QuestEngine INSTANCE = QuestEngine.getInstance();
    }

    private static final class InstanceEngineFallback {
        private static final InstanceEngine INSTANCE = InstanceEngine.getInstance();
    }

    private static final class Ai2EngineFallback {
        private static final AI2Engine INSTANCE = AI2Engine.getInstance();
    }

    private static final class ChatProcessorFallback {
        private static final ChatProcessor INSTANCE = ChatProcessor.getInstance();
    }
}
