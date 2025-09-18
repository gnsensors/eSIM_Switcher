package com.esimswitcher

import android.content.Context
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
                
                val subscriptionInfos = subscriptionManager.availableSubscriptionInfoList ?: emptyList()
                val activeSubscriptionId = getActiveSubscriptionId(subscriptionManager)
                
                for (subscriptionInfo in subscriptionInfos) {
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                
                val result = subscriptionManager.switchToSubscription(
                    profile.subscriptionId,
                    null,
                    context.mainExecutor
                ) { resultCode ->
                    callback(resultCode == TelephonyManager.SET_OPPORTUNISTIC_SUB_SUCCESS)
                }
                
                if (!result) {
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