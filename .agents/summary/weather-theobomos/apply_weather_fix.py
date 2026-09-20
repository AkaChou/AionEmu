# -*- coding: utf-8 -*-
"""Apply the Theobomos weather repair patch (WeatherService + admin Weather command)."""
import sys

WS = 'src/main/java/com/aionemu/gameserver/services/WeatherService.java'
WC = 'src/main/java/com/aionemu/gameserver/commands/admin/Weather.java'


def patch(path, edits):
    src = open(path, encoding='utf-8').read()
    for old, new in edits:
        n = src.count(old)
        if n != 1:
            sys.exit('FAIL %s: expected 1 occurrence, got %d for:\n%s' % (path, n, old[:200]))
        src = src.replace(old, new, 1)
    open(path, 'w', encoding='utf-8').write(src)
    print('patched', path)


ws_edits = [
    # A1: javadoc of getRandomWeather
    ("""\t/**
\t * 按属性等级与时段修正随机选取天气条目。
\t * Randomly picks a weather entry by attribute ranking with daytime correction.
\t *
\t * creation time
\t * weather table
\t * zone id
\t * weather entry
\t */
""",
     """\t/**
\t * 按属性等级与时段修正随机选取天气条目；前兆（before）与残留（after）档不作为独立天气参与抽取。
\t * Randomly picks a weather entry by attribute ranking with daytime correction; sign (before) and remain
\t * (after) entries never roll as standalone weather.
\t *
\t * @param createdTime 天气创建时间，用于时段修正 / creation time used for the daytime correction
\t * @param table 该地图的天气表 / weather table of the map
\t * @param zoneId 天气区序号 / weather-zone ordinal
\t * @return 选中的天气条目；未选中时为该区域的放晴条目 / the chosen entry, or a clear-weather entry for the zone
\t */
"""),
    # A2: skip before/after entries while collecting candidates
    ("""\t\t\tfor (WeatherEntry entry : weathers) {
\t\t\t\tif (entry.getAttRanking() == -1) {
\t\t\t\t\treturn entry;
\t\t\t\t}
\t\t\t\tif (entry.getAttRanking() == attRanking) {
""",
     """\t\t\tfor (WeatherEntry entry : weathers) {
\t\t\t\tif (entry.getAttRanking() == -1) {
\t\t\t\t\treturn entry;
\t\t\t\t}
\t\t\t\t// 前兆/残留档只作为过渡态，不能被抽成当前天气。
\t\t\t\t// Sign and remain entries are transitional states and must never become the current weather.
\t\t\t\tif (entry.isBefore() || entry.isAfter()) {
\t\t\t\t\tcontinue;
\t\t\t\t}
\t\t\t\tif (entry.getAttRanking() == attRanking) {
"""),
    # A3: clear entry keeps the zone ordinal; drop the name-based before/after rewrite
    ("""\t\tWeatherEntry newWeather = null;
\t\tif (chosenWeather.size() == 0) {
\t\t\tnewWeather = new WeatherEntry();
\t\t} else {
\t\t\tnewWeather = chosenWeather.get(Rnd.get(chosenWeather.size()));
\t\t\t// 天气之前。 / Weather Before.
\t\t\tif (!newWeather.isBefore()) {
\t\t\t\tfor (WeatherEntry entry : weathers) {
\t\t\t\t\tif (newWeather.getWeatherName().equals(entry.getWeatherName()) && entry.isBefore()) {
\t\t\t\t\t\tnewWeather = entry;
\t\t\t\t\t\tbreak;
\t\t\t\t\t}
\t\t\t\t}
\t\t\t}
\t\t\t// 天气之后。 / Weather After.
\t\t\tif (!newWeather.isAfter()) {
\t\t\t\tfor (WeatherEntry entry : weathers) {
\t\t\t\t\tif (newWeather.getWeatherName().equals(entry.getWeatherName()) && entry.isAfter()) {
\t\t\t\t\t\tnewWeather = entry;
\t\t\t\t\t\tbreak;
\t\t\t\t\t}
\t\t\t\t}
\t\t\t}
\t\t\tint dayTimeCorrection = 1;
""",
     """\t\tWeatherEntry newWeather = null;
\t\tif (chosenWeather.size() == 0) {
\t\t\tnewWeather = clearWeather(zoneId);
\t\t} else {
\t\t\tnewWeather = chosenWeather.get(Rnd.get(chosenWeather.size()));
\t\t\tint dayTimeCorrection = 1;
"""),
    # A4: clear entry on the probability gate + new helper
    ("""\t\t\t\t\t|| (newWeather.getAttRanking() == 2 && chance > 66 / dayTimeCorrection)) {
\t\t\t\tnewWeather = new WeatherEntry();
\t\t\t}
\t\t}
\t\treturn newWeather;
\t}
""",
     """\t\t\t\t\t|| (newWeather.getAttRanking() == 2 && chance > 66 / dayTimeCorrection)) {
\t\t\t\tnewWeather = clearWeather(zoneId);
\t\t\t}
\t\t}
\t\treturn newWeather;
\t}

\t/**
\t * 生成某个天气区的放晴条目，并保留正确的天气区序号。
\t * Creates a clear-weather entry for the given zone, keeping the correct weather-zone ordinal.
\t *
\t * @param zoneId 天气区序号 / weather-zone ordinal
\t * @return 放晴条目（code=0） / clear-weather entry (code=0)
\t */
\tprivate WeatherEntry clearWeather(int zoneId) {
\t\treturn new WeatherEntry(zoneId, 0);
\t}
"""),
    # B: changeRegionWeather keeps canonical zone ordinals
    ("""\t\tfor (int i = 0; i < weatherEntries.length; i++) {
\t\t\tWeatherEntry oldEntry = weatherEntries[i];
\t\t\tif (oldEntry == null) {
\t\t\t\tweatherEntries[i] = new WeatherEntry(0, weatherCode);
\t\t\t} else {
\t\t\t\tweatherEntries[i] = new WeatherEntry(oldEntry.getZoneId(), weatherCode);
\t\t\t}
\t\t}
""",
     """\t\tfor (int i = 0; i < weatherEntries.length; i++) {
\t\t\t// 天气区序号固定取 i + 1，避免旧条目 zoneId=0 污染后续查询。
\t\t\t// The zone ordinal is always i + 1 so a polluted zone id (0) can never leak into later lookups.
\t\t\tweatherEntries[i] = new WeatherEntry(i + 1, weatherCode);
\t\t}
"""),
    # C: resetWeather uses the helper and is null safe
    ("""\t\tfor (WeatherKey key : loadedWeathers) {
\t\t\tWeatherEntry[] oldEntries = worldZoneWeathers.get(key);
\t\t\tfor (int i = 0; i < oldEntries.length; i++) {
\t\t\t\toldEntries[i] = new WeatherEntry(oldEntries[i].getZoneId(), 0);
\t\t\t}
\t\t\tonWeatherChange(key.getMapId(), null);
\t\t}
""",
     """\t\tfor (WeatherKey key : loadedWeathers) {
\t\t\tWeatherEntry[] oldEntries = worldZoneWeathers.get(key);
\t\t\tif (oldEntries == null) {
\t\t\t\tcontinue;
\t\t\t}
\t\t\tfor (int i = 0; i < oldEntries.length; i++) {
\t\t\t\toldEntries[i] = clearWeather(i + 1);
\t\t\t}
\t\t\tonWeatherChange(key.getMapId(), null);
\t\t}
"""),
    # D: getWeatherCode null safety + snapshot accessor for the admin command
    ("""\tpublic int getWeatherCode(int mapId, int weatherZoneId) {
\t\tWeatherEntry[] weatherEntries = getWeatherEntries(mapId);
\t\tfor (WeatherEntry entry : weatherEntries) {
\t\t\tif (entry != null && entry.getZoneId() == weatherZoneId) {
\t\t\t\treturn entry.getCode();
\t\t\t}
\t\t}
\t\treturn 0;
\t}
""",
     """\tpublic int getWeatherCode(int mapId, int weatherZoneId) {
\t\tWeatherEntry[] weatherEntries = getWeatherEntries(mapId);
\t\tif (weatherEntries == null) {
\t\t\treturn 0;
\t\t}
\t\tfor (WeatherEntry entry : weatherEntries) {
\t\t\tif (entry != null && entry.getZoneId() == weatherZoneId) {
\t\t\t\treturn entry.getCode();
\t\t\t}
\t\t}
\t\treturn 0;
\t}

\t/**
\t * 返回指定地图当前天气条目的只读快照，供管理命令排查展示。
\t * Returns a read-only snapshot of the given map's current weather entries for admin diagnostics.
\t *
\t * @param mapId 地图 ID / map id
\t * @return 天气条目快照；该地图没有天气表时为空数组 / weather-entry snapshot; empty when the map has no weather table
\t */
\tpublic WeatherEntry[] getWeatherSnapshot(int mapId) {
\t\tWeatherEntry[] weatherEntries = getWeatherEntries(mapId);
\t\treturn weatherEntries == null ? new WeatherEntry[0] : weatherEntries.clone();
\t}
"""),
]

wc_edits = [
    # per-zone readout keeps the resolved weather-zone ordinal, plus whole-map snapshot
    ("""\t\tif (params.length == 0) {
\t\t\tint weatherCode = -1;
\t\t\tList<ZoneInstance> zones = admin.getActiveRegion().getZones(admin);
\t\t\tfor (ZoneInstance regionZone : zones) {
\t\t\t\tif (regionZone.getZoneTemplate().getZoneType() == ZoneClassName.WEATHER) {
\t\t\t\t\tint weatherZoneId = DataManager.ZONE_DATA.getWeatherZoneId(regionZone.getZoneTemplate());
\t\t\t\t\tweatherCode = GameRuntimeServices.weatherService().getWeatherCode(admin.getWorldId(), weatherZoneId);
\t\t\t\t\tregionName = regionZone.getZoneTemplate().getXmlName();
\t\t\t\t\tbreak;
\t\t\t\t}
\t\t\t} if (weatherCode == -1) {
\t\t\t\tPacketSendUtility.sendMessage(admin, "No weather.");
\t\t\t} else {
\t\t\t\tPacketSendUtility.sendMessage(admin, "Weather code for region " + regionName + " is " + weatherCode);
\t\t\t}
\t\t\treturn;
\t\t}
""",
     """\t\tif (params.length == 0) {
\t\t\tint weatherCode = -1;
\t\t\tint weatherZoneId = 0;
\t\t\tList<ZoneInstance> zones = admin.getActiveRegion().getZones(admin);
\t\t\tfor (ZoneInstance regionZone : zones) {
\t\t\t\tif (regionZone.getZoneTemplate().getZoneType() == ZoneClassName.WEATHER) {
\t\t\t\t\tweatherZoneId = DataManager.ZONE_DATA.getWeatherZoneId(regionZone.getZoneTemplate());
\t\t\t\t\tweatherCode = GameRuntimeServices.weatherService().getWeatherCode(admin.getWorldId(), weatherZoneId);
\t\t\t\t\tregionName = regionZone.getZoneTemplate().getXmlName();
\t\t\t\t\tbreak;
\t\t\t\t}
\t\t\t} if (weatherCode == -1) {
\t\t\t\tPacketSendUtility.sendMessage(admin, "No weather.");
\t\t\t} else {
\t\t\t\tPacketSendUtility.sendMessage(admin, "Weather code for region " + regionName + " (weather zone "
\t\t\t\t\t+ weatherZoneId + ") is " + weatherCode);
\t\t\t}
\t\t\t// 附带整图快照，避免单点读数被误读。 / Also dump the whole-map snapshot so a single-point reading cannot be misread.
\t\t\tPacketSendUtility.sendMessage(admin, "Map " + admin.getWorldId() + " weather: " + describeWeather(admin.getWorldId()));
\t\t\treturn;
\t\t}
"""),
    # import WeatherEntry
    ("""import com.aionemu.gameserver.model.templates.world.WeatherTable;
""",
     """import com.aionemu.gameserver.model.templates.world.WeatherEntry;
import com.aionemu.gameserver.model.templates.world.WeatherTable;
"""),
    # helper method before onFail
    ("""\t/**
\t * 参数错误时的用法提示。
\t * Usage hint on invalid parameters.
\t *
\t */
\t@Override
\tpublic void onFail(Player player, String message) {
""",
     """\t/**
\t * 生成该地图的天气快照文本（zone=code(name)）。
\t * Builds the map weather snapshot text (zone=code(name)).
\t *
\t * @param mapId 地图 ID / map id
\t * @return 快照文本 / snapshot text
\t */
\tprivate String describeWeather(int mapId) {
\t\tWeatherEntry[] entries = GameRuntimeServices.weatherService().getWeatherSnapshot(mapId);
\t\tif (entries.length == 0) {
\t\t\treturn "all clear (no weather table)";
\t\t}
\t\tStringBuilder builder = new StringBuilder();
\t\tfor (WeatherEntry entry : entries) {
\t\t\tif (builder.length() > 0) {
\t\t\t\tbuilder.append(", ");
\t\t\t}
\t\t\tif (entry == null) {
\t\t\t\tbuilder.append("zone?=clear");
\t\t\t\tcontinue;
\t\t\t}
\t\t\tbuilder.append("zone ").append(entry.getZoneId()).append("=").append(entry.getCode());
\t\t\tif (entry.getWeatherName() != null) {
\t\t\t\tbuilder.append("(").append(entry.getWeatherName()).append(")");
\t\t\t}
\t\t}
\t\treturn builder.toString();
\t}

\t/**
\t * 参数错误时的用法提示。
\t * Usage hint on invalid parameters.
\t *
\t */
\t@Override
\tpublic void onFail(Player player, String message) {
"""),
]

patch(WS, ws_edits)
patch(WC, wc_edits)
