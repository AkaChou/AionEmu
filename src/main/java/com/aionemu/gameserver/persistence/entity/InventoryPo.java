package com.aionemu.gameserver.persistence.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.io.Serial;

import lombok.Data;

/**
 * 表 inventory 的持久化实体。
 * Persistence entity for table inventory.
 * <p>
 * 布尔列（DDL {@code tinyint(1)}）统一为 {@link Boolean}：遗留实现以 {@code getInt}/{@code getBoolean} 读取后
 * 与 {@code 1} 比较，取值仍是非 0 即真；同表已被其它闭包（邮箱附件、军团仓库）以 {@code Boolean.TRUE.equals(...)}
 * 消费，保持 {@link Boolean} 才能避免这些调用点恒为假。
 * Boolean columns (DDL {@code tinyint(1)}) are {@link Boolean}: the legacy implementation read them via
 * {@code getInt}/{@code getBoolean} and compared with {@code 1} (non-zero is still true), and sibling closures
 * (mail attachments, legion warehouse) already consume them via {@code Boolean.TRUE.equals(...)}, which would
 * silently always be false for an {@link Integer}.
 * </p>
 */
@Data
@Table("inventory")
public class InventoryPo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	private Integer itemUniqueId;

	private Integer itemId;

	private Long itemCount;

	private Integer itemColor;

	private Integer colorExpires;

	private Integer itemOwner;

	private String itemCreator;

	/**
	 * DDL 类型 timestamp；遗留实现未读取该列。
	 * DDL type timestamp; not read by the legacy implementation.
	 */
	@Column("itemCreationTime")
	private Timestamp itemCreationTime;

	private Integer expireTime;

	/**
	 * DDL 类型 tinyint(1)。
	 * DDL type tinyint(1).
	 */
	@Column("is_equiped")
	private Boolean equiped;

	/**
	 * DDL 类型 tinyint(1)。
	 * DDL type tinyint(1).
	 */
	@Column("is_soul_bound")
	private Boolean soulBound;

	private Long slot;

	private Integer itemLocation;

	private Integer enchant;

	private Integer enchantBonus;

	private Integer itemSkin;

	private Integer fusionedItem;

	private Integer optionalSocket;

	private Integer optionalFusionSocket;

	private Integer activationCount;

	private Integer charge;

	private Integer rndBonus;

	private Integer rndCount;

	private Integer wrappableCount;

	/**
	 * DDL 类型 tinyint(1)。
	 * DDL type tinyint(1).
	 */
	@Column("is_packed")
	private Boolean packed;

	private Integer temperingLevel;

	/**
	 * DDL 类型 tinyint(1)。
	 * DDL type tinyint(1).
	 */
	@Column("is_topped")
	private Boolean topped;

	private Integer strengthenSkill;

	private Integer skinSkill;

	/**
	 * DDL 类型 tinyint(1)；遗留实现经 {@code getInt} 读取后与 {@code 1} 比较。
	 * DDL type tinyint(1); read via {@code getInt} and compared with {@code 1}.
	 */
	private Boolean lunaReskin;

	/**
	 * DDL 类型 int；遗留实现经 {@code getInt} 读取，非布尔列。
	 * DDL type int; read via {@code getInt} and not a boolean column.
	 */
	private Integer reductionLevel;

	/**
	 * DDL 类型 int；遗留实现经 {@code getInt} 读取，非布尔列。
	 * DDL type int; read via {@code getInt} and not a boolean column.
	 */
	@Column("is_seal")
	private Integer seal;

	/**
	 * DDL 类型 tinyint(1)；遗留实现经 {@code getBoolean} 读取。
	 * DDL type tinyint(1); read via {@code getBoolean}.
	 */
	@Column("isEnhance")
	private Boolean isEnhance;

	@Column("enhanceSkillId")
	private Integer enhanceSkillId;

	@Column("enhanceSkillEnchant")
	private Integer enhanceSkillEnchant;

}
