package com.aionemu.gameserver.dataholders;


import com.aionemu.boot.i18n.I18n;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import com.aionemu.gameserver.dataholders.loadingutils.XmlDataLoader;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemMask;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.restriction.ItemCleanupTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;

import com.aionemu.commons.utils.collections.IntObjectHashMap;
import lombok.Getter;

/**
 * 物品模板数据容器，持有并索引全部 {@link ItemTemplate}，支持热重载与限制清理。
 * Item template data holder, indexing all {@link ItemTemplate} instances with reload and restriction cleanup support.
 *
 * @author Luno
 */
@Getter
@XmlRootElement(name = "item_templates")
@XmlAccessorType(XmlAccessType.FIELD)
public class ItemData extends ReloadableData {

	@XmlElement(name = "item_template")
	private List<ItemTemplate> its;

	@XmlTransient
	private IntObjectHashMap<ItemTemplate> items;
	@XmlTransient
	private Map<String, ItemTemplate> itemsByName;

	@XmlTransient
	private final IntObjectHashMap<ItemTemplate> petEggs = new IntObjectHashMap<>();

	/**
	 * 返回按等级分组的魔石模板映射。
	 * Returns manastone templates grouped by level.
	 * 等级到魔石列表的映射 / map of level to manastone list
	 */
	@XmlTransient
	Map<Integer, List<ItemTemplate>> manastones = new HashMap<>();

	/**
	 * 返回全部物品模板映射。
	 * Returns the full item template map.
	 * ID 到物品模板的映射 / map of id to item template
	 */
	@XmlTransient
	Map<Integer, ItemTemplate> allItems;

	/**
	 * JAXB 反序列化完成后，按模板 ID 建立索引并释放原始列表。
	 * After JAXB unmarshalling, indexes templates by id and clears the raw list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		indexTemplates(its == null ? List.of() : its);
		its = null;
	}

	/**
	 * 以已合并的分片映射和轻量覆盖模板列表装配物品数据，最后一次性重建全部索引。
	 * Assembles item data from a merged shard mapping and a lightweight override list, then rebuilds
	 * all indexes in one pass.
	 * @param merged          已按顺序合并的分片映射 / shard mappings merged in order
	 * @param customOverrides 自定义覆盖模板，可为 null / custom override templates, may be {@code null}
	 */
	public void assembleFromMerged(Map<Integer, ItemTemplate> merged, Collection<ItemTemplate> customOverrides) {
		int replaced = 0;
		int added = 0;
		if (customOverrides != null) {
			for (ItemTemplate override : customOverrides) {
				int templateId = override.getTemplateId();
				if (merged.containsKey(templateId)) {
					replaced++;
				} else {
					added++;
				}
				merged.put(templateId, override);
			}
			log.info(I18n.get("log.static_data.item_custom_overrides", replaced + added, replaced, added));
		}
		indexTemplates(merged.values());
	}

	/**
	 * 按模板 ID 建立物品与名称索引；{@code allItems} 复用同一索引，避免再复制一份大映射。
	 * Builds item and name indexes by template id; {@code allItems} reuses the same index instead
	 * of duplicating a large map.
	 * @param templates 物品模板 / item templates
	 */
	private void indexTemplates(Collection<ItemTemplate> templates) {
		items = new IntObjectHashMap<>(templates.size());
		int nameCapacity = templates.size() > Integer.MAX_VALUE / 2
			? Integer.MAX_VALUE
			: Math.max(16, templates.size() * 2);
		itemsByName = new HashMap<>(nameCapacity);
		for (ItemTemplate it : templates) {
			items.put(it.getTemplateId(), it);
			if (it.getNamedesc() != null && !it.getNamedesc().isBlank()) {
				itemsByName.putIfAbsent(it.getNamedesc().toLowerCase(Locale.ROOT), it);
			}
			if (!it.getName().isBlank()) {
				itemsByName.putIfAbsent(it.getName().toLowerCase(Locale.ROOT), it);
			}
		}
		allItems = items;
	}

	/**
	 * 根据清理规则覆盖物品的交易 / 出售 / 仓库存取掩码。
	 * sell / warehouse storage masks.
	 */
	public void cleanup() {
		for (ItemCleanupTemplate ict : DataManager.ITEM_CLEAN_UP.getList()) {
			ItemTemplate template = items.get(ict.getId());
			applyCleanup(template, ict.resultTrade(), ItemMask.TRADEABLE);
			applyCleanup(template, ict.resultSell(), ItemMask.SELLABLE);
			applyCleanup(template, ict.resultWH(), ItemMask.STORABLE_IN_WH);
			applyCleanup(template, ict.resultAccountWH(), ItemMask.STORABLE_IN_AWH);
			applyCleanup(template, ict.resultLegionWH(), ItemMask.STORABLE_IN_LWH);
		}
	}

	private void applyCleanup(ItemTemplate item, byte result, int mask) {
		if (result != -1) {
			if (result == 1) {
				item.modifyMask(true, mask);
			} else if (result == 0) {
				item.modifyMask(false, mask);
			}
		}
	}

	/**
	 * 按物品 ID 获取物品模板。
	 * Returns the item template for the given item id.
	 *
	 * @param itemId 物品 ID / item id
	 * @return 物品模板或 null / item template or null
	 */
	public ItemTemplate getItemTemplate(int itemId) {
		return items.get(itemId);
	}

	public ItemTemplate getItemTemplate(String name) {
		return name == null ? null : itemsByName.get(name.toLowerCase(Locale.ROOT));
	}

	/**
	 * 返回已加载的物品模板数量。
	 * Returns the number of loaded item templates.
	 *
	 * @return 已加载的物品模板数量 / Returns the number of loaded item templates.
	 */
	public int size() {
		return items.size();
	}

	/**
	 * 通过分片并行路径热重载物品模板并通知管理员。
	 * Hot-reloads item templates via the parallel shard path and notifies the admin.
	 *
	 * @param admin 触发重载的管理员 / admin who triggered the reload
	 */
	@Override
	public void reload(Player admin) {
		try {
			ItemData fresh = XmlDataLoader.getInstance().loadItemData();
			DataManager.ITEM_DATA.setData(new ArrayList<>(fresh.getItemData().values()));
		} catch (Exception e) {
			PacketSendUtility.sendMessage(admin, "Item templates reload failed!");
			log.error(I18n.get("log.b18c257924d5"), e);
		} finally {
			PacketSendUtility.sendMessage(admin,
				"Item templates reload Success! Total loaded: " + DataManager.ITEM_DATA.size());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void setData(List<?> data) {
		this.its = (List<ItemTemplate>) data;
		this.afterUnmarshal(null, null);
	}

	/**
	 * 返回内部物品模板哈希表。
	 * Returns the internal item template hash map.
	 *
	 * @return 物品模板映射 / item template map
	 */
	public IntObjectHashMap<ItemTemplate> getItemData() {
		return items;
	}
}
