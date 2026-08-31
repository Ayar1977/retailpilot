package com.salespro.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMeasurementType(value: MeasurementType?): String? = value?.name

    @TypeConverter
    fun toMeasurementType(value: String?): MeasurementType? = value?.let {
        try { enumValueOf<MeasurementType>(it) } catch (e: Exception) { null }
    }

    @TypeConverter
    fun fromSaleStatus(value: SaleStatus?): String? = value?.name

    @TypeConverter
    fun toSaleStatus(value: String?): SaleStatus? = value?.let {
        try { enumValueOf<SaleStatus>(it) } catch (e: Exception) { null }
    }

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus?): String? = value?.name

    @TypeConverter
    fun toSyncStatus(value: String?): SyncStatus? = value?.let {
        try { enumValueOf<SyncStatus>(it) } catch (e: Exception) { null }
    }

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus?): String? = value?.name

    @TypeConverter
    fun toPaymentStatus(value: String?): PaymentStatus? = value?.let {
        try { enumValueOf<PaymentStatus>(it) } catch (e: Exception) { null }
    }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? = value?.let {
        try { enumValueOf<PaymentMethod>(it) } catch (e: Exception) { null }
    }

    @TypeConverter
    fun fromStockMovementType(value: StockMovementType?): String? = value?.name

    @TypeConverter
    fun toStockMovementType(value: String?): StockMovementType? = value?.let {
        try { enumValueOf<StockMovementType>(it) } catch (e: Exception) { null }
    }
}
