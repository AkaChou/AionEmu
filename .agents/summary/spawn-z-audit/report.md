# 刷点 Z 投影缺口审计（只读）

判据：geo=geo/<id>.geo.gz；terrain=geo/<id>.png（TERRAIN_DISABLED_MAPS 内视为无）；path=geo/path/<id>.idx+.path.gz

只统计 world_maps.xml 中已启用（未被注释）的 world；被注释但有刷点文件的 world: [300260000, 301160000, 301632000, 600041000, 600200000]

| bucket | worlds | mobile_spots | no_projection | no_proj&no_runtime |
|---|---|---|---|---|
| OK | 98 | 156952 | 0 | 0 |
| TERRAIN_ONLY | 0 | 0 | 0 | 0 |
| PATH_ONLY | 80 | 32825 | 0 | 0 |
| NO_TERRAIN_NO_PATH | 0 | 0 | 0 | 0 |
| NO_GEO | 0 | 0 | 0 | 0 |

## 风险 world（前 40）

| world_id | name | spots | mobile | no_projection | no_proj&no_runtime |
|---|---|---|---|---|---|
