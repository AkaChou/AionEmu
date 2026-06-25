package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.utils.Util;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GameHtmlGateway {

    private ObjectProvider<HTMLCache> htmlCacheProvider;

    @Autowired(required = false)
    void setHtmlCacheProvider(ObjectProvider<HTMLCache> htmlCacheProvider) {
        this.htmlCacheProvider = htmlCacheProvider;
    }

    public void start() {
        Util.printSection(" *** HTML *** ");
        htmlCache();
    }

    private HTMLCache htmlCache() {
        if (htmlCacheProvider == null) {
            return HTMLCache.getInstance();
        }
        return htmlCacheProvider.getIfAvailable(HTMLCache::getInstance);
    }
}
