package com.tomdunkley.dailypuzzles.data.network

import com.tomdunkley.dailypuzzles.data.network.dto.ChangePasswordRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.ForgotPasswordRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.FriendRequestSummaryDto
import com.tomdunkley.dailypuzzles.data.network.dto.FriendSummaryDto
import com.tomdunkley.dailypuzzles.data.network.dto.GameSummaryDto
import com.tomdunkley.dailypuzzles.data.network.dto.GoogleSignInRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.LeaderboardDto
import com.tomdunkley.dailypuzzles.data.network.dto.LoginRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.PuzzleDto
import com.tomdunkley.dailypuzzles.data.network.dto.RefreshRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.RegisterRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.ResetPasswordRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.SetPasswordRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.ScoreDetailDto
import com.tomdunkley.dailypuzzles.data.network.dto.ScoreSubmissionDto
import com.tomdunkley.dailypuzzles.data.network.dto.ScoreSubmissionResultDto
import com.tomdunkley.dailypuzzles.data.network.dto.SendFriendRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.TokenPairDto
import com.tomdunkley.dailypuzzles.data.network.dto.UpdateProfileRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.UserProfileDto
import com.tomdunkley.dailypuzzles.data.network.dto.UserSearchResultDto
import com.tomdunkley.dailypuzzles.data.network.dto.AchievementSummaryDto
import com.tomdunkley.dailypuzzles.data.network.dto.ClaimAchievementRequest
import com.tomdunkley.dailypuzzles.data.network.dto.ClaimAchievementResponse
import com.tomdunkley.dailypuzzles.data.network.dto.AllWordsResponseDto
import com.tomdunkley.dailypuzzles.data.network.dto.VerifyEmailRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("v1/auth/google")
    suspend fun signInWithGoogle(@Body body: GoogleSignInRequestDto): TokenPairDto

    @POST("v1/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): TokenPairDto

    @POST("v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): TokenPairDto

    @POST("v1/auth/guest")
    suspend fun guestSignIn(): TokenPairDto

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): TokenPairDto

    @POST("v1/auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequestDto)

    @POST("v1/auth/resend-verification")
    suspend fun resendVerification()

    @POST("v1/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequestDto)

    @POST("v1/auth/resend-password-reset")
    suspend fun resendPasswordReset(@Body body: ForgotPasswordRequestDto)

    @POST("v1/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequestDto)

    @POST("v1/auth/set-password")
    suspend fun setPassword(@Body body: SetPasswordRequestDto)

    @POST("v1/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequestDto)

    @POST("v1/auth/link-google")
    suspend fun linkGoogleAccount(@Body body: GoogleSignInRequestDto)

    @GET("v1/users/me")
    suspend fun getMyProfile(): UserProfileDto

    @PATCH("v1/users/me")
    suspend fun updateMyProfile(@Body body: UpdateProfileRequestDto): UserProfileDto

    @DELETE("v1/users/me")
    suspend fun deleteAccount()

    @GET("v1/games")
    suspend fun getGames(): List<GameSummaryDto>

    @GET("v1/puzzles/today")
    suspend fun getTodayPuzzle(@Query("game") game: String): PuzzleDto

    @GET("v1/puzzles/{puzzleId}/all-words")
    suspend fun getPuzzleAllWords(@Path("puzzleId") puzzleId: String): AllWordsResponseDto

    @POST("v1/scores")
    suspend fun submitScore(@Body body: ScoreSubmissionDto): ScoreSubmissionResultDto

    @GET("v1/leaderboards/{puzzleId}")
    suspend fun getLeaderboard(@Path("puzzleId") puzzleId: String): LeaderboardDto

    @GET("v1/leaderboards/{puzzleId}/global")
    suspend fun getGlobalLeaderboard(@Path("puzzleId") puzzleId: String): LeaderboardDto

    @GET("v1/scores/{puzzleId}/{userId}")
    suspend fun getScoreDetail(@Path("puzzleId") puzzleId: String, @Path("userId") userId: String): ScoreDetailDto

    @GET("v1/users/search")
    suspend fun searchUsers(@Query("q") query: String): List<UserSearchResultDto>

    @POST("v1/friends/requests")
    suspend fun sendFriendRequest(@Body body: SendFriendRequestDto)

    @GET("v1/friends/requests/incoming")
    suspend fun getIncomingRequests(): List<FriendRequestSummaryDto>

    @GET("v1/friends/requests/outgoing")
    suspend fun getOutgoingRequests(): List<FriendRequestSummaryDto>

    @POST("v1/friends/requests/{fromUserId}/accept")
    suspend fun acceptFriendRequest(@Path("fromUserId") fromUserId: String)

    @POST("v1/friends/requests/{fromUserId}/decline")
    suspend fun declineFriendRequest(@Path("fromUserId") fromUserId: String)

    @GET("v1/friends")
    suspend fun getFriends(): List<FriendSummaryDto>

    @DELETE("v1/friends/{friendId}")
    suspend fun unfriend(@Path("friendId") friendId: String)

    @GET("v1/users/me/achievements")
    suspend fun getMyAchievements(): AchievementSummaryDto

    @POST("v1/users/me/achievements/claim")
    suspend fun claimAchievement(@Body body: ClaimAchievementRequest): ClaimAchievementResponse

    @POST("v1/dev/reset-progress")
    suspend fun resetDevProgress()

    @POST("v1/dev/unlock-all-achievements")
    suspend fun unlockAllAchievements()

    @POST("v1/dev/reset-achievements")
    suspend fun resetAchievements()
}
