package com.aionemu.gameserver.spawnengine;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameWorldServices;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.RateConfig;
import com.aionemu.gameserver.configs.main.SiegeConfig;
import com.aionemu.gameserver.controllers.GatherableController;
import com.aionemu.gameserver.controllers.MinionController;
import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.controllers.PetController;
import com.aionemu.gameserver.controllers.SiegeWeaponController;
import com.aionemu.gameserver.controllers.SummonController;
import com.aionemu.gameserver.controllers.effect.EffectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.geoEngine.collision.CollisionIntention;
import com.aionemu.gameserver.geoEngine.math.Vector3f;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.agent.AgentLocation;
import com.aionemu.gameserver.model.anoha.AnohaLocation;
import com.aionemu.gameserver.model.base.BaseLocation;
import com.aionemu.gameserver.model.beritra.BeritraLocation;
import com.aionemu.gameserver.model.conquest.ConquestLocation;
import com.aionemu.gameserver.model.dynamicrift.DynamicRiftLocation;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.GroupGate;
import com.aionemu.gameserver.model.gameobjects.Homing;
import com.aionemu.gameserver.model.gameobjects.Kisk;
import com.aionemu.gameserver.model.gameobjects.Minion;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.NpcObjectType;
import com.aionemu.gameserver.model.gameobjects.Pet;
import com.aionemu.gameserver.model.gameobjects.Servant;
import com.aionemu.gameserver.model.gameobjects.Summon;
import com.aionemu.gameserver.model.gameobjects.SummonedHouseNpc;
import com.aionemu.gameserver.model.gameobjects.Trap;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.base.BaseNpc;
import com.aionemu.gameserver.model.gameobjects.outpost.OutpostNpc;
import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.siege.SiegeNpc;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.idiandepths.IdianDepthsLocation;
import com.aionemu.gameserver.model.instancerift.InstanceRiftLocation;
import com.aionemu.gameserver.model.iu.IuLocation;
import com.aionemu.gameserver.model.landing.LandingLocation;
import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.moltenus.MoltenusLocation;
import com.aionemu.gameserver.model.nightmarecircus.NightmareCircusLocation;
import com.aionemu.gameserver.model.outpost.OutpostLocation;
import com.aionemu.gameserver.model.rift.RiftLocation;
import com.aionemu.gameserver.model.rvr.RvrLocation;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeRace;
import com.aionemu.gameserver.model.skill.NpcSkillEntry;
import com.aionemu.gameserver.model.svs.SvsLocation;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.minion.MinionTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcRating;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.pet.PetTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.agentspawns.AgentSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.anohaspawns.AnohaSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.basespawns.BaseSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.beritraspawns.BeritraSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.conquestspawns.ConquestSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.dynamicriftspawns.DynamicRiftSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.idiandepthsspawns.IdianDepthsSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.instanceriftspawns.InstanceRiftSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.iuspawns.IuSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.landingspawns.LandingSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.landingspecialspawns.LandingSpecialSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.moltenusspawns.MoltenusSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.nightmarecircusspawns.NightmareCircusSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.outpostspawns.OutpostSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.riftspawns.RiftSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.rvrspawns.RvrSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.siegespawns.SiegeSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.svsspawns.SvsSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.towerofeternityspawns.TowerOfEternitySpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.vortexspawns.VortexSpawnTemplate;
import com.aionemu.gameserver.model.templates.spawns.zorshivdredgionspawns.ZorshivDredgionSpawnTemplate;
import com.aionemu.gameserver.model.templates.stats.NpcStatsTemplate;
import com.aionemu.gameserver.model.towerofeternity.TowerOfEternityLocation;
import com.aionemu.gameserver.model.vortex.VortexLocation;
import com.aionemu.gameserver.model.zorshivdredgion.ZorshivDredgionLocation;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE;
import com.aionemu.gameserver.services.AbyssLandingSpecialService;
import com.aionemu.gameserver.services.AgentService;
import com.aionemu.gameserver.services.AnohaService;
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
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.services.SvsService;
import com.aionemu.gameserver.services.VortexService;
import com.aionemu.gameserver.services.ZorshivDredgionService;
import com.aionemu.gameserver.skillengine.effect.SummonOwner;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.CreatureAwareKnownList;
import com.aionemu.gameserver.world.knownlist.NpcKnownList;
import com.aionemu.gameserver.world.knownlist.PlayerAwareKnownList;
/**
 * 可见对象刷怪器：根据刷怪模板创建 NPC、采集物、攻城单位等并刷入世界。
 * Visible object spawner: creates NPCs, gatherables, siege units and more from spawn templates.
 * <p>
 * 文件体量较大，方法级注释在后续迭代补充；本类仅维护类级双语说明。
 * Large file; method-level docs deferred. Class-level bilingual docs only for now.
 */
@Slf4j
public class VisibleObjectSpawner {

    
    private static final java.util.concurrent.ConcurrentHashMap<Integer, NpcStatsTemplate> ORIGINAL_STATS = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * @param objId 按配置倍率缩放 NPC 属性（生命、物攻 / 魔攻、命中、物防/魔防、回避等）。
     * @return Scale NPC attributes by config rates (HP, physical / magical attack, accuracy, defenses, evasion).
     */
    protected static NpcTemplate RatedTemplate(int objId) {
        NpcTemplate npcTemplate = DataManager.NPC_DATA.getNpcTemplate(objId);
        if (npcTemplate == null) {
            log.error(I18n.get("log.e37e95107d9d", objId));
            return null;
        }
        
        // 首次访问时保存原始属性 / Store original stats on first access
        if (!ORIGINAL_STATS.containsKey(objId)) {
            NpcStatsTemplate originalStats = cloneStats(npcTemplate.getStatsTemplate());
            ORIGINAL_STATS.put(objId, originalStats);
        }
        
        NpcStatsTemplate originalStats = ORIGINAL_STATS.get(objId);
        NpcStatsTemplate currentStats = npcTemplate.getStatsTemplate();
        
        double RateHP = 1.0;
        double RatePW = 1.0;
        
        NpcRating rating = npcTemplate.getRating();
        
        switch (rating) {
            case NORMAL:
                RateHP = RateConfig.NORMAL_MOBS_RATE_HP;
                RatePW = RateConfig.NORMAL_MOBS_RATE_PW;
                break;
            case ELITE:
                RateHP = RateConfig.ELITE_MOBS_RATE_HP;
                RatePW = RateConfig.ELITE_MOBS_RATE_PW;
                break;
            case HERO:
                RateHP = RateConfig.HERO_MOBS_RATE_HP;
                RatePW = RateConfig.HERO_MOBS_RATE_PW;
                break;
            case LEGENDARY:
                RateHP = RateConfig.LEGENDARY_MOBS_RATE_HP;
                RatePW = RateConfig.LEGENDARY_MOBS_RATE_PW;
                break;
            case JUNK:
            default:
                break;
        }
        
        if (npcTemplate.getLevel() >= 1 && rating != NpcRating.JUNK) {
            
            // ===== 生命值 ===== / ===== HP =====
            currentStats.setMaxHp((int) (originalStats.getMaxHp() * RateHP));
            
            // ===== 力量（魔法攻击） ===== / ===== POWER (Magical Attack) =====
            currentStats.setPower((int) (originalStats.getPower() * RatePW));
            
            // ===== 物理攻击 ===== / ===== PHYSICAL ATTACK =====
            currentStats.setMainHandAttack((int) (originalStats.getMainHandAttack() * RatePW));
            
            // ===== 命中 ===== / ===== ACCURACY =====
            currentStats.setMainHandAccuracy((int) (originalStats.getMainHandAccuracy() * RatePW));
            
            // ===== 物理防御 ===== / ===== PHYSICAL DEFENSE =====
            currentStats.setPdef((int) (originalStats.getPdef() * RatePW));
            
            // ===== 魔法防御 ===== / ===== MAGICAL DEFENSE =====
            currentStats.setMdef((int) (originalStats.getMdef() * RatePW));
            
            // ===== 闪避 ===== / ===== EVASION =====
            currentStats.setEvasion((int) (originalStats.getEvasion() * RatePW));
            
            if (log.isDebugEnabled()) {
                log.debug("Scaled NPC [{}] {} - Rating: {}", objId, npcTemplate.getName(), rating);
                log.debug("  HP: {} -> {} (x{})", originalStats.getMaxHp(), currentStats.getMaxHp(), RateHP);
                log.debug("  Power (Mag Attack): {} -> {} (x{})", originalStats.getPower(), currentStats.getPower(), RatePW);
                log.debug("  Phys Attack: {} -> {} (x{})", originalStats.getMainHandAttack(), currentStats.getMainHandAttack(), RatePW);
                log.debug("  Accuracy: {} -> {} (x{})", originalStats.getMainHandAccuracy(), currentStats.getMainHandAccuracy(), RatePW);
                log.debug("  Phys Defense: {} -> {} (x{})", originalStats.getPdef(), currentStats.getPdef(), RatePW);
                log.debug("  Mag Defense: {} -> {} (x{})", originalStats.getMdef(), currentStats.getMdef(), RatePW);
                log.debug("  Evasion: {} -> {} (x{})", originalStats.getEvasion(), currentStats.getEvasion(), RatePW);
            }
        }
        
        return npcTemplate;
    }
    
    /**
     * 克隆 NpcStatsTemplate（仅复制所需属性）。
     * Clone NpcStatsTemplate for required stats only.
     */
    private static NpcStatsTemplate cloneStats(NpcStatsTemplate original) {
        NpcStatsTemplate clone = new NpcStatsTemplate();
        
        // HP
        clone.setMaxHp(original.getMaxHp());
        
        // 力量（魔法攻击） / Power (Magical Attack)
        clone.setPower(original.getPower());
        
        // 物理攻击 / Physical Attack
        clone.setMainHandAttack(original.getMainHandAttack());
        
        // 命中 / Accuracy
        clone.setMainHandAccuracy(original.getMainHandAccuracy());
        
        // 物理防御 / Physical Defense
        clone.setPdef(original.getPdef());
        
        // 魔法防御 / Magical Defense
        clone.setMdef(original.getMdef());
        
        // 闪避 / Evasion
        clone.setEvasion(original.getEvasion());
        
        return clone;
    }

    protected static VisibleObject spawnNpc(SpawnTemplate spawn, int instanceIndex) {
        int objectId = spawn.getNpcId();
        if (spawn.getAlternateIds()!=null){
            int[] selectprobs = spawn.getSelectProbs();
            int[] alternateIds = spawn.getAlternateIds();
            double rand = Math.random()*10000;
            int temp =10000;
            for (int i =0; i< alternateIds.length; i++){
                if ((alternateIds[i]!=0)&&(rand<selectprobs[i])&&(selectprobs[i]<temp)){
                    temp = selectprobs[i];
                    objectId = alternateIds[i];
                }
            }
        }
        
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            log.error(I18n.get("log.b1ee12e6edee", String.valueOf(objectId)));
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();

        Npc npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
        npc.setCreatorId(spawn.getCreatorId());
        npc.setMasterName(spawn.getMasterName());
        npc.setKnownlist(new NpcKnownList(npc));
        npc.setEffectController(new EffectController(npc));
        if (WalkerFormator.processClusteredNpc(npc, spawn.getWorldId(), instanceIndex)) {
            return npc;
        }
        try {
            SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        } catch (Exception ex) {
            log.error(I18n.get("log.25b31202cf98", new Object[] { npcTemplate.getTemplateId(), spawn.getWorldId(), spawn.getX(), spawn.getY() }));
            log.error(I18n.get("log.0b740221960f", npcTemplate.getTemplateId(), ex));
            com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(npc);
        }
        return npc;
    }

    public static SummonedHouseNpc spawnHouseNpc(SpawnTemplate spawn, int instanceIndex, House creator, String masterName) {
        int npcId = spawn.getNpcId();
        NpcTemplate template = RatedTemplate(npcId);
        SummonedHouseNpc npc = new SummonedHouseNpc(GameWorldBootstrapServices.idFactory().nextId(), new NpcController(), spawn, template, creator, masterName);
        npc.setKnownlist(new PlayerAwareKnownList(npc));
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnBaseNpc(BaseSpawnTemplate spawn, int instanceIndex) {
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        int spawnId = spawn.getId();
        boolean isActive = GameFeatureServices.baseService().isActive(spawnId);
        BaseLocation base = GameFeatureServices.baseService().getBaseLocation(spawnId);
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        if (!isActive && spawn.getBaseRace() != base.getRace()) {
            return null;
        }
        if (isActive && spawn.getBaseRace() == base.getRace()) {
            npc = new BaseNpc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnOutpostNpc(OutpostSpawnTemplate spawn, int instanceIndex) {
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        int spawnId = spawn.getId();
        boolean isActive = GameLocationBootstrapServices.outpostService().isActive(spawnId);
        OutpostLocation outpost = GameLocationBootstrapServices.outpostService().getOutpostLocation(spawnId);
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        if (!isActive && spawn.getOutpostRace() != outpost.getRace()) {
            return null;
        }
        if (isActive && spawn.getOutpostRace() == outpost.getRace()) {
            npc = new OutpostNpc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnRiftNpc(RiftSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.RIFT_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        RiftLocation loc = GameLocationBootstrapServices.riftService().getRiftLocation(spawnId);
        if (loc.isOpened() && spawnId == loc.getId()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnSiegeNpc(SiegeSpawnTemplate spawn, int instanceIndex) {
        if (!SiegeConfig.SIEGE_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc = null;
        int spawnSiegeId = spawn.getSiegeId();
        SiegeLocation loc = GameFeatureServices.siegeService().getSiegeLocation(spawnSiegeId);
        if ((spawn.isPeace() || loc.isVulnerable()) && spawnSiegeId == loc.getLocationId() && spawn.getSiegeRace() == loc.getRace()) {
            npc = new SiegeNpc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (spawn.isAssault() && loc.isVulnerable() && spawn.getSiegeRace().equals(SiegeRace.BALAUR)) {
            npc = new SiegeNpc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnInvasionNpc(VortexSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.VORTEX_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        VortexLocation loc = GameLocationBootstrapServices.vortexService().getVortexLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isInvasion()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isPeace()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnBeritraNpc(BeritraSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.BERITRA_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        BeritraLocation loc = GameLocationBootstrapServices.beritraService().getBeritraLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isBeritraInvasion()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isBeritraPeace()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnAgentNpc(AgentSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.AGENT_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        AgentLocation loc = GameLocationBootstrapServices.agentService().getAgentLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isAgentFight()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isAgentPeace()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnAnohaNpc(AnohaSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.ANOHA_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        AnohaLocation loc = GameLocationBootstrapServices.anohaService().getAnohaLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isAnohaFight()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isAnohaPeace()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnConquestNpc(ConquestSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.CONQUEST_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        ConquestLocation loc = GameLocationBootstrapServices.conquestService().getConquestLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isConquest()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isConquestPeace()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnSvsNpc(SvsSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.SVS_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        SvsLocation loc = GameLocationBootstrapServices.svsService().getSvsLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isSvs()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isSvsPeace()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnRvrNpc(RvrSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.RVR_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        RvrLocation loc = GameLocationBootstrapServices.rvrService().getRvrLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isRvr()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isRvrPeace()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnIuNpc(IuSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.IU_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        IuLocation loc = GameLocationBootstrapServices.iuService().getIuLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isOpen()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isClosed()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnMoltenusNpc(MoltenusSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.MOLTENUS_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        MoltenusLocation loc = GameLocationBootstrapServices.moltenusService().getMoltenusLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isFight()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isPeace()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnDynamicRiftNpc(DynamicRiftSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.DYNAMIC_RIFT_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        DynamicRiftLocation loc = GameLocationBootstrapServices.dynamicRiftService().getDynamicRiftLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isDynamicRiftOpen()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isDynamicRiftClosed()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnInstanceRiftNpc(InstanceRiftSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.INSTANCE_RIFT_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        InstanceRiftLocation loc = GameLocationBootstrapServices.instanceRiftService().getInstanceRiftLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isInstanceRiftOpen()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isInstanceRiftClosed()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnNightmareCircusNpc(NightmareCircusSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.NIGHTMARE_CIRCUS_ENABLE) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        NightmareCircusLocation loc = GameLocationBootstrapServices.nightmareCircusService().getNightmareCircusLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isCircusOpen()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isCircusClosed()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnIdianDepthsNpc(IdianDepthsSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.IDIAN_DEPTHS_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        IdianDepthsLocation loc = GameLocationBootstrapServices.idianDepthsService().getIdianDepthsLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isIdianDepthsOpen()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isIdianDepthsClosed()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnZorshivDredgionNpc(ZorshivDredgionSpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.ZORSHIV_DREDGION_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        ZorshivDredgionLocation loc = GameLocationBootstrapServices.zorshivDredgionService().getZorshivDredgionLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isLanding()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isPeace()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnGatherable(SpawnTemplate spawn, int instanceIndex) {
        int objectId = spawn.getNpcId();
        VisibleObjectTemplate template = DataManager.GATHERABLE_DATA.getGatherableTemplate(objectId);
        Gatherable gatherable = new Gatherable(spawn, template, GameWorldBootstrapServices.idFactory().nextId(), new GatherableController());
        gatherable.setKnownlist(new PlayerAwareKnownList(gatherable));
        SpawnEngine.bringIntoWorld(gatherable, spawn, instanceIndex);
        return gatherable;
    }

    public static Trap spawnTrap(SpawnTemplate spawn, int instanceIndex, Creature creator) {
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        Trap trap = new Trap(GameWorldBootstrapServices.idFactory().nextId(), new NpcController(), spawn, npcTemplate);
        trap.setKnownlist(new NpcKnownList(trap));
        trap.setEffectController(new EffectController(trap));
        trap.setCreator(creator);
        trap.setVisualState(CreatureVisualState.HIDE1);
        SpawnEngine.bringIntoWorld(trap, spawn, instanceIndex);
        PacketSendUtility.broadcastPacket(trap, new SM_PLAYER_STATE(trap));
        return trap;
    }

    public static GroupGate spawnGroupGate(SpawnTemplate spawn, int instanceIndex, Creature creator) {
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        GroupGate groupgate = new GroupGate(GameWorldBootstrapServices.idFactory().nextId(), new NpcController(), spawn, npcTemplate);
        groupgate.setKnownlist(new PlayerAwareKnownList(groupgate));
        groupgate.setEffectController(new EffectController(groupgate));
        groupgate.setCreator(creator);
        SpawnEngine.bringIntoWorld(groupgate, spawn, instanceIndex);
        return groupgate;
    }

    public static Kisk spawnKisk(SpawnTemplate spawn, int instanceIndex, Player creator) {
        int npcId = spawn.getNpcId();
        NpcTemplate template = RatedTemplate(npcId);
        Kisk kisk = new Kisk(GameWorldBootstrapServices.idFactory().nextId(), new NpcController(), spawn, template, creator);
        kisk.setKnownlist(new PlayerAwareKnownList(kisk));
        kisk.setCreator(creator);
        kisk.setEffectController(new EffectController(kisk));
        SpawnEngine.bringIntoWorld(kisk, spawn, instanceIndex);
        return kisk;
    }

    public static Npc spawnPostman(final Player owner) {
        int npcId = owner.getRace() == Race.ELYOS ? 798100 : 798101;
        NpcData npcData = DataManager.NPC_DATA;
        NpcTemplate template = npcData.getNpcTemplate(npcId);
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        int worldId = owner.getWorldId();
        int instanceId = owner.getInstanceId();
        double radian = Math.toRadians(MathUtil.convertHeadingToDegree(owner.getHeading()));
        Vector3f pos = GameWorldServices.geoService().getClosestCollision(owner, owner.getX() + (float) (Math.cos(radian) * 5), owner.getY() + (float) (Math.sin(radian) * 5), owner.getZ(), false, CollisionIntention.PHYSICAL.getId());
        SpawnTemplate spawn = SpawnEngine.addNewSingleTimeSpawn(worldId, npcId, pos.getX(), pos.getY(), pos.getZ(), (byte) 0);
        final Npc postman = new Npc(iDFactory.nextId(), new NpcController(), spawn, template);
        postman.setKnownlist(new PlayerAwareKnownList(postman));
        postman.setEffectController(new EffectController(postman));
        postman.getAi2().onCustomEvent(1, owner);
        SpawnEngine.bringIntoWorld(postman, spawn, instanceId);
        owner.setPostman(postman);
        return postman;
    }

    public static Npc spawnFunctionalNpc(final Player owner, int npcId, SummonOwner summonOwner) {
        NpcData npcData = DataManager.NPC_DATA;
        NpcTemplate template = npcData.getNpcTemplate(npcId);
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        int worldId = owner.getWorldId();
        int instanceId = owner.getInstanceId();
        double radian = Math.toRadians(MathUtil.convertHeadingToDegree(owner.getHeading()));
        Vector3f pos = GameWorldServices.geoService().getClosestCollision(owner, owner.getX() + (float) (Math.cos(radian) * 5), owner.getY() + (float) (Math.sin(radian) * 5), owner.getZ(), false, CollisionIntention.PHYSICAL.getId());
        SpawnTemplate spawn = SpawnEngine.addNewSingleTimeSpawn(worldId, npcId, pos.getX(), pos.getY(), pos.getZ(), (byte) 0);
        final Npc functionalNpc = new Npc(iDFactory.nextId(), new NpcController(), spawn, template);
        functionalNpc.setKnownlist(new PlayerAwareKnownList(functionalNpc));
        functionalNpc.setEffectController(new EffectController(functionalNpc));
        functionalNpc.getAi2().onCustomEvent(1, owner);
        SpawnEngine.bringIntoWorld(functionalNpc, spawn, instanceId);
        return functionalNpc;
    }

    public static Servant spawnServant(SpawnTemplate spawn, int instanceIndex, Creature creator, int skillId, int level, NpcObjectType objectType) {
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        int creatureLevel = creator.getLevel();
        level = SkillLearnService.getSkillLearnLevel(skillId, creatureLevel, level);
        byte servantLevel = (byte) SkillLearnService.getSkillMinLevel(skillId, creatureLevel, level);
        Servant servant = new Servant(GameWorldBootstrapServices.idFactory().nextId(), new NpcController(), spawn, npcTemplate, servantLevel);
        servant.setKnownlist(new NpcKnownList(servant));
        servant.setEffectController(new EffectController(servant));
        servant.setCreator(creator);
        servant.setNpcObjectType(objectType);
        servant.getSkillList().addSkill(servant, skillId, 1);
        SpawnEngine.bringIntoWorld(servant, spawn, instanceIndex);
        SkillTemplate st = DataManager.SKILL_DATA.getSkillTemplate(skillId);
        if (st.getStartconditions() != null && st.getHpCondition() != null) {
            int hp = (st.getHpCondition().getHpValue() * 3);
            servant.getLifeStats().setCurrentHp(hp);
        }
        return servant;
    }

    public static Servant spawnEnemyServant(SpawnTemplate spawn, int instanceIndex, Creature creator, byte servantLvl) {
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        Servant servant = new Servant(GameWorldBootstrapServices.idFactory().nextId(), new NpcController(), spawn, npcTemplate, servantLvl);
        servant.setKnownlist(new NpcKnownList(servant));
        servant.setEffectController(new EffectController(servant));
        servant.setCreator(creator);
        servant.setNpcObjectType(NpcObjectType.SERVANT);
        SpawnEngine.bringIntoWorld(servant, spawn, instanceIndex);
        return servant;
    }

    public static Homing spawnHoming(SpawnTemplate spawn, int instanceIndex, Creature creator, int attackCount, int skillId, int level) {
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        int creatureLevel = creator.getLevel();
        level = SkillLearnService.getSkillLearnLevel(skillId, creatureLevel, level);
        byte homingLevel = (byte) SkillLearnService.getSkillMinLevel(skillId, creatureLevel, level);
        Homing homing = new Homing(GameWorldBootstrapServices.idFactory().nextId(), new NpcController(), spawn, npcTemplate, homingLevel, skillId);
        homing.setState(CreatureState.WEAPON_EQUIPPED);
        homing.setKnownlist(new NpcKnownList(homing));
        homing.setEffectController(new EffectController(homing));
        homing.setCreator(creator);
        int homingSkillId = 0;
        if (homing.getSkillList() != null) {
            NpcSkillEntry hmSkill = homing.getSkillList().getRandomSkill();
            if (hmSkill != null) {
                homingSkillId = hmSkill.getSkillId();
            }
        }
        if (homingSkillId != 0) {
            homing.getSkillList().addSkill(homing, homingSkillId, 1);
        }
        homing.setActiveSkillId(homingSkillId);
        homing.setAttackCount(attackCount);
        SpawnEngine.bringIntoWorld(homing, spawn, instanceIndex);
        return homing;
    }

    public static Summon spawnSummon(Player creator, int npcId, int skillId, int skillLevel, int time) {
        float x = creator.getX();
        float y = creator.getY();
        float z = creator.getZ();
        byte heading = creator.getHeading();
        int worldId = creator.getWorldId();
        int instanceId = creator.getInstanceId();
        SpawnTemplate spawn = SpawnEngine.createSpawnTemplate(worldId, npcId, x, y, z, heading);
        NpcTemplate npcTemplate = RatedTemplate(npcId);
        skillLevel = SkillLearnService.getSkillLearnLevel(skillId, creator.getCommonData().getLevel(), skillLevel);
        byte level = (byte) SkillLearnService.getSkillMinLevel(skillId, creator.getCommonData().getLevel(), skillLevel);
        boolean isSiegeWeapon = npcTemplate.getAi().equals("siege_weapon");
        Summon summon = new Summon(GameWorldBootstrapServices.idFactory().nextId(), isSiegeWeapon ? new SiegeWeaponController(npcId) : new SummonController(), spawn, npcTemplate, isSiegeWeapon ? npcTemplate.getLevel() : level, time);
        summon.setKnownlist(new CreatureAwareKnownList(summon));
        summon.setEffectController(new EffectController(summon));
        summon.setMaster(creator);
        summon.getLifeStats().synchronizeWithMaxStats();
        SpawnEngine.bringIntoWorld(summon, spawn, instanceId);
        return summon;
    }

    public static Pet spawnPet(Player player, int petId) {
        PetCommonData petCommonData = player.getPetList().getPet(petId);
        if (petCommonData == null) {
            return null;
        }
        PetTemplate petTemplate = DataManager.PET_DATA.getPetTemplate(petId);
        if (petTemplate == null) {
            return null;
        }
        PetController controller = new PetController();
        Pet pet = new Pet(petTemplate, controller, petCommonData, player);
        pet.setKnownlist(new PlayerAwareKnownList(pet));
        player.setToyPet(pet);
        float x = player.getX();
        float y = player.getY();
        float z = player.getZ();
        byte heading = player.getHeading();
        int worldId = player.getWorldId();
        int instanceId = player.getInstanceId();
        SpawnTemplate spawn = SpawnEngine.createSpawnTemplate(worldId, petId, x, y, z, heading);
        SpawnEngine.bringIntoWorld(pet, spawn, instanceId);
        return pet;
    }

    public static Minion spawnMinion(Player player, int minionId) {
        MinionCommonData minionCommonData = player.getMinionList().getMinion(minionId);
        if (minionCommonData == null) {
            return null;
        }
        MinionTemplate minionTemplate = DataManager.MINION_DATA.getMinionTemplate(minionId);
        if (minionTemplate == null) {
            return null;
        }

        MinionController controller = new MinionController();
        Minion minion = new Minion(minionTemplate, controller, minionCommonData, player);
        minion.setKnownlist(new PlayerAwareKnownList(minion));
        player.setMinion(minion);

        float x = player.getX() - 2;
        float y = player.getY();
        float z = player.getZ();
        byte heading = player.getHeading();
        int worldId = player.getWorldId();
        int instanceId = player.getInstanceId();
        SpawnTemplate spawn = SpawnEngine.createSpawnTemplate(worldId, minionId, x, y, z, heading);

        SpawnEngine.bringIntoWorld(minion, spawn, instanceId);
        return minion;
    }

    protected static VisibleObject spawnLandingNpc(LandingSpawnTemplate spawn, int instanceIndex) {
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        LandingLocation loc = GameLocationBootstrapServices.abyssLandingService().getLandingLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isLandingOpen()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isLandingClosed()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnLandingSpecialNpc(LandingSpecialSpawnTemplate spawn, int instanceIndex) {
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        LandingSpecialLocation loc = GameLocationBootstrapServices.abyssLandingSpecialService().getLandingSpecialLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isSpecialLandingActive()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isSpecialLandingActive()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }

    protected static VisibleObject spawnTowerOfEternityNpc(TowerOfEternitySpawnTemplate spawn, int instanceIndex) {
        if (!CustomConfig.TOWER_OF_ETERNITY_ENABLED) {
            return null;
        }
        int objectId = spawn.getNpcId();
        NpcTemplate npcTemplate = RatedTemplate(objectId);
        if (npcTemplate == null) {
            return null;
        }
        IDFactory iDFactory = GameWorldBootstrapServices.idFactory();
        Npc npc;
        int spawnId = spawn.getId();
        TowerOfEternityLocation loc = GameLocationBootstrapServices.towerOfEternityService().getTowerOfEternityLocation(spawnId);
        if (loc.isActive() && spawnId == loc.getId() && spawn.isTowerOfEternityOpen()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else if (!loc.isActive() && spawnId == loc.getId() && spawn.isTowerOfEternityClosed()) {
            npc = new Npc(iDFactory.nextId(), new NpcController(), spawn, npcTemplate);
            npc.setKnownlist(new NpcKnownList(npc));
        } else {
            return null;
        }
        npc.setEffectController(new EffectController(npc));
        SpawnEngine.bringIntoWorld(npc, spawn, instanceIndex);
        return npc;
    }
}
