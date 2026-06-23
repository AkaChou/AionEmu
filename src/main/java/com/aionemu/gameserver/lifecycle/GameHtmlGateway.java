package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.utils.Util;
import org.springframework.stereotype.Component;

@Component
public class GameHtmlGateway {

    public void start() {
        Util.printSection(" *** HTML *** ");
        HTMLCache.getInstance();
    }
}
