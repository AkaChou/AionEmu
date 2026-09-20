import jakarta.xml.bind.*;
import java.io.*;
import java.lang.reflect.*;
import com.aionemu.gameserver.dataholders.MapWeatherData;
import com.aionemu.gameserver.model.templates.world.WeatherEntry;
import com.aionemu.gameserver.model.templates.world.WeatherTable;
import com.aionemu.gameserver.services.WeatherService;
import com.aionemu.gameserver.utils.gametime.GameTime;

JAXBContext ctx = JAXBContext.newInstance(MapWeatherData.class);
Unmarshaller um = ctx.createUnmarshaller();
MapWeatherData data = (MapWeatherData) um.unmarshal(new File(System.getProperty("probe.xml")));
WeatherTable t = data.getWeather(210060000);
System.out.println("[table] 210060000 zoneCount=" + t.getZoneCount() + " weatherCount=" + t.getWeatherCount() + " zoneData=" + t.getZoneData());
System.out.println("[lookup] getWeathersForZone(1)=" + t.getWeathersForZone(1) + " getWeatherAfter(clearEntry)=" + t.getWeatherAfter(new WeatherEntry(1, 0)));

Field unsafef = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
unsafef.setAccessible(true);
Object unsafe = unsafef.get(null);
Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);
WeatherService service = (WeatherService) allocate.invoke(unsafe, WeatherService.class);
Method pick = WeatherService.class.getDeclaredMethod("getRandomWeather", GameTime.class, WeatherTable.class, int.class);
pick.setAccessible(true);
WeatherEntry picked = (WeatherEntry) pick.invoke(service, null, t, 1);
System.out.println("[service] getRandomWeather(null, table, 1) -> zoneId=" + picked.getZoneId() + " code=" + picked.getCode() + " name=" + picked.getWeatherName());
System.out.println("[gm-guard] Weather command only blocks forcing when zoneCount==0, actual zoneCount=" + t.getZoneCount());

WeatherTable poeta = data.getWeather(210010000);
System.out.println("[regression] poeta zoneData size=" + poeta.getZoneData().size() + " zone1 entries=" + poeta.getWeathersForZone(1).size());
/exit
