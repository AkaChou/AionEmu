package com.aionemu.gameserver.dataholders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.templates.pet.PetFlavour;

/**
 * 宠物喂食口味数据容器，按口味 ID 索引 PetFlavour。
 * Pet feed flavour data holder, indexing PetFlavour by flavour id.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "flavours" })
@XmlRootElement(name = "pet_feed")
public class PetFeedData {

	@XmlElement(name = "flavour")
	protected List<PetFlavour> flavours;

	@XmlTransient
	private Map<Integer, PetFlavour> petFlavoursById = new HashMap<Integer, PetFlavour>();

	/**
	 * JAXB 反序列化完成后，将口味写入 ID 索引并释放列表。
	 * After JAXB unmarshalling, indexes flavours by id and releases the list.
	 */
	void afterUnmarshal(Unmarshaller u, Object parent) {
		if (flavours == null) {
			return;
		}
		for (PetFlavour flavour : flavours) {
			petFlavoursById.put(flavour.getId(), flavour);
		}
		flavours.clear();
		flavours = null;
	}

	/**
	 * 按口味 ID 获取宠物喂食口味。
	 * Returns the pet feed flavour for the given flavour id.
	 *
	 * flavour id
	 *
	 * @param flavourId @return 口味模板，不存在则为 null / flavour template or null
	 */
	public PetFlavour getFlavourById(int flavourId) {
		return petFlavoursById.get(flavourId);
	}

	/**
	 * 返回已加载的口味数量。
	 * Returns the number of loaded flavours.
	 *
	 * flavour count
	 */
	public int size() {
		return petFlavoursById.size();
	}

	/**
	 * 返回全部宠物口味数组。
	 * Returns all pet flavours as an array.
	 *
	 * flavour array
	 */
	public PetFlavour[] getPetFlavours() {
		return petFlavoursById.values().toArray(new PetFlavour[0]);
	}
}
