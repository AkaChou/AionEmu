package com.aionemu.gameserver.dataholders;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.XMLConstants;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.model.geometry.Area;
import com.aionemu.gameserver.model.geometry.CylinderArea;
import com.aionemu.gameserver.model.geometry.PolyArea;
import com.aionemu.gameserver.model.geometry.SemisphereArea;
import com.aionemu.gameserver.model.geometry.SphereArea;
import com.aionemu.gameserver.model.templates.zone.ZoneClassName;
import com.aionemu.gameserver.model.templates.zone.ZoneInfo;
import com.aionemu.gameserver.model.templates.zone.ZoneTemplate;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * 区域数据容器，按地图 ID 索引区域信息，并维护天气区域序号。
 * Zone data holder, indexing zone info by map id and tracking weather-zone order numbers.
 *
 * @author ATracer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "zones")
@Slf4j
public class ZoneData {


	@XmlElement(name = "zone")
	public List<ZoneTemplate> zoneList;

	@XmlTransient
	private IntObjectHashMap<List<ZoneInfo>> zoneNameMap = new IntObjectHashMap<List<ZoneInfo>>();

	@XmlTransient
	private HashMap<ZoneTemplate, Integer> weatherZoneIds = new HashMap<ZoneTemplate, Integer>();

	@XmlTransient
	private int count;

	/**
	 * JAXB 反序列化完成后，按区域类型构建几何区域，按地图 ID 分组，并为天气区域分配序号。
	 * After JAXB unmarshalling, builds geometry areas by type, groups them by map id, and assigns weather-zone order numbers.
	 */
	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		int lastMapId = 0;
		int weatherZoneId = 1;
		for (ZoneTemplate zone : zoneList) {
			Area area = null;
			switch (zone.getAreaType()) {
			case POLYGON:
				area = new PolyArea(zone.getName(), zone.getMapid(), zone.getPoints().getPoint(),
						zone.getPoints().getBottom(), zone.getPoints().getTop());
				break;
			case CYLINDER:
				area = new CylinderArea(zone.getName(), zone.getMapid(), zone.getCylinder().getX(),
						zone.getCylinder().getY(), zone.getCylinder().getR(), zone.getCylinder().getBottom(),
						zone.getCylinder().getTop());
				break;
			case SPHERE:
				area = new SphereArea(zone.getName(), zone.getMapid(), zone.getSphere().getX(), zone.getSphere().getY(),
						zone.getSphere().getZ(), zone.getSphere().getR());
				break;
			case SEMISPHERE:
				area = new SemisphereArea(zone.getName(), zone.getMapid(), zone.getSemisphere().getX(),
						zone.getSemisphere().getY(), zone.getSemisphere().getZ(), zone.getSemisphere().getR());
			}
			if (area != null) {
				List<ZoneInfo> zones = zoneNameMap.get(zone.getMapid());
				if (zones == null) {
					zones = new ArrayList<ZoneInfo>();
					zoneNameMap.put(zone.getMapid(), zones);
				}
				if (zone.getZoneType() == ZoneClassName.WEATHER) {
					if (lastMapId != zone.getMapid()) {
						lastMapId = zone.getMapid();
						weatherZoneId = 1;
					}
					weatherZoneIds.put(zone, weatherZoneId++);
				}
				zones.add(new ZoneInfo(area, zone));
				count++;
			}
		}
		zoneList.clear();
		zoneList = null;
	}

	/**
	 * 返回按地图 ID 分组的区域信息映射。
	 * Returns the zone-info map keyed by map id.
	 *
	 * @return 区域信息映射 / zone-info map
	 */
	public IntObjectHashMap<List<ZoneInfo>> getZones() {
		return zoneNameMap;
	}

	/**
	 * 返回已加载的区域数量。
	 * Returns the number of loaded zones.
	 *
	 * zone count
	 */
	public int size() {
		return count;
	}

	/**
	 * 获取天气区域序号（从 1 开始）；模板未登记时返回 0。
	 * Returns the weather-zone order number (starting from 1), or 0 if the template is unregistered.
	 *
	 * zone template
	 *
	 * @param template
	 * @return 天气区域序号，未登记则为 0 / weather-zone id or 0
	 */
	public int getWeatherZoneId(ZoneTemplate template) {
		Integer id = weatherZoneIds.get(template);
		if (id == null) {
			return 0;
		}
		return id;
	}

	/**
	 * 将当前区域数据按 XSD 校验后写出到 generated_zones.xml。
	 * Marshals the current zone data to generated_zones.xml after XSD validation.
	 */
	public void saveData() {
		Schema schema = null;
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		try {
			schema = sf.newSchema(Config.dataFile("./data/static_data/zones/zones.xsd"));
		} catch (SAXException e1) {
			log.error(I18n.get("log.a52b870058c9", e1.getMessage(), e1.getCause()));
			return;
		}

		File xml = Config.dataFile("./data/static_data/zones/generated_zones.xml");
		JAXBContext jc;
		Marshaller marshaller;
		try {
			jc = JAXBContext.newInstance(ZoneData.class);
			marshaller = jc.createMarshaller();
			marshaller.setSchema(schema);
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.marshal(this, xml);
		} catch (JAXBException e) {
			log.error(I18n.get("log.a52b870058c9", e.getMessage(), e.getCause()));
			return;
		}
	}
}
