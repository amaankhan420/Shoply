package com.example.shoply.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shoply.R
import com.example.shoply.database.AppDatabase
import com.example.shoply.repositories.AuthRepository
import com.example.shoply.repositories.CartRepository
import com.example.shoply.repositories.CheckoutRepository
import com.example.shoply.repositories.HomeRepository
import com.example.shoply.sharedPref.SettingsDataStore
import com.example.shoply.ui.theme.ShoplyTheme
import com.example.shoply.viewmodels.factory.AppViewModelFactory
import com.example.shoply.viewmodels.AuthViewModel
import com.example.shoply.viewmodels.CartCheckoutViewModel
import com.example.shoply.viewmodels.HomeViewModel
import com.example.shoply.viewmodels.OrderHistoryViewModel
import com.example.shoply.viewmodels.ProfileViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AppNavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()

    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val authRepository = remember { AuthRepository(firebaseAuth, firestore, googleSignInClient) }
    val homeRepository = remember { HomeRepository(firestore) }
    val cartRepository = remember { CartRepository(AppDatabase.getDatabase(context)) }
    val checkoutRepository =
        remember { CheckoutRepository(AppDatabase.getDatabase(context), firestore) }

    val authViewModel: AuthViewModel = viewModel(
        factory = AppViewModelFactory(authRepository = authRepository)
    )
    val homeViewModel: HomeViewModel = viewModel(
        factory = AppViewModelFactory(
            homeRepository = homeRepository,
            cartRepository = cartRepository
        )
    )
    val cartCheckoutViewModel: CartCheckoutViewModel = viewModel(
        factory = AppViewModelFactory(
            cartRepository = cartRepository,
            checkoutRepository = checkoutRepository
        )
    )

    val profileViewModel: ProfileViewModel = viewModel(
        factory = AppViewModelFactory(authRepository = authRepository)
    )
    val orderHistoryViewModel: OrderHistoryViewModel = viewModel(
        factory = AppViewModelFactory(checkoutRepository = checkoutRepository)
    )

    val authState by authViewModel.authState.collectAsState()

    var isDataStoreInitialized by remember { mutableStateOf(false) }
    var isOnboardingSeen by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        authViewModel.initializeAuthState()
        SettingsDataStore.initialize(context)
        SettingsDataStore.isOnboardingSeen.collect { seen ->
            isOnboardingSeen = seen
            isDataStoreInitialized = true
        }
    }
    if (!isDataStoreInitialized || isOnboardingSeen == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = when {
        !isOnboardingSeen!! -> "onboarding"
        authState == null -> "auth"
        else -> "home"
    }

    // Define custom animation specs
    val slideAnimationSpec = tween<IntOffset>(
        durationMillis = 400,
        easing = FastOutSlowInEasing
    )

    val fadeAnimationSpec = tween<Float>(
        durationMillis = 300,
        easing = LinearOutSlowInEasing
    )

    NavHost(
        navController = navController,
        startDestination = startDestination,
        // Smoother enter transition with easing
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = slideAnimationSpec
            ) + fadeIn(animationSpec = fadeAnimationSpec)
        },
        // Smoother exit transition
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 }, // Only slide 1/3 of the way
                animationSpec = slideAnimationSpec
            ) + fadeOut(animationSpec = fadeAnimationSpec)
        },
        // Smoother pop enter transition
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = slideAnimationSpec
            ) + fadeIn(animationSpec = fadeAnimationSpec)
        },
        // Smoother pop exit transition
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = slideAnimationSpec
            ) + fadeOut(animationSpec = fadeAnimationSpec)
        }
    ) {
        // Onboarding with special fade transition
        composable(
            "onboarding",
            enterTransition = { fadeIn(animationSpec = tween(600)) },
            exitTransition = { fadeOut(animationSpec = tween(400)) }
        ) {
            OnboardingScreen(navController)
        }

        // Auth screen with slide up transition
        composable(
            "auth",
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(400, easing = LinearOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            AuthenticationScreen(
                navController = navController,
                authViewModel = authViewModel,
                googleSignInClient = googleSignInClient
            )
        }

        composable("home") {
            HomeScreen(
                navController = navController,
                homeViewModel = homeViewModel
            )
        }

        // Settings with scale transition
        composable(
            "settings",
            enterTransition = {
                scaleIn(
                    initialScale = 0.9f,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                scaleOut(
                    targetScale = 0.9f,
                    animationSpec = tween(300, easing = LinearOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            SettingsScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable("cart") {
            CartScreen(
                navController = navController,
                cartCheckoutViewModel = cartCheckoutViewModel
            )
        }

        // Checkout with slide up from bottom
        composable(
            "checkout",
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(500, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(400, easing = LinearOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            CheckoutScreen(
                navController = navController,
                cartCheckoutViewModel = cartCheckoutViewModel
            )
        }

        // Product details with shared element feel
        composable(
            "productDetails/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType }),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                ) + scaleIn(
                    initialScale = 0.95f,
                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(350))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(350, easing = LinearOutSlowInEasing)
                ) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(350, easing = LinearOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            }
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            val uiState by homeViewModel.uiState.collectAsState()
            val product = remember(productId, uiState) {
                uiState.products.find { it.id == productId }
            }

            LaunchedEffect(product) {
                if (product == null && productId != null) {
                    navController.popBackStack()
                }
            }

            product?.let {
                ProductDetailsScreen(
                    navController = navController,
                    product = it,
                    addToCart = { product -> cartCheckoutViewModel.addToCart(product) }
                )
            }
        }

        composable("profile") {
            ProfileScreen(
                profileViewModel = profileViewModel,
                navController = navController
            )
        }

        composable("orderHistory") {
            OrderHistoryScreen(
                orderHistoryViewModel = orderHistoryViewModel,
                navController = navController
            )
        }
    }

}