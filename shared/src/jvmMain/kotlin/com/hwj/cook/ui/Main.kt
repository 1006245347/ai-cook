package com.hwj.cook.ui

import com.hwj.cook.agent.provider.testConsoleAgent1

/**
 * @author by jason-何伟杰，2026/1/23
 * des: 单纯测试java应用
 * 菜单Run -> Edit Configurations-> 顶左添加Application-> 选Main Class(就是本文件)->apply
 * 执行： 在编译器 run/debug configurations选本文件执行，控制台输入就会自动响应最后一行输入
 */
suspend fun main() {
    testConsoleAgent1()
}