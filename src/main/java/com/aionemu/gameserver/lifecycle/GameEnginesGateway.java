package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.ai2.AI2Engine;
import com.aionemu.gameserver.instance.InstanceEngine;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameEnginesGateway {

    public void printSection() {
        Util.printSection(" *** Engines *** ");
    }

    public List<GameEngine> engines() {
        return List.of(
            QuestEngine.getInstance(),
            InstanceEngine.getInstance(),
            AI2Engine.getInstance(),
            ChatProcessor.getInstance()
        );
    }

    public void execute(Runnable runnable) {
        ThreadPoolManager.getInstance().execute(runnable);
    }
}
