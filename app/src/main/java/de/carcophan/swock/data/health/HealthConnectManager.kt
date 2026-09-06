package de.carcophan.swock.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

data class HealthCaloriesResult(
    val totalCaloriesBurned: Int,
    val activeCaloriesBurned: Int,
    val projectedTotalForDay: Int = 0
)

class HealthConnectManager(private val context: Context) {

    val healthConnectClient by lazy {
        if (isAvailable()) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    fun getSdkStatus(): Int {
        return HealthConnectClient.getSdkStatus(context)
    }

    fun isAvailable(): Boolean {
        return getSdkStatus() == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getBurnedCalories(startTime: Instant, endTime: Instant): HealthCaloriesResult {
        val client = healthConnectClient ?: return HealthCaloriesResult(0, 0)
        return try {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val totalKcal = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.toInt() ?: 0
            val activeKcal = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toInt() ?: 0

            HealthCaloriesResult(
                totalCaloriesBurned = totalKcal,
                activeCaloriesBurned = activeKcal
            )
        } catch (e: Exception) {
            HealthCaloriesResult(0, 0)
        }
    }
}
