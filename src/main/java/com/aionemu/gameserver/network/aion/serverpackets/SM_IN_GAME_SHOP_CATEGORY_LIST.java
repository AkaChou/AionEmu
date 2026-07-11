package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.configs.ingameshop.InGameShopProperty;
import com.aionemu.gameserver.model.ingameshop.InGameShopEn;
import com.aionemu.gameserver.model.templates.ingameshop.IGCategory;
import com.aionemu.gameserver.model.templates.ingameshop.IGSubCategory;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端发送商城分类或子分类列表的服务端包。
 * Server packet that sends in-game shop category or subcategory lists to the client.
 */
public class SM_IN_GAME_SHOP_CATEGORY_LIST extends AionServerPacket {
	private int type;
	private int categoryId;
	private InGameShopProperty ing;

	/**
	 * 构造商城分类列表包。
	 * Creates an in-game shop category list packet.
	 *
	 * @param type 列表类型（0=主分类，2=子分类） / list type (0=categories, 2=subcategories)
	 * @param category 主分类 ID（子分类列表时使用） / parent category id (used for subcategories)
	 */
	public SM_IN_GAME_SHOP_CATEGORY_LIST(int type, int category) {
		this.type = type;
		categoryId = category;
		ing = GameRuntimeServices.inGameShopEn().getIGSProperty();
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(type);
		switch (type) {
		case 0:
			writeH(ing.size());
			for (IGCategory category : ing.getCategories()) {
				writeD(category.getId());
				writeS(category.getName());
			}
			break;
		case 2:
			if (categoryId < ing.size()) {
				IGCategory iGCategory = ing.getCategories().get(categoryId);
				writeH(iGCategory.getSubCategories().size());
				for (IGSubCategory subCategory : iGCategory.getSubCategories()) {
					writeD(subCategory.getId());
					writeS(subCategory.getName());
				}
			}
			break;
		}
	}
}
