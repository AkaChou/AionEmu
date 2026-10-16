package com.aionemu.gameserver.services.player;

import java.util.List;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.templates.atreian_bestiary.AtreianBestiaryTemplate;
import com.aionemu.gameserver.model.templates.atreian_bestiary.AtreianBestiaryTemplate.AtreianBestiaryAchievementTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_BESTIARY;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATREIAN_BESTIARY_LIST;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.persistence.dao.PlayerAtreianBestiaryDao;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 艾特雷亚图鉴服务，记录击杀、阶段（grade）推进与奖励领取。
 * Atreian bestiary service tracking kills, grade progress and reward claims.
 * <p>语义对齐真端 {@code UserMonsterAchievements}（MonsterAchievement.cpp）：击杀数达到下一阶段条件即
 * 自动 {@code grade+1}；领奖门为「已领阶段数 receivedReward &lt; grade」，领取后 receivedReward+1 并按
 * 所领阶段发放经验。条目状态四元组 (id, killCount, grade, receivedReward) 经 0x15B/0x15C 下发，
 * 字节序为 grade 在前、receivedReward 在后（真端 {@code _SendAllToClient/_SendUpdatedToClient}）。</p>
 * @author Ranastic
 */
@Slf4j
public class AtreianBestiaryService {

	/** 客户端系统消息：已获得%0图鉴（首次登记提醒）。 / Client message: obtained %0 bestiary (first registration). */
	private static final int MSG_ACHIEVEMENT_GET = 1404072;
	/** 客户端系统消息：已完成%0的%1阶段图鉴。 / Client message: completed %0 grade %1. */
	private static final int MSG_ACHIEVEMENT_COMPLETION = 1404073;
	/** 客户端系统消息：已完成怪物图鉴（开启图鉴提示）。 / Client message: bestiary completed (open-book hint). */
	private static final int MSG_ACHIEVEMENT_COMPLETION2 = 1404074;

    private static volatile ObjectProvider<AtreianBestiaryService> instanceProvider;
    private static volatile AtreianBestiaryService resolvedInstance;
	private final PlayerAtreianBestiaryDao playerABDAO;

	/**
	 * 使用指定的 PlayerABDAO 构造图鉴服务。
	 * Constructs the bestiary service with the given PlayerABDAO.
	 *
	 * @param playerABDAO 图鉴数据访问对象 / bestiary DAO
	 */
	public AtreianBestiaryService(@NonNull PlayerAtreianBestiaryDao playerABDAO) {
		this.playerABDAO = playerABDAO;
	}

	/**
	 * 玩家登录时同步状态。
	 * Syncs state when a player logs in.
	 * @param player 玩家 / player
	 */
	public void onLogin(Player player) {
		PacketSendUtility.sendPacket(player, new SM_ATREIAN_BESTIARY_LIST(player));
	}

	/**
	 * 构造图鉴名的客户端显示参数：优先用首个 NPC 的名称 ID（DescriptionId，客户端按语言表本地化），
	 * NPC 模板缺失时退回内部名（真端 {@code GetExternalNameByID} 显示同为怪物本地化名）。
	 * Builds the client display param for the book name: prefer the first NPC's name id (DescriptionId,
	 * localized by the client); falls back to the internal name when the NPC template is missing.
	 * @param template 图鉴模板 / bestiary template
	 * @return 消息 %0 参数 / the %0 message param
	 */
	private static Object bookDisplayName(AtreianBestiaryTemplate template) {
		List<Integer> npcIds = template.getNpcIds();
		if (!npcIds.isEmpty()) {
			NpcTemplate npcTemplate = DataManager.NPC_DATA.getNpcTemplate(npcIds.getFirst());
			if (npcTemplate != null) {
				// 客户端名称串表 ID = nameId * 2 + 1（同 NpcController.defeatNamedMsg 击杀公告）。
				// Client name-string id = nameId * 2 + 1 (same as the named-kill broadcast).
				return new DescriptionId(npcTemplate.getNameId() * 2 + 1);
			}
		}
		return template.getName();
	}

	/**
	 * 击杀时处理：登记图鉴条目、累计击杀并按条件自动升阶段。
	 * Handles a kill event: registers the entry, accumulates kills and auto grade-ups on conditions.
	 * <p>真端 {@code OnKillMonster/_TryGradeUp}：首次登记下发 1404072；升阶段下发 1404073，
	 * 且已领奖励追平阶段时追加 1404074（可领奖提示）。</p>
	 * @param player 玩家 / player
	 * @param npcId 被击杀 NPC ID / killed NPC id
	 */
	public void onKill(Player player, int npcId) {
		AtreianBestiaryTemplate template = DataManager.ATREIAN_BESTIARY.getAtreianBestiaryTemplateByNpcId(npcId);
		if (template == null) {
			return;
		}
		List<AtreianBestiaryAchievementTemplate> achievements = template.getAtreianBestiaryAchievementTemplate();
		if (achievements == null || achievements.isEmpty()) {
			return;
		}
		int objId = player.getObjectId();
		int killCount = playerABDAO.getKillCountById(objId, template.getId());
		int grade = playerABDAO.getLevelById(objId, template.getId());
		int receivedReward = playerABDAO.getClaimRewardById(objId, template.getId());
		if (grade == 0 && receivedReward > 0) {
			// 旧版缺陷把 claim_reward 写成 level+1 而等级恒为 0（奖励从未发放）⇒ 脏行回零。
			// Legacy defect wrote claim_reward = level+1 while level stayed 0; no reward was granted, reset.
			receivedReward = 0;
		}
		boolean newEntry = killCount == 0;
		killCount++;
		boolean gradeUp = false;
		while (grade < achievements.size() && killCount >= achievements.get(grade).getKillCondition()) {
			grade++;
			gradeUp = true;
		}
		if (newEntry) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(MSG_ACHIEVEMENT_GET, bookDisplayName(template)));
		}
		if (gradeUp) {
			PacketSendUtility.sendPacket(player,
				new SM_SYSTEM_MESSAGE(MSG_ACHIEVEMENT_COMPLETION, bookDisplayName(template), grade));
			if (receivedReward == grade - 1) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(MSG_ACHIEVEMENT_COMPLETION2));
			}
		}
		PacketSendUtility.sendPacket(player,
			new SM_ATREIAN_BESTIARY(template.getId(), killCount, grade, receivedReward));
		player.getAtreianBestiary().add(player, template.getId(), killCount, grade, receivedReward);
	}

	/**
	 * 领取阶段奖励：真端领奖门为「已领阶段数 &lt; 当前阶段」（{@code SendGiveReward}），
	 * 领取后已领数 +1，按所领阶段发放经验并回发条目状态。
	 * Claims the pending grade reward: the retail gate is {@code receivedReward < grade}; a successful
	 * claim bumps the counter, grants that grade's exp and echoes the entry state.
	 * @param player 玩家 / player
	 * @param id 图鉴条目 ID / bestiary entry id
	 */
	public void onLvlUp(Player player, int id) {
		AtreianBestiaryTemplate template = DataManager.ATREIAN_BESTIARY.getAtreianBestiaryTemplate(id);
		if (template == null) {
			return;
		}
		List<AtreianBestiaryAchievementTemplate> achievements = template.getAtreianBestiaryAchievementTemplate();
		if (achievements == null || achievements.isEmpty()) {
			return;
		}
		int objId = player.getObjectId();
		int killCount = playerABDAO.getKillCountById(objId, id);
		int grade = playerABDAO.getLevelById(objId, id);
		int receivedReward = playerABDAO.getClaimRewardById(objId, id);
		if (grade == 0 && receivedReward > 0) {
			receivedReward = 0;
		}
		if (receivedReward >= grade) {
			// 没有可领的阶段奖励；与真端一致静默返回。
			return;
		}
		receivedReward++;
		long exp = receivedReward <= achievements.size()
			? achievements.get(receivedReward - 1).getRewardExp()
			: 0;
		if (exp > 0) {
			player.getCommonData().addExp(exp, RewardType.MONSTER_BOOK);
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GET_EXP2(exp));
		}
		PacketSendUtility.sendPacket(player, new SM_ATREIAN_BESTIARY(id, killCount, grade, receivedReward));
		player.getAtreianBestiary().add(player, id, killCount, grade, receivedReward);
	}

	/**
	 * 获取实例：必须由 Spring 提供（{@link #setInstanceProvider(ObjectProvider)}）。
	 * Returns the instance, which must be supplied by Spring.
	 * <p>双源静态兜底已退役：缺少 provider 时直接 fail-fast，避免在容器之外静默创建第二套实例。
	 * The legacy static fallback is retired: a missing provider now fails fast instead of silently
	 * creating a second instance outside the container.</p>
	 * @return 由 Spring 提供的实例 / the Spring-provided instance
	 * @throws IllegalStateException provider 未注入或容器中没有该 Bean /
	 *         when no provider or bean is available
	 */
	public static AtreianBestiaryService getInstance() {
		AtreianBestiaryService resolved = resolvedInstance;
		if (resolved != null) {
			return resolved;
		}
		ObjectProvider<AtreianBestiaryService> provider = instanceProvider;
		resolved = provider == null ? null : provider.getIfAvailable();
		if (resolved == null) {
			throw new IllegalStateException("AtreianBestiaryService 未由 Spring 提供："
				+ (provider == null ? "instanceProvider 未注入" : "容器中不存在该 Bean")
				+ "（静态兜底已退役，见 LegacySingletonFallbackAuditTest）");
		}
		resolvedInstance = resolved;
		return resolved;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Sets the Spring instance provider.
	 *
	 * @param instanceProvider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<AtreianBestiaryService> instanceProvider) {
		AtreianBestiaryService.instanceProvider = instanceProvider;
		resolvedInstance = null;
	}

}
