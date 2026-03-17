package com.example.petcare.ui.screens.auth

import com.example.petcare.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.petcare.R
import com.example.petcare.ui.components.*
import com.example.petcare.ui.theme.PetCareTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit,
    onGoToSignUp: () -> Unit,
    viewModel: AuthViewModel = viewModel()
){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("")}
    val authState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Success) {
            onSignInSuccess()
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching { task.getResult(ApiException::class.java) }
            .onSuccess { account -> account.idToken?.let { viewModel.loginWithGoogle(it) } }
            .onFailure { viewModel.resetState() }
    }

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)
        .fillMaxSize()
        .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            FeatureCard(
                title = "PetCare",
                subtitle = "Your pet's health companion",
                icon = Icons.Outlined.Pets
            )
        }
        Spacer(modifier = Modifier.height(30.dp))

        ButtonSwitch(
            option1 = "Sign In",
            option2 = "Create account",
            selectedIndex = 0,
            onSelectionChange = { if (it == 1) onGoToSignUp() }
        )


        Spacer(modifier = Modifier.height(24.dp))

        TextFieldComponent(
            name = "Email Address",
            label = "you@gmail.com",
            value = email,
            onValueChange = { email = it}
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextFieldComponent(
            name = "Password",
            label = "........",
            value = password,
            onValueChange = { password = it },
            visualTransformation = if (passwordVisible)
                VisualTransformation.None
            else
                PasswordVisualTransformation(),
            icon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Default.Visibility
                        else
                            Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide Password" else "Show Password",
                        tint = Color.LightGray
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Forgot password?",
                modifier = Modifier.align(Alignment.CenterEnd),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (authState is AuthViewModel.AuthState.Error) {
            Text(
                text = (authState as AuthViewModel.AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        ButtonDefault(
            onclick = { viewModel.login(email, password) },
            bgColor = MaterialTheme.colorScheme.secondary,
            textColor = Color.White,
            width = 342.dp,
            height = 56.dp,
            text = if (authState is AuthViewModel.AuthState.Loading) "..." else "Sign In"
        )


        Spacer(modifier = Modifier.height(24.dp))


        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp), thickness = DividerDefaults.Thickness, color = Color.LightGray
            )

            Text(
                text = "or continue with",
                modifier = Modifier.padding(horizontal = 12.dp),
                color = Color.Gray
            )

            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp), thickness = DividerDefaults.Thickness, color = Color.LightGray
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        ButtonOutline(
            onclick = {
                val client = getGoogleSignInClient(context)
                client.signOut().addOnCompleteListener {
                    googleLauncher.launch(client.signInIntent)
                }
            },
            bgColor = MaterialTheme.colorScheme.background,
            outlineColor = GrayBorder,
            textColor = MaterialTheme.colorScheme.tertiary,
            width = 342.dp,
            height = 56.dp,
            text = "Continue with Google",
            icon = { Icon(
                painter = painterResource(id = R.drawable.google),
                contentDescription = "Call Owner Now",
                tint = Color.Unspecified
            )
            }
        )
    }
}

@Preview
@Composable
fun SignInScreenPreview(){
    PetCareTheme {
        SignInScreen(
            onSignInSuccess = {},
            onGoToSignUp = {}
        )
    }
}