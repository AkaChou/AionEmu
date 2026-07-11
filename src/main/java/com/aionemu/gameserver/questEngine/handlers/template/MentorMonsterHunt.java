package com.aionemu.gameserver.questEngine.handlers.template;

import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.questEngine.handlers.models.Monster;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 导师击杀任务模板：在 {@link MonsterHunt} 基础上，仅当组队中存在符合等级/距离条件的导师或学员时才计杀。
 * Mentor monster-hunt template: extends {@link MonsterHunt} and only counts kills when a valid mentor/mentee is in range in the group.
 */
public class MentorMonsterHunt extends MonsterHunt {
	/** 学员最低等级 / mentee minimum level */
	private int menteMinLevel;
	/** 学员最高等级 / mentee maximum level */
	private int menteMaxLevel;
	/** 任务模板（含导师类型） / quest template (includes mentor type) */
	private QuestTemplate qt;

	/**
	 * 构造导师击杀任务处理器。
	 * Constructs a mentor monster-hunt quest handler.
	 *
	 * quest id
	 * start NPC list
	 * end NPC list
	 * @param monsters 怪物配置映射 / monster config map
	 * @param menteMinLevel 学员最低等级 / mentee min level
	 * @param menteMaxLevel 学员最高等级 / mentee max level
	 */
	public MentorMonsterHunt(int questId, List<Integer> startNpcIds, List<Integer> endNpcIds, Map<Monster, Set<Integer>> monsters, int menteMinLevel, int menteMaxLevel) {
        super(questId, startNpcIds, endNpcIds, monsters, 0, 0, null, 0, false);
		this.menteMinLevel = menteMinLevel;
		this.menteMaxLevel = menteMaxLevel;
		this.qt = DataManager.QUEST_DATA.getQuestById(questId);
	}

	/**
	 * 处理击杀事件：按导师/学员类型校验组队条件后委托父类累计击杀。
	 * Handles kill events: validates mentor/mentee group conditions then delegates kill counting to the parent.
	 *
	 * @param env 任务环境 / quest environment
	 * @return 是否已处理 / whether the kill event was handled
	 */
	@Override
	public boolean onKillEvent(QuestEnv env) {
		Player player = env.getPlayer();
		QuestState qs = player.getQuestStateList().getQuestState(getQuestId());
		if (qs != null && qs.getStatus() == QuestStatus.START) {
			switch (qt.getMentorType()) {
			case MENTOR:
				if (player.isMentor()) {
					PlayerGroup group = player.getPlayerGroup2();
					for (Player member : group.getMembers()) {
						if (member.getLevel() >= menteMinLevel && member.getLevel() <= menteMaxLevel && MathUtil.getDistance(player, member) < GroupConfig.GROUP_MAX_DISTANCE) {
							return super.onKillEvent(env);
						}
					}
				}
				break;
			case MENTE:
				if (player.isInGroup2()) {
					PlayerGroup group = player.getPlayerGroup2();
					for (Player member : group.getMembers()) {
						if (member.isMentor() && MathUtil.getDistance(player, member) < GroupConfig.GROUP_MAX_DISTANCE) {
							return super.onKillEvent(env);
						}
					}
				}
				break;
			}
		}
		return false;
	}
}
