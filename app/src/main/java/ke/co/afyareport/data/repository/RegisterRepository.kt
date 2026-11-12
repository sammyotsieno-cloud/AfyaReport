package ke.co.afyareport.data.repository

import ke.co.afyareport.data.local.dao.RegisterDefinitionDao
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.RegisterDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for register definitions, their variables and code options.
 */
class RegisterRepository(
    private val dao: RegisterDefinitionDao
) {

    fun observeAllRegisters(): Flow<List<RegisterDefinitionEntity>> =
        dao.observeAll()

    suspend fun getRegister(id: Long): RegisterDefinitionEntity? =
        dao.getById(id)

    suspend fun getVariables(registerId: Long): List<VariableDefinitionEntity> =
        dao.getVariablesForRegister(registerId)

    suspend fun getCodeOptions(variableId: Long): List<CodeOptionEntity> =
        dao.getCodeOptions(variableId)

    /**
     * Creates a new register together with its variables and any coded options.
     * Returns the new register id.
     */
    suspend fun createRegister(
        name: String,
        description: String = "",
        variablesWithOptions: List<Pair<VariableDefinitionEntity, List<CodeOptionEntity>>>
    ): Long {
        val register = RegisterDefinitionEntity(
            name = name.trim(),
            description = description.trim()
        )
        return dao.insertFullRegister(register, variablesWithOptions)
    }

    suspend fun updateRegister(register: RegisterDefinitionEntity) {
        dao.update(
            register.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteRegister(id: Long) {
        dao.deleteById(id)
    }
}
