package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.AgentService;
import com.aionemu.gameserver.services.AnohaService;
import com.aionemu.gameserver.services.BeritraService;
import com.aionemu.gameserver.services.DynamicRiftService;
import com.aionemu.gameserver.services.InstanceRiftService;
import com.aionemu.gameserver.services.IuService;
import com.aionemu.gameserver.services.NightmareCircusService;
import com.aionemu.gameserver.services.RvrService;
import com.aionemu.gameserver.services.SvsService;
import com.aionemu.gameserver.services.VortexService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public final class GameLocationBootstrapServices implements DisposableBean {

    public GameLocationBootstrapServices(ObjectProvider<VortexService> vortexServiceProvider,
            ObjectProvider<BeritraService> beritraServiceProvider, ObjectProvider<AgentService> agentServiceProvider,
            ObjectProvider<AnohaService> anohaServiceProvider, ObjectProvider<SvsService> svsServiceProvider,
            ObjectProvider<RvrService> rvrServiceProvider, ObjectProvider<IuService> iuServiceProvider,
            ObjectProvider<NightmareCircusService> nightmareCircusServiceProvider,
            ObjectProvider<DynamicRiftService> dynamicRiftServiceProvider,
            ObjectProvider<InstanceRiftService> instanceRiftServiceProvider) {
        VortexService.setInstanceProvider(vortexServiceProvider);
        BeritraService.setInstanceProvider(beritraServiceProvider);
        AgentService.setInstanceProvider(agentServiceProvider);
        AnohaService.setInstanceProvider(anohaServiceProvider);
        SvsService.setInstanceProvider(svsServiceProvider);
        RvrService.setInstanceProvider(rvrServiceProvider);
        IuService.setInstanceProvider(iuServiceProvider);
        NightmareCircusService.setInstanceProvider(nightmareCircusServiceProvider);
        DynamicRiftService.setInstanceProvider(dynamicRiftServiceProvider);
        InstanceRiftService.setInstanceProvider(instanceRiftServiceProvider);
    }

    @Override
    public void destroy() {
        VortexService.setInstanceProvider(null);
        BeritraService.setInstanceProvider(null);
        AgentService.setInstanceProvider(null);
        AnohaService.setInstanceProvider(null);
        SvsService.setInstanceProvider(null);
        RvrService.setInstanceProvider(null);
        IuService.setInstanceProvider(null);
        NightmareCircusService.setInstanceProvider(null);
        DynamicRiftService.setInstanceProvider(null);
        InstanceRiftService.setInstanceProvider(null);
    }
}
