package com.aionemu.gameserver.lifecycle;

import com.aionemu.gameserver.services.AgentService;
import com.aionemu.gameserver.services.AnohaService;
import com.aionemu.gameserver.services.AbyssLandingService;
import com.aionemu.gameserver.services.BaseService;
import com.aionemu.gameserver.services.BeritraService;
import com.aionemu.gameserver.services.ConquestService;
import com.aionemu.gameserver.services.DynamicRiftService;
import com.aionemu.gameserver.services.IdianDepthsService;
import com.aionemu.gameserver.services.InstanceRiftService;
import com.aionemu.gameserver.services.IuService;
import com.aionemu.gameserver.services.MoltenusService;
import com.aionemu.gameserver.services.NightmareCircusService;
import com.aionemu.gameserver.services.OutpostService;
import com.aionemu.gameserver.services.RiftService;
import com.aionemu.gameserver.services.RvrService;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.services.SvsService;
import com.aionemu.gameserver.services.TowerOfEternityService;
import com.aionemu.gameserver.services.VortexService;
import com.aionemu.gameserver.services.ZorshivDredgionService;
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
            ObjectProvider<InstanceRiftService> instanceRiftServiceProvider,
            ObjectProvider<SiegeService> siegeServiceProvider, ObjectProvider<BaseService> baseServiceProvider,
            ObjectProvider<OutpostService> outpostServiceProvider,
            ObjectProvider<ZorshivDredgionService> zorshivDredgionServiceProvider,
            ObjectProvider<MoltenusService> moltenusServiceProvider, ObjectProvider<RiftService> riftServiceProvider,
            ObjectProvider<ConquestService> conquestServiceProvider,
            ObjectProvider<IdianDepthsService> idianDepthsServiceProvider,
            ObjectProvider<TowerOfEternityService> towerOfEternityServiceProvider,
            ObjectProvider<AbyssLandingService> abyssLandingServiceProvider) {
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
        SiegeService.setInstanceProvider(siegeServiceProvider);
        BaseService.setInstanceProvider(baseServiceProvider);
        OutpostService.setInstanceProvider(outpostServiceProvider);
        ZorshivDredgionService.setInstanceProvider(zorshivDredgionServiceProvider);
        MoltenusService.setInstanceProvider(moltenusServiceProvider);
        RiftService.setInstanceProvider(riftServiceProvider);
        ConquestService.setInstanceProvider(conquestServiceProvider);
        IdianDepthsService.setInstanceProvider(idianDepthsServiceProvider);
        TowerOfEternityService.setInstanceProvider(towerOfEternityServiceProvider);
        AbyssLandingService.setInstanceProvider(abyssLandingServiceProvider);
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
        SiegeService.setInstanceProvider(null);
        BaseService.setInstanceProvider(null);
        OutpostService.setInstanceProvider(null);
        ZorshivDredgionService.setInstanceProvider(null);
        MoltenusService.setInstanceProvider(null);
        RiftService.setInstanceProvider(null);
        ConquestService.setInstanceProvider(null);
        IdianDepthsService.setInstanceProvider(null);
        TowerOfEternityService.setInstanceProvider(null);
        AbyssLandingService.setInstanceProvider(null);
    }
}
