# PATH 投影失败统计（离线复现 PathData.projectPoint）

范围：已加载世界中**没有 terrain**、依赖 PATH 兜底的可移动非飞行刷点（`resolve_z` 点走 geo，不计入）。

- 依赖 PATH 的刷点总数：22539
- PATH 投影失败（会退回 XML z）：3909（17.3%）

| world_id | 依赖 PATH 的点 | 投影失败 | 失败率 |
|---|---|---|---|
| 400010000 | 9695 | 2502 | 25.8% |
| 400040000 | 2256 | 311 | 13.8% |
| 400020000 | 2258 | 306 | 13.6% |
| 400050000 | 2256 | 304 | 13.5% |
| 400060000 | 2244 | 304 | 13.5% |
| 301550000 | 204 | 74 | 36.3% |
| 300100000 | 365 | 24 | 6.6% |
| 301600000 | 18 | 17 | 94.4% |
| 301510000 | 70 | 16 | 22.9% |
| 300150000 | 46 | 8 | 17.4% |
| 300110000 | 80 | 7 | 8.8% |
| 301560000 | 154 | 7 | 4.5% |
| 300190000 | 126 | 3 | 2.4% |
| 300440000 | 145 | 3 | 2.1% |
| 301520000 | 81 | 3 | 3.7% |
| 301650000 | 130 | 3 | 2.3% |
| 220120000 | 7 | 2 | 28.6% |
| 300210000 | 130 | 2 | 1.5% |
| 320080000 | 57 | 2 | 3.5% |
| 320130000 | 90 | 2 | 2.2% |
| 120020000 | 7 | 1 | 14.3% |
| 300240000 | 278 | 1 | 0.4% |
| 300241000 | 278 | 1 | 0.4% |
| 301610000 | 23 | 1 | 4.3% |
| 302320000 | 58 | 1 | 1.7% |
| 302370000 | 58 | 1 | 1.7% |
| 302390000 | 58 | 1 | 1.7% |
| 302420000 | 58 | 1 | 1.7% |
| 310110000 | 96 | 1 | 1.0% |
| 110020000 | 2 | 0 | 0.0% |
| 110070000 | 35 | 0 | 0.0% |
| 130090000 | 5 | 0 | 0.0% |
| 140010000 | 4 | 0 | 0.0% |
| 210110000 | 5 | 0 | 0.0% |
| 220090000 | 1 | 0 | 0.0% |
| 300070000 | 59 | 0 | 0.0% |
| 300080000 | 68 | 0 | 0.0% |
| 300090000 | 11 | 0 | 0.0% |
| 300120000 | 25 | 0 | 0.0% |
| 300130000 | 2 | 0 | 0.0% |
| 300140000 | 2 | 0 | 0.0% |
| 300160000 | 155 | 0 | 0.0% |
| 300460000 | 4 | 0 | 0.0% |
| 300700000 | 112 | 0 | 0.0% |
| 300800000 | 5 | 0 | 0.0% |
| 301270000 | 3 | 0 | 0.0% |
| 301340000 | 3 | 0 | 0.0% |
| 301390000 | 66 | 0 | 0.0% |
| 301540000 | 365 | 0 | 0.0% |
| 301570000 | 35 | 0 | 0.0% |
| 301620000 | 9 | 0 | 0.0% |
| 301630000 | 1 | 0 | 0.0% |
| 301631000 | 1 | 0 | 0.0% |
| 310030000 | 11 | 0 | 0.0% |
| 310050000 | 1 | 0 | 0.0% |
| 310090000 | 2 | 0 | 0.0% |
| 310100000 | 2 | 0 | 0.0% |
| 310120000 | 1 | 0 | 0.0% |
| 320030000 | 10 | 0 | 0.0% |
| 320050000 | 119 | 0 | 0.0% |
| 320070000 | 1 | 0 | 0.0% |
| 320090000 | 1 | 0 | 0.0% |
| 320100000 | 70 | 0 | 0.0% |
| 320120000 | 15 | 0 | 0.0% |
| 320140000 | 2 | 0 | 0.0% |

## 失败样例

| world_id | npc_id | x | y | z | 文件 |
|---|---|---|---|---|---|
| 120020000 | 805693 | 500.022 | 476.698 | 499.600 | src/main/resources/aion/data/static_data/spawns/Npcs/120020000_Convent_Of_Marchutan.xml |
| 220120000 | 806296 | 498.579 | 499.922 | 349.709 | src/main/resources/aion/data/static_data/spawns/Npcs/220120000_Tower_Of_Eternity.xml |
| 220120000 | 806296 | 498.579 | 499.922 | 348.791 | src/main/resources/aion/data/static_data/spawns/Npcs/220120000_Tower_Of_Eternity.xml |
| 300100000 | 215007 | 287.027 | 525.295 | 950.665 | src/main/resources/aion/data/static_data/spawns/Instances/300100000_Steel Rake.xml |
| 300100000 | 215023 | 306.721 | 504.763 | 950.665 | src/main/resources/aion/data/static_data/spawns/Instances/300100000_Steel Rake.xml |
| 300100000 | 215023 | 306.674 | 507.718 | 950.665 | src/main/resources/aion/data/static_data/spawns/Instances/300100000_Steel Rake.xml |
| 300110000 | 214813 | 412.458 | 298.619 | 410.060 | src/main/resources/aion/data/static_data/spawns/Instances/300110000_Baranath_Dredgion.xml |
| 300110000 | 798323 | 379.971 | 697.239 | 404.074 | src/main/resources/aion/data/static_data/spawns/Instances/300110000_Baranath_Dredgion.xml |
| 300110000 | 798324 | 377.189 | 704.678 | 404.063 | src/main/resources/aion/data/static_data/spawns/Instances/300110000_Baranath_Dredgion.xml |
| 300150000 | 215857 | 595.821 | 640.241 | 131.000 | src/main/resources/aion/data/static_data/spawns/Instances/300150000_Udas_Temple.xml |
| 300150000 | 215857 | 441.130 | 435.650 | 131.000 | src/main/resources/aion/data/static_data/spawns/Instances/300150000_Udas_Temple.xml |
| 300150000 | 215857 | 524.769 | 387.584 | 136.000 | src/main/resources/aion/data/static_data/spawns/Instances/300150000_Udas_Temple.xml |
| 300190000 | 401041 | 273.491 | 313.032 | 1187.712 | src/main/resources/aion/data/static_data/spawns/Gather/300190000_Taloc's_Hollow.xml |
| 300190000 | 401042 | 507.779 | 871.028 | 1275.629 | src/main/resources/aion/data/static_data/spawns/Gather/300190000_Taloc's_Hollow.xml |
| 300190000 | 215456 | 145.098 | 377.186 | 1143.000 | src/main/resources/aion/data/static_data/spawns/Instances/300190000_Taloc's_Hollow.xml |
| 300210000 | 216856 | 428.875 | 264.208 | 410.099 | src/main/resources/aion/data/static_data/spawns/Instances/300210000_Chantra_Dredgion.xml |
| 300210000 | 216860 | 412.458 | 298.619 | 410.060 | src/main/resources/aion/data/static_data/spawns/Instances/300210000_Chantra_Dredgion.xml |
| 300240000 | 217369 | 223.457 | 793.630 | 900.696 | src/main/resources/aion/data/static_data/spawns/Instances/300240000_Aturam_Sky_Fortress.xml |
| 300241000 | 217369 | 223.457 | 793.630 | 900.696 | src/main/resources/aion/data/static_data/spawns/Instances/300241000_Event_Aturam_Sky_Fortress.xml |
| 300440000 | 219240 | 428.875 | 264.208 | 410.099 | src/main/resources/aion/data/static_data/spawns/Instances/300440000_Terath_Dredgion.xml |
| 300440000 | 219250 | 554.824 | 298.654 | 409.848 | src/main/resources/aion/data/static_data/spawns/Instances/300440000_Terath_Dredgion.xml |
| 300440000 | 219250 | 412.458 | 298.619 | 410.060 | src/main/resources/aion/data/static_data/spawns/Instances/300440000_Terath_Dredgion.xml |
| 301510000 | 237180 | 994.237 | 1192.421 | 94.389 | src/main/resources/aion/data/static_data/spawns/Instances/301510000_Sealed_Argent_Manor.xml |
| 301510000 | 237180 | 969.539 | 1200.404 | 94.186 | src/main/resources/aion/data/static_data/spawns/Instances/301510000_Sealed_Argent_Manor.xml |
| 301510000 | 237180 | 1007.103 | 1193.073 | 94.389 | src/main/resources/aion/data/static_data/spawns/Instances/301510000_Sealed_Argent_Manor.xml |
| 301520000 | 206417 | 683.479 | 217.433 | 1645.405 | src/main/resources/aion/data/static_data/spawns/Instances/301520000_Drakenspire_Depths.xml |
| 301520000 | 206418 | 807.640 | 283.815 | 1696.597 | src/main/resources/aion/data/static_data/spawns/Instances/301520000_Drakenspire_Depths.xml |
| 301520000 | 206427 | 904.630 | 385.640 | 1647.309 | src/main/resources/aion/data/static_data/spawns/Instances/301520000_Drakenspire_Depths.xml |
| 301550000 | 220494 | 516.505 | 1454.298 | 918.325 | src/main/resources/aion/data/static_data/spawns/Instances/301550000_Cradle_Of_Eternity.xml |
| 301550000 | 220494 | 425.880 | 1373.423 | 933.325 | src/main/resources/aion/data/static_data/spawns/Instances/301550000_Cradle_Of_Eternity.xml |
| 301550000 | 220494 | 486.255 | 1389.298 | 888.950 | src/main/resources/aion/data/static_data/spawns/Instances/301550000_Cradle_Of_Eternity.xml |
| 301560000 | 246392 | 1141.253 | 1020.467 | 760.922 | src/main/resources/aion/data/static_data/spawns/Instances/301560000_Trials_Of_Eternity.xml |
| 301560000 | 246394 | 1152.713 | 1016.288 | 760.922 | src/main/resources/aion/data/static_data/spawns/Instances/301560000_Trials_Of_Eternity.xml |
| 301560000 | 246394 | 1143.380 | 1016.820 | 760.922 | src/main/resources/aion/data/static_data/spawns/Instances/301560000_Trials_Of_Eternity.xml |
| 301600000 | 220414 | 469.495 | 479.393 | 174.447 | src/main/resources/aion/data/static_data/spawns/Instances/301600000_Adma's_Fall.xml |
| 301600000 | 220414 | 488.376 | 475.520 | 174.447 | src/main/resources/aion/data/static_data/spawns/Instances/301600000_Adma's_Fall.xml |
| 301600000 | 220414 | 488.438 | 501.533 | 174.447 | src/main/resources/aion/data/static_data/spawns/Instances/301600000_Adma's_Fall.xml |
| 301610000 | 220423 | 204.033 | 345.413 | 203.316 | src/main/resources/aion/data/static_data/spawns/Instances/301610000_Theobomos_Test_Chamber.xml |
| 301650000 | 243792 | 428.875 | 264.208 | 410.099 | src/main/resources/aion/data/static_data/spawns/Instances/301650000_Ashunatal_Dredgion.xml |
| 301650000 | 243802 | 554.824 | 298.654 | 409.848 | src/main/resources/aion/data/static_data/spawns/Instances/301650000_Ashunatal_Dredgion.xml |
| 301650000 | 243802 | 412.458 | 298.619 | 410.060 | src/main/resources/aion/data/static_data/spawns/Instances/301650000_Ashunatal_Dredgion.xml |
| 302320000 | 834995 | 362.459 | 246.988 | 230.560 | src/main/resources/aion/data/static_data/spawns/Instances/302320000_Hall_Of_Tenacity.xml |
| 302370000 | 834995 | 362.459 | 246.988 | 230.560 | src/main/resources/aion/data/static_data/spawns/Instances/302370000_Grand_Arena_Group_Camp.xml |
| 302390000 | 834995 | 362.459 | 246.988 | 230.560 | src/main/resources/aion/data/static_data/spawns/Instances/302390000_Grand_Arena_Training_Camp.xml |
| 302420000 | 834995 | 362.459 | 246.988 | 230.560 | src/main/resources/aion/data/static_data/spawns/Instances/302420000_Grand_Arena_Training_Camp.xml |
| 310110000 | 798224 | 253.596 | 513.169 | 189.000 | src/main/resources/aion/data/static_data/spawns/Instances/310110000_Theobomos_Lab.xml |
| 320080000 | 236924 | 487.832 | 595.032 | 518.001 | src/main/resources/aion/data/static_data/spawns/Instances/320080000_Draupnir_Cave.xml |
| 320080000 | 236924 | 487.832 | 595.032 | 517.001 | src/main/resources/aion/data/static_data/spawns/Instances/320080000_Draupnir_Cave.xml |
| 320130000 | 798224 | 342.826 | 409.157 | 188.000 | src/main/resources/aion/data/static_data/spawns/Instances/320130000_Adma_Stronghold.xml |
| 320130000 | 205206 | 455.238 | 591.754 | 163.999 | src/main/resources/aion/data/static_data/spawns/Instances/320130000_Adma_Stronghold.xml |
| 400010000 | 805634 | 1760.623 | 145.061 | 2938.036 | src/main/resources/aion/data/static_data/spawns/Landing/400010000_Reshanta.xml |
| 400010000 | 805798 | 1842.336 | 226.372 | 2939.593 | src/main/resources/aion/data/static_data/spawns/Landing/400010000_Reshanta.xml |
| 400010000 | 805634 | 1760.623 | 145.061 | 2938.036 | src/main/resources/aion/data/static_data/spawns/Landing/400010000_Reshanta.xml |
| 400020000 | 881023 | 1721.413 | 817.503 | 1494.402 | src/main/resources/aion/data/static_data/spawns/Npcs/400020000_Belus.xml |
| 400020000 | 881023 | 1736.016 | 842.581 | 1494.342 | src/main/resources/aion/data/static_data/spawns/Npcs/400020000_Belus.xml |
| 400020000 | 881023 | 1736.643 | 754.993 | 1494.921 | src/main/resources/aion/data/static_data/spawns/Npcs/400020000_Belus.xml |
| 400040000 | 881131 | 1721.413 | 817.503 | 1494.402 | src/main/resources/aion/data/static_data/spawns/Npcs/400040000_Aspida.xml |
| 400040000 | 881131 | 1736.016 | 842.581 | 1494.342 | src/main/resources/aion/data/static_data/spawns/Npcs/400040000_Aspida.xml |
| 400040000 | 881131 | 1736.643 | 754.993 | 1494.921 | src/main/resources/aion/data/static_data/spawns/Npcs/400040000_Aspida.xml |
| 400050000 | 881239 | 1721.413 | 817.503 | 1494.402 | src/main/resources/aion/data/static_data/spawns/Npcs/400050000_Atanatos.xml |
