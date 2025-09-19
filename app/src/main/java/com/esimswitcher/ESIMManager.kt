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
        Log.d(TAG, "Attempting to switch to profile: ${profile.displayName} (ID: ${profile.subscriptionId})")
        
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            
            // For Android 10+, use the modern approach
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    // Method 1: Try setPreferredDataSubscriptionId (Android R/API 30+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Log.d(TAG, "Trying setPreferredDataSubscriptionId for profile ${profile.subscriptionId}")
                        val method = subscriptionManager.javaClass.getDeclaredMethod(
                            "setPreferredDataSubscriptionId",
                            Int::class.java,
                            Boolean::class.java,
                            java.util.concurrent.Executor::class.java,
                            java.util.function.Consumer::class.java
                        )
                        method.isAccessible = true
                        method.invoke(
                            subscriptionManager,
                            profile.subscriptionId,
                            false,
                            context.mainExecutor
                        ) { result: Int ->
                            Log.d(TAG, "setPreferredDataSubscriptionId result: $result")
                            // Success is typically 0, but let's also verify by checking active subscription
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                val currentActiveId = getActiveSubscriptionId(subscriptionManager)
                                val success = currentActiveId == profile.subscriptionId
                                Log.d(TAG, "Modern API switch verification: currentActive=$currentActiveId, target=${profile.subscriptionId}, success=$success")
                                callback(success)
                            }, 1500)
                        }
                    } else {
                        Log.d(TAG, "setPreferredDataSubscriptionId not available on this Android version, trying alternative")
                        tryAlternativeSwitchMethod(context, subscriptionManager, profile, callback)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "setPreferredDataSubscriptionId failed: ${e.message}")
                    tryAlternativeSwitchMethod(context, subscriptionManager, profile, callback)
                }
            } else {
                // For older Android versions
                Log.w(TAG, "eSIM switching not fully supported on Android versions below 10")
                tryLegacySwitchMethod(context, subscriptionManager, profile, callback)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied while switching eSIM profile: ${e.message}", e)
            callback(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error switching eSIM profile: ${e.message}", e)
            callback(false)
        }
    }
    
    private fun tryAlternativeSwitchMethod(context: Context, subscriptionManager: SubscriptionManager, profile: ESIMProfile, callback: (Boolean) -> Unit) {
        try {
            Log.d(TAG, "Trying alternative switch method for profile ${profile.subscriptionId}")
            
            // Create a proper broadcast intent for the result
            val intent = Intent("com.esimswitcher.ESIM_SWITCH_RESULT")
            intent.putExtra("subscription_id", profile.subscriptionId)
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                profile.subscriptionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Use the deprecated but sometimes working switchToSubscription
            @Suppress("DEPRECATION")
            subscriptionManager.switchToSubscription(profile.subscriptionId, pendingIntent)
            
            // Since we can't easily track the broadcast result here, 
            // we'll check the subscription status after a delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val currentActiveId = getActiveSubscriptionId(subscriptionManager)
                val success = currentActiveId == profile.subscriptionId
                Log.d(TAG, "Switch result check: currentActive=$currentActiveId, target=${profile.subscriptionId}, success=$success")
                callback(success)
            }, 2000) // Wait 2 seconds for the switch to complete
            
        } catch (e: Exception) {
            Log.e(TAG, "Alternative switch method failed: ${e.message}", e)
            callback(false)
        }
    }
    
    private fun tryLegacySwitchMethod(context: Context, subscriptionManager: SubscriptionManager, profile: ESIMProfile, callback: (Boolean) -> Unit) {
        try {
            Log.d(TAG, "Trying legacy switch method for profile ${profile.subscriptionId}")
            
            // For older versions, we can try to set the default subscription using reflection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    // Use reflection to access the hidden setDefaultDataSubId methods
                    val setDefaultDataMethod = SubscriptionManager::class.java.getDeclaredMethod("setDefaultDataSubId", Int::class.java)
                    setDefaultDataMethod.isAccessible = true
                    setDefaultDataMethod.invoke(null, profile.subscriptionId)
                    
                    val setDefaultSmsMethod = SubscriptionManager::class.java.getDeclaredMethod("setDefaultSmsSubId", Int::class.java)
                    setDefaultSmsMethod.isAccessible = true
                    setDefaultSmsMethod.invoke(null, profile.subscriptionId)
                    
                    val setDefaultVoiceMethod = SubscriptionManager::class.java.getDeclaredMethod("setDefaultVoiceSubId", Int::class.java)
                    setDefaultVoiceMethod.isAccessible = true
                    setDefaultVoiceMethod.invoke(null, profile.subscriptionId)
                    
                    Log.d(TAG, "Called legacy setDefault methods via reflection")
                    
                    // Check if it worked
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val currentActiveId = getActiveSubscriptionId(subscriptionManager)
                        val success = currentActiveId == profile.subscriptionId
                        Log.d(TAG, "Legacy switch result: currentActive=$currentActiveId, target=${profile.subscriptionId}, success=$success")
                        callback(success)
                    }, 1000)
                } catch (e: Exception) {
                    Log.w(TAG, "Legacy reflection methods failed: ${e.message}")
                    // Final fallback - just try to enable the subscription
                    trySimpleEnableMethod(context, subscriptionManager, profile, callback)
                }
            } else {
                Log.w(TAG, "No eSIM switching support for this Android version")
                callback(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Legacy switch method failed: ${e.message}", e)
            callback(false)
        }
    }
    
    private fun trySimpleEnableMethod(context: Context, subscriptionManager: SubscriptionManager, profile: ESIMProfile, callback: (Boolean) -> Unit) {
        try {
            Log.d(TAG, "Trying simple enable method for profile ${profile.subscriptionId}")
            
            // Last resort: try to use any available method to activate the subscription
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val enableMethod = subscriptionManager.javaClass.getDeclaredMethod("setSubscriptionEnabled", Int::class.java, Boolean::class.java)
                    enableMethod.isAccessible = true
                    enableMethod.invoke(subscriptionManager, profile.subscriptionId, true)
                    Log.d(TAG, "Called setSubscriptionEnabled via reflection")
                    
                    // Check if it worked
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val currentActiveId = getActiveSubscriptionId(subscriptionManager)
                        val success = currentActiveId == profile.subscriptionId
                        Log.d(TAG, "Simple enable result: currentActive=$currentActiveId, target=${profile.subscriptionId}, success=$success")
                        callback(success)
                    }, 1500)
                } catch (e: Exception) {
                    Log.w(TAG, "setSubscriptionEnabled failed: ${e.message}")
                    callback(false)
                }
            } else {
                Log.w(TAG, "No enable method available for this Android version")
                callback(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Simple enable method failed: ${e.message}", e)
            callback(false)
        }
    }
}