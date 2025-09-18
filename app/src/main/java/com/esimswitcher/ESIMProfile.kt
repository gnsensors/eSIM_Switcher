package com.esimswitcher

data class ESIMProfile(
    val subscriptionId: Int,
    val iccId: String,
    val displayName: String,
    val carrierName: String,
    val isActive: Boolean,
    val isEmbedded: Boolean
) {
    fun getFormattedIccId(): String {
        return if (iccId.length > 10) {
            "${iccId.substring(0, 4)}...${iccId.takeLast(4)}"
        } else {
            iccId
        }
    }
}