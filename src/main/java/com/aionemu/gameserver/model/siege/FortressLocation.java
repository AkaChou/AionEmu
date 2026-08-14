package com.aionemu.gameserver.model.siege;

import java.util.List;

import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Kisk;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.siegelocation.SiegeLegionReward;
import com.aionemu.gameserver.model.templates.siegelocation.SiegeLocationTemplate;
import com.aionemu.gameserver.model.templates.siegelocation.SiegeReward;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * Fortress 位置，用于要塞相关逻辑。
 * Fortress Location for siege logic.
 *
 * @author Source
 */
public class FortressLocation extends SiegeLocation {

	protected List<SiegeReward> siegeRewards;
	protected List<SiegeLegionReward> siegeLegionRewards;
	protected boolean isUnderShield;
	protected boolean isUnderAssault;
	protected boolean isCanTeleport;

	public FortressLocation() {
	}

	public FortressLocation(SiegeLocationTemplate template) {
		super(template);
		this.siegeRewards = template.getSiegeRewards() != null ? template.getSiegeRewards() : null;
		this.siegeLegionRewards = template.getSiegeLegionRewards() != null ? template.getSiegeLegionRewards() : null;
	}

	/** 获取奖励。 / Returns the reward. */
	public List<SiegeReward> getReward() {
		return this.siegeRewards;
	}

	/** 获取军团奖励。 / Returns the legion reward. */
	public List<SiegeLegionReward> getLegionReward() {
		return this.siegeLegionRewards;
	}

	/**
	 * 判断生物是否与要塞阵营敌对。
	 * Checks whether the creature is hostile to the fortress race.
	 *
	 * @param creature 待判断的生物 / creature to check
	 * @return 是否敌对 / isEnemy
	 */
	public boolean isEnemy(Creature creature) {
		return creature.getRace().getRaceId() != getRace().getRaceId();
	}

	/**
	 * @return 是否处于护盾下 / isUnderShield
	 */
	@Override
	public boolean isUnderShield() {
		return this.isUnderShield;
	}

	/**
	 * @param value 新的护盾状态值 / new undershield value
	 */
	@Override
	public void setUnderShield(boolean value) {
		this.isUnderShield = value;
	}

	/**
	 * 判断玩家是否可传送（同阵营限制）。
	 * Checks whether the player can teleport (limited to the owning race).
	 *
	 * @param player 待判断的玩家 / player to check
	 * @return 是否可传送 / isCanTeleport
	 */
	@Override
	public boolean isCanTeleport(Player player) {
		if (player == null)
			return isCanTeleport;
		return isCanTeleport && player.getRace().getRaceId() == getRace().getRaceId();
	}

	/**
	 * @param status 传送状态 / Teleportation status
	 */
	@Override
	public void setCanTeleport(boolean status) {
		this.isCanTeleport = status;
	}

	/**
	 * @return 含要塞名称的 DescriptionId / DescriptionId object with fortress name
	 */
	public DescriptionId getNameAsDescriptionId() {
		return new DescriptionId(template.getNameId());
	}

	/** 进入区域时 / On Enter Zone */
	public void onEnterZone(Creature creature, ZoneInstance zone) {
		super.onEnterZone(creature, zone);
		if (isVulnerable())
			creature.setInsideZoneType(ZoneType.SIEGE);
	}

	/** 离开区域时 / On Leave Zone */
	@Override
	public void onLeaveZone(Creature creature, ZoneInstance zone) {
		super.onLeaveZone(creature, zone);
		if (this.isVulnerable())
			creature.unsetInsideZoneType(ZoneType.SIEGE);
	}

	/** 清空位置。 / Clear location. */
	public void clearLocation() {
		for (Creature creature : getCreaturesSnapshot()) {
			if ((isEnemy(creature)) && ((creature instanceof Kisk))) {
				Kisk kisk = (Kisk) creature;
				kisk.getController().die();
			}
		}
	}
}
