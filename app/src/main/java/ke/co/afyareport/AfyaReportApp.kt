package ke.co.afyareport

import android.app.Application
import ke.co.afyareport.di.AppContainer

class AfyaReportApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
