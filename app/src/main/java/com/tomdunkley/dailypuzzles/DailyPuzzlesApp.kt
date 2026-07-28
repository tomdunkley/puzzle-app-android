package com.tomdunkley.dailypuzzles

import android.app.Application
import com.tomdunkley.dailypuzzles.audio.SoundFeedback
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.auth.AuthTokenStore
import com.tomdunkley.dailypuzzles.data.auth.GuestSession
import com.tomdunkley.dailypuzzles.data.boggle.BoggleDictionary
import com.tomdunkley.dailypuzzles.data.boggle.BoggleProgressStore
import com.tomdunkley.dailypuzzles.data.developer.DeveloperStore
import com.tomdunkley.dailypuzzles.data.unlimited.UnlimitedHighScoreStore
import com.tomdunkley.dailypuzzles.data.network.ApiClient
import com.tomdunkley.dailypuzzles.data.roots.RootsProgressStore
import com.tomdunkley.dailypuzzles.data.numbers.NumbersProgressStore
import com.tomdunkley.dailypuzzles.data.trophies.TrophySeenStore

class DailyPuzzlesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val tokenStore = AuthTokenStore(this)
        ApiClient.init(tokenStore)
        AuthRepository.init(tokenStore)
        AuthRepository.restoreSession()
        GuestSession.init(this)
        BoggleProgressStore.init(this)
        NumbersProgressStore.init(this)
        RootsProgressStore.init(this)
        DeveloperStore.init(this)
        UnlimitedHighScoreStore.init(this)
        TrophySeenStore.init(this)
        BoggleDictionary.init(this)
        SoundFeedback.init()
    }
}
