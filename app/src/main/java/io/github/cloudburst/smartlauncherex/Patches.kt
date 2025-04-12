package io.github.cloudburst.smartlauncherex

import android.content.Intent
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.cloudburst.smartlauncherex.Module.TAG
import org.luckypray.dexkit.DexKitBridge

fun patchAnalytics(cl: ClassLoader) {
    val firebase = cl.loadClass("com.google.firebase.analytics.FirebaseAnalytics")

    XposedBridge.hookAllMethods(firebase, "logEvent", object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam?) {
            param?.result = null
        }
    });
}

fun patchPurchasableItem(bridge: DexKitBridge, cl: ClassLoader): Any? = try {
    val purchasableItem = bridge.findClass {
        matcher {
            usingStrings(
                "ginlemon.action.hasPremiumAccessChanged",
                "PurchasableItem(activationString="
            )
        }
    }

    val isActivated = purchasableItem.findMethod {
        matcher {
            returnType = "boolean"
            paramCount = 0
        }
    }.first()

    XposedBridge.hookMethod(isActivated.getMethodInstance(cl), object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam?) {
            param?.result = true
        }
    })
} catch (e: Exception) {
    Log.e(TAG, "Failed to patch purchasable item", e)
}

fun patchHomeScreen(cl: ClassLoader): Any? = try {
    val onboarding = cl.loadClass("ginlemon.flower.HomeScreen")

    XposedBridge.hookAllMethods(onboarding, "onCreate", object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam?) {
            var intent = Intent("ginlemon.action.hasPremiumAccessChanged")
            var context = param?.thisObject as android.app.Activity

            // broadcast the intent
            context.sendBroadcast(intent)
            Log.d(TAG, "Broadcasting ginlemon.action.hasPremiumAccessChanged")
        }
    })
} catch (e: Exception) {
    Log.e(TAG, "Failed to patch onboarding", e)
}