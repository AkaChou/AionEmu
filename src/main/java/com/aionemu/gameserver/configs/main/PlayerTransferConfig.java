package com.aionemu.gameserver.configs.main;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.aionemu.commons.configuration.Property;

import lombok.Getter;

/**
 * 角色跨服转移相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Player server-transfer related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 * @author KID
 */
@Component
@ConfigurationProperties(prefix = "ptransfer")
public class PlayerTransferConfig {
	/**
	 * 转移允许携带的最大基纳。
	 * Maximum kinah allowed during transfer.
	 */
	@Property(key = "ptransfer.max.kinah", defaultValue = "0")
	public static long MAX_KINAH;

	/**
	 * 天族转移后绑定点坐标。
	 * Elyos bind point after transfer.
	 */
	@Property(key = "ptransfer.bindpoint.elyos", defaultValue = "210010000 1212.9423 1044.8516 140.75568 32")
	public static String BIND_ELYOS;

	/**
	 * 魔族转移后绑定点坐标。
	 * Asmodian bind point after transfer.
	 */
	@Property(key = "ptransfer.bindpoint.asmo", defaultValue = "220010000 571.0388 2787.3420 299.8750 32")
	public static String BIND_ASMO;

	/**
	 * 是否转移表情数据。
	 * Whether emotions are transferred.
	 */
	@Property(key = "ptransfer.allow.emotions", defaultValue = "true")
	public static boolean ALLOW_EMOTIONS;

	/**
	 * 是否转移动作数据。
	 * Whether motions are transferred.
	 */
	@Property(key = "ptransfer.allow.motions", defaultValue = "true")
	public static boolean ALLOW_MOTIONS;

	/**
	 * 是否转移宏数据。
	 * Whether macros are transferred.
	 */
	@Property(key = "ptransfer.allow.macro", defaultValue = "true")
	public static boolean ALLOW_MACRO;

	/**
	 * 是否转移 NPC 阵营声望。
	 * Whether NPC factions are transferred.
	 */
	@Property(key = "ptransfer.allow.npcfactions", defaultValue = "true")
	public static boolean ALLOW_NPCFACTIONS;

	/**
	 * 是否转移宠物数据。
	 * Whether pets are transferred.
	 */
	@Property(key = "ptransfer.allow.pets", defaultValue = "true")
	public static boolean ALLOW_PETS;

	/**
	 * 是否转移配方数据。
	 * Whether recipes are transferred.
	 */
	@Property(key = "ptransfer.allow.recipes", defaultValue = "true")
	public static boolean ALLOW_RECIPES;

	/**
	 * 是否转移技能数据。
	 * Whether skills are transferred.
	 */
	@Property(key = "ptransfer.allow.skills", defaultValue = "true")
	public static boolean ALLOW_SKILLS;

	/**
	 * 是否转移称号数据。
	 * Whether titles are transferred.
	 */
	@Property(key = "ptransfer.allow.titles", defaultValue = "true")
	public static boolean ALLOW_TITLES;

	/**
	 * 是否转移任务数据。
	 * Whether quests are transferred.
	 */
	@Property(key = "ptransfer.allow.quests", defaultValue = "true")
	public static boolean ALLOW_QUESTS;

	/**
	 * 是否转移背包物品。
	 * Whether inventory is transferred.
	 */
	@Property(key = "ptransfer.allow.inventory", defaultValue = "true")
	public static boolean ALLOW_INV;

	/**
	 * 是否转移仓库物品。
	 * Whether warehouse is transferred.
	 */
	@Property(key = "ptransfer.allow.warehouse", defaultValue = "true")
	public static boolean ALLOW_WAREHOUSE;

	/**
	 * 是否转移印记数据。
	 * Whether stigma data is transferred.
	 */
	@Property(key = "ptransfer.allow.stigma", defaultValue = "true")
	public static boolean ALLOW_STIGMA;

	/**
	 * 是否禁止使用与原角色相同的名称。
	 * Whether same character name is blocked after transfer.
	 */
	@Property(key = "ptransfer.block.samename", defaultValue = "false")
	public static boolean BLOCK_SAMENAME;

	/**
	 * 转移后角色名称前缀。
	 * Name prefix applied after server transfer.
	 */
	@Property(key = "ptransfer.server.name.prefix", defaultValue = "_UNK")
	public static String NAME_PREFIX;

	/**
	 * 再次转移冷却时间（小时）。
	 * Hours before re-transfer is allowed.
	 */
	@Property(key = "ptransfer.retransfer.hours", defaultValue = "0")
	public static int REUSE_HOURS;

	/**
	 * 转移时移除的技能列表。
	 * Skill list removed during transfer.
	 */
	@Property(key = "ptransfer.remove.skills.list", defaultValue = "*")
	public static String REMOVE_SKILL_LIST;

	/** 绑定树 / binding tree. */
	@Getter
	private final MaxKinahProperties max = new MaxKinahProperties();
	@Getter
	private final BindpointProperties bindpoint = new BindpointProperties();
	@Getter
	private final AllowProperties allow = new AllowProperties();
	@Getter
	private final BlockProperties block = new BlockProperties();
	@Getter
	private final ServerProperties server = new ServerProperties();
	@Getter
	private final RetransferProperties retransfer = new RetransferProperties();
	@Getter
	private final RemoveProperties remove = new RemoveProperties();


	/**
	 * 最大基纳节点：绑定 ptransfer.max.kinah。
	 * Max-kinah node binding ptransfer.max.kinah.
	 */
	public static class MaxKinahProperties {
		public long getKinah() {
			return MAX_KINAH;
		}

		public void setKinah(long kinah) {
			MAX_KINAH = kinah;
		}
	}

	/**
	 * 绑定点节点：绑定 ptransfer.bindpoint.*。
	 * Bind-point node binding ptransfer.bindpoint.*.
	 */
	@Getter
	public static class BindpointProperties {
		public String getElyos() {
			return BIND_ELYOS;
		}

		public void setElyos(String elyos) {
			BIND_ELYOS = elyos;
		}

		public String getAsmo() {
			return BIND_ASMO;
		}

		public void setAsmo(String asmo) {
			BIND_ASMO = asmo;
		}
	}

	/**
	 * 转移内容节点：绑定 ptransfer.allow.*。
	 * Transfer-content node binding ptransfer.allow.*.
	 */
	@Getter
	public static class AllowProperties {
		public boolean isEmotions() {
			return ALLOW_EMOTIONS;
		}

		public void setEmotions(boolean emotions) {
			ALLOW_EMOTIONS = emotions;
		}

		public boolean isMotions() {
			return ALLOW_MOTIONS;
		}

		public void setMotions(boolean motions) {
			ALLOW_MOTIONS = motions;
		}

		public boolean isMacro() {
			return ALLOW_MACRO;
		}

		public void setMacro(boolean macro) {
			ALLOW_MACRO = macro;
		}

		public boolean isNpcfactions() {
			return ALLOW_NPCFACTIONS;
		}

		public void setNpcfactions(boolean npcfactions) {
			ALLOW_NPCFACTIONS = npcfactions;
		}

		public boolean isPets() {
			return ALLOW_PETS;
		}

		public void setPets(boolean pets) {
			ALLOW_PETS = pets;
		}

		public boolean isRecipes() {
			return ALLOW_RECIPES;
		}

		public void setRecipes(boolean recipes) {
			ALLOW_RECIPES = recipes;
		}

		public boolean isSkills() {
			return ALLOW_SKILLS;
		}

		public void setSkills(boolean skills) {
			ALLOW_SKILLS = skills;
		}

		public boolean isTitles() {
			return ALLOW_TITLES;
		}

		public void setTitles(boolean titles) {
			ALLOW_TITLES = titles;
		}

		public boolean isQuests() {
			return ALLOW_QUESTS;
		}

		public void setQuests(boolean quests) {
			ALLOW_QUESTS = quests;
		}

		public boolean isInventory() {
			return ALLOW_INV;
		}

		public void setInventory(boolean inventory) {
			ALLOW_INV = inventory;
		}

		public boolean isWarehouse() {
			return ALLOW_WAREHOUSE;
		}

		public void setWarehouse(boolean warehouse) {
			ALLOW_WAREHOUSE = warehouse;
		}

		public boolean isStigma() {
			return ALLOW_STIGMA;
		}

		public void setStigma(boolean stigma) {
			ALLOW_STIGMA = stigma;
		}
	}

	/**
	 * 同名阻止节点：绑定 ptransfer.block.samename。
	 * Same-name block node binding ptransfer.block.samename.
	 */
	public static class BlockProperties {
		public boolean isSamename() {
			return BLOCK_SAMENAME;
		}

		public void setSamename(boolean samename) {
			BLOCK_SAMENAME = samename;
		}
	}

	/**
	 * 服务器名父节点：对应配置元素 ptransfer.server。
	 * Server-name parent node matching the ptransfer.server element.
	 */
	@Getter
	public static class ServerProperties {
		private final NameProperties name = new NameProperties();
	}

	/**
	 * 服务器名前缀节点：绑定 ptransfer.server.name.prefix。
	 * Server-name prefix node binding ptransfer.server.name.prefix.
	 */
	public static class NameProperties {
		public String getPrefix() {
			return NAME_PREFIX;
		}

		public void setPrefix(String prefix) {
			NAME_PREFIX = prefix;
		}
	}

	/**
	 * 再次转移节点：绑定 ptransfer.retransfer.hours。
	 * Re-transfer node binding ptransfer.retransfer.hours.
	 */
	public static class RetransferProperties {
		public int getHours() {
			return REUSE_HOURS;
		}

		public void setHours(int hours) {
			REUSE_HOURS = hours;
		}
	}

	/**
	 * 技能移除父节点：对应配置元素 ptransfer.remove。
	 * Skill-removal parent node matching the ptransfer.remove element.
	 */
	@Getter
	public static class RemoveProperties {
		private final SkillsProperties skills = new SkillsProperties();
	}

	/**
	 * 技能移除节点：绑定 ptransfer.remove.skills.list。
	 * Skill-removal node binding ptransfer.remove.skills.list.
	 */
	public static class SkillsProperties {
		public String getList() {
			return REMOVE_SKILL_LIST;
		}

		public void setList(String list) {
			REMOVE_SKILL_LIST = list;
		}
	}
}
