package com.example.shoply.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.shoply.viewmodels.AuthUiEvent
import com.example.shoply.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuthUserCollisionException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    googleSignInClient: GoogleSignInClient,
) {
    val authState by authViewModel.authState.collectAsState()
    val loginErrorMessage by authViewModel.loginErrorMessage.collectAsState()
    val signupErrorMessage by authViewModel.signupErrorMessage.collectAsState()
    val uiEvent by authViewModel.uiEvent.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var loginEmail by rememberSaveable { mutableStateOf("") }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    var signupEmail by rememberSaveable { mutableStateOf("") }
    var signupPassword by rememberSaveable { mutableStateOf("") }
    var signupDisplayName by rememberSaveable { mutableStateOf("") }
    var passwordVisibleLogin by rememberSaveable { mutableStateOf(false) }
    var passwordVisibleSignup by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var hasNavigatedToHome by rememberSaveable { mutableStateOf(false) }
    var googleSignInErrorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(authState) {
        if (authState != null && !hasNavigatedToHome) {
            hasNavigatedToHome = true
            navController.navigate("home") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    LaunchedEffect(uiEvent) {
        if (uiEvent is AuthUiEvent.SignupSuccess) {
            snackbarHostState.showSnackbar("Welcome to Shoply! 🎉")
            signupEmail = ""
            signupPassword = ""
            signupDisplayName = ""
            authViewModel.resetUiEvent()

            navController.navigate("home") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    LaunchedEffect(googleSignInErrorMessage) {
        googleSignInErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            googleSignInErrorMessage = null
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                if (account != null) {
                    authViewModel.signInWithGoogle(account)
                }
            } catch (e: Exception) {
                val message = if (e is FirebaseAuthUserCollisionException) {
                    "An account with this email already exists with a different sign-in method."
                } else {
                    e.localizedMessage ?: "Google Sign-In failed"
                }
                googleSignInErrorMessage = message
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus(force = true)
                }
        ) {
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // App Title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Shoply",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Your favorite shopping companion",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Auth Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Custom Tab Row
                        AuthTabRow(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Animated Content
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                slideInHorizontally { width ->
                                    if (targetState > initialState) width else -width
                                } togetherWith slideOutHorizontally { width ->
                                    if (targetState > initialState) -width else width
                                }
                            },
                            label = "auth_content"
                        ) { tabIndex ->
                            if (tabIndex == 0) {
                                LoginForm(
                                    email = loginEmail,
                                    password = loginPassword,
                                    passwordVisible = passwordVisibleLogin,
                                    onEmailChange = { loginEmail = it },
                                    onPasswordChange = { loginPassword = it },
                                    onPasswordVisibilityToggle = {
                                        passwordVisibleLogin = !passwordVisibleLogin
                                    },
                                    onLoginClick = {
                                        hasNavigatedToHome = false
                                        focusManager.clearFocus(force = true)
                                        authViewModel.signInWithEmail(loginEmail, loginPassword)
                                    },
                                    errorMessage = loginErrorMessage,
                                    snackbarHostState = snackbarHostState
                                )
                            } else {
                                SignupForm(
                                    displayName = signupDisplayName,
                                    email = signupEmail,
                                    password = signupPassword,
                                    passwordVisible = passwordVisibleSignup,
                                    onDisplayNameChange = { signupDisplayName = it },
                                    onEmailChange = { signupEmail = it },
                                    onPasswordChange = { signupPassword = it },
                                    onPasswordVisibilityToggle = {
                                        passwordVisibleSignup = !passwordVisibleSignup
                                    },
                                    onSignupClick = {
                                        hasNavigatedToHome = false
                                        focusManager.clearFocus(force = true)
                                        authViewModel.signUpWithEmail(
                                            signupEmail,
                                            signupPassword,
                                            signupDisplayName
                                        )
                                    },
                                    errorMessage = signupErrorMessage,
                                    snackbarHostState = snackbarHostState
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Divider with "OR"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "OR",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Google Sign-In Button
                GoogleSignInButton(
                    onClick = {
                        hasNavigatedToHome = false
                        focusManager.clearFocus(force = true)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AuthTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(4.dp)
    ) {
        listOf("Login", "Sign Up").forEachIndexed { index, title ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (selectedTab == index)
                            MaterialTheme.colorScheme.primary
                        else
                            androidx.compose.ui.graphics.Color.Transparent
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTab == index)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    email: String,
    password: String,
    passwordVisible: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onLoginClick: () -> Unit,
    errorMessage: String?,
    snackbarHostState: SnackbarHostState
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email Address",
            leadingIcon = Icons.Filled.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        AuthTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            leadingIcon = Icons.Filled.Lock,
            visualTransformation = if (!passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = email.isNotBlank() && password.isNotBlank(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Sign In",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        errorMessage?.let {
            LaunchedEffect(it) {
                snackbarHostState.showSnackbar(it)
            }
        }
    }
}

@Composable
private fun SignupForm(
    displayName: String,
    email: String,
    password: String,
    passwordVisible: Boolean,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSignupClick: () -> Unit,
    errorMessage: String?,
    snackbarHostState: SnackbarHostState
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AuthTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = "Full Name",
            leadingIcon = Icons.Filled.Person
        )

        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email Address",
            leadingIcon = Icons.Filled.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        AuthTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            leadingIcon = Icons.Filled.Lock,
            visualTransformation = if (!passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSignupClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = displayName.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        errorMessage?.let {
            LaunchedEffect(it) {
                snackbarHostState.showSnackbar(it)
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth(),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = MaterialTheme.colorScheme.primary
        ),
        maxLines = 1
    )
}

@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Continue with Google",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}