package com.aionemu.gameserver.model.skinskill;

import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerSkillSkinListDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.SkillSkinTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.taskmanager.tasks.ExpireTimerTask;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 技能外观列表。
 * Skill Skin List model.
 *
 * @author Rinzler (Encom)
 */
public class SkillSkinList {

	private final Map<Integer, SkillSkin> skillskins;
	private Player owner;

	public SkillSkinList() {
		skillskins = new HashMap<>();
		owner = null;
	}

	/** 返回所有者 / Returns the owner*/
	public Player getOwner() {
		return owner;
	}

	/** 设置所有者 / Sets the owner*/
	public void setOwner(Player owner) {
		this.owner = owner;
	}

	/** 是否包含。 / Contains. */
	public boolean contains(int skinId) {
		return skillskins.containsKey(skinId);
	}

	/** 添加条目。 / Adds entry. */
	public void addEntry(int skinId, int remaining, int active) {
		SkillSkinTemplate sst = DataManager.SKILL_SKIN_DATA.getSkillSkinTemplate(skinId);
		if (sst == null) {
			throw new IllegalArgumentException("Invalid skill skin id " + skinId);
		}
		skillskins.put(skinId, new SkillSkin(sst, skinId, remaining, active));
	}

	/** 添加技能外观。 / Adds skill skin. */
	public boolean addSkillSkin(int skinId, int time, int expireTime) {
		SkillSkinTemplate sst = DataManager.SKILL_SKIN_DATA.getSkillSkinTemplate(skinId);
		if (sst == null) {
			throw new IllegalArgumentException("Invalid skin id " + skinId);
		}
		if (owner != null) {
			SkillSkin skillSkin = new SkillSkin(sst, skinId, expireTime, 1); // expireTime = System.currentTimeMillis() / 1000
																				// + minutes * 60（在 SkillAnimationAction 中计算）
																				// + minutes * 60 (Calculated in SkillAnimationAction)
			if (!skillskins.containsKey(skinId)) {
				skillskins.put(skinId, skillSkin);
				if (time != 0) {
					GameTaskManagerServices.expireTimerTask().addTask(skillSkin, owner);
				}
				DAOManager.getDAO(PlayerSkillSkinListDAO.class).storeSkillSkins(owner, skillSkin);
			} else {
				PacketSendUtility.sendPacket(owner, SM_SYSTEM_MESSAGE.STR_MSG_COSTUME_SKILL_ALREADY_HAS_COSTUME);
				return false;
			}
			PacketSendUtility.sendPacket(owner, SM_SYSTEM_MESSAGE.STR_MSG_GET_ITEM(sst.getName()));
			PacketSendUtility.sendPacket(owner, new SM_SKILL_ANIMATION(skinId, time)); // time = templateTime * 60
																						// （在 SkillAnimationAction 中计算） / (Calculated
																						// in SkillAnimationAction)
			return true;
		}
		return false;
	}

	/** 移除技能外观。 / Removes skill skin. */
	public void removeSkillSkin(int skinId) {
		if (!skillskins.containsKey(skinId)) {
			return;
		}
		skillskins.remove(skinId);
		PacketSendUtility.sendPacket(owner, new SM_SKILL_ANIMATION(owner));
		DAOManager.getDAO(PlayerSkillSkinListDAO.class).removeSkillSkin(owner.getObjectId(), skinId);
	}

	/** 设置 active / Sets the active */
	public void setActive(int skinId) {
		DAOManager.getDAO(PlayerSkillSkinListDAO.class).setActive(owner.getObjectId(), skinId);
		owner.setSkillSkinList(DAOManager.getDAO(PlayerSkillSkinListDAO.class).loadSkillSkinList(owner.getObjectId()));
		PacketSendUtility.sendPacket(owner, new SM_SKILL_ANIMATION(owner));
	}

	/** 设置 deactive / Sets the deactive */
	public void setDeactive(int skillId) {
		int skinIdToremove = 0;
		SkillTemplate skillGroup = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		if (this.owner.getSkillSkinList() != null) {
			for (SkillSkin skillSkin : owner.getSkillSkinList().getSkillSkins()) {
				if (skillSkin.getTemplate() != null) {
					if (skillSkin.getTemplate().getSkillGroup().equalsIgnoreCase(skillGroup.getSkillGroup())
							&& skillSkin.getIsActive() == 1) {
						skinIdToremove = skillSkin.getId();
						break;
					}
				}
			}
		}
		DAOManager.getDAO(PlayerSkillSkinListDAO.class).setDeactive(owner.getObjectId(), skinIdToremove);
		owner.setSkillSkinList(DAOManager.getDAO(PlayerSkillSkinListDAO.class).loadSkillSkinList(owner.getObjectId()));
		PacketSendUtility.sendPacket(owner, new SM_SKILL_ANIMATION(owner));
	}

	/** 返回皮肤 ID / Returns the skin id */
	public int getSkinId(int skillId) {
		int skinId = 0;
		if (skillId == 0 || getOwner().getSkillSkinList() == null || getOwner() == null) {
			return 0;
		}
		for (SkillSkin skillSkin : getOwner().getSkillSkinList().getSkillSkins()) {
			if (DataManager.SKILL_DATA.getSkillTemplate(skillId).getSkillGroup() != null) {
				if (skillSkin.getTemplate().getSkillGroup()
						.equalsIgnoreCase(DataManager.SKILL_DATA.getSkillTemplate(skillId).getSkillGroup())
						&& skillSkin.getIsActive() == 1) {
					skinId = skillSkin.getId();
				}
			}
		}
		return skinId;
	}

	/** 大小 / size. */
	public int size() {
		return skillskins.size();
	}

	/** 返回 skill skins / Returns the skill skins */
	public Collection<SkillSkin> getSkillSkins() {
		return skillskins.values();
	}
}
