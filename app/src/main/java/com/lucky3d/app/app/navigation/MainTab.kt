package com.lucky3d.app.app.navigation

enum class MainTab(
    val label: String,
    val accessibilityLabel: String,
) {
    HOME("首页", "首页"),
    TREND("走势", "走势分析"),
    PICK("选号", "号码筛选"),
    PLANS("方案", "已保存方案"),
    CAIBAO("彩报", "彩报静态预览"),
}
