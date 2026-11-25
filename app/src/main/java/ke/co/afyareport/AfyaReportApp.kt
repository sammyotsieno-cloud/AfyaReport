package ke.co.afyareport

import android.app.Application
import ke.co.afyareport.data.AppDatabase
import ke.co.afyareport.data.Repo
import ke.co.afyareport.domain.RegisterPrefs

class AfyaReportApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val repo by lazy { Repo(database.dao()) }
    val prefs by lazy { RegisterPrefs(this) }
}
