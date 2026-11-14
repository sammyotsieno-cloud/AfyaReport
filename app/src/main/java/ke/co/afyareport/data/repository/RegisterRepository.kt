package ke.co.afyareport.data.repository

import ke.co.afyareport.data.local.dao.RegisterDefinitionDao
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.RegisterDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import kotlinx.coroutines.flow.Flow

class RegisterRepository(private val dao: RegisterDefinitionDao) {
    fun observeAllRegisters(): Flow<List<RegisterDefinitionEntity>> = dao.observeAll()

    suspend fun getRegister(id: Long): RegisterDefinitionEntity? = dao.getById(id)

    suspend fun getVariablesForVersion(registerId: Long, version: Int): List<VariableDefinitionEntity> =
        dao.getVariablesForVersion(registerId, version)

    suspend fun getLatestVariables(registerId: Long): List<VariableDefinitionEntity> =
        dao.getLatestVariables(registerId)

    /** Compatibility alias for screens intentionally showing the latest definition. */
    suspend fun getVariables(registerId: Long): List<VariableDefinitionEntity> =
        getLatestVariables(registerId)

    suspend fun getCodeOptions(variableId: Long): List<CodeOptionEntity> =
        dao.getCodeOptions(variableId)

    suspend fun createRegister(
        name: String,
        description: String = "",
        variablesWithOptions: List<Pair<VariableDefinitionEntity, List<CodeOptionEntity>>>
    ): Long {
        val register = RegisterDefinitionEntity(
            name = name.trim(),
            description = description.trim(),
            version = 1
        )
        return dao.insertFullRegister(register, variablesWithOptions)
    }

    suspend fun createNewVersion(
        register: RegisterDefinitionEntity,
        variablesWithOptions: List<Pair<VariableDefinitionEntity, List<CodeOptionEntity>>>
    ) {
        dao.insertNewVersion(
            register.copy(name = register.name.trim(), description = register.description.trim()),
            register.version + 1,
            variablesWithOptions
        )
    }

    suspend fun updateRegisterMetadata(register: RegisterDefinitionEntity) {
        dao.update(register.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteRegister(id: Long) {
        dao.deleteById(id)
    }
}
