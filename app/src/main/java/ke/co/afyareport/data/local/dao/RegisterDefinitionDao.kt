package ke.co.afyareport.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ke.co.afyareport.data.local.entity.CodeOptionEntity
import ke.co.afyareport.data.local.entity.RegisterDefinitionEntity
import ke.co.afyareport.data.local.entity.VariableDefinitionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegisterDefinitionDao {
    @Query("SELECT * FROM register_definitions ORDER BY name ASC")
    fun observeAll(): Flow<List<RegisterDefinitionEntity>>

    @Query("SELECT * FROM register_definitions WHERE id = :id")
    suspend fun getById(id: Long): RegisterDefinitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(register: RegisterDefinitionEntity): Long

    @Update
    suspend fun update(register: RegisterDefinitionEntity)

    @Query("DELETE FROM register_definitions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT * FROM variable_definitions
        WHERE registerDefinitionId = :registerId AND registerVersion = :version
        ORDER BY sortOrder ASC
    """)
    suspend fun getVariablesForVersion(registerId: Long, version: Int): List<VariableDefinitionEntity>

    @Query("""
        SELECT * FROM variable_definitions
        WHERE registerDefinitionId = :registerId
          AND registerVersion = (SELECT version FROM register_definitions WHERE id = :registerId)
        ORDER BY sortOrder ASC
    """)
    suspend fun getLatestVariables(registerId: Long): List<VariableDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariable(variable: VariableDefinitionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCodeOptions(options: List<CodeOptionEntity>): List<Long>

    @Query("SELECT * FROM code_options WHERE variableDefinitionId = :variableId ORDER BY sortOrder ASC")
    suspend fun getCodeOptions(variableId: Long): List<CodeOptionEntity>

    @Transaction
    suspend fun insertFullRegister(
        register: RegisterDefinitionEntity,
        variablesWithOptions: List<Pair<VariableDefinitionEntity, List<CodeOptionEntity>>>
    ): Long {
        val registerId = insert(register.copy(version = 1))
        variablesWithOptions.forEachIndexed { index, (variable, options) ->
            val variableId = insertVariable(variable.copy(
                id = 0,
                registerDefinitionId = registerId,
                registerVersion = 1,
                sortOrder = index
            ))
            if (options.isNotEmpty()) {
                insertCodeOptions(options.mapIndexed { optionIndex, option ->
                    option.copy(id = 0, variableDefinitionId = variableId, sortOrder = optionIndex)
                })
            }
        }
        return registerId
    }

    @Transaction
    suspend fun insertNewVersion(
        register: RegisterDefinitionEntity,
        newVersion: Int,
        variablesWithOptions: List<Pair<VariableDefinitionEntity, List<CodeOptionEntity>>>
    ) {
        update(register.copy(version = newVersion, updatedAt = System.currentTimeMillis()))
        variablesWithOptions.forEachIndexed { index, (variable, options) ->
            val variableId = insertVariable(variable.copy(
                id = 0,
                registerDefinitionId = register.id,
                registerVersion = newVersion,
                sortOrder = index
            ))
            if (options.isNotEmpty()) {
                insertCodeOptions(options.mapIndexed { optionIndex, option ->
                    option.copy(id = 0, variableDefinitionId = variableId, sortOrder = optionIndex)
                })
            }
        }
    }
}
