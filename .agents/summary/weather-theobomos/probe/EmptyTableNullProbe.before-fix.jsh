import jakarta.xml.bind.*;
import java.io.*;
import com.aionemu.gameserver.dataholders.MapWeatherData;
import com.aionemu.gameserver.model.templates.world.WeatherTable;

JAXBContext ctx = JAXBContext.newInstance(MapWeatherData.class);
Unmarshaller u = ctx.createUnmarshaller();
MapWeatherData data = (MapWeatherData) u.unmarshal(new File(System.getProperty("probe.xml")));
WeatherTable t = data.getWeather(210060000);
System.out.println("tables loaded = " + data.size());
System.out.println("210060000 table = " + t + ", zoneCount=" + (t == null ? -1 : t.getZoneCount()) + ", weatherCount=" + (t == null ? -1 : t.getWeatherCount()));
System.out.println("zoneData = " + (t == null ? "n/a" : String.valueOf(t.getZoneData())));
try {
	System.out.println("getWeathersForZone(1) = " + t.getWeathersForZone(1));
} catch (Throwable e) {
	System.out.println("getWeathersForZone(1) THREW " + e);
}
WeatherTable poeta = data.getWeather(210010000);
System.out.println("poeta zoneData size = " + poeta.getZoneData().size());
/exit
