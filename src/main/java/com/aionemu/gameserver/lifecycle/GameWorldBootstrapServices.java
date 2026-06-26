package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.idfactory.IDFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameWorldBootstrapServices implements DisposableBean {

    public GameWorldBootstrapServices(ObjectProvider<IDFactory> idFactoryProvider) {
        IDFactory.setInstanceProvider(idFactoryProvider);
    }

    @Override
    public void destroy() {
        IDFactory.setInstanceProvider(null);
    }
}
