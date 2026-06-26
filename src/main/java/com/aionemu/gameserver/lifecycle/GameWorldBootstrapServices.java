package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameWorldBootstrapServices implements DisposableBean {

    public GameWorldBootstrapServices(ObjectProvider<IDFactory> idFactoryProvider, ObjectProvider<World> worldProvider) {
        IDFactory.setInstanceProvider(idFactoryProvider);
        World.setInstanceProvider(worldProvider);
    }

    @Override
    public void destroy() {
        IDFactory.setInstanceProvider(null);
        World.setInstanceProvider(null);
    }
}
