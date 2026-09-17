package com.aionemu.gameserver.services;

import com.aionemu.gameserver.services.abyss.AbyssRankCleaningService;
import com.aionemu.gameserver.services.craft.CraftSkillUpdateService;
import com.aionemu.gameserver.services.craft.RelinquishCraftStatus;
import com.aionemu.gameserver.services.events.ArcadeUpgradeService;
import com.aionemu.gameserver.services.item.CoalescenceService;
import com.aionemu.gameserver.services.player.AtreianBestiaryService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativityEssenceService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativitySkillService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativityStatsService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativityTransfoService;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Accuracy;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Agility;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Health;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Knowledge;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Power;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Precision;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Will;
import com.aionemu.gameserver.services.player.GrowthEnergy;
import com.aionemu.gameserver.services.reward.BonusService;
import com.aionemu.gameserver.services.toypet.PetService;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.taskmanager.tasks.MoveTaskManager;
import com.aionemu.gameserver.taskmanager.tasks.MovementNotifyTask;
import com.aionemu.gameserver.taskmanager.tasks.PlayerMoveTaskManager;
import com.aionemu.gameserver.taskmanager.tasks.TeamEffectUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TeamMoveUpdater;
import com.aionemu.gameserver.taskmanager.tasks.TemporaryTradeTimeTask;
import com.aionemu.gameserver.world.zone.ZoneUpdateService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * TaskAndStatBeans：从 GameLegacyServiceBridgeConfiguration 拆出的域内 Bean 装配。
 * TaskAndStatBeans: domain-scoped bean wiring split out of GameLegacyServiceBridgeConfiguration.
 */
@Configuration(proxyBeanMethods = false)
public class TaskAndStatBeans {

    @Bean
    @Lazy
    public BonusService bonusService() {
        return new BonusService();
    }

    @Bean
    @Lazy
    public PetService petService() {
        return new PetService();
    }

    @Bean
    @Lazy
    public ArcadeUpgradeService arcadeUpgradeService() {
        return new ArcadeUpgradeService();
    }

    @Bean
    @Lazy
    public AtreianBestiaryService atreianBestiaryService() {
        return new AtreianBestiaryService();
    }

    @Bean
    @Lazy
    public CoalescenceService coalescenceService() {
        return new CoalescenceService();
    }

    @Bean
    @Lazy
    public GrowthEnergy growthEnergy() {
        return new GrowthEnergy();
    }

    @Bean
    @Lazy
    public ExpireTimerTask expireTimerTask() {
        return new ExpireTimerTask();
    }

    @Bean
    @Lazy
    public TeamEffectUpdater teamEffectUpdater() {
        return new TeamEffectUpdater();
    }

    @Bean
    @Lazy
    public TeamMoveUpdater teamMoveUpdater() {
        return new TeamMoveUpdater();
    }

    @Bean
    @Lazy
    public TemporaryTradeTimeTask temporaryTradeTimeTask() {
        return new TemporaryTradeTimeTask();
    }

    @Bean
    @Lazy
    public CreativityEssenceService creativityEssenceService() {
        return new CreativityEssenceService();
    }

    @Bean
    @Lazy
    public CreativitySkillService creativitySkillService() {
        return new CreativitySkillService();
    }

    @Bean
    @Lazy
    public CreativityStatsService creativityStatsService() {
        return new CreativityStatsService();
    }

    @Bean
    @Lazy
    public CreativityTransfoService creativityTransfoService() {
        return new CreativityTransfoService();
    }

    @Bean
    @Lazy
    public Accuracy accuracy() {
        return new Accuracy();
    }

    @Bean
    @Lazy
    public Agility agility() {
        return new Agility();
    }

    @Bean
    @Lazy
    public Health health() {
        return new Health();
    }

    @Bean
    @Lazy
    public Knowledge knowledge() {
        return new Knowledge();
    }

    @Bean
    @Lazy
    public Power power() {
        return new Power();
    }

    @Bean
    @Lazy
    public Precision precision() {
        return new Precision();
    }

    @Bean
    @Lazy
    public Will will() {
        return new Will();
    }

    @Bean
    @Lazy
    public CraftSkillUpdateService craftSkillUpdateService() {
        return new CraftSkillUpdateService();
    }

    @Bean
    @Lazy
    public RelinquishCraftStatus relinquishCraftStatus() {
        return new RelinquishCraftStatus();
    }

    @Bean
    @Lazy
    public MovementNotifyTask movementNotifyTask() {
        return new MovementNotifyTask();
    }

    @Bean
    @Lazy
    public MoveTaskManager moveTaskManager() {
        return new MoveTaskManager();
    }

    @Bean
    @Lazy
    public PlayerMoveTaskManager playerMoveTaskManager() {
        return new PlayerMoveTaskManager();
    }

    @Bean
    @Lazy
    public ZoneUpdateService zoneUpdateService() {
        return new ZoneUpdateService();
    }

    @Bean
    @Lazy
    public DatabaseCleaningService databaseCleaningService() {
        return new DatabaseCleaningService();
    }

    @Bean
    @Lazy
    public AbyssRankCleaningService abyssRankCleaningService() {
        return new AbyssRankCleaningService();
    }
}
