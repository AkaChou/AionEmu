package com.aionemu.gameserver.model.templates.item;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Random;

/**
 * Disassemble 物品模板（静态数据/XML）。
 * XML template.
 *
 * @author BeckUp.Media
 */
@XmlRootElement(name = "create")
public class DisassembleItem {
	@XmlAttribute(name = "itemId")
	private int ItemId;
	@XmlAttribute(name = "count")
	private String Count;
	@XmlAttribute(name = "disuse")
	private boolean disuse;

    private final Random random = new Random();

	/** 返回物品 ID / Returns the item id */
	public int getItemId() {
		return ItemId;
	}

    /** 获取计数。 / Returns the count. */
    public int getCount() {
        if (Count == null || Count.isEmpty()) {
            return 0;
        }
        try {
            // 检查 Count 是否包含范围（min-max 格式） / Check if Count contains a range (min-max format)
            if (Count.contains("-")) {
                String[] parts = Count.split("-");
                if (parts.length == 2) {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    return getRandomInRange(min, max);
                }
            }
            // 若不是范围，尝试转换为数字 / If it's not a range, try to convert it to a number
            return Integer.parseInt(Count.trim());
        } catch (NumberFormatException e) {
            return 0; // In case of error, return 0
        }
    }


    /**
	 * 返回 random 编号在 specifiedrange ( inclusive )。 / Returns a random number in the specified range (inclusive)
	 *
	 * @param min minimum value
	 * @param max maximum value
	 * @return random number in the range [min, max]
	 */
    private int getRandomInRange(int min, int max) {
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }
        return random.nextInt((max - min) + 1) + min;
    }

	/**
	 * @return Whether disuse
	 */
	public boolean isDisuse() {
		return disuse;
	}
}
