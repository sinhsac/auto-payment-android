package com.hotroso.autopay.common

object Constants {
    val UNSUPPORTED_APPS: List<String> = listOf("android")

    const val MY_ACTION_ON_REBOOT = "hotroso-auto-payment"
    const val NOTIFICATION_CHANNEL_ID = "autopay_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Auto Pay Service"
    const val PREF_NAME = "autopay_prefs"
    const val PREF_SERVER_URL = "server_url"
    const val DEFAULT_SERVER_URL = "https://your-server.com/notify"

    // AutoStart brands
    const val BRAND_XIAOMI = "xiaomi"
    const val BRAND_XIAOMI_POCO = "poco"
    const val BRAND_XIAOMI_REDMI = "redmi"
    const val PACKAGE_XIAOMI_MAIN = "com.miui.securitycenter"
    const val PACKAGE_XIAOMI_COMPONENT = "com.miui.permcenter.autostart.AutoStartManagementActivity"

    const val BRAND_LETV = "letv"
    const val PACKAGE_LETV_MAIN = "com.letv.android.letvsafe"
    const val PACKAGE_LETV_COMPONENT = "com.letv.android.letvsafe.AutobootManageActivity"

    const val BRAND_ASUS = "asus"
    const val PACKAGE_ASUS_MAIN = "com.asus.mobilemanager"
    const val PACKAGE_ASUS_COMPONENT = "com.asus.mobilemanager.powersaver.PowerSaverSettings"
    const val PACKAGE_ASUS_COMPONENT_FALLBACK = "com.asus.mobilemanager.autostart.AutoStartActivity"

    const val BRAND_HONOR = "honor"
    const val PACKAGE_HONOR_MAIN = "com.huawei.systemmanager"
    const val PACKAGE_HONOR_COMPONENT = "com.huawei.systemmanager.optimize.process.ProtectActivity"

    const val BRAND_HUAWEI = "huawei"
    const val PACKAGE_HUAWEI_MAIN = "com.huawei.systemmanager"
    const val PACKAGE_HUAWEI_COMPONENT = "com.huawei.systemmanager.optimize.process.ProtectActivity"
    const val PACKAGE_HUAWEI_COMPONENT_FALLBACK = "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"

    const val BRAND_OPPO = "oppo"
    const val PACKAGE_OPPO_MAIN = "com.coloros.safecenter"
    const val PACKAGE_OPPO_FALLBACK = "com.oppo.safe"
    const val PACKAGE_OPPO_COMPONENT = "com.coloros.safecenter.permission.startup.StartupAppListActivity"
    const val PACKAGE_OPPO_COMPONENT_FALLBACK = "com.oppo.safe.permission.startup.StartupAppListActivity"
    const val PACKAGE_OPPO_COMPONENT_FALLBACK_A = "com.coloros.safecenter.startupapp.StartupAppListActivity"

    const val BRAND_VIVO = "vivo"
    const val PACKAGE_VIVO_MAIN = "com.iqoo.secure"
    const val PACKAGE_VIVO_FALLBACK = "com.vivo.permissionmanager"
    const val PACKAGE_VIVO_COMPONENT = "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
    const val PACKAGE_VIVO_COMPONENT_FALLBACK = "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
    const val PACKAGE_VIVO_COMPONENT_FALLBACK_A = "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"

    const val BRAND_NOKIA = "nokia"
    const val PACKAGE_NOKIA_MAIN = "com.evenwell.powersaving.g3"
    const val PACKAGE_NOKIA_COMPONENT = "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"

    const val BRAND_SAMSUNG = "samsung"
    const val PACKAGE_SAMSUNG_MAIN1 = "com.samsung.android.lool"
    const val PACKAGE_SAMSUNG_MAIN2 = "com.samsung.android.sm"
    const val PACKAGE_SAMSUNG_COMPONENT1 = "com.samsung.android.sm.ui.battery.BatteryActivity"
    const val PACKAGE_SAMSUNG_COMPONENT2 = "com.samsung.android.sm.battery.ui.BatteryActivity"

    const val BRAND_ONE_PLUS = "oneplus"
    const val PACKAGE_ONE_PLUS_MAIN = "com.oneplus.security"
    const val PACKAGE_ONE_PLUS_COMPONENT = "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
}
