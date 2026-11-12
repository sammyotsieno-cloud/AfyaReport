package ke.co.afyareport.data.local

import androidx.room.TypeConverter

/**
 * Room type converters for the domain enums.
 */
class Converters {

    @TypeConverter
    fun fromVariableType(value: VariableType): String = value.name

    @TypeConverter
    fun toVariableType(value: String): VariableType = VariableType.valueOf(value)

    @TypeConverter
    fun fromVisitType(value: VisitType): String = value.name

    @TypeConverter
    fun toVisitType(value: String): VisitType = VisitType.valueOf(value)
}
