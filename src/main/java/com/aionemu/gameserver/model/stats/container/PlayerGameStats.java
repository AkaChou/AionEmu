package com.aionemu.gameserver.model.stats.container;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.stats.calc.AdditionStat;
import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.templates.item.ArmorType;
import com.aionemu.gameserver.model.templates.item.WeaponType;
import com.aionemu.gameserver.model.templates.ride.RideInfo;
import com.aionemu.gameserver.model.templates.stats.PlayerStatsTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATS_INFO;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster.BroadcastMode;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.CalculationType;
import org.apache.commons.lang3.ArrayUtils;

/**
 * 玩家游戏属性，用于属性相关逻辑。
 * Player Game Stats for stats logic.
 *
 * @author xavier
 */
public class PlayerGameStats extends CreatureGameStats<Player> {

	private int cachedSpeed;
	private int cachedAttackSpeed;
	private int maxDamageChance;
	private float minDamageRatio;
	private float skillEfficiency;

	/**
	 * @param owner
	 */
	public PlayerGameStats(Player owner) {
		super(owner);
	}

	@Override
	protected void onStatsChange() {
		super.onStatsChange();
		updateStatsAndSpeedVisually();
	}

	/** 更新 stats and speed visually / Update stats and speed visually */
	public void updateStatsAndSpeedVisually() {
		updateStatsVisually();
		checkSpeedStats();
	}

	/** 更新 stats visually / Update stats visually */
	public void updateStatsVisually() {
		owner.addPacketBroadcastMask(BroadcastMode.UPDATE_STATS);
	}

	private void checkSpeedStats() {
		int current = getMovementSpeed().getCurrent();
		int currentAttackSpeed = getAttackSpeed().getCurrent();
		if (current != cachedSpeed || currentAttackSpeed != cachedAttackSpeed) {
			owner.addPacketBroadcastMask(BroadcastMode.UPDATE_SPEED);
		}
		cachedSpeed = current;
		cachedAttackSpeed = currentAttackSpeed;
	}

	/** 返回最大生命 / Returns the max hp*/
	@Override
	public Stat2 getMaxHp() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		Stat2 stat = getStat(StatEnum.MAXHP, pst.getMaxHp());
		int HVIT = ((Player) owner).getGameStats().getStat(StatEnum.HVIT, 0).getCurrent();
		int MaxHpCalculation = Math.round(19118 * HVIT / (825.0F + HVIT));
		stat.addToBonus(MaxHpCalculation);
		return stat;
	}

	/** 返回最大魔法 / Returns the max mp*/
	@Override
	public Stat2 getMaxMp() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		Stat2 stat = getStat(StatEnum.MAXMP, pst.getMaxMp());
		int HWIL = ((Player) owner).getGameStats().getStat(StatEnum.HWIL, 0).getCurrent();
		int MaxMpCalculation = Math.round(20540 * HWIL / (825.0F + HWIL));
		stat.addToBonus(MaxMpCalculation);
		return stat;
	}

	/** 返回 strike resist / Returns the strike resist */
	public Stat2 getStrikeResist() {
		Stat2 stat = getStat(StatEnum.PHYSICAL_CRITICAL_RESIST, 0);
		int HDEX = ((Player) owner).getGameStats().getStat(StatEnum.HDEX, 0).getCurrent();
		int Pcrculation = Math.round(1144 * HDEX / (187.0F + HDEX));
		stat.addToBonus(Pcrculation);
		return stat;		
	}

	/** 返回 strike fort / Returns the strike fort */
	public Stat2 getStrikeFort() {
		return getStat(StatEnum.PHYSICAL_CRITICAL_DAMAGE_REDUCE, 0);
	}

	/** 返回 spell resist / Returns the spell resist */
	public Stat2 getSpellResist() {
		int base = 0;
		int Pclass = owner.getPlayerClass().getClassId();
		if (Pclass == 7 || Pclass == 8 || Pclass == 10) {
			base = 50;
		}		
		Stat2 stat = getStat(StatEnum.MAGICAL_CRITICAL_RESIST, base);
		int HWIL = ((Player) owner).getGameStats().getStat(StatEnum.HWIL, 0).getCurrent();
		int MCrCalculation = Math.round(1236 * HWIL / (376.0F + HWIL));
		stat.addToBonus(MCrCalculation);
		return stat;
	}

	/** 返回 spell fort / Returns the spell fort */
	public Stat2 getSpellFort() {
		return getStat(StatEnum.MAGICAL_CRITICAL_DAMAGE_REDUCE, 0);
	}

	/** 返回最大神圣力 / Returns the max dp*/
	public Stat2 getMaxDp() {
		return getStat(StatEnum.MAXDP, 4000);
	}

	/** 返回飞行时间 / Returns the fly time*/
	public Stat2 getFlyTime() {
		return getStat(StatEnum.FLY_TIME, CustomConfig.BASE_FLYTIME);
	}

	/** 返回 all speed / Returns the all speed */
	public Stat2 getAllSpeed() {
		return getStat(StatEnum.ALLSPEED, 7500);
	}

	/** 返回 attack speed / Returns the attack speed */
	@Override
	public Stat2 getAttackSpeed() {
		int base = 1500;
		Equipment equipment = owner.getEquipment();
		Item mainHandWeapon = equipment.getMainHandWeapon();
		if (mainHandWeapon != null) {
			base = mainHandWeapon.getItemTemplate().getWeaponStats().getAttackSpeed();
			Item offWeapon = owner.getEquipment().getOffHandWeapon();
			if (offWeapon != null) {
				base += offWeapon.getItemTemplate().getWeaponStats().getAttackSpeed() / 4;
			}
		}
		Stat2 aSpeed = getStat(StatEnum.ATTACK_SPEED, base);
		return aSpeed;
	}

	/** 返回 b casting time / Returns the b casting time */
	@Override
	public Stat2 getBCastingTime() {
		int base = 0;
		int casterClass = owner.getPlayerClass().getClassId();
		if (casterClass == 7 || // Sorcerer.
				casterClass == 8 || // Spirit-Master.
				casterClass == 16) { // Songweaver.
			base = 800;
		}
		return getStat(StatEnum.BOOST_CASTING_TIME, base);
	}

	/** 返回 concentration / Returns the concentration */
	@Override
	public Stat2 getConcentration() {
		int base = 0;
		int sorcerer1 = owner.getPlayerClass().getClassId();
		int spiritMaster1 = owner.getPlayerClass().getClassId();
		int HDEX = ((Player) owner).getGameStats().getStat(StatEnum.HDEX, 0).getCurrent();
		int ConcentrationCalculation = Math.round(471 * HDEX / (825.0F + HDEX));		
		if (sorcerer1 == 7) {
			base = 25;
		} else if (spiritMaster1 == 8 && owner.getLevel() >= 56) {
			base = 100;
		}
		Stat2 stat = getStat(StatEnum.CONCENTRATION, base);
		stat.addToBonus(ConcentrationCalculation);				
		return stat;
	}

	/** 返回 root resistance / Returns the root resistance */
	@Override
	public Stat2 getRootResistance() {
		int base = 0;
		int aethertech2 = owner.getPlayerClass().getClassId();
		if (aethertech2 == 13) {
			base = 200;
		}
		return getStat(StatEnum.ROOT_RESISTANCE, base);
	}

	/** 返回 snare resistance / Returns the snare resistance */
	@Override
	public Stat2 getSnareResistance() {
		int base = 0;
		int aethertech3 = owner.getPlayerClass().getClassId();
		if (aethertech3 == 13) {
			base = 200;
		}
		return getStat(StatEnum.SNARE_RESISTANCE, base);
	}

	/** 返回 bind resistance / Returns the bind resistance */
	@Override
	public Stat2 getBindResistance() {
		int base = 0;
		int aethertech4 = owner.getPlayerClass().getClassId();
		if (aethertech4 == 13) {
			base = 200;
		}
		return getStat(StatEnum.BIND_RESISTANCE, base);
	}

	/** 返回 fear resistance / Returns the fear resistance */
	@Override
	public Stat2 getFearResistance() {
		int base = 0;
		int aethertech5 = owner.getPlayerClass().getClassId();
		if (aethertech5 == 13) {
			base = -200;
		}
		return getStat(StatEnum.FEAR_RESISTANCE, base);
	}

	/** 返回 sleep resistance / Returns the sleep resistance */
	@Override
	public Stat2 getSleepResistance() {
		int base = 0;
		int aethertech6 = owner.getPlayerClass().getClassId();
		if (aethertech6 == 13) {
			base = -200;
		}
		return getStat(StatEnum.SLEEP_RESISTANCE, base);
	}

	/** 返回 p def / Returns the p def */
	@Override
	public Stat2 getPDef() {
		int base = 0;
		Stat2 stats = getStat(StatEnum.PHYSICAL_DEFENSE, base);
		int HSTR = ((Player) owner).getGameStats().getStat(StatEnum.HSTR, 0).getCurrent();
		int phyDefCalculation = Math.round(3440 * HSTR / (135.0F + HSTR));		
		int gunslinger = owner.getPlayerClass().getClassId();
		int aethertech = owner.getPlayerClass().getClassId();
		if (gunslinger == 14) {
			base = 100;
		} else if (aethertech == 13) {
			base = 350;
		}
		stats.addToBonus(phyDefCalculation);
		return stats;
	}

	/** 返回 m resist / Returns the m resist */
	@Override
	public Stat2 getMResist() {
		int base = 0;
		int assassin = owner.getPlayerClass().getClassId();
		if (assassin == 4 && owner.getLevel() >= 37) {
			base = 30;
		}
		Stat2 stat = getStat(StatEnum.MAGICAL_RESIST, base);
		int HWIL = ((Player) owner).getGameStats().getStat(StatEnum.HWIL, 0).getCurrent();
		int MResistCalculation = Math.round(2844 * HWIL / (825.0F + HWIL));
		stat.addToBonus(MResistCalculation);  
		return stat;
	}

	/** 返回 mb resist / Returns the mb resist */
	@Override
	public Stat2 getMBResist() {
		int base = 0;
		int cleric = owner.getPlayerClass().getClassId();
		int sorcerer2 = owner.getPlayerClass().getClassId();
		int spiritMaster2 = owner.getPlayerClass().getClassId();
		if (cleric == 10 && owner.getLevel() >= 60) {
			base = 140;
		}
		if (sorcerer2 == 7 && owner.getLevel() >= 60) {
			base = 180;
		}
		if (spiritMaster2 == 8 && owner.getLevel() >= 60) {
			base = 180;
		}
		
		Stat2 stat = getStat(StatEnum.MAGIC_SKILL_BOOST_RESIST, base);
		int HKNO = ((Player) owner).getGameStats().getStat(StatEnum.HKNO, 0).getCurrent();
		int MBResistCalculation = Math.round(1392 * HKNO / (129.0F + HKNO));
		stat.addToBonus(MBResistCalculation);
		return stat;
	}

	/** 返回 movement speed / Returns the movement speed */
	@Override
	public Stat2 getMovementSpeed() {
		Stat2 movementSpeed;
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		if (owner.isInPlayerMode(PlayerMode.RIDE)) {
			RideInfo ride = owner.ride;
			int runSpeed = (int) pst.getRunSpeed() * 1000;
			if (owner.isInState(CreatureState.FLYING)) {
				movementSpeed = new AdditionStat(StatEnum.FLY_SPEED, runSpeed, owner);
				movementSpeed.addToBonus((int) (ride.getFlySpeed() * 1000) - runSpeed);
			} else {
				float speed = owner.isInSprintMode() ? ride.getSprintSpeed() : ride.getMoveSpeed();
				movementSpeed = new AdditionStat(StatEnum.SPEED, runSpeed, owner);
				movementSpeed.addToBonus((int) (speed * 1000) - runSpeed);
			}
		} else if (owner.isInFlyingState()) {
			movementSpeed = getStat(StatEnum.FLY_SPEED, Math.round(pst.getFlySpeed() * 1000));
		} else if (owner.isInState(CreatureState.FLIGHT_TELEPORT) && !owner.isInState(CreatureState.RESTING)) {
			movementSpeed = getStat(StatEnum.SPEED, 12000);
		} else if (owner.isInState(CreatureState.WALKING)) {
			movementSpeed = getStat(StatEnum.SPEED, Math.round(pst.getWalkSpeed() * 1000));
		} else if (getAllSpeed().getBonus() != 0) {
			movementSpeed = getStat(StatEnum.SPEED, getAllSpeed().getCurrent());
		} else {
			movementSpeed = getStat(StatEnum.SPEED, Math.round(pst.getRunSpeed() * 1000));
		}
		return movementSpeed;
	}

	/** 返回攻击范围 / Returns the attack range*/
	@Override
	public Stat2 getAttackRange() {
		int base = 1500;
		Equipment equipment = owner.getEquipment();
		Item mainHandWeapon = equipment.getMainHandWeapon();
		Item offHandWeapon = equipment.getOffHandWeapon();
		if (mainHandWeapon != null) {
			base = mainHandWeapon.getItemTemplate().getWeaponStats().getAttackRange();
			if (!mainHandWeapon.getItemTemplate().isTwoHandWeapon() && mainHandWeapon != null && offHandWeapon != null
					&& offHandWeapon.getItemTemplate().getArmorType() != ArmorType.SHIELD) {
				if (mainHandWeapon.getItemTemplate().getWeaponStats().getAttackRange() != offHandWeapon
						.getItemTemplate().getWeaponStats().getAttackRange()) {
					if (mainHandWeapon.getItemTemplate().getWeaponType() == WeaponType.DAGGER_1H
							&& offHandWeapon.getItemTemplate().getWeaponType() == WeaponType.DAGGER_1H) {
						base = 1500;
					} else if (mainHandWeapon.getItemTemplate().getWeaponType() == WeaponType.DAGGER_1H
							&& offHandWeapon.getItemTemplate().getWeaponType() == WeaponType.SWORD_1H) {
						base = 1500;
					} else if (mainHandWeapon.getItemTemplate().getWeaponType() == WeaponType.SWORD_1H
							&& offHandWeapon.getItemTemplate().getWeaponType() == WeaponType.DAGGER_1H) {
						base = 1500;
					} else if (mainHandWeapon.getItemTemplate().getWeaponType() == WeaponType.DAGGER_1H
							&& offHandWeapon.getItemTemplate().getWeaponType() == WeaponType.MACE_1H) {
						base = 1500;
					} else if (mainHandWeapon.getItemTemplate().getWeaponType() == WeaponType.MACE_1H
							&& offHandWeapon.getItemTemplate().getWeaponType() == WeaponType.DAGGER_1H) {
						base = 1500;
					} else if (mainHandWeapon.getItemTemplate().getWeaponType() == WeaponType.MACE_1H
							&& offHandWeapon.getItemTemplate().getWeaponType() == WeaponType.SWORD_1H) {
						base = 1500;
					} else if (mainHandWeapon.getItemTemplate().getWeaponType() == WeaponType.MACE_1H
							&& offHandWeapon.getItemTemplate().getWeaponType() == WeaponType.MACE_1H) {
						base = 1500;
					} else if (mainHandWeapon.getItemTemplate().getWeaponType() == WeaponType.SWORD_1H
							&& offHandWeapon.getItemTemplate().getWeaponType() == WeaponType.MACE_1H) {
						base = 1500;
					} else {
						if (mainHandWeapon != null && offHandWeapon != null
								&& offHandWeapon.getItemTemplate().getArmorType() != ArmorType.SHIELD) {
							base = mainHandWeapon.getItemTemplate().getWeaponStats().getAttackRange();
							log.info(I18n.get("log.69a559ecbbb6", owner.getObjectId(), mainHandWeapon.getItemTemplate().getItemType(),
									offHandWeapon.getItemTemplate().getItemType()));
						}
					}
				}
			}
		}
		return getStat(StatEnum.ATTACK_RANGE, base);
	}

	/** 返回 m def / Returns the m def */
	@Override
	public Stat2 getMDef() {
		return getStat(StatEnum.MAGICAL_DEFEND, 0);
	}

	/** 返回 power / Returns the power */
	@Override
	public Stat2 getPower() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		return getStat(StatEnum.POWER, pst.getPower());
	}

	/** 返回 health / Returns the health */
	@Override
	public Stat2 getHealth() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		return getStat(StatEnum.HEALTH, pst.getHealth());
	}

	/** 返回 accuracy / Returns the accuracy */
	@Override
	public Stat2 getAccuracy() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		return getStat(StatEnum.ACCURACY, pst.getAccuracy());
	}

	/** 返回 agility / Returns the agility */
	@Override
	public Stat2 getAgility() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		return getStat(StatEnum.AGILITY, pst.getAgility());
	}

	/** 返回 knowledge / Returns the knowledge */
	@Override
	public Stat2 getKnowledge() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		return getStat(StatEnum.KNOWLEDGE, pst.getKnowledge());
	}

	/** 返回 will / Returns the will */
	@Override
	public Stat2 getWill() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		return getStat(StatEnum.WILL, pst.getWill());
	}

	/** 返回 evasion / Returns the evasion */
	@Override
	public Stat2 getEvasion() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		Stat2 stat = getStat(StatEnum.EVASION, pst.getEvasion());
		int HDEX = ((Player) owner).getGameStats().getStat(StatEnum.HDEX, 0).getCurrent();
		int EvasionCalculation = Math.round(3140 * HDEX / (800.0F + HDEX));
		stat.addToBonus(EvasionCalculation);
		return stat;
	}

	/** 返回 parry / Returns the parry */
	@Override
	public Stat2 getParry() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		int base = pst.getParry();
		Item mainHandWeapon = owner.getEquipment().getMainHandWeapon();
		if (mainHandWeapon != null) {
			base += mainHandWeapon.getItemTemplate().getWeaponStats().getParry();
		}
		Stat2 stat = getStat(StatEnum.PARRY, base);
		int HDEX = ((Player) owner).getGameStats().getStat(StatEnum.HDEX, 0).getCurrent();
		int ParryCalculation = Math.round(3112 * HDEX / (550.0F + HDEX));
		stat.addToBonus(ParryCalculation);
		return stat;
	}

	/** 返回黑名单 / Returns the block */
	@Override
	public Stat2 getBlock() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		Stat2 stat = getStat(StatEnum.BLOCK, pst.getBlock());
		int HVIT = ((Player) owner).getGameStats().getStat(StatEnum.HVIT, 0).getCurrent();
		int BlockCalculation = Math.round(4740 * HVIT / (825.0F + HVIT));
		stat.addToBonus(BlockCalculation);
		return stat;
	}

	/** 返回 main hand p attack / Returns the main hand p attack */
	@Override
	public Stat2 getMainHandPAttack() {
		return getMainHandPAttack(new CalculationType[0]);
	}

	/** 返回 main hand p attack / Returns the main hand p attack */
	@Override
	public Stat2 getMainHandPAttack(CalculationType... calculationTypes) {
		calculationTypes = ArrayUtils.add(calculationTypes, CalculationType.MAIN_HAND);
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		float base = pst.getMainHandAttack();
		Equipment equipment = owner.getEquipment();
		Item mainHandWeapon = equipment.getMainHandWeapon();
		if (mainHandWeapon != null) {
			if (mainHandWeapon.getItemTemplate().getAttackType().isMagical()) {
				return new AdditionStat(StatEnum.MAIN_HAND_POWER, 0, owner);
			}
			if (ArrayUtils.contains(calculationTypes, CalculationType.DISPLAY)) {
				base = mainHandWeapon.getItemTemplate().getWeaponStats().getMeanDamage();
			} else {
				base = Rnd.get(mainHandWeapon.getItemTemplate().getWeaponStats().getMinDamage(),
						mainHandWeapon.getItemTemplate().getWeaponStats().getMaxDamage());
			}
			if (ArrayUtils.contains(calculationTypes, CalculationType.APPLY_POWER_SHARD_DAMAGE)) {
				base += getPowerShardDamage(true, ArrayUtils.contains(calculationTypes, CalculationType.REMOVE_POWER_SHARD));
			}
		}
		Stat2 stat = getStat(StatEnum.PHYSICAL_ATTACK, base, calculationTypes);
		int HSTR = ((Player) owner).getGameStats().getStat(StatEnum.HSTR, 0).getCurrent();
		int PhyAtkCalculation = Math.round(1256 * HSTR / (825.0F + HSTR));
		stat.addToBonus(PhyAtkCalculation);		
		return getStat(StatEnum.MAIN_HAND_POWER, stat, calculationTypes);
	}

	/** 返回 off hand p attack / Returns the off hand p attack */
	public Stat2 getOffHandPAttack() {
		return getOffHandPAttack(new CalculationType[0]);
	}

	/** 返回 off hand p attack / Returns the off hand p attack */
	public Stat2 getOffHandPAttack(CalculationType... calculationTypes) {
		Equipment equipment = owner.getEquipment();
		Item offHandWeapon = equipment.getOffHandWeapon();
		if (offHandWeapon != null && offHandWeapon != equipment.getMainHandWeapon() && offHandWeapon.getItemTemplate().isWeapon()
				&& offHandWeapon.getItemTemplate().getArmorType() != ArmorType.SHIELD) {
			calculationTypes = ArrayUtils.add(calculationTypes, CalculationType.OFF_HAND);
			float base;
			if (ArrayUtils.contains(calculationTypes, CalculationType.DISPLAY)) {
				base = offHandWeapon.getItemTemplate().getWeaponStats().getMeanDamage();
			} else {
				base = Rnd.get(offHandWeapon.getItemTemplate().getWeaponStats().getMinDamage(),
						offHandWeapon.getItemTemplate().getWeaponStats().getMaxDamage());
			}
			if (ArrayUtils.contains(calculationTypes, CalculationType.APPLY_POWER_SHARD_DAMAGE)) {
				base += getPowerShardDamage(false, ArrayUtils.contains(calculationTypes, CalculationType.REMOVE_POWER_SHARD));
			}
			Stat2 stat = getStat(StatEnum.PHYSICAL_ATTACK, base, calculationTypes);
			if (ArrayUtils.contains(calculationTypes, CalculationType.DISPLAY)) {
				stat.setBaseRate(stat.getBaseRate() * getOffHandDamageRatio());
				stat.setBonusRate(stat.getBonusRate() * getOffHandDamageRatio());
			}
			int HSTR = ((Player) owner).getGameStats().getStat(StatEnum.HSTR, 0).getCurrent();
			int PhyAtkCalculation = Math.round(1256 * HSTR / (825.0F + HSTR));
			stat.addToBonus(PhyAtkCalculation);			
			return getStat(StatEnum.OFF_HAND_POWER, stat, calculationTypes);
		}
		return new AdditionStat(StatEnum.OFF_HAND_POWER, 0, owner);
	}

	/** 返回 main hand p critical / Returns the main hand p critical */
	@Override
	public Stat2 getMainHandPCritical() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), (owner.getLevel()));
		int base = pst.getMainHandCritRate();
		Equipment equipment = owner.getEquipment();
		Item mainHandWeapon = equipment.getMainHandWeapon();
		if (mainHandWeapon != null) {
			base = mainHandWeapon.getItemTemplate().getWeaponStats().getPhysicalCritical();
		}
		Stat2 stat = getStat(StatEnum.PHYSICAL_CRITICAL, base);
		int HAGI = ((Player) owner).getGameStats().getStat(StatEnum.HAGI, 0).getCurrent();
		int PhyCriticalCalculation = Math.round(3160 * HAGI / (825.0F + HAGI));
		stat.addToBonus(PhyCriticalCalculation);  
		return stat;
	}

	/** 返回 off hand p critical / Returns the off hand p critical */
	public Stat2 getOffHandPCritical() {
		Equipment equipment = owner.getEquipment();
		Item offHandWeapon = equipment.getOffHandWeapon();
		if (offHandWeapon != null && offHandWeapon.getItemTemplate().isWeapon()) {
			int base = offHandWeapon.getItemTemplate().getWeaponStats().getPhysicalCritical();
			Stat2 stat = getStat(StatEnum.PHYSICAL_CRITICAL, base);
			int HAGI = ((Player) owner).getGameStats().getStat(StatEnum.HAGI, 0).getCurrent();
			int PhyCriticalCalculation = Math.round(3160 * HAGI / (825.0F + HAGI));
			stat.addToBonus(PhyCriticalCalculation);
			return stat;
		}
		return new AdditionStat(StatEnum.OFF_HAND_CRITICAL, 0, owner);
	}

	/** 返回 main hand p accuracy / Returns the main hand p accuracy */
	@Override
	public Stat2 getMainHandPAccuracy() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		int base = pst.getMainHandAccuracy();
		Equipment equipment = owner.getEquipment();
		Item mainHandWeapon = equipment.getMainHandWeapon();
		if (mainHandWeapon != null) {
			base += mainHandWeapon.getItemTemplate().getWeaponStats().getPhysicalAccuracy();
		}
		Stat2 stat = getStat(StatEnum.PHYSICAL_ACCURACY, base);
		int HAGI = ((Player) owner).getGameStats().getStat(StatEnum.HAGI, 0).getCurrent();
		int PhyAccuracyCalculation = Math.round(4020 * HAGI / (510.0F + HAGI));
		stat.addToBonus(PhyAccuracyCalculation);
		return stat;
	}

	/** 返回 off hand p accuracy / Returns the off hand p accuracy */
	public Stat2 getOffHandPAccuracy() {
		Equipment equipment = owner.getEquipment();
		Item offHandWeapon = equipment.getOffHandWeapon();
		if (offHandWeapon != null && offHandWeapon.getItemTemplate().isWeapon()) {
			PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(),
					owner.getLevel());
			int base = pst.getMainHandAccuracy();
			base += offHandWeapon.getItemTemplate().getWeaponStats().getPhysicalAccuracy();
			
			Stat2 stat = getStat(StatEnum.PHYSICAL_ACCURACY, base);
			int HAGI = ((Player) owner).getGameStats().getStat(StatEnum.HAGI, 0).getCurrent();
			int PhyAccuracyCalculation = Math.round(4020 * HAGI / (510.0F + HAGI));
			stat.addToBonus(PhyAccuracyCalculation);
			return stat;
		}
		return new AdditionStat(StatEnum.OFF_HAND_ACCURACY, 0, owner);
	}

	/** 返回 m attack / Returns the m attack */
	@Override
	public Stat2 getMAttack() {
		int base;
		Equipment equipment = owner.getEquipment();
		Item mainHandWeapon = equipment.getMainHandWeapon();
		if (mainHandWeapon != null) {
			if (!mainHandWeapon.getItemTemplate().getAttackType().isMagical()) {
				return new AdditionStat(StatEnum.MAGICAL_ATTACK, 0, owner);
			}
			base = mainHandWeapon.getItemTemplate().getWeaponStats().getMeanDamage();
		} else {
			base = Rnd.get(16, 20);
		}
		return getStat(StatEnum.MAGICAL_ATTACK, base);
	}

	/** 返回 main hand m attack / Returns the main hand m attack */
	@Override
	public Stat2 getMainHandMAttack() {
		return getMainHandMAttack(new CalculationType[0]);
	}

	/** 返回 main hand m attack / Returns the main hand m attack */
	@Override
	public Stat2 getMainHandMAttack(CalculationType... calculationTypes) {
		calculationTypes = ArrayUtils.add(calculationTypes, CalculationType.MAIN_HAND);
		float base = 0;
		Equipment equipment = owner.getEquipment();
		Item mainHandWeapon = equipment.getMainHandWeapon();
		if (mainHandWeapon != null) {
			if (!mainHandWeapon.getItemTemplate().getAttackType().isMagical()) {
				return new AdditionStat(StatEnum.MAIN_HAND_MAGICAL_POWER, 0, owner);
			}
			base = mainHandWeapon.getItemTemplate().getWeaponStats().getMeanDamage();
			if (ArrayUtils.contains(calculationTypes, CalculationType.APPLY_POWER_SHARD_DAMAGE)) {
				base += getPowerShardDamage(true, ArrayUtils.contains(calculationTypes, CalculationType.REMOVE_POWER_SHARD));
			}
		}
		Stat2 stat = getStat(StatEnum.MAGICAL_ATTACK, base, calculationTypes);
		return getStat(StatEnum.MAIN_HAND_MAGICAL_POWER, stat, calculationTypes);
	}

	/** 返回 off hand m attack / Returns the off hand m attack */
	@Override
	public Stat2 getOffHandMAttack() {
		return getOffHandMAttack(new CalculationType[0]);
	}

	/** 返回 off hand m attack / Returns the off hand m attack */
	@Override
	public Stat2 getOffHandMAttack(CalculationType... calculationTypes) {
		float base = 0;
		Equipment equipment = owner.getEquipment();
		Item offHandWeapon = equipment.getOffHandWeapon();
		if (offHandWeapon != null && offHandWeapon != equipment.getMainHandWeapon() && offHandWeapon.getItemTemplate().isWeapon()
				&& offHandWeapon.getItemTemplate().getArmorType() != ArmorType.SHIELD) {
			calculationTypes = ArrayUtils.add(calculationTypes, CalculationType.OFF_HAND);
			base = offHandWeapon.getItemTemplate().getWeaponStats().getMeanDamage();
			if (ArrayUtils.contains(calculationTypes, CalculationType.APPLY_POWER_SHARD_DAMAGE)) {
				base += getPowerShardDamage(false, ArrayUtils.contains(calculationTypes, CalculationType.REMOVE_POWER_SHARD));
			}
			Stat2 stat = getStat(StatEnum.MAGICAL_ATTACK, base, calculationTypes);
			if (ArrayUtils.contains(calculationTypes, CalculationType.DISPLAY)) {
				stat.setBaseRate(stat.getBaseRate() * getOffHandDamageRatio());
				stat.setBonusRate(stat.getBonusRate() * getOffHandDamageRatio());
			}
			return getStat(StatEnum.OFF_HAND_MAGICAL_POWER, stat, calculationTypes);
		}
		return new AdditionStat(StatEnum.OFF_HAND_MAGICAL_POWER, 0, owner);
	}

	/** 返回 m boost / Returns the m boost */
	@Override
	public Stat2 getMBoost() {
		int base = 0;
		Item mainHandWeapon = owner.getEquipment().getMainHandWeapon();
		if (mainHandWeapon != null) {
			base += mainHandWeapon.getItemTemplate().getWeaponStats().getBoostMagicalSkill();
		}
		
		Stat2 stat = getStat(StatEnum.BOOST_MAGICAL_SKILL, base);
		int HKNO = ((Player) owner).getGameStats().getStat(StatEnum.HKNO, 0).getCurrent();
		int MBoostCalculation = Math.round(5056 * HKNO / (825.0F + HKNO));
		stat.addToBonus(MBoostCalculation);
		return stat;
	}

	/** 返回 m accuracy / Returns the m accuracy */
	@Override
	public Stat2 getMAccuracy() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		int base = pst.getMagicAccuracy();
		Item mainHandWeapon = owner.getEquipment().getMainHandWeapon();
		if (mainHandWeapon != null) {
			base += mainHandWeapon.getItemTemplate().getWeaponStats().getMagicalAccuracy();
		}
		Stat2 stat = getStat(StatEnum.MAGICAL_ACCURACY, base);
		int HAGI = ((Player) owner).getGameStats().getStat(StatEnum.HAGI, 0).getCurrent();
		int MAccuracyCalculation = Math.round(2286 * HAGI / (376.0F + HAGI));
		stat.addToBonus(MAccuracyCalculation);
		return stat;
	}

	/** 返回 m critical / Returns the m critical */
	@Override
	public Stat2 getMCritical() {
		PlayerStatsTemplate pst = DataManager.PLAYER_STATS_DATA.getTemplate(owner.getPlayerClass(), owner.getLevel());
		int base = pst.getMCritical();
		
		Stat2 stat = getStat(StatEnum.MAGICAL_CRITICAL, base);
		int HKNO = ((Player) owner).getGameStats().getStat(StatEnum.HKNO, 0).getCurrent();
		int MCriticalCalculation = Math.round(1884 * HKNO / (825.0F + HKNO));
		stat.addToBonus(MCriticalCalculation);
		return stat;
	}

	/** 返回 hp regen rate / Returns the hp regen rate */
	@Override
	public Stat2 getHpRegenRate() {
		int base = owner.getLevel() + 3;
		if (owner.isInState(CreatureState.RESTING)) {
			base *= 8;
		}
		base *= getHealth().getCurrent() / 100f;
		Stat2 stat = getStat(StatEnum.REGEN_HP, base);
		int HVIT = ((Player) owner).getGameStats().getStat(StatEnum.HVIT, 0).getCurrent();
		int RegenHpCalculation = Math.round(316 * HVIT / (825.0F + HVIT));
		stat.addToBonus(RegenHpCalculation);
		return stat;
	}

	/** 返回 mp regen rate / Returns the mp regen rate */
	@Override
	public Stat2 getMpRegenRate() {
		int base = owner.getLevel() + 8;
		if (owner.isInState(CreatureState.RESTING)) {
			base *= 8;
		}
		base *= getWill().getCurrent() / 100f;
		Stat2 stat = getStat(StatEnum.REGEN_MP, base);
		int HWIL = ((Player) owner).getGameStats().getStat(StatEnum.HWIL, 0).getCurrent();
		int RegenMpCalculation = Math.round(158 * HWIL / (825.0F + HWIL));
		stat.addToBonus(RegenMpCalculation);
		return stat;
	}

	/** 更新属性信息。 / Update stat info. */
	@Override
	public void updateStatInfo() {
		PacketSendUtility.sendPacket(owner, new SM_STATS_INFO(owner));
	}

	/** 更新 speed info / Update speed info */
	@Override
	public void updateSpeedInfo() {
		PacketSendUtility.broadcastPacket(owner, new SM_EMOTION(owner, EmotionType.START_EMOTE2, 0, 0), true);
	}

	private int getPowerShardDamage(boolean mainHand, boolean removePowerShards) {
		if (!owner.isInState(CreatureState.POWERSHARD)) {
			return 0;
		}
		Equipment equipment = owner.getEquipment();
		Item weapon = mainHand ? equipment.getMainHandWeapon() : equipment.getOffHandWeapon();
		if (weapon == null || weapon.getItemTemplate().getArmorType() == ArmorType.SHIELD) {
			return 0;
		}
		Item firstShard = equipment.getMainHandPowerShard();
		Item secondShard = equipment.getOffHandPowerShard();
		int damage = 0;
		if (mainHand) {
			if (firstShard != null) {
				damage += firstShard.getItemTemplate().getWeaponBoost();
				if (removePowerShards) {
					equipment.usePowerShard(firstShard, 1);
				}
			}
			if (weapon.getItemTemplate().isTwoHandWeapon() && secondShard != null) {
				damage += secondShard.getItemTemplate().getWeaponBoost();
				if (removePowerShards) {
					equipment.usePowerShard(secondShard, 1);
				}
			}
		} else if (secondShard != null) {
			damage += secondShard.getItemTemplate().getWeaponBoost();
			if (removePowerShards) {
				equipment.usePowerShard(secondShard, 1);
			}
		}
		return damage;
	}

	/** 返回 skill efficiency / Returns the skill efficiency */
	public float getSkillEfficiency() {
		return skillEfficiency;
	}

	/** 返回最大伤害概率 / Returns the max damage chance*/
	public int getMaxDamageChance() {
		return maxDamageChance;
	}

	/** 返回 min damage ratio / Returns the min damage ratio */
	public float getMinDamageRatio() {
		return minDamageRatio;
	}

	/** 设置 skill efficiency / Sets the skill efficiency */
	public void setSkillEfficiency(float skillEfficiency) {
		this.skillEfficiency = skillEfficiency;
	}

	/** 设置最大伤害概率 / Sets the max damage chance*/
	public void setMaxDamageChance(int maxDamageChance) {
		this.maxDamageChance = maxDamageChance;
	}

	/** 设置 min damage ratio / Sets the min damage ratio */
	public void setMinDamageRatio(float minDamageRatio) {
		this.minDamageRatio = minDamageRatio;
	}

	/** 返回 off hand damage ratio / Returns the off hand damage ratio */
	public float getOffHandDamageRatio() {
		return getMinDamageRatio() * (1 - getMaxDamageChance() / 1000f) + getMaxDamageChance() / 1000f;
	}
}
