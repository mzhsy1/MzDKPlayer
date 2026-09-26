package org.mz.mzdkplayer.tool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mz.mzdkplayer.data.model.PhoneThemeMode

/**
 * 手机端主题取值的纯逻辑：存储字符串 → 枚举，枚举 + 系统深色 → 是否用深色。
 */
class PhoneThemeLogicTest {

    @Test
    fun `存储为空或 null 时跟随系统`() {
        assertEquals(PhoneThemeMode.SYSTEM, PhoneThemeLogic.themeModeFromStorage(null))
        assertEquals(PhoneThemeMode.SYSTEM, PhoneThemeLogic.themeModeFromStorage(""))
        assertEquals(PhoneThemeMode.SYSTEM, PhoneThemeLogic.themeModeFromStorage("   "))
    }

    @Test
    fun `枚举名能正确还原`() {
        assertEquals(PhoneThemeMode.SYSTEM, PhoneThemeLogic.themeModeFromStorage("SYSTEM"))
        assertEquals(PhoneThemeMode.LIGHT, PhoneThemeLogic.themeModeFromStorage("LIGHT"))
        assertEquals(PhoneThemeMode.DARK, PhoneThemeLogic.themeModeFromStorage("DARK"))
    }

    @Test
    fun `枚举名忽略大小写与首尾空格`() {
        assertEquals(PhoneThemeMode.DARK, PhoneThemeLogic.themeModeFromStorage(" dark "))
        assertEquals(PhoneThemeMode.LIGHT, PhoneThemeLogic.themeModeFromStorage("Light"))
    }

    @Test
    fun `非法值软降级为跟随系统`() {
        assertEquals(PhoneThemeMode.SYSTEM, PhoneThemeLogic.themeModeFromStorage("2"))
        assertEquals(PhoneThemeMode.SYSTEM, PhoneThemeLogic.themeModeFromStorage("blue"))
        assertEquals(PhoneThemeMode.SYSTEM, PhoneThemeLogic.themeModeFromStorage("darktheme"))
    }

    @Test
    fun `跟随系统时由系统决定深浅`() {
        assertTrue(PhoneThemeLogic.resolveDarkTheme(PhoneThemeMode.SYSTEM, systemInDarkTheme = true))
        assertFalse(PhoneThemeLogic.resolveDarkTheme(PhoneThemeMode.SYSTEM, systemInDarkTheme = false))
    }

    @Test
    fun `手动浅色时忽略系统深色`() {
        assertFalse(PhoneThemeLogic.resolveDarkTheme(PhoneThemeMode.LIGHT, systemInDarkTheme = true))
        assertFalse(PhoneThemeLogic.resolveDarkTheme(PhoneThemeMode.LIGHT, systemInDarkTheme = false))
    }

    @Test
    fun `手动深色时忽略系统浅色`() {
        assertTrue(PhoneThemeLogic.resolveDarkTheme(PhoneThemeMode.DARK, systemInDarkTheme = false))
        assertTrue(PhoneThemeLogic.resolveDarkTheme(PhoneThemeMode.DARK, systemInDarkTheme = true))
    }

    @Test
    fun `每个模式都有确定的深浅结果`() {
        PhoneThemeMode.entries.forEach { mode ->
            PhoneThemeLogic.resolveDarkTheme(mode, systemInDarkTheme = true)
            PhoneThemeLogic.resolveDarkTheme(mode, systemInDarkTheme = false)
        }
    }
}
