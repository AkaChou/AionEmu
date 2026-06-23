package com.aionemu.gameserver.model.templates.item;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Random;

/**
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

	public int getItemId() {
		return ItemId;
	}

    public int getCount() {
        if (Count == null || Count.isEmpty()) {
            return 0;
        }
        try {
            // Check if Count contains a range (min-max format)
            if (Count.contains("-")) {
                String[] parts = Count.split("-");
                if (parts.length == 2) {
                    int min = Integer.parseInt(parts[0].trim());
                    int max = Integer.parseInt(parts[1].trim());
                    return getRandomInRange(min, max);
                }
            }
            // If it's not a range, try to convert it to a number
            return Integer.parseInt(Count.trim());
        } catch (NumberFormatException e) {
            return 0; // In case of error, return 0
        }
    }


    /**
    * Returns a random number in the specified range (inclusive)
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

	public boolean isDisuse() {
		return disuse;
	}
}