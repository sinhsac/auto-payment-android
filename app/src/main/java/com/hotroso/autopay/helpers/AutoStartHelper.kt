package com.hotroso.autopay.helpers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.ApplicationInfo
import android.os.Build
import android.widget.Toast
import com.hotroso.autopay.common.Constants
import java.util.Locale

class AutoStartHelper private constructor() {

    fun getAutoStartPermission(context: Context) {
        when (Build.BRAND.lowercase(Locale.getDefault())) {
            Constants.BRAND_ASUS -> autoStartAsus(context)
            Constants.BRAND_XIAOMI, Constants.BRAND_XIAOMI_POCO, Constants.BRAND_XIAOMI_REDMI -> autoStartXiaomi(context)
            Constants.BRAND_LETV -> autoStartLetv(context)
            Constants.BRAND_HONOR -> autoStartHonor(context)
            Constants.BRAND_HUAWEI -> autoStartHuawei(context)
            Constants.BRAND_OPPO -> autoStartOppo(context)
            Constants.BRAND_VIVO -> autoStartVivo(context)
            Constants.BRAND_NOKIA -> autoStartNokia(context)
            Constants.BRAND_SAMSUNG -> autoStartSamsung(context)
            Constants.BRAND_ONE_PLUS -> autoStartOnePlus(context)
            else -> Toast.makeText(context, "Không hỗ trợ thiết bị này", Toast.LENGTH_SHORT).show()
        }
    }

    private fun autoStartSamsung(context: Context) {
        val pkg = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) Constants.PACKAGE_SAMSUNG_MAIN1 else Constants.PACKAGE_SAMSUNG_MAIN2
        if (isPackageExists(context, pkg)) {
            try { startIntent(context, pkg, Constants.PACKAGE_SAMSUNG_COMPONENT1) }
            catch (e: Exception) {
                try { startIntent(context, pkg, Constants.PACKAGE_SAMSUNG_COMPONENT2) } catch (e1: Exception) { e1.printStackTrace() }
            }
        }
    }

    private fun autoStartAsus(context: Context) {
        if (isPackageExists(context, Constants.PACKAGE_ASUS_MAIN)) {
            try { startIntent(context, Constants.PACKAGE_ASUS_MAIN, Constants.PACKAGE_ASUS_COMPONENT) }
            catch (e: Exception) {
                try { startIntent(context, Constants.PACKAGE_ASUS_MAIN, Constants.PACKAGE_ASUS_COMPONENT_FALLBACK) } catch (e1: Exception) { e1.printStackTrace() }
            }
        }
    }

    private fun autoStartXiaomi(context: Context) {
        if (isPackageExists(context, Constants.PACKAGE_XIAOMI_MAIN)) {
            try { startIntent(context, Constants.PACKAGE_XIAOMI_MAIN, Constants.PACKAGE_XIAOMI_COMPONENT) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun autoStartLetv(context: Context) {
        if (isPackageExists(context, Constants.PACKAGE_LETV_MAIN)) {
            try { startIntent(context, Constants.PACKAGE_LETV_MAIN, Constants.PACKAGE_LETV_COMPONENT) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun autoStartHonor(context: Context) {
        if (isPackageExists(context, Constants.PACKAGE_HONOR_MAIN)) {
            try { startIntent(context, Constants.PACKAGE_HONOR_MAIN, Constants.PACKAGE_HONOR_COMPONENT) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun autoStartHuawei(context: Context) {
        if (isPackageExists(context, Constants.PACKAGE_HUAWEI_MAIN)) {
            try { startIntent(context, Constants.PACKAGE_HUAWEI_MAIN, Constants.PACKAGE_HUAWEI_COMPONENT) }
            catch (e: Exception) {
                try { startIntent(context, Constants.PACKAGE_HUAWEI_MAIN, Constants.PACKAGE_HUAWEI_COMPONENT_FALLBACK) } catch (e1: Exception) { e1.printStackTrace() }
            }
        }
    }

    private fun autoStartOppo(context: Context) {
        if (isPackageExists(context, Constants.PACKAGE_OPPO_MAIN) || isPackageExists(context, Constants.PACKAGE_OPPO_FALLBACK)) {
            try { startIntent(context, Constants.PACKAGE_OPPO_MAIN, Constants.PACKAGE_OPPO_COMPONENT) }
            catch (e: Exception) {
                try { startIntent(context, Constants.PACKAGE_OPPO_FALLBACK, Constants.PACKAGE_OPPO_COMPONENT_FALLBACK) }
                catch (ex: Exception) {
                    try { startIntent(context, Constants.PACKAGE_OPPO_MAIN, Constants.PACKAGE_OPPO_COMPONENT_FALLBACK_A) } catch (exx: Exception) { exx.printStackTrace() }
                }
            }
        }
    }

    private fun autoStartVivo(context: Context) {
        if (isPackageExists(context, Constants.PACKAGE_VIVO_MAIN) || isPackageExists(context, Constants.PACKAGE_VIVO_FALLBACK)) {
            try { startIntent(context, Constants.PACKAGE_VIVO_MAIN, Constants.PACKAGE_VIVO_COMPONENT) }
            catch (e: Exception) {
                try { startIntent(context, Constants.PACKAGE_VIVO_FALLBACK, Constants.PACKAGE_VIVO_COMPONENT_FALLBACK) }
                catch (ex: Exception) {
                    try { startIntent(context, Constants.PACKAGE_VIVO_MAIN, Constants.PACKAGE_VIVO_COMPONENT_FALLBACK_A) } catch (exx: Exception) { exx.printStackTrace() }
                }
            }
        }
    }

    private fun autoStartNokia(context: Context) {
        if (isPackageExists(context, Constants.PACKAGE_NOKIA_MAIN)) {
            try { startIntent(context, Constants.PACKAGE_NOKIA_MAIN, Constants.PACKAGE_NOKIA_COMPONENT) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun autoStartOnePlus(context: Context) {
        if (isPackageExists(context, Constants.PACKAGE_ONE_PLUS_MAIN)) {
            try { startIntent(context, Constants.PACKAGE_ONE_PLUS_MAIN, Constants.PACKAGE_ONE_PLUS_COMPONENT) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    @Throws(Exception::class)
    private fun startIntent(context: Context, packageName: String, componentName: String) {
        val intent = Intent().apply {
            component = ComponentName(packageName, componentName)
            addFlags(FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isPackageExists(context: Context, targetPackage: String): Boolean {
        return context.packageManager.getInstalledApplications(0)
            .any { it.packageName == targetPackage }
    }

    companion object {
        @JvmStatic
        val instance: AutoStartHelper get() = AutoStartHelper()
    }
}
