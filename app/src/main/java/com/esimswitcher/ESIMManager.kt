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
                    Log.d(TAG, "Found ${activeSubscriptions.size} active subscriptions")
                    allSubscriptionInfos.addAll(activeSubscriptions)
                    
                    // Log all active subscriptions
                    activeSubscriptions.forEachIndexed { index, sub ->
                        Log.d(TAG, "Active subscription $index: ID=${sub.subscriptionId}, isEmbedded=${sub.isEmbedded}, carrier=${sub.carrierName}, display=${sub.displayName}")
                    }
                    
                    // Try additional methods to get all subscriptions including inactive ones
                    try {
                        // Method 1: Try getAvailableSubscriptionInfoList (Android P+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            try {
                                val availableMethod = subscriptionManager.javaClass.getDeclaredMethod("getAvailableSubscriptionInfoList")
                                availableMethod.isAccessible = true
                                @Suppress("UNCHECKED_CAST")
                                val availableSubscriptions = availableMethod.invoke(subscriptionManager) as? List<SubscriptionInfo>
                                Log.d(TAG, "Found ${availableSubscriptions?.size ?: 0} available subscriptions via getAvailableSubscriptionInfoList")
                                availableSubscriptions?.let { subs ->
                                    subs.forEachIndexed { index, sub ->
                                        Log.d(TAG, "Available subscription $index: ID=${sub.subscriptionId}, isEmbedded=${sub.isEmbedded}, carrier=${sub.carrierName}")
                                        if (allSubscriptionInfos.none { it.subscriptionId == sub.subscriptionId }) {
                                            allSubscriptionInfos.add(sub)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not access getAvailableSubscriptionInfoList: ${e.message}")
                            }
                        }
                        
                        // Method 2: Try getAccessibleSubscriptionInfoList (Android Q+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            try {
                                val accessibleMethod = subscriptionManager.javaClass.getDeclaredMethod("getAccessibleSubscriptionInfoList")
                                accessibleMethod.isAccessible = true
                                @Suppress("UNCHECKED_CAST")
                                val accessibleSubscriptions = accessibleMethod.invoke(subscriptionManager) as? List<SubscriptionInfo>
                                Log.d(TAG, "Found ${accessibleSubscriptions?.size ?: 0} accessible subscriptions via getAccessibleSubscriptionInfoList")
                                accessibleSubscriptions?.let { subs ->
                                    subs.forEachIndexed { index, sub ->
                                        Log.d(TAG, "Accessible subscription $index: ID=${sub.subscriptionId}, isEmbedded=${sub.isEmbedded}, carrier=${sub.carrierName}")
                                        if (allSubscriptionInfos.none { it.subscriptionId == sub.subscriptionId }) {
                                            allSubscriptionInfos.add(sub)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not access getAccessibleSubscriptionInfoList: ${e.message}")
                            }
                        }
                        
                        // Method 3: Try getAllSubscriptionInfoList (hidden API)
                        try {
                            val allMethod = subscriptionManager.javaClass.getDeclaredMethod("getAllSubscriptionInfoList")
                            allMethod.isAccessible = true
                            @Suppress("UNCHECKED_CAST")
                            val allSubscriptions = allMethod.invoke(subscriptionManager) as? List<SubscriptionInfo>
                            Log.d(TAG, "Found ${allSubscriptions?.size ?: 0} total subscriptions via getAllSubscriptionInfoList")
                            allSubscriptions?.let { subs ->
                                subs.forEachIndexed { index, sub ->
                                    Log.d(TAG, "All subscription $index: ID=${sub.subscriptionId}, isEmbedded=${sub.isEmbedded}, carrier=${sub.carrierName}")
                                    if (allSubscriptionInfos.none { it.subscriptionId == sub.subscriptionId }) {
                                        allSubscriptionInfos.add(sub)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not access getAllSubscriptionInfoList: ${e.message}")
                        }
                        
                    } catch (e: Exception) {
                        Log.w(TAG, "Error trying additional subscription methods: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting subscription info lists", e)
                }
                
                val activeSubscriptionId = getActiveSubscriptionId(subscriptionManager)
                
                Log.d(TAG, "Processing ${allSubscriptionInfos.size} total subscriptions for eSIM profiles")
                for (subscriptionInfo in allSubscriptionInfos) {
                    Log.d(TAG, "Checking subscription ID=${subscriptionInfo.subscriptionId}, isEmbedded=${subscriptionInfo.isEmbedded}")
                    if (subscriptionInfo.isEmbedded) {
                        val profile = ESIMProfile(
                            subscriptionId = subscriptionInfo.subscriptionId,
                            iccId = subscriptionInfo.iccId ?: "Unknown",
                            displayName = subscriptionInfo.displayName?.toString() ?: "eSIM Profile",
                            carrierName = subscriptionInfo.carrierName?.toString() ?: "Unknown Carrier",
                            isActive = subscriptionInfo.subscriptionId == activeSubscriptionId,
                            isEmbedded = true
                        )
                        Log.d(TAG, "Created eSIM profile: ${profile.displayName} (${profile.carrierName}) - Active: ${profile.isActive}")
                        profiles.add(profile)
                    }
                }
                
                Log.d(TAG, "Returning ${profiles.size} eSIM profiles total")
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