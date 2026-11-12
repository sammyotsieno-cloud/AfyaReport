package ke.co.afyareport.di

import android.content.Context
import ke.co.afyareport.data.local.AfyaReportDatabase
import ke.co.afyareport.data.repository.RegisterRepository
import ke.co.afyareport.data.repository.SessionRepository

/**
 * Simple manual dependency container.
 * Keeps the project free of Hilt/Koin for now while remaining testable.
 */
class AppContainer(context: Context) {

    private val database = AfyaReportDatabase.getInstance(context)

    val registerRepository: RegisterRepository by lazy {
        RegisterRepository(database.registerDefinitionDao())
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(database.reportingSessionDao())
    }
}
