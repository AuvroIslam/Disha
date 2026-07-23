package com.example.gemmachat

import android.app.Application
import com.example.gemmachat.data.AppPrefs
import com.example.gemmachat.data.ChatRepository
import com.example.gemmachat.data.CommunityRepository
import com.example.gemmachat.data.FamilyRepository
import com.example.gemmachat.data.SosRepository
import com.example.gemmachat.data.download.HfDownloadRepository
import com.example.gemmachat.inference.EngineHolder
import com.example.gemmachat.location.LocationProvider
import com.example.gemmachat.mesh.MeshHub
import java.io.File

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
    lateinit var communityRepository: CommunityRepository
        private set
    lateinit var familyRepository: FamilyRepository
        private set
    lateinit var locationProvider: LocationProvider
        private set
    lateinit var meshHub: MeshHub
        private set

    override fun onCreate() {
        super.onCreate()
        chatRepository = ChatRepository(this)
        downloadRepository = HfDownloadRepository()
        engineHolder = EngineHolder(this)
        prefs = AppPrefs(this)
        sosRepository = SosRepository(File(filesDir, "sos_store.json"))
        communityRepository = CommunityRepository(File(filesDir, "community_store.json"))
        familyRepository = FamilyRepository(File(filesDir, "family_store.json"))
        locationProvider = LocationProvider(this)
        meshHub = MeshHub(this, sosRepository, communityRepository, familyRepository,
            locationProvider, prefs)
    }
}
