package com.salespro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = 1")
    fun getProfile(): Flow<BusinessProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: BusinessProfile)

    @Query("DELETE FROM business_profile WHERE id = 1")
    suspend fun resetProfile()
}
