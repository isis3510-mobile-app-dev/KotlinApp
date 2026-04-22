package com.example.petcare.ui.screens.auth

import com.example.petcare.ui.theme.*

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.petcare.R
import com.example.petcare.ui.components.ButtonDefault
import com.example.petcare.ui.components.ButtonOutline
import com.example.petcare.ui.components.ButtonSwitch
import com.example.petcare.ui.components.TextFieldComponent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.sp
import com.example.petcare.ui.theme.RobotoMedium
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.petcare.util.InputFieldPolicy
import com.example.petcare.util.InputTextLimits


@Composable
fun FeatureCard(
    title: String,
    subtitle: String
) {
    Column (
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Image(
            painter = painterResource(id = R.drawable.logo_dog),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
        )

        Column (
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(5.dp)
        ){

            Text(
                text = title,
                color = MaterialTheme.colorScheme.tertiary,
                fontFamily = RobotoMedium,
                fontSize = 26.sp
            )

            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
@Composable
fun LoginScreen(
    onSignUpSuccess: () -> Unit,
    onGoToSignIn: () -> Unit,
    viewModel: AuthViewModel = viewModel()
){
    var fullName by remember { mutableStateOf("") }
    var email by remember {mutableStateOf("")}
    var password by remember {mutableStateOf("")}
    val authState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Success) onSignUpSuccess()
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
        .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            FeatureCard(
                title = "PetCare",
                subtitle = "Your pet's health companion"
            )
        }
        Spacer(modifier = Modifier.height(30.dp))

        ButtonSwitch(
            option1 = "Sign In",
            option2 = "Create account",
            selectedIndex = 1,
            onSelectionChange = { if (it == 0) onGoToSignIn() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextFieldComponent(
            name = "Full Name",
            label = "Sarah Johnson",
            value = fullName,
            onValueChange = { fullName = it},
            maxLength = InputTextLimits.USER_NAME,
            fieldPolicy = InputFieldPolicy.GENERAL_TEXT
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextFieldComponent(
            name = "Email Address",
            label = "you@gmail.com",
            value = email,
            onValueChange = { email = it },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
            ),
            maxLength = InputTextLimits.EMAIL,
            fieldPolicy = InputFieldPolicy.EMAIL
        )

        Spacer(modifier = Modifier.height(24.dp))

        TextFieldComponent(
            name = "Password",
            label = "Min. 8 characters",
            value = password,
            onValueChange = { password = it},
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            maxLength = InputTextLimits.PASSWORD,
            fieldPolicy = InputFieldPolicy.PASSWORD
        )

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
            onclick = { viewModel.register(email, password, fullName) },
            bgColor = MaterialTheme.colorScheme.secondary,
            textColor = MaterialTheme.colorScheme.onSecondary,
            width = 342.dp,
            height = 56.dp,
            text = if (authState is AuthViewModel.AuthState.Loading) "... Loading" else "Create Account"
        )


        Spacer(modifier = Modifier.height(24.dp))


        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp), thickness = DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outlineVariant
            )

            Text(
                text = "or continue with",
                modifier = Modifier.padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp), thickness = DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outlineVariant
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
            outlineColor = MaterialTheme.colorScheme.outlineVariant,
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
fun LoginScreenPreview(){
    PetCareTheme {
        LoginScreen(
            onSignUpSuccess = {},
            onGoToSignIn = {}
        )
    }
}
