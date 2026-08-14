package com.aionemu.gameserver.services.player.CreativityPanel;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CREATIVITY_POINTS_APPLY;
import com.aionemu.gameserver.services.SkillLearnService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 创造力面板变形服务，处理变形技能学习与附魔。
 * Creativity panel transformation service handling transform skill learn/enchant.
 */
public class CreativityTransfoService {
	private static volatile ObjectProvider<CreativityTransfoService> instanceProvider;

	/**
	 * 应用变形效果。
	 * Applies transformation effects.
	 *
	 * 玩家 / player
	 * @param type 类型 / type
	 * @param size 槽位大小 / size
	 * @param id ID / id
	 * @param point 点数 / point
	 */
	public void onTransfoApply(Player player, int type, int size, int id, int point) {
		if (id >= 7 && id <= 14 || id >= 401 && id <= 408) {
			learnTransfo(player, id, point);
		}
		PacketSendUtility.sendPacket(player, new SM_CREATIVITY_POINTS_APPLY(type, size, id, point));
	}

	/**
	 * 学习变形。
	 * Learns a transformation.
	 *
	 * 玩家 / player
	 * @param id ID / id
	 * @param point 点数 / point
	 */
	public void learnTransfo(Player player, int id, int point) {
		if (point >= 1) {
			switch (id) {
			case 7:
				player.getSkillList().addSkill(player, 4696, 1); // 变形：风之容器 / Transformation: Vessel Of Wind.
				player.getSkillList().addSkill(player, 4697, 1); // 水银爆裂 / Mercurial Blast.
				break;
			case 9:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4700, 1); // 变形：火之容器 / Transformation: Vessel Of Fire.
					player.getSkillList().addSkill(player, 4701, 1); // 引爆（天族） / Detonate (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4700, 1); // 变形：火之容器 / Transformation: Vessel Of Fire.
					player.getSkillList().addSkill(player, 4704, 1); // 引爆（魔族） / Detonate (Asmodians)
				}
				break;
			case 11:
				player.getSkillList().addSkill(player, 4702, 1); // 变形：水之容器 / Transformation: Vessel Of Water.
				player.getSkillList().addSkill(player, 4703, 1); // 水缚 / Waterbind.
				break;
			case 13:
				player.getSkillList().addSkill(player, 4698, 1); // 变形：地之容器 / Transformation: Vessel Of Earth.
				player.getSkillList().addSkill(player, 4699, 1); // 地形改造 / Terraform.
				break;
			case 401:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4768, 1); // 变形：风之化身（天族） / Transformation: Avatar Of Wind (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4807, 1); // 变形：风之化身（魔族） / Transformation: Avatar Of Wind (Asmodians)
				}
				break;
			case 403:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4752, 1); // 变形：火之化身（天族） / Transformation: Avatar Of Fire (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4804, 1); // 变形：火之化身（魔族） / Transformation: Avatar Of Fire (Asmodians)
				}
				break;
			case 405:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4757, 1); // 变形：水之化身（天族） / Transformation: Avatar Of Water (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4805, 1); // 变形：水之化身（魔族） / Transformation: Avatar Of Water (Asmodians)
				}
				break;
			case 407:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4762, 1); // 变形：地之化身（天族） / Transformation: Avatar Of Earth (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4806, 1); // 变形：地之化身（魔族） / Transformation: Avatar Of Earth (Asmodians)
				}
				break;
			case 8:
				player.getSkillList().addSkill(player, 4696, point + 1); // 变形：风之容器 / Transformation: Vessel Of Wind.
				break;
			case 10:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4700, point + 1); // 变形：火之容器 / Transformation: Vessel Of Fire.
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4700, point + 1); // 变形：火之容器 / Transformation: Vessel Of Fire.
				}
				break;
			case 12:
				player.getSkillList().addSkill(player, 4702, point + 1); // 变形：水之容器 / Transformation: Vessel Of Water.
				break;
			case 14:
				player.getSkillList().addSkill(player, 4698, point + 1); // 变形：地之容器 / Transformation: Vessel Of Earth.
				break;
			case 402:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4768, point + 1); // 变形：风之化身（天族） / Transformation: Avatar Of Wind (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4807, point + 1); // 变形：风之化身 / Transformation: Avatar Of Wind
																				// （魔族） / (Asmodians)
				}
				break;
			case 404:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4752, point + 1); // 变形：火之化身（天族） / Transformation: Avatar Of Fire (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4804, point + 1); // 变形：火之化身 / Transformation: Avatar Of Fire
																				// （魔族） / (Asmodians)
				}
				break;
			case 406:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4757, point + 1); // 变形：水之化身（天族） / Transformation: Avatar Of Water (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4805, point + 1); // 变形：水之化身 / Transformation: Avatar Of Water
																				// （魔族） / (Asmodians)
				}
				break;
			case 408:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4762, point + 1); // 变形：地之化身（天族） / Transformation: Avatar Of Earth (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4806, point + 1); // 变形：地之化身 / Transformation: Avatar Of Earth
																				// （魔族） / (Asmodians)
				}
				break;
			}
			player.getCP().addPoint(player, id, point);
		} else if (point == 0) {
			switch (id) {
			case 7:
				SkillLearnService.removeSkill(player, 4696); // 变形：风之容器 / Transformation: Vessel Of Wind.
				SkillLearnService.removeSkill(player, 4697); // 水银爆裂 / Mercurial Blast.
				break;
			case 9:
				if (player.getRace() == Race.ELYOS) {
					SkillLearnService.removeSkill(player, 4700); // 变形：火之容器 / Transformation: Vessel Of Fire.
					SkillLearnService.removeSkill(player, 4701); // 引爆（天族） / Detonate (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					SkillLearnService.removeSkill(player, 4700); // 变形：火之容器 / Transformation: Vessel Of Fire.
					SkillLearnService.removeSkill(player, 4704); // 引爆（魔族） / Detonate (Asmodians)
				}
				break;
			case 11:
				SkillLearnService.removeSkill(player, 4702); // 变形：水之容器 / Transformation: Vessel Of Water.
				SkillLearnService.removeSkill(player, 4703); // 水缚 / Waterbind.
				break;
			case 13:
				SkillLearnService.removeSkill(player, 4698); // 变形：地之容器 / Transformation: Vessel Of Earth.
				SkillLearnService.removeSkill(player, 4699); // 地形改造 / Terraform.
				break;
			case 401:
				if (player.getRace() == Race.ELYOS) {
					SkillLearnService.removeSkill(player, 4768); // 变形：风之化身（天族） / Transformation: Avatar Of Wind (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					SkillLearnService.removeSkill(player, 4807); // 变形：风之化身（魔族） / Transformation: Avatar Of Wind (Asmodians)
				}
				break;
			case 403:
				if (player.getRace() == Race.ELYOS) {
					SkillLearnService.removeSkill(player, 4752); // 变形：火之化身（天族） / Transformation: Avatar Of Fire (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					SkillLearnService.removeSkill(player, 4804); // 变形：火之化身（魔族） / Transformation: Avatar Of Fire (Asmodians)
				}
				break;
			case 405:
				if (player.getRace() == Race.ELYOS) {
					SkillLearnService.removeSkill(player, 4757); // 变形：水之化身（天族） / Transformation: Avatar Of Water (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					SkillLearnService.removeSkill(player, 4805); // 变形：水之化身（魔族） / Transformation: Avatar Of Water (Asmodians)
				}
				break;
			case 407:
				if (player.getRace() == Race.ELYOS) {
					SkillLearnService.removeSkill(player, 4762); // 变形：地之化身（天族） / Transformation: Avatar Of Earth (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					SkillLearnService.removeSkill(player, 4806); // 变形：地之化身（魔族） / Transformation: Avatar Of Earth (Asmodians)
				}
				break;
			case 8:
				player.getSkillList().addSkill(player, 4696, 1); // 变形：风之容器 / Transformation: Vessel Of Wind.
				break;
			case 10:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4700, 1); // 变形：火之容器 / Transformation: Vessel Of Fire.
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4700, 1); // 变形：火之容器 / Transformation: Vessel Of Fire.
				}
				break;
			case 12:
				player.getSkillList().addSkill(player, 4702, 1); // 变形：水之容器 / Transformation: Vessel Of Water.
				break;
			case 14:
				player.getSkillList().addSkill(player, 4698, 1); // 变形：地之容器 / Transformation: Vessel Of Earth.
				break;
			case 402:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4768, 1); // 变形：风之化身（天族） / Transformation: Avatar Of Wind (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4807, 1); // 变形：风之化身（魔族） / Transformation: Avatar Of Wind (Asmodians)
				}
				break;
			case 404:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4752, 1); // 变形：火之化身（天族） / Transformation: Avatar Of Fire (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4804, 1); // 变形：火之化身（魔族） / Transformation: Avatar Of Fire (Asmodians)
				}
				break;
			case 406:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4757, 1); // 变形：水之化身（天族） / Transformation: Avatar Of Water (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4805, 1); // 变形：水之化身（魔族） / Transformation: Avatar Of Water (Asmodians)
				}
				break;
			case 408:
				if (player.getRace() == Race.ELYOS) {
					player.getSkillList().addSkill(player, 4762, 1); // 变形：地之化身（天族） / Transformation: Avatar Of Earth (Elyos)
				} else if (player.getRace() == Race.ASMODIANS) {
					player.getSkillList().addSkill(player, 4806, 1); // 变形：地之化身（魔族） / Transformation: Avatar Of Earth (Asmodians)
				}
				break;
			}
			player.getCP().removePoint(player, id);
		}
	}

	/**
	 * 附魔变形。
	 * Enchants a transformation.
	 *
	 * 玩家 / player
	 * @param id ID / id
	 * @param point 点数 / point
	 */
	public void enchantTransfo(Player player, int id, int point) {
		if (point >= 1) {
			player.getCP().addPoint(player, id, point);
		} else if (point == 0) {
			player.getCP().removePoint(player, id);
		}
	}

	/**
	 * 获取服务单例。
	 * Returns the service singleton.
	 * @return 服务实例 / service instance
	 */
	public static CreativityTransfoService getInstance() {
		ObjectProvider<CreativityTransfoService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> NewSingletonHolder.INSTANCE);
		}
		return NewSingletonHolder.INSTANCE;
	}

	/**
	 * setInstanceProvider 方法。
	 * setInstanceProvider method.
	 *
	 * @param provider 提供者 / provider
	 */
	public static void setInstanceProvider(ObjectProvider<CreativityTransfoService> provider) {
		instanceProvider = provider;
	}

	private static class NewSingletonHolder {

		private static final CreativityTransfoService INSTANCE = new CreativityTransfoService();
	}
}
