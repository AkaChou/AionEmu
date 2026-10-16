package com.aionemu.webshop.model;

import lombok.Data;

/**
 * 网页商城视角的角色摘要（只读投影，供下单时选择发货对象）。
 * Character summary as seen by the web shop (read-only projection used to pick a delivery target).
 * <p>刻意用可变 POJO 而非 record：查询结果依赖 setter 注入，不依赖 MyBatis 的构造器自动映射配置。
 * Deliberately a mutable POJO rather than a record: result injection goes through setters and therefore
 * does not depend on MyBatis constructor auto-mapping configuration.</p>
 */
@Data
public class ShopCharacter {

	/** 角色 ID（= players.id，也是 web_reward.item_owner 的取值）。 / character id (= players.id, also web_reward.item_owner). */
	private int id;

	private String name;

	/** 种族枚举字面量（ELYOS / ASMODIANS）。 / race enum literal (ELYOS / ASMODIANS). */
	private String race;

	/** 性别枚举字面量（MALE / FEMALE）。 / gender enum literal (MALE / FEMALE). */
	private String gender;

	/** 职业枚举字面量。 / class enum literal. */
	private String playerClass;
}
