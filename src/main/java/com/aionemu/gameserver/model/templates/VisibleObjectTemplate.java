package com.aionemu.gameserver.model.templates;

/**
 * 可见对象模板（静态数据/XML）。
 * Visible object template (static data / XML).
 *
 * @author ATracer
 */
public abstract class VisibleObjectTemplate {

	/**
	 * 对 NPC：从模板 XML 返回 NPC ID。 / For Npcs it will return npcid from templates xml.
	 */
	public abstract int getTemplateId();

	/**
	 * 对 NPC：从模板 XML 返回名称。 / For Npcs it will return name from templates xml.
	 */
	public abstract String getName();

	/**
	 * 对象模板的名称 ID。 / Name id of object template.
	 */
	public abstract int getNameId();

	// /**
	// * 对象的全局种族。
	// * Global race of the object.
	// *
	// * @return
	// */
	// public abstract Race getRace();

	/**
	 * 获取边界半径。
	 * Get the bound radius.
	 *
	 * @return 边界半径 / Bound radius
	 */
	public BoundRadius getBoundRadius() {
		return BoundRadius.DEFAULT;
	}

	/**
	 * 获取对象默认状态。
	 * Get the default object state.
	 *
	 * @return 默认状态 / Default state
	 */
	public int getState() {
		return 0;
	}
}
