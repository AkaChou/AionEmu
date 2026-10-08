# -*- coding: utf-8 -*-
"""四个客户端对话 HTML 的修复定义（对照同族完好文件取证）。

取证：
- linocus/aluna：同族 12 个 match_maker 文件均为「npcfuncs 内只含 match_maker 子元素」；
  两文件互证目标值 =「萨德哈德雷得奇安渗透」（client_strings_msg.xml:26965 有该正规短语）。
- df4_v06_d_master_stigma：同行 stigma_open 完好；兄弟 1011_d_master_stigma 为
  <stigma_enchant>烙印之石强化</stigma_enchant>；本文件仅缺开标签的 '>'，只补结构不改文本。
- ideternity_war_l_wpseller_sp_03：对照 d_wpseller_sp_03（完好）与 l_wpseller_sp_02：
  引导句应为正文 <p>（</body> 前），npcfuncs 只含 trade_in。
"""
FIXES = {
    "Dialogs/ldf4b/ldf4b_li/linocus.html": [
        ("</match_maker>德雷得奇安渗透</npcfuncs>",
         "</match_maker></npcfuncs>"),
    ],
    "Dialogs/ldf4b/ldf4b_da/aluna.html": [
        ("<match_maker></match_maker>萨德哈德雷得奇安渗透</npcfuncs>",
         "<match_maker>萨德哈德雷得奇安渗透</match_maker></npcfuncs>"),
    ],
    "Dialogs/df4_m/df4_v06_d_master_stigma.html": [
        ("<stigma_enchant烙印强化</stigma_enchant>",
         "<stigma_enchant>烙印强化</stigma_enchant>"),
    ],
    "Dialogs/ideternity_war/ideternity_war_l_wpseller_sp_03.html": [
        ("\t\t  <p> </p>\r\n        </body>",
         "\t\t  <p> </p>\r\n\t\t  <p>如果你需要更高性能的战场物资，就去找[%dic:STR_DIC_N_IDEternity_War_L_WPSeller_04]%吧。</p>\r\n        </body>"),
        ("    <npcfuncs>如果你需要更高性能的战场物资，就去找[%dic:STR_DIC_N_IDEternity_War_L_WPSeller_04]%吧。<trade_in>交换战场物资</trade_in>\r\n      </npcfuncs>",
         "    <npcfuncs>\r\n      <trade_in>交换战场物资</trade_in>\r\n    </npcfuncs>"),
    ],
}

def apply_fix(entry, text):
    for old, new in FIXES[entry]:
        n = text.count(old)
        assert n == 1, f"{entry}: 锚点出现 {n} 次而非 1 次: {old[:60]!r}"
        text = text.replace(old, new)
    return text
