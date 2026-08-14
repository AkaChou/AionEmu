package com.aionemu.gameserver.model.siege;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import java.util.Iterator;
import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INFLUENCE_RATIO;
import com.aionemu.gameserver.services.SiegeService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;

/**
 * Influence，用于要塞相关逻辑。
 * Influence for siege logic.
 */

public class Influence {
	private static volatile ObjectProvider<Influence> instanceProvider;

	// ======[欧比斯]============= / ======[ABYSS]=============
	private float abyss_e = 0;
	private float abyss_a = 0;
	private float abyss_b = 0;
	// ======[卡尔多]============ / ======[KALDOR]============
	private float kaldor_e = 0;
	private float kaldor_a = 0;
	private float kaldor_b = 0;
	// ======[贝卢斯]============= / ======[BELUS]=============
	private float belus_e = 0;
	private float belus_a = 0;
	private float belus_b = 0;
	// ======[阿斯皮达]============ / ======[ASPIDA]============
	private float aspida_e = 0;
	private float aspida_a = 0;
	private float aspida_b = 0;
	// ======[阿塔纳托斯]========== / ======[ATANATOS]==========
	private float atanatos_e = 0;
	private float atanatos_a = 0;
	private float atanatos_b = 0;
	// ======[迪西隆]========== / ======[DISILLON]==========
	private float disillon_e = 0;
	private float disillon_a = 0;
	private float disillon_b = 0;
	// ======[全局]============ / ======[GLOBAL]============
	private float global_e = 0;
	private float global_a = 0;
	private float global_b = 0;

	public Influence() {
		calculateInfluence();
	}

	/** 获取实例。 / Returns the instance. */
	public static Influence getInstance() {
		ObjectProvider<Influence> provider = instanceProvider;
		if (provider == null) {
			return SingletonHolder.instance;
		}
		return provider.getIfAvailable(() -> SingletonHolder.instance);
	}

	/** 设置实例提供者。 / Sets the instance provider. */
	public static void setInstanceProvider(ObjectProvider<Influence> instanceProvider) {
		Influence.instanceProvider = instanceProvider;
	}

	private static class SingletonHolder {
		protected static final Influence instance = new Influence();
	}

	/** 重算影响力。 / Recalculates influence. */
	public void recalculateInfluence() {
		calculateInfluence();
	}

	private void calculateInfluence() {
		float balaurea = 0.0019512194f;
		float abyss = 0.006097561f;
		// ======[欧比斯]========== / ======[ABYSS]==========
		float e_abyss = 0f;
		float a_abyss = 0f;
		float b_abyss = 0f;
		float t_abyss = 0f;
		// ======[卡尔多]====== / ======[KALDOR]======
		float e_kaldor = 0f;
		float a_kaldor = 0f;
		float b_kaldor = 0f;
		float t_kaldor = 0f;
		for (SiegeLocation sLoc : GameFeatureServices.siegeService().getSiegeLocations().values()) {
			switch (sLoc.getWorldId()) {
			// ======[欧比斯]========== / ======[ABYSS]==========
			case 400010000:
				t_abyss += sLoc.getInfluenceValue();
				switch (sLoc.getRace()) {
				case ELYOS:
					e_abyss += sLoc.getInfluenceValue();
					break;
				case ASMODIANS:
					a_abyss += sLoc.getInfluenceValue();
					break;
				case BALAUR:
					b_abyss += sLoc.getInfluenceValue();
					break;
				}
				break;
			// ======[卡尔多]====== / ======[KALDOR]======
			case 600090000:
				if (sLoc instanceof FortressLocation) {
					t_kaldor += sLoc.getInfluenceValue();
					switch (sLoc.getRace()) {
					case ELYOS:
						e_kaldor += sLoc.getInfluenceValue();
						break;
					case ASMODIANS:
						a_kaldor += sLoc.getInfluenceValue();
						break;
					case BALAUR:
						b_kaldor += sLoc.getInfluenceValue();
						break;
					}
				}
				break;
			}
		}
		// ======[欧比斯]========= / ======[ABYSS]=========
		abyss_e = (e_abyss / t_abyss);
		abyss_a = (a_abyss / t_abyss);
		abyss_b = (b_abyss / t_abyss);
		// ======[卡尔多]====== / ======[KALDOR]=====
		kaldor_e = (e_kaldor / t_kaldor);
		kaldor_a = (a_kaldor / t_kaldor);
		kaldor_b = (b_kaldor / t_kaldor);
		// ======[全局]======== / ======[GLOBAL]========
		global_e = (kaldor_e * balaurea + abyss_e * abyss) * 100f;
		global_a = (kaldor_a * balaurea + abyss_a * abyss) * 100f;
		global_b = (kaldor_b * balaurea + abyss_b * abyss) * 100f;
	}

	@SuppressWarnings("unused")
	private void broadcastInfluencePacket() {
		SM_INFLUENCE_RATIO pkt = new SM_INFLUENCE_RATIO();
		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();
		while (iter.hasNext()) {
			Player player = iter.next();
			PacketSendUtility.sendPacket(player, pkt);
		}
	}

	// =======[全局]========= / =======[GLOBAL]=========
	// ========================
	/** 返回天族全局影响力 / Returns the global elyos influence */
	public float getGlobalElyosInfluence() {
		return global_e;
	}

	/** 返回魔族全局影响力 / Returns the global asmodians influence */
	public float getGlobalAsmodiansInfluence() {
		return global_a;
	}

	/** 返回龙族全局影响力 / Returns the global balaurs influence */
	public float getGlobalBalaursInfluence() {
		return global_b;
	}

	// ========[欧比斯]======== / ========[ABYSS]========
	// =======================
	/** 返回欧比斯天族影响力 / Returns the abyss elyos influence*/
	public float getAbyssElyosInfluence() {
		return abyss_e;
	}

	/** 返回欧比斯魔族影响力 / Returns the abyss asmodians influence*/
	public float getAbyssAsmodiansInfluence() {
		return abyss_a;
	}

	/** 返回欧比斯龙族影响力 / Returns the abyss balaurs influence */
	public float getAbyssBalaursInfluence() {
		return abyss_b;
	}

	// =======[卡尔多]======== / =======[KALDOR]========
	// =======================
	/** 返回卡尔多天族影响力 / Returns the kaldor elyos influence */
	public float getKaldorElyosInfluence() {
		return kaldor_e;
	}

	/** 返回卡尔多魔族影响力 / Returns the kaldor asmodians influence */
	public float getKaldorAsmodiansInfluence() {
		return kaldor_a;
	}

	/** 返回卡尔多龙族影响力 / Returns the kaldor balaurs influence */
	public float getKaldorBalaursInfluence() {
		return kaldor_b;
	}

	// ======[帕内斯特拉]===== / ======[PANESTERRA]=====
	// =======================
	/** 返回贝卢斯天族影响力 / Returns the belus elyos influence */
	public float getBelusElyosInfluence() {
		return belus_e;
	}

	/** 返回贝卢斯魔族影响力 / Returns the belus asmodians influence */
	public float getBelusAsmodiansInfluence() {
		return belus_a;
	}

	/** 返回贝卢斯龙族影响力 / Returns the belus balaurs influence */
	public float getBelusBalaursInfluence() {
		return belus_b;
	}

	/** 返回阿斯皮达天族影响力 / Returns the aspida elyos influence */
	public float getAspidaElyosInfluence() {
		return aspida_e;
	}

	/** 返回阿斯皮达魔族影响力 / Returns the aspida asmodians influence */
	public float getAspidaAsmodiansInfluence() {
		return aspida_a;
	}

	/** 返回阿斯皮达龙族影响力 / Returns the aspida balaurs influence */
	public float getAspidaBalaursInfluence() {
		return aspida_b;
	}

	/** 返回阿塔纳托斯天族影响力 / Returns the atanatos elyos influence */
	public float getAtanatosElyosInfluence() {
		return atanatos_e;
	}

	/**
	 * 返回阿塔纳托斯魔族影响力。
	 * Returns the atanatos asmodians influence.
	 */
	public float getAtanatosAsmodiansInfluence() {
		return atanatos_a;
	}

	/** 返回阿塔纳托斯龙族影响力 / Returns the atanatos balaurs influence */
	public float getAtanatosBalaursInfluence() {
		return atanatos_b;
	}

	/** 返回迪西隆天族影响力 / Returns the disillon elyos influence */
	public float getDisillonElyosInfluence() {
		return disillon_e;
	}

	/**
	 * 返回迪西隆魔族影响力。
	 * Returns the disillon asmodians influence.
	 */
	public float getDisillonAsmodiansInfluence() {
		return disillon_a;
	}

	/** 返回迪西隆龙族影响力 / Returns the disillon balaurs influence */
	public float getDisillonBalaursInfluence() {
		return disillon_b;
	}

	/** 返回 PVP 种族加成 / Returns the pvp race bonus */
	public float getPvpRaceBonus(Race attRace) {
		float bonus = 1;
		float elyos = getGlobalElyosInfluence();
		float asmo = getGlobalAsmodiansInfluence();
		switch (attRace) {
		case ASMODIANS:
			if (elyos >= 0.81f && asmo <= 0.10f) {
				bonus = 1.2f;
			} else if (elyos >= 0.81f || (elyos >= 0.71f && asmo <= 0.10f)) {
				bonus = 1.15f;
			} else if (elyos >= 0.71f) {
				bonus = 1.1f;
			}
			break;
		case ELYOS:
			if (asmo >= 0.81f && elyos <= 0.10f) {
				bonus = 1.2f;
			} else if (asmo >= 0.81f || (asmo >= 0.71f && elyos <= 0.10f)) {
				bonus = 1.15f;
			} else if (asmo >= 0.71f) {
				bonus = 1.1f;
			}
			break;
		default:
			break;
		}
		return bonus;
	}
}
