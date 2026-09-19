#!/usr/bin/env python3
"""Split GameLegacyServiceBridgeConfiguration (162 @Bean) into per-domain @Configuration classes.

The original class is kept as a thin @Import aggregator so existing tests/registrations keep working.
"""
import re
from pathlib import Path

SRC = Path('src/main/java/com/aionemu/gameserver/services/GameLegacyServiceBridgeConfiguration.java')
PKG = 'com.aionemu.gameserver.services'

GROUPS = {
    'CoreRuntimeServiceBeans': ['adminService','gamePlayerTransferService','periodicSaveService','territoryService','gameTimeService','announcementService','debugService','weatherService','brokerService','influence','exchangeService','petitionService','flyRingService','curingZoneService','springZoneService','boostEventService','taskManagerFromDB','limitedItemTradeService','gmService','gameRuntimeServiceBridge','rewardService','veteranRewardsService','housingService','surveyService','findGroupService','webshopService','inGameShopEn','legionService','balaurAssaultService','battlefieldUnionService','thievesGuildService','pvpService','autoGroupService','abyssRankingCache','mailService','dropService','riftManager','seasonRankingService','lifeStatsRestoreService','duelService'],
    'EngineBeans': ['threadPoolManager','questEngine','instanceEngine','ai2Engine','chatProcessor','gameIdFactory','dataManager','htmlCache','xmlDataLoader','world','zoneService','hotspotTeleportService','roadService','dropRegistrationService','geoService','pathService','staticDoorService','kiskService','repurchaseService','dropDistributionService','systemMailService'],
    'NetworkBeans': ['shutdownHook','bannedMacManager','packetLoggerService','networkController','aionPacketHandlerFactory','packetFloodFilter','lsPacketHandlerFactory','loginServer','chatServer'],
    'EventBeans': ['eventService','playerEventService','crazyDaevaService','abyssRankUpdateService','packetBroadcaster','eventScheduler','ffaService','ladderService','bgService','banditService','playerLimitService','npcShoutsService','shieldService','lunaShopService','minionService','shugoSweepService','atreianPassportService','eventWindowService','windyGorgeService','aStationService','f2pService','motionLoggingService'],
    'SiegeBattlefieldBeans': ['housingBidService','maintenanceTask','townService','challengeTaskService','kamarBattlefieldService','engulfedOphidanBridgeService','suspiciousOphidanBridgeService','ironWallWarfrontService','idgelDomeService','idgelDomeLandmarkService','hallOfTenacityService','grandArenaTrainingCampService','idRunService','disputeLandService','outpostService','dredgionService','asyunatarService','shugoImperialTombSpawnManager','seasonRankingUpdateService','protectorConquerorService','siegeService','baseService','vortexService','beritraService','agentService','anohaService','svsService','rvrService','iuService','nightmareCircusService','dynamicRiftService','instanceRiftService','zorshivDredgionService','moltenusService','riftService','conquestService','idianDepthsService','towerOfEternityService','abyssLandingService','landingUpdateService','abyssLandingSpecialService'],
    'TaskAndStatBeans': ['bonusService','petService','arcadeUpgradeService','atreianBestiaryService','coalescenceService','growthEnergy','expireTimerTask','teamEffectUpdater','teamMoveUpdater','temporaryTradeTimeTask','creativityEssenceService','creativitySkillService','creativityStatsService','creativityTransfoService','accuracy','agility','health','knowledge','power','precision','will','craftSkillUpdateService','relinquishCraftStatus','movementNotifyTask','moveTaskManager','playerMoveTaskManager','zoneUpdateService','databaseCleaningService','abyssRankCleaningService'],
}

def main():
    text = SRC.read_text(encoding='utf-8')
    lines = text.splitlines()
    import_lines = [l for l in lines if l.startswith('import ')]

    # locate each @Bean block
    blocks = {}
    i = 0
    while i < len(lines):
        if lines[i].strip() == '@Bean':
            # javadoc start
            s = i
            while s - 1 >= 0 and not lines[s - 1].strip().startswith('/**'):
                s -= 1
            if not lines[s].strip().startswith('/**'):
                s = i
            # method end via braces
            j = i
            bal = 0
            started = False
            while j < len(lines):
                bal += lines[j].count('{') - lines[j].count('}')
                if '{' in lines[j]:
                    started = True
                if started and bal == 0:
                    break
                j += 1
            sig = next(lines[k] for k in range(i, j + 1) if re.search(r'\bpublic\b.*\(', lines[k]))
            name = re.search(r'\b(\w+)\(', sig).group(1)
            blocks[name] = lines[s:j + 1]
            i = j + 1
        else:
            i += 1

    print('parsed @Bean methods:', len(blocks))
    assigned = [n for names in GROUPS.values() for n in names]
    missing = [n for n in blocks if n not in assigned]
    dupes = [n for n in set(assigned) if assigned.count(n) > 1]
    assert not missing, f'unassigned beans: {missing}'
    assert not dupes, f'duplicated assignments: {dupes}'
    assert set(assigned) == set(blocks), 'group/bean mismatch'

    annotation_imports = {
        'Configuration': 'org.springframework.context.annotation.Configuration',
        'Bean': 'org.springframework.context.annotation.Bean',
        'Lazy': 'org.springframework.context.annotation.Lazy',
        'DependsOn': 'org.springframework.context.annotation.DependsOn',
        'Primary': 'org.springframework.context.annotation.Primary',
        'Import': 'org.springframework.context.annotation.Import',
    }
    for group, names in GROUPS.items():
        body = '\n\n'.join('\n'.join(blocks[n]) for n in names)
        used = set(re.findall(r'\b([A-Z]\w+)\b', body))
        imports = []
        for imp in import_lines:
            simple = imp.split('.')[-1].rstrip(';')
            if simple in used:
                imports.append(imp)
        for simple, fqn in annotation_imports.items():
            if re.search(rf'\b{simple}\b', body) and f'import {fqn};' not in imports:
                imports.append(f'import {fqn};')
        imports = sorted(set(imports))
        header = (f'package {PKG};\n\n' + '\n'.join(imports) + '\n\n'
                  '/**\n'
                  f' * {group}：从 GameLegacyServiceBridgeConfiguration 拆出的域内 Bean 装配。\n'
                  f' * {group}: domain-scoped bean wiring split out of GameLegacyServiceBridgeConfiguration.\n'
                  ' */\n'
                  '@Configuration(proxyBeanMethods = false)\n'
                  f'public class {group} {{\n\n')
        (SRC.parent / f'{group}.java').write_text(header + body + '\n}\n', encoding='utf-8')
        print(f'wrote {group}.java with {len(names)} beans')

    # aggregator
    imports = '\n'.join(f'import {PKG}.{g};' for g in GROUPS)
    imports = imports.replace(f'import {PKG}.', 'import ')
    agg = (f'package {PKG};\n\nimport org.springframework.context.annotation.Configuration;\n'
           'import org.springframework.context.annotation.Import;\n\n'
           + '\n'.join(f'import {g};' for g in GROUPS).replace('import ', 'import ' + PKG + '.') + '\n\n'
           '/**\n'
           ' * 遗留服务 Bean 装配聚合入口：按域拆分到多个配置类。\n'
           ' * Aggregator for legacy service beans; delegates to the per-domain configuration classes.\n'
           ' */\n'
           '@Configuration(proxyBeanMethods = false)\n'
           '@Import({\n' + ',\n'.join(f'    {g}.class' for g in GROUPS) + '\n})\n'
           'public class GameLegacyServiceBridgeConfiguration {\n}\n')
    SRC.write_text(agg, encoding='utf-8')
    print('rewrote aggregator:', SRC)
    print('groups:', ', '.join(f'{g}={len(n)}' for g, n in GROUPS.items()))

if __name__ == '__main__':
    main()
