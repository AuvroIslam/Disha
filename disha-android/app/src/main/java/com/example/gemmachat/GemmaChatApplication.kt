package com.example.gemmachat

import android.app.Application
import com.example.gemmachat.data.AppPrefs
import com.example.gemmachat.data.ChatRepository
import com.example.gemmachat.data.SosRepository
import com.example.gemmachat.data.download.HfDownloadRepository
import com.example.gemmachat.inference.EngineHolder
import com.example.gemmachat.location.LocationProvider

class GemmaChatApplication : Application() {

    lateinit var chatRepository: ChatRepository
        private set
    lateinit var downloadRepository: HfDownloadRepository
        private set
    lateinit var engineHolder: EngineHolder
        private set
    lateinit var prefs: AppPrefs
        private set
    lateinit var sosRepository: SosRepository
        private set
    lateinit var locationProvider: LocationProvider
        private set

    override fun onCreate() {
        super.onCreate()
        chatRepository = ChatRepository(this)
        downloadRepository = HfDownloadRepository()
        engineHolder = EngineHolder(this)
        prefs = AppPrefs(this)
        sosRepository = SosRepository()
        locationProvider = LocationProvider(this)
    }
}
