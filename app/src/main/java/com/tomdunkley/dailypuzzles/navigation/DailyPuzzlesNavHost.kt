package com.tomdunkley.dailypuzzles.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.ui.AppViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.auth.AuthState
import com.tomdunkley.dailypuzzles.ui.screens.boggle.BoggleScreen
import com.tomdunkley.dailypuzzles.ui.screens.boggle.BoggleUnlimitedScreen
import com.tomdunkley.dailypuzzles.ui.screens.friends.FriendsScreen
import com.tomdunkley.dailypuzzles.ui.screens.home.HomeScreen
import com.tomdunkley.dailypuzzles.ui.screens.leaderboard.LeaderboardScreen
import com.tomdunkley.dailypuzzles.ui.screens.numbers.NumbersScreen
import com.tomdunkley.dailypuzzles.ui.screens.numbers.NumbersUnlimitedScreen
import com.tomdunkley.dailypuzzles.ui.screens.scoredetail.ScoreDetailScreen
import com.tomdunkley.dailypuzzles.ui.screens.achievements.AchievementsScreen
import com.tomdunkley.dailypuzzles.ui.screens.profile.UserProfileScreen
import com.tomdunkley.dailypuzzles.ui.screens.settings.AccountSettingsScreen
import com.tomdunkley.dailypuzzles.ui.screens.settings.AvatarPickerScreen
import com.tomdunkley.dailypuzzles.ui.screens.settings.ChangePasswordScreen
import com.tomdunkley.dailypuzzles.ui.screens.settings.ForgotPasswordScreen
import com.tomdunkley.dailypuzzles.ui.screens.settings.SettingsScreen
import com.tomdunkley.dailypuzzles.ui.screens.verify.VerifyEmailScreen

@Composable
fun DailyPuzzlesNavHost() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val authState by AuthRepository.state.collectAsState()
    val appViewModel: AppViewModel = viewModel()
    val hasPendingFriendRequests by appViewModel.hasPendingFriendRequests.collectAsState()
    val newTrophyCount by appViewModel.newTrophyCount.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.SignedIn) appViewModel.refreshFriendRequestBadge()
    }

    // Boggle and Numbers both hide the bottom bar while actively playing (Playing/
    // Submitting/Loading), but the bar should reappear once either lands on its own
    // Results/summary state -- NavHost can't see that nested UI state directly, so each
    // screen reports it back via this callback.
    var boggleShowBottomBar by remember { mutableStateOf(false) }
    var boggleUnlimitedShowBottomBar by remember { mutableStateOf(false) }
    var numbersShowBottomBar by remember { mutableStateOf(false) }
    var numbersUnlimitedShowBottomBar by remember { mutableStateOf(false) }

    // Wherever the user is, an account that becomes unverified (right after registering,
    // or because a gameplay call just 403'd) gets routed to the verify-email screen.
    // Conversely, signing out (or a refresh-token failure forcing a sign-out) while on
    // that screen must navigate back, or the screen is left stranded showing stale,
    // now-unauthenticated content.
    LaunchedEffect(authState) {
        if (authState is AuthState.SignedInUnverified && currentRoute != Routes.VERIFY_EMAIL) {
            navController.navigate(Routes.VERIFY_EMAIL)
        } else if (authState is AuthState.SignedOut && currentRoute == Routes.VERIFY_EMAIL) {
            navController.navigate(Routes.HOME) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    val hideBottomBar = currentRoute == Routes.VERIFY_EMAIL ||
        (currentRoute == Routes.BOGGLE && !boggleShowBottomBar) ||
        (currentRoute == Routes.BOGGLE_UNLIMITED && !boggleUnlimitedShowBottomBar) ||
        (currentRoute == Routes.NUMBERS && !numbersShowBottomBar) ||
        (currentRoute == Routes.NUMBERS_UNLIMITED && !numbersUnlimitedShowBottomBar)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!hideBottomBar) {
                Column {
                    HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                    ) {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    // If this tab's destination is already somewhere in the back
                                    // stack (e.g. you're on a friend's score detail, reached via
                                    // Leaderboard), pop straight back to that existing instance --
                                    // this is what "pressing a tab returns you to its root" means
                                    // for screens (Boggle, ScoreDetail) that aren't part of any
                                    // tab's own selected state. Falls back to the standard
                                    // popUpTo-to-root navigation for a tab not yet on the stack;
                                    // navigate()'s own popUpTo+restoreState combo is unreliable
                                    // when the target route is already mid-stack.
                                    val poppedToExisting = navController.popBackStack(item.route, inclusive = false)
                                    if (!poppedToExisting) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    val showBadge = (item.route == Routes.SETTINGS && hasPendingFriendRequests) ||
                                        (item.route == Routes.ACHIEVEMENTS && newTrophyCount > 0)
                                    BadgedBox(badge = { if (showBadge) Badge(modifier = Modifier.size(10.dp)) }) {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                },
                                label = { Text(item.label.uppercase()) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    isSignedIn = authState is AuthState.SignedIn,
                    onPuzzleClick = { puzzleId ->
                        when (puzzleId) {
                            "boggle" -> navController.navigate(Routes.BOGGLE)
                            "numbers" -> navController.navigate(Routes.NUMBERS)
                        }
                    },
                    onUnlimitedPuzzleClick = { puzzleId ->
                        when (puzzleId) {
                            "boggle" -> navController.navigate(Routes.BOGGLE_UNLIMITED)
                            "numbers" -> navController.navigate(Routes.NUMBERS_UNLIMITED)
                        }
                    },
                    onSignInClick = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.BOGGLE) {
                BoggleScreen(
                    isSignedIn = authState is AuthState.SignedIn,
                    onBack = { navController.popBackStack() },
                    onViewDetail = { puzzleId, userId ->
                        navController.navigate(Routes.scoreDetail(puzzleId, userId))
                    },
                    onSignInClick = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onShowBottomBarChange = { boggleShowBottomBar = it },
                )
            }
            composable(Routes.NUMBERS) {
                NumbersScreen(
                    isSignedIn = authState is AuthState.SignedIn,
                    onBack = { navController.popBackStack() },
                    onViewDetail = { puzzleId, userId ->
                        navController.navigate(Routes.scoreDetail(puzzleId, userId))
                    },
                    onSignInClick = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onShowBottomBarChange = { numbersShowBottomBar = it },
                )
            }
            composable(Routes.BOGGLE_UNLIMITED) {
                BoggleUnlimitedScreen(
                    onBack = { navController.popBackStack() },
                    onShowBottomBarChange = { boggleUnlimitedShowBottomBar = it },
                )
            }
            composable(Routes.NUMBERS_UNLIMITED) {
                NumbersUnlimitedScreen(
                    onBack = { navController.popBackStack() },
                    onShowBottomBarChange = { numbersUnlimitedShowBottomBar = it },
                )
            }
            composable(Routes.FRIENDS) {
                FriendsScreen(
                    onBack = { navController.popBackStack() },
                    onGoToSettings = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onMutation = { appViewModel.refreshFriendRequestBadge() },
                    onViewProfile = { userId -> navController.navigate(Routes.userProfile(userId)) },
                )
            }
            composable(Routes.LEADERBOARD) {
                LeaderboardScreen(
                    onAddFriendsClick = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onGoToSettings = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onViewScore = { puzzleId, userId ->
                        navController.navigate(Routes.scoreDetail(puzzleId, userId))
                    },
                )
            }
            composable(Routes.ACHIEVEMENTS) {
                AchievementsScreen(
                    isSignedIn = authState is AuthState.SignedIn,
                    onSignInClick = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onForgotPasswordClick = { navController.navigate(Routes.FORGOT_PASSWORD) },
                    onEditAvatarClick = { navController.navigate(Routes.AVATAR_PICKER) },
                    onAccountSettingsClick = { navController.navigate(Routes.ACCOUNT_SETTINGS) },
                    onFriendsClick = { navController.navigate(Routes.FRIENDS) },
                    hasPendingFriendRequests = hasPendingFriendRequests,
                )
            }
            composable(Routes.ACCOUNT_SETTINGS) {
                AccountSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onChangePasswordClick = { navController.navigate(Routes.CHANGE_PASSWORD) },
                )
            }
            composable(Routes.AVATAR_PICKER) {
                AvatarPickerScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                    onBack = { navController.popBackStack() },
                    onResetComplete = { navController.popBackStack() },
                )
            }
            composable(Routes.CHANGE_PASSWORD) {
                ChangePasswordScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.VERIFY_EMAIL) {
                VerifyEmailScreen(onVerified = { navController.popBackStack() })
            }
            composable(
                Routes.SCORE_DETAIL,
                arguments = listOf(
                    navArgument("puzzleId") { type = NavType.StringType },
                    navArgument("userId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                ScoreDetailScreen(
                    puzzleId = backStackEntry.arguments?.getString("puzzleId").orEmpty(),
                    userId = backStackEntry.arguments?.getString("userId").orEmpty(),
                    isSignedIn = authState is AuthState.SignedIn,
                    onBack = { navController.popBackStack() },
                    onSignInClick = {
                        navController.navigate(Routes.SETTINGS) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                Routes.USER_PROFILE,
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { backStackEntry ->
                UserProfileScreen(
                    userId = backStackEntry.arguments?.getString("userId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onViewScore = { puzzleId, userId ->
                        navController.navigate(Routes.scoreDetail(puzzleId, userId))
                    },
                )
            }
        }
    }
}
