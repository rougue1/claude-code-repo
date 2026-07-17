package com.localchat.app

import android.app.Application
import com.localchat.app.data.ChatRepository

class LocalChatApp : Application() {
    lateinit var repository: ChatRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ChatRepository(this)
    }
}
