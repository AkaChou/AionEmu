package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.DisputeLandService;
import com.aionemu.gameserver.services.instance.AsyunatarService;
import com.aionemu.gameserver.services.instance.DredgionService2;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameFeatureServices implements DisposableBean {

    public GameFeatureServices(ObjectProvider<DisputeLandService> disputeLandServiceProvider,
            ObjectProvider<DredgionService2> dredgionServiceProvider,
            ObjectProvider<AsyunatarService> asyunatarServiceProvider) {
        DisputeLandService.setInstanceProvider(disputeLandServiceProvider);
        DredgionService2.setInstanceProvider(dredgionServiceProvider);
        AsyunatarService.setInstanceProvider(asyunatarServiceProvider);
    }

    @Override
    public void destroy() {
        DisputeLandService.setInstanceProvider(null);
        DredgionService2.setInstanceProvider(null);
        AsyunatarService.setInstanceProvider(null);
    }
}
