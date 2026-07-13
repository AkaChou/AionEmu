package com.aionemu.gameserver.services;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.dataholders.MotionData;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.WeaponType;
import com.aionemu.gameserver.skillengine.model.Motion;
import com.aionemu.gameserver.skillengine.model.MotionTime;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.skillengine.model.Times;
import com.aionemu.gameserver.skillengine.model.WeaponTypeWrapper;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 技能动作时间采集服务：记录客户端动作耗时，支持 SQL 持久化与 XML 导出，用于校准技能动作时间表。
 * Skill motion timing service: records client motion times, supports SQL persistence and XML export for calibrating skill motion tables.
 *
 * @author kecimis
 */
@Slf4j
public class MotionLoggingService {

	private static volatile ObjectProvider<MotionLoggingService> instanceProvider;

	private Map<String, MotionLog> motionsMap = new LinkedHashMap<String, MotionLog>();

	private boolean advancedLog = false;

	private boolean started = false;

	/**
	 * 获取服务单例（优先 Spring 提供者）。
	 * Returns the service singleton (preferring the Spring provider).
	 *
	 * service instance
	 */
	public static final MotionLoggingService getInstance() {
		ObjectProvider<MotionLoggingService> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 注入 Spring 的实例提供者。
	 * Injects the Spring instance provider.
	 *
	 * @param provider 实例提供者 / instance provider
	 */
	public static void setInstanceProvider(ObjectProvider<MotionLoggingService> provider) {
		instanceProvider = provider;
	}

	/**
	 * 启动动作日志采集。
	 * Starts motion logging collection.
	 */
	public void start() {
		if (started) {
			return;
		}
		this.started = true;
		// 从 SQL 加载数据 / load data from sql
		this.loadFromSql();
	}

	/**
	 * 记录玩家某次技能动作的客户端时间与距离。
	 * Logs client time and distance for a player skill motion.
	 *
	 * 玩家 / player
	 * @param sk 技能模板 / skill template
	 * @param clientTime 客户端时间 / client time
	 * distance
	 */
	public void logTime(Player player, SkillTemplate sk, int clientTime, double distance) {
		int currentAttackSpeed = 0;
		if (!started) {
			return;
		}
		if (sk == null) {
			return;
		}
		if (player.getEquipment().getMainHandWeaponType() == null) {
			return;
		}
		Motion motion = sk.getMotion();

		if (motion == null) {
			return;
		}
		currentAttackSpeed = player.getGameStats().getAttackSpeed().getCurrent();

		int skillId = sk.getSkillId();
		WeaponType mainHandWeapon = player.getEquipment().getMainHandWeaponType();
		WeaponType offHandWeapon = player.getEquipment().getOffHandWeaponType();
		String motionName = motion.getName();
		// clientTime 由客户端发送 / clientTime is send from client
		int baseTime = clientTime;// adjusted time

		if (motion.getInstantSkill()) {
			PacketSendUtility.sendMessage(player, "Skill: " + skillId + " is instant");
			return;
		} else if (clientTime == 0) {
			PacketSendUtility.sendMessage(player, "ClientTime is 0 for skill: " + skillId);
			return;
		}
		if (motion.getName() == null) {
			return;
		}
		long ammoTime = 0;
		if (sk.getAmmoSpeed() != 0) {
			ammoTime = Math.round(distance / sk.getAmmoSpeed() * 1000);// checked with client
		}
		// 按弹药速度调整 / adjusting with ammospeed
		baseTime -= ammoTime;

		// 若播放速度不是 100 则调整 clientTime / adjust clientTime if play speed is not 100
		if (motion.getSpeed() != 100) {
			baseTime /= motion.getSpeed();
			baseTime *= 100;
		}

		// 日志 / logging
		if (advancedLog) {
			PacketSendUtility.sendMessage(player, "skillId: " + sk.getSkillId() + " motionName: " + motionName);
			PacketSendUtility.sendMessage(player, "attackSpeed: " + currentAttackSpeed + " mainHand: "
					+ mainHandWeapon.toString() + " isDual: " + (offHandWeapon != null));
			PacketSendUtility.sendMessage(player,
					"clientTime: " + clientTime + " baseTime: " + baseTime + " playSpeed: " + motion.getSpeed());
			PacketSendUtility.sendMessage(player,
					"ammoTime: " + ammoTime + " ammoSpeed: " + sk.getAmmoSpeed() + " distance: " + distance);
			PacketSendUtility.sendMessage(player, "-------------------");
		} else {
			PacketSendUtility.sendMessage(player,
					"motionName: " + motionName + " clientTime: " + clientTime + " baseTime: " + baseTime);
		}
		Race race = player.getRace();
		Gender gender = player.getGender();

		// create WeaponTypeWrapper
		WeaponTypeWrapper weapon = new WeaponTypeWrapper(mainHandWeapon, offHandWeapon);
		// 检查是否存在 / check if its present
		if (this.isPresent(motionName, weapon, skillId, currentAttackSpeed, race, gender)) {
			log.info(I18n.get("log.a4a1d59f05a4", motionName, (offHandWeapon != null ? "dual" : mainHandWeapon.toString()), skillId, currentAttackSpeed, baseTime, this.getTime(motionName, weapon, skillId, currentAttackSpeed, race, gender)));
			PacketSendUtility.sendMessage(player, "Its already stored. storedTime: "
					+ this.getTime(motionName, weapon, skillId, currentAttackSpeed, race, gender));
			return;
		}

		// 添加时间 / addtime
		if (this.addTime(motionName, weapon, skillId, currentAttackSpeed, race, gender, baseTime)) {
			PacketSendUtility.sendMessage(player,
					"BaseTime: " + baseTime + " for motion: " + motionName + " was added.");
		} else {
			PacketSendUtility.sendMessage(player,
					"Couldnt add baseTime: " + baseTime + " for motion: " + motionName + "!");
		}
	}

	/**
	 * 生成分析文件（当前为空实现）。
	 * Creates analysis files (currently a no-op).
	 */
	public void createAnalyzeFiles() {

	}

	/**
	 * 汇总动作数据并导出最终 XML。
	 * Aggregates motion data and exports the final XML.
	 */
	public void createFinalFile() {
		MotionData motionData = new MotionData();
		List<MotionTime> motionTimes = motionData.getMotionTimes();

		// 创建结果 / create results
		TreeMap<String, List<WeaponTime>> results = new TreeMap<String, List<WeaponTime>>();
		for (Entry<String, MotionLog> entry : motionsMap.entrySet()) {
			WeaponTime weaponTimeAm = new WeaponTime(Race.ASMODIANS, Gender.MALE);
			WeaponTime weaponTimeAf = new WeaponTime(Race.ASMODIANS, Gender.FEMALE);
			WeaponTime weaponTimeEm = new WeaponTime(Race.ELYOS, Gender.MALE);
			WeaponTime weaponTimeEf = new WeaponTime(Race.ELYOS, Gender.FEMALE);
			if (entry.getValue() == null) {
				continue;
			}
			// loop through weaponType
			for (Entry<WeaponTypeWrapper, List<SkillTime>> entry2 : entry.getValue().getMotionLog().entrySet()) {
				WeaponTypeWrapper weapon = entry2.getKey();

				if (entry2.getValue() == null) {
					continue;
				}
				for (SkillTime st : entry2.getValue()) {
					switch (st.getRace()) {
					case ASMODIANS:
						if (st.getGender() == Gender.MALE) {
							weaponTimeAm.add(weapon,
									this.recalculate("base", weapon, st.getAttackSpeed(), st.getClientTime()));
						} else {
							weaponTimeAf.add(weapon,
									this.recalculate("base", weapon, st.getAttackSpeed(), st.getClientTime()));
						}
						break;
					case ELYOS:
						if (st.getGender() == Gender.MALE) {
							weaponTimeEm.add(weapon,
									this.recalculate("base", weapon, st.getAttackSpeed(), st.getClientTime()));
						} else {
							weaponTimeEf.add(weapon,
									this.recalculate("base", weapon, st.getAttackSpeed(), st.getClientTime()));
						}
						break;
					}
				}
				List<WeaponTime> weaponTimes = new ArrayList<WeaponTime>(4);
				weaponTimes.add(weaponTimeAm);
				weaponTimes.add(weaponTimeAf);
				weaponTimes.add(weaponTimeEm);
				weaponTimes.add(weaponTimeEf);
				// 填充结果 / fill results
				results.put(entry.getKey(), weaponTimes);
			}
		}

		for (Entry<String, List<WeaponTime>> entry : results.entrySet()) {
			Set<WeaponTypeWrapper> listofWeapons = new TreeSet<WeaponTypeWrapper>();
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.BOOK_2H, null));
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.BOW, null));
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.DAGGER_1H, null));
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.MACE_1H, null));
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.ORB_2H, null));
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.POLEARM_2H, null));
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.STAFF_2H, null));
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.SWORD_1H, null));
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.SWORD_2H, null));
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.SWORD_1H, WeaponType.SWORD_1H));
			// 4.3
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.GUN_1H, null));
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.CANNON_2H, null));
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.HARP_2H, null));
			// 4.5
			listofWeapons.add(new WeaponTypeWrapper(WeaponType.KEYBLADE_2H, null));

			// create MotionTime
			MotionTime motion = new MotionTime();
			motion.setName(entry.getKey());

			for (WeaponTime wt : entry.getValue()) {
				// 处理数值 / process values
				TreeMap<WeaponTypeWrapper, Integer> map = wt.process();

				StringBuilder sb = new StringBuilder();
				boolean first = true;
				// 创建时间 / create time
				for (WeaponTypeWrapper weapon : listofWeapons) {
					if (first) {
						sb.append((map.containsKey(weapon) ? map.get(weapon) : "0"));
						first = false;
					} else {
						sb.append("," + (map.containsKey(weapon) ? map.get(weapon) : "0"));
					}
				}

				Times times = new Times();
				times.setTimes(sb.toString());
				switch (wt.getRace()) {
				case ASMODIANS:
					if (wt.getGender() == Gender.MALE)
						motion.setAm(times);
					else
						motion.setAf(times);
					break;
				case ELYOS:
					if (wt.getGender() == Gender.MALE)
						motion.setEm(times);
					else
						motion.setEf(times);
					break;
				}
			}
			motionTimes.add(motion);
		}
		// 编组最终 XML 文件 / marshall the final xml file
		marshallFile(motionData, "compact/skills/new_motion_times.xml");
	}

	/**
	 * 将模板对象 JAXB 序列化到文件。
	 * Marshals a template object to a file via JAXB.
	 *
	 * template object
	 * @param file 输出路径 / output path
	 */
	public static void marshallFile(Object templates, String file) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(templates.getClass());
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(templates, new FileOutputStream(Config.definitionFile(file)));
		} catch (JAXBException e) {
			log.error(I18n.get("log.a34c1da0cbcb", file, e));
		} catch (FileNotFoundException e) {
			log.error(I18n.get("log.23cf814a22a6", file, e));
		}
	}

	/**
	 * 按基础值/上限/给定攻速重算时间。 / method used to recalculate time to base, cap or given attackspeed.
	 */
	private int recalculate(String method, WeaponTypeWrapper weapon, int attackSpeed, int time) {
		int finalTime = 0;
		TreeMap<WeaponType, float[]> list = new TreeMap<WeaponType, float[]>();

		float mace1h[] = { 750f, 1500f };
		list.put(WeaponType.MACE_1H, mace1h);

		float sword1h[] = { 700f, 1400f };
		list.put(WeaponType.SWORD_1H, sword1h);

		float gun1h[] = { 900f, 1800f };
		list.put(WeaponType.GUN_1H, gun1h);

		float staff2h[] = { 1000f, 2000f };
		list.put(WeaponType.STAFF_2H, staff2h);

		float dagger1h[] = { 600f, 1200f };
		list.put(WeaponType.DAGGER_1H, dagger1h);

		float book_orb[] = { 1100f, 2200f };
		list.put(WeaponType.BOOK_2H, book_orb);
		list.put(WeaponType.ORB_2H, book_orb);

		float polearm_cannon[] = { 1400f, 2800f };
		list.put(WeaponType.POLEARM_2H, polearm_cannon);
		list.put(WeaponType.CANNON_2H, polearm_cannon);

		float sword_bow_keyblade_harp[] = { 1200f, 2400f };
		list.put(WeaponType.BOW, sword_bow_keyblade_harp);
		list.put(WeaponType.SWORD_2H, sword_bow_keyblade_harp);
		list.put(WeaponType.HARP_2H, sword_bow_keyblade_harp);
		list.put(WeaponType.KEYBLADE_2H, sword_bow_keyblade_harp);

		float speed = 0;
		if (method.equalsIgnoreCase("base")) {
			speed = list.get(weapon.getMainHand())[1];
			if (weapon.getOffHand() != null) {
				speed += (list.get(weapon.getOffHand())[1] * 0.25);
			}
		} else if (method.equalsIgnoreCase("cap")) {
			speed = list.get(weapon.getMainHand())[0];
			if (weapon.getOffHand() != null) {
				speed += (list.get(weapon.getOffHand())[0] * 0.25);
			}
		} else {
			try {
				speed = Float.parseFloat(method);
			} catch (Exception e) {
				// 日志 / log
			}
		}
		finalTime = Math.round((float) time / (float) attackSpeed * speed);

		return finalTime;
	}

	// 保存到 SQL / save to sql
	/**
	 * 将采集到的动作时间写入数据库。
	 * Persists collected motion times to the database.
	 */
	public void saveToSql() {
		Connection con = null;

		final String INSERT_QUERY = "INSERT INTO skill_motions (motion_name, weapon_type, off_weapon_type, skill_id, attack_speed, race, gender, time) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE motion_name = ?";

		try {
			con = DatabaseFactory.getConnection();
			PreparedStatement stmt = con.prepareStatement(INSERT_QUERY);
			for (Entry<String, MotionLog> entry : motionsMap.entrySet()) {
				String motionName = entry.getKey();
				// set motion_name
				stmt.setString(1, motionName);
				stmt.setString(9, motionName);
				if (entry.getValue() == null) {
					continue;
				}
				// loop through weaponType
				for (Entry<WeaponTypeWrapper, List<SkillTime>> entry2 : entry.getValue().getMotionLog().entrySet()) {
					String weaponType = (entry2.getKey().getMainHand() != null
							? entry2.getKey().getMainHand().toString()
							: "null");
					String offWeaponType = (entry2.getKey().getOffHand() != null
							? entry2.getKey().getOffHand().toString()
							: "null");
					// set weapon_type
					stmt.setString(2, weaponType);
					stmt.setString(3, offWeaponType);

					if (entry2.getValue() == null) {
						continue;
					}
					// sort by skillId
					Collections.sort(entry2.getValue());
					for (SkillTime st : entry2.getValue()) {
						stmt.setInt(4, st.getSkillId());
						stmt.setInt(5, st.getAttackSpeed());
						stmt.setString(6, st.getRace().toString());
						stmt.setString(7, st.getGender().toString());
						stmt.setInt(8, st.getClientTime());
						stmt.execute();
					}
				}
			}
			stmt.close();
		} catch (SQLException e) {
			log.error(I18n.get("log.59fa1e0145de", e));
		} finally {
			DatabaseFactory.close(con);
		}
	}

	// 从 SQL 加载 / load from sql
	/**
	 * 从数据库加载动作时间。
	 * Loads motion times from the database.
	 */
	public void loadFromSql() {
		Connection con = null;

		final String SELECT_QUERY = "SELECT * FROM skill_motions";
		try {
			con = DatabaseFactory.getConnection();
			PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);

			ResultSet resultSet = stmt.executeQuery();

			while (resultSet.next()) {
				String motionName = resultSet.getString("motion_name");
				WeaponType mainHandWeapon = WeaponType.valueOf(resultSet.getString("weapon_type"));
				WeaponType offHandWeapon = (resultSet.getString("off_weapon_type").contains("null") ? null
						: WeaponType.valueOf(resultSet.getString("off_weapon_type")));
				int skillId = resultSet.getInt("skill_id");
				int attackSpeed = resultSet.getInt("attack_speed");
				int time = resultSet.getInt("time");
				String sRace = resultSet.getString("race");
				String sGender = resultSet.getString("gender");
				WeaponTypeWrapper weapon = new WeaponTypeWrapper(mainHandWeapon, offHandWeapon);

				Race race = null;
				Gender gender = null;
				try {
					race = Race.valueOf(sRace);
					gender = Gender.valueOf(sGender);
				} catch (Exception e) {
					log.info(I18n.get("log.bd04b9af9457", motionName));
				} finally {
					this.addTime(motionName, weapon, skillId, attackSpeed, race, gender, time);
				}
			}
			resultSet.close();
			stmt.close();
		} catch (SQLException e) {
			log.error(I18n.get("log.59fa1e0145de", e));
		} finally {
			DatabaseFactory.close(con);
		}
	}

	/**
	 * 清空内存后从数据库重新加载动作时间。
	 * Clears memory and reloads motion times from the database.
	 */
	public void reloadFromSql() {
		this.clearMotions();
		this.loadFromSql();
	}

	private void clearMotions() {
		for (MotionLog mLog : this.motionsMap.values()) {
			mLog.getMotionLog().clear();
		}
		this.motionsMap.clear();
	}

	private boolean isPresent(String motionName, WeaponTypeWrapper weapon, int skillId, int currentAttackSpeed,
			Race race, Gender gender) {
		if (motionsMap.containsKey(motionName)) {
			return motionsMap.get(motionName).isPresent(weapon, skillId, currentAttackSpeed, race, gender);
		}
		return false;
	}

	private int getTime(String motionName, WeaponTypeWrapper weapon, int skillId, int currentAttackSpeed, Race race,
			Gender gender) {
		if (motionsMap.containsKey(motionName)) {
			return motionsMap.get(motionName).getTime(weapon, skillId, currentAttackSpeed, race, gender);
		}
		return 0;
	}

	/**
	 * 添加或更新一条动作时间样本。
	 * Adds or updates a motion time sample.
	 *
	 * motion name
	 * @param weapon 武器类型包装 / weapon wrapper
	 * skill id
	 * @param currentAttackSpeed 当前攻击速度 / attack speed
	 * 阵营 / race
	 * gender
	 * @param clientTime 客户端时间 / client time
	 * @return 是否新增成功 / whether newly added
	 */
	public boolean addTime(String motionName, WeaponTypeWrapper weapon, int skillId, int currentAttackSpeed, Race race,
			Gender gender, int clientTime) {
		if (!motionsMap.containsKey(motionName)) {
			MotionLog motionLog = new MotionLog();
			boolean result = motionLog.addSkillTime(weapon,
					new SkillTime(skillId, currentAttackSpeed, race, gender, clientTime));
			motionsMap.put(motionName, motionLog);
			return result;
		} else {
			return motionsMap.get(motionName).addSkillTime(weapon,
					new SkillTime(skillId, currentAttackSpeed, race, gender, clientTime));
		}
	}

	/**
	 * 开关高级日志输出。
	 * Toggles advanced logging output.
	 *
	 * @param bol 是否开启 / whether enabled
	 */
	public void setAdvancedLog(boolean bol) {
		this.advancedLog = bol;
	}

	/**
	 * 是否开启高级日志。
	 * Whether advanced logging is enabled.
	 *
	 * whether enabled
	 */
	public boolean getAdvancedLog() {
		return this.advancedLog;
	}

	/**
	 * 默认构造并打印启动日志。
	 * Default constructor that logs service startup.
	 */
	public MotionLoggingService() {
		log.info(I18n.get("log.ef112ac23508"));
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		protected static final MotionLoggingService instance = new MotionLoggingService();
	}

	private class MotionLog {
		private Map<WeaponTypeWrapper, List<SkillTime>> motionsForWeapons = new LinkedHashMap<WeaponTypeWrapper, List<SkillTime>>();

		public Map<WeaponTypeWrapper, List<SkillTime>> getMotionLog() {
			return this.motionsForWeapons;
		}

		public boolean addSkillTime(WeaponTypeWrapper weapon, SkillTime skillTime) {
			if (motionsForWeapons.containsKey(weapon)) {
				if (!motionsForWeapons.containsValue(skillTime)) {
					motionsForWeapons.get(weapon).add(skillTime);
					return true;
				}
			} else {
				List<SkillTime> list = new ArrayList<SkillTime>();
				list.add(skillTime);
				motionsForWeapons.put(weapon, list);
				return true;
			}
			return false;
		}

		public int getTime(WeaponTypeWrapper weapon, int skillId, int currentAttackSpeed, Race race, Gender gender) {
			if (motionsForWeapons.containsKey(weapon)) {
				for (SkillTime st : motionsForWeapons.get(weapon)) {
					if (st.getSkillId() == skillId && st.getAttackSpeed() == currentAttackSpeed && st.getRace() == race
							&& st.getGender() == gender) {
						return st.getClientTime();
					}
				}
			}
			return 0;
		}

		public boolean isPresent(WeaponTypeWrapper weapon, int skillId, int currentAttackSpeed, Race race,
				Gender gender) {
			if (motionsForWeapons.containsKey(weapon)) {
				for (SkillTime st : motionsForWeapons.get(weapon)) {
					if (st.getSkillId() == skillId && st.getAttackSpeed() == currentAttackSpeed && st.getRace() == race
							&& st.getGender() == gender) {
						return true;
					}
				}
			}
			return false;
		}
	}

	private class SkillTime implements Comparable<SkillTime> {
		private int skillId;
		private int attackSpeed;
		private int clientTime;
		private Race race;
		private Gender gender;

		public SkillTime(int skillId, int attackSpeed, Race race, Gender gender, int clientTime) {
			this.skillId = skillId;
			this.attackSpeed = attackSpeed;
			this.clientTime = clientTime;
			this.race = race;
			this.gender = gender;
		}

		@Override
		public int compareTo(SkillTime o) {
			if (skillId < o.getSkillId())
				return -1;
			else if (skillId > o.getSkillId())
				return 1;
			else
				return 0;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + attackSpeed;
			result = prime * result + clientTime;
			result = prime * result + ((gender == null) ? 0 : gender.hashCode());
			result = prime * result + ((race == null) ? 0 : race.hashCode());
			result = prime * result + skillId;
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			SkillTime other = (SkillTime) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (attackSpeed != other.attackSpeed) {
				return false;
			}
			if (clientTime != other.clientTime) {
				return false;
			}
			if (gender != other.gender) {
				return false;
			}
			if (race != other.race) {
				return false;
			}
			if (skillId != other.skillId) {
				return false;
			}
			return true;
		}

		public int getSkillId() {
			return this.skillId;
		}

		public int getAttackSpeed() {
			return this.attackSpeed;
		}

		public int getClientTime() {
			return this.clientTime;
		}

		public Race getRace() {
			return race;
		}

		public Gender getGender() {
			return gender;
		}

		private MotionLoggingService getOuterType() {
			return MotionLoggingService.this;
		}
	}

	private class WeaponTime {
		private TreeMap<WeaponTypeWrapper, List<Integer>> values = new TreeMap<WeaponTypeWrapper, List<Integer>>();
		private Race race;
		private Gender gender;

		public WeaponTime(Race race, Gender gender) {
			this.race = race;
			this.gender = gender;
		}

		/**
		 * @return the race
		 */
		public Race getRace() {
			return race;
		}

		/**
		 * @param race the race to set
		 */
		public void setRace(Race race) {
			this.race = race;
		}

		/**
		 * @return the gender
		 */
		public Gender getGender() {
			return gender;
		}

		/**
		 * @param gender the gender to set
		 */
		public void setGender(Gender gender) {
			this.gender = gender;
		}

		public void add(WeaponTypeWrapper weapon, int value) {
			if (values.containsKey(weapon)) {
				values.get(weapon).add(value);
			} else {
				List<Integer> list = new ArrayList<Integer>();
				list.add(value);
				values.put(weapon, list);
			}
		}

		public TreeMap<WeaponTypeWrapper, Integer> process() {
			TreeMap<WeaponTypeWrapper, Integer> weaponMap = new TreeMap<WeaponTypeWrapper, Integer>();

			for (Entry<WeaponTypeWrapper, List<Integer>> entry2 : values.entrySet()) {
				// 按 weaponType 计算一个值的逻辑 / logic to calculate one value per weaponType
				// 统计出现次数最多的元素 / count the element with the most occurencies
				int finalValue = 0;
				int maxFrequency = 0;
				int value = 0;
				int total = 0;
				for (Integer i : entry2.getValue()) {
					total += i;
					if (calculateFrequency(entry2.getValue(), i) > maxFrequency) {
						maxFrequency = calculateFrequency(entry2.getValue(), i);
						value = i;
					}
				}
				log.info(I18n.get("log.610325e4b2f3", maxFrequency, value, entry2.getValue().size()));
				// 若给定值频率高于 70% 则采用，否则 / if frequency of given value is higher than 70% take it, otherwise do
				// 算术平均 / Arithmetic mean
				if (Math.round((float) entry2.getValue().size() * 0.7f) <= maxFrequency) {
					finalValue = value;
				} else {
					finalValue = total / entry2.getValue().size();
				}
				log.info(I18n.get("log.41911f5d1ef5", finalValue));
				weaponMap.put(entry2.getKey(), finalValue);
			}
			return weaponMap;
		}
	}

	private int calculateFrequency(List<Integer> list, int value) {
		int frequency = 0;

		// 10% 容差 / 10% tolerance
		int min = Math.round(value * 0.90f);
		int max = Math.round(value * 1.1f);
		for (Integer i : list) {
			if (i == null) {
				continue;
			}
			if (min <= value && max >= value) {
				frequency++;
			}
		}
		return frequency;
	}
}
