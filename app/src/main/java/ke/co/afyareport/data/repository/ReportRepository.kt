package ke.co.afyareport.data.repository

import ke.co.afyareport.data.local.dao.ReportDao
import ke.co.afyareport.data.local.entity.PatientEntryEntity
import ke.co.afyareport.data.local.entity.ReportIndicatorEntity
import ke.co.afyareport.data.local.entity.ReportSettingsEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableValueEntity

class ReportRepository(private val dao: ReportDao) {
    suspend fun getIndicators(registerId: Long): List<ReportIndicatorEntity> = dao.getIndicators(registerId)
    suspend fun saveIndicator(indicator: ReportIndicatorEntity): Long = dao.upsertIndicator(indicator)
    suspend fun deleteIndicator(id: Long) = dao.deleteIndicator(id)
    suspend fun getSettings(registerId: Long): ReportSettingsEntity =
        dao.getSettings(registerId) ?: ReportSettingsEntity(registerDefinitionId = registerId)
    suspend fun saveSettings(settings: ReportSettingsEntity) = dao.upsertSettings(settings)

    suspend fun getAllVariables(registerId: Long): List<VariableDefinitionEntity> = dao.getAllVariables(registerId)
    suspend fun getEntriesForRegister(registerId: Long): List<PatientEntryEntity> = dao.getEntriesForRegister(registerId)
    suspend fun getValuesForRegister(registerId: Long): List<VariableValueEntity> = dao.getValuesForRegister(registerId)
}
