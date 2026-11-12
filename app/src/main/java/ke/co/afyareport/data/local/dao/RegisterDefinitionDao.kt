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

    // ── Variables ──────────────────────────────────────────────

    @Query("SELECT * FROM variable_definitions WHERE registerDefinitionId = :registerId ORDER BY sortOrder ASC")
    suspend fun getVariablesForRegister(registerId: Long): List<VariableDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariable(variable: VariableDefinitionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariables(variables: List<VariableDefinitionEntity>): List<Long>

    @Query("DELETE FROM variable_definitions WHERE registerDefinitionId = :registerId")
    suspend fun deleteVariablesForRegister(registerId: Long)

    // ── Code options ───────────────────────────────────────────

    @Query("SELECT * FROM code_options WHERE variableDefinitionId = :variableId ORDER BY sortOrder ASC")
    suspend fun getCodeOptions(variableId: Long): List<CodeOptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCodeOption(option: CodeOptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCodeOptions(options: List<CodeOptionEntity>): List<Long>

    @Query("DELETE FROM code_options WHERE variableDefinitionId = :variableId")
    suspend fun deleteCodeOptionsForVariable(variableId: Long)

    /**
     * Convenience transaction: insert a full register definition
     * together with its variables and any coded options.
     */
    @Transaction
    suspend fun insertFullRegister(
        register: RegisterDefinitionEntity,
        variablesWithOptions: List<Pair<VariableDefinitionEntity, List<CodeOptionEntity>>>
    ): Long {
        val registerId = insert(register)
        variablesWithOptions.forEachIndexed { index, (variable, options) ->
            val variableId = insertVariable(
                variable.copy(
                    registerDefinitionId = registerId,
                    sortOrder = index
                )
            )
            if (options.isNotEmpty()) {
                insertCodeOptions(
                    options.mapIndexed { optIndex, opt ->
                        opt.copy(
                            variableDefinitionId = variableId,
                            sortOrder = optIndex
                        )
                    }
                )
            }
        }
        return registerId
    }
}
