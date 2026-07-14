package com.tomdunkley.dailypuzzles.data.boggle

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/** On-device copy of the same word list the backend validates against (mirrors
 * webapp/app/data/dictionary.txt) -- lets BoggleViewModel reject a word the moment a
 * drag ends, instead of only finding out it was invalid when the server scores it.
 */
object BoggleDictionary {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var wordsDeferred: Deferred<Set<String>>

    fun init(context: Context) {
        val appContext = context.applicationContext
        wordsDeferred = scope.async {
            appContext.assets.open("dictionary.txt").bufferedReader().useLines { lines ->
                lines.mapTo(HashSet()) { it.trim().uppercase() }
            }
        }
    }

    suspend fun contains(word: String): Boolean = wordsDeferred.await().contains(word.uppercase())

    suspend fun getAll(): Set<String> = wordsDeferred.await()
}
