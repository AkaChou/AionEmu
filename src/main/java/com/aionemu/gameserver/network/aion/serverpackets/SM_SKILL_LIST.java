package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步技能列表（基础/连锁/印记或新增技能）。
 * Server packet synchronizing the skill list (basic/linked/stigma or new skill) to the client.
 */
public class SM_SKILL_LIST extends AionServerPacket {
	private PlayerSkillEntry[] skillList;
	private int messageId;
	private int skillNameId;
	private String skillLvl;
	public static final int YOU_LEARNED_SKILL = 1300050;
	boolean isNew = false;
	private Player player;
	private int state;

	/**
	 * 使用给定参数构造 SM_SKILL_LIST 包。
	 * Creates a SM_SKILL_LIST packet with the given parameters.
	 *
	 * 玩家 / player
	 * @param basicSkills 基础技能数组 / basic skills
	 */
	public SM_SKILL_LIST(Player player, PlayerSkillEntry[] basicSkills) {
		this.player = player;
		this.skillList = player.getSkillList().getBasicSkills();
		this.messageId = 0;
	}

	/**
	 * 使用给定参数构造 SM_SKILL_LIST 包。
	 * Creates a SM_SKILL_LIST packet with the given parameters.
	 *
	 * 玩家 / player
	 * @param linkedSkills 连锁技能数组 / linked skills
	 * state
	 */
	public SM_SKILL_LIST(Player player, PlayerSkillEntry[] linkedSkills, int state) {
		this.player = player;
		this.skillList = player.getSkillList().getLinkedSkills();
		this.state = state;
		this.messageId = 0;
		this.isNew = true;
	}

	/**
	 * 使用给定参数构造 SM_SKILL_LIST 包。
	 * Creates a SM_SKILL_LIST packet with the given parameters.
	 *
	 * 玩家 / player
	 * stigma skill
	 */
	public SM_SKILL_LIST(Player player, PlayerSkillEntry stigmaSkill) {
		this.skillList = new PlayerSkillEntry[] { stigmaSkill };
		this.messageId = 0;
	}

	/**
	 * 使用给定参数构造 SM_SKILL_LIST 包。
	 * Creates a SM_SKILL_LIST packet with the given parameters.
	 *
	 * @param skillListEntry 技能列表项 / skill list entry
	 * message id
	 * @param isNew 是否新技能 / is new
	 */
	public SM_SKILL_LIST(PlayerSkillEntry skillListEntry, int messageId, boolean isNew) {
		this.skillList = new PlayerSkillEntry[] { skillListEntry };
		this.messageId = messageId;
	    this.skillNameId = DataManager.SKILL_DATA.getSkillTemplate(skillListEntry.getSkillId()) != null ? DataManager.SKILL_DATA.getSkillTemplate(skillListEntry.getSkillId()).getNameId() : 0;
		this.skillLvl = String.valueOf(skillListEntry.getSkillLevel());
		this.isNew = isNew;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		final int size = skillList.length;
		writeH(size);
		if (isNew) {
			writeC(0);
		} else {
			writeC(1);
		}
		if (size > 0) {
			for (PlayerSkillEntry entry : skillList) {
				writeH(entry.getSkillId());
				writeH(entry.getSkillLevel());
				writeC(0x00);
				int extraLevel = entry.getExtraLvl();
				writeC(extraLevel);
				if (isNew && extraLevel == 0 && !entry.isStigma()) {
					writeD((int) (System.currentTimeMillis() / 1000));
				} else {
					writeD(0);
				}
				if (entry.isStigma()) {
					writeC(1);
				} else if (entry.isLinked()) {
					writeC(3);
				} else {
					writeC(0);
				}
			}
		}
		writeD(messageId);
		if (messageId != 0) {
			writeH(0x24);
			writeD(skillNameId);
			writeH(0x00);
			writeS(skillLvl);
			writeH(0x00);
		}
	}
}
