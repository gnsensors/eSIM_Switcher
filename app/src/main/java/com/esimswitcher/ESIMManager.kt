package com.esimswitcher

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi

class ESIMManager {
    
    companion object {
        private const val TAG = "ESIMManager"
    }
    
    fun getESIMProfiles(context: Context): List<ESIMProfile> {
        val profiles = mutableListOf<ESIMProfile>()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                
                // Get all subscriptions (both active and inactive)
                val allSubscriptionInfos = mutableListOf<SubscriptionInfo>()
                
                // Try to get all subscription info including inactive ones
                try {
                    // First get active subscriptions
                    val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList ?: emptyList<SubscriptionInfo>()
                    allSubscriptionInfos.addAll(activeSubscriptions)
                    
                    // For Android Q+, try to get accessible subscriptions using reflection as a fallback
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            val method = subscriptionManager.javaClass.getDeclaredMethod("getAccessibleSubscriptionInfoList")
                            method.isAccessible = true
                            @Suppress("UNCHECKED_CAST")
                            val accessibleSubscriptions = method.invoke(subscriptionManager) as? List<SubscriptionInfo>
                            accessibleSubscriptions?.let { subs ->
                                // Add any subscriptions not already in the list
                                subs.forEach { sub ->
                                    if (allSubscriptionInfos.none { it.subscriptionId == sub.subscriptionId }) {
                                        allSubscriptionInfos.add(sub)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Could not access getAccessibleSubscriptionInfoList", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting subscription info lists", e)
                }
                
                val activeSubscriptionId = getActiveSubscriptionId(subscriptionManager)
                
                for (subscriptionInfo in allSubscriptionInfos) {
                    if (subscriptionInfo.isEmbedded) {
                        val profile = ESIMProfile(
                            subscriptionId = subscriptionInfo.subscriptionId,
                            iccId = subscriptionInfo.iccId ?: "Unknown",
                            displayName = subscriptionInfo.displayName?.toString() ?: "eSIM Profile",
                            carrierName = subscriptionInfo.carrierName?.toString() ?: "Unknown Carrier",
                            isActive = subscriptionInfo.subscriptionId == activeSubscriptionId,
                            isEmbedded = true
                        )
                        profiles.add(profile)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied while accessing subscription info", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving eSIM profiles", e)
        }
        
        return profiles
    }
    
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private fun getActiveSubscriptionId(subscriptionManager: SubscriptionManager): Int {
        return try {
            val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
            activeSubscriptionInfoList?.firstOrNull()?.subscriptionId ?: -1
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active subscription ID", e)
            -1
        }
    }
    
    fun switchToProfile(context: Context, profile: ESIMProfile, callback: (Boolean) -> Unit) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                
                // Create a PendingIntent for the callback
                val intent = Intent()
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                try {
                    subscriptionManager.switchToSubscription(
                        profile.subscriptionId,
                        pendingIntent
                    )
                    callback(true)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to switch subscription", e)
                    callback(false)
                }
            } else {
                Log.w(TAG, "eSIM switching not supported on this Android version")
                callback(false)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied while switching eSIM profile", e)
            callback(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error switching eSIM profile", e)
            callback(false)
        }
    }
}