# Theobomos（210060000）天空修复补丁（客户端资源 / Client-side patch）

修复现象：进入泰奥勃莫斯后，天空由入图瞬间的晴朗变为红云/沙尘，地面偏黄昏。
根因在客户端 level 数据（`mission_mission0.xml` 的 WeatherSystem / cutscene TimeEnv），
服务端协议无法控制，详见 `../server-control-boundary.md`。

## 安装（Install）

把本目录下的 `Levels/` **按客户端目录层级**覆盖到 Aion 5.8 客户端根目录
（例如 `F:\永恒之塔5.8\`），最终路径为：

```
<客户端根目录>\Levels\lf2a\Level.pak
```

覆盖后必须**完全退出并重新启动客户端进程**（pak 在进程启动时挂载；换角色/重登不重新挂载）。

> 本 README 仅作说明，不需要复制到客户端。

## 校验（Verify）

| 项 | 值 |
|---|---|
| 路径 | `Levels/lf2a/Level.pak` |
| 大小 | 9,809,347 字节 |
| MD5 | `f72b44b54f0989b9235519a4f3d21571` |

## 回滚（Rollback）

用原始 `Level.pak`（MD5 `fb49f0c3f1fce43d798b453e1def6dcf`，9,766,479 字节）覆盖后重启客户端。

## 修改内容（Level.pak → mission_mission0.xml）

1. `WeatherOption SandRain` / `SandRain_Before`：
   - `SkyCloud2D` / `SkySkyBox`：`LF2A.Weather.LF2A_Rain` → `LF2A.TimeEnv.LF2A_Daylight`；
   - `FogApply=0`、`EnvColor=230,230,230`，清空沙尘粒子（`ParticleName` / `ParticleType`）。
2. `TimeEnvOption name="invade_direct_portal"` / `"WorldRaid"`（原 `cutscenetype="1"`）：
   - 改名为 `invade_direct_portal_disabled` / `WorldRaid_disabled`，`time_name` 同步改名，
     `zonename="__disabled__"`，`skydome_name="default"`，`sky_box_name="LF2A.TimeEnv.LF2A_Daylight"`；
3. 对应的 `TimeofDayGroup cutscene="invade_direct_portal" / "WorldRaid"` 同步改名。

效果：天空回到 `LF2A.TimeEnv.LF2A_Daylight` 天空盒，地面/雾不再被入侵/世界突袭 cutscene 环境染色。

## 复现 / 维护（需要 unpak 工具：`/Users/mc/PycharmProjects/unpak`）

```bash
# 解包（自动解密/解码 XML）
python3 aion_pak.py unpack "<客户端>/Levels/lf2a/Level.pak" -o unpacked --overwrite --no-progress

# 编辑 unpacked/mission_mission0.xml 后回封（--template 用原 pak 保持容器格式）
python3 aion_pak.py pack unpacked -o Level.pak --template "<原 Level.pak>" --overwrite
```

## 关联

- 服务端修复：commit `32b7dba63`（移除 `weather_table.xml` 的 210060000 条目，不再下发沙尘天气码）。
- 排查记录：`../diagnosis-sandrain.md`、`../server-control-boundary.md`。
