package com.tomdunkley.dailypuzzles.data.network

import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import retrofit2.HttpException

/** True if `error` is the "please verify your email" 403 that gates gameplay-adjacent
 * endpoints (today's puzzle, score submission, the leaderboard's puzzle lookup) -- in
 * that case flips AuthRepository's state so the nav host redirects to the verify-email
 * screen, instead of surfacing the raw error message to the user.
 */
fun handleIfVerificationRequired(error: Throwable): Boolean {
    if ((error as? HttpException)?.code() != 403) return false
    AuthRepository.markUnverified()
    return true
}
