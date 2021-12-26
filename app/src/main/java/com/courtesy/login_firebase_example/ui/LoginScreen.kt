package com.courtesy.login_firebase_example.ui

import android.content.Intent
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.courtesy.login_firebase_example.LoginViewModel
import com.courtesy.login_firebase_example.R
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private const val TAG = "LoginScreen"

@Composable
fun LoginScreen(emailLoginClick: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 24.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val buttonWidth = 300.dp

        Spacer(modifier = Modifier.height(18.dp))

        if (viewModel.error.value.isNotBlank()) {
            ErrorField(viewModel)
        }
        SignInWithEmailButton(buttonWidth, emailLoginClick)
        SignInWithGoogleButton(buttonWidth, viewModel)
        SignInWithFacebookButton(buttonWidth, viewModel)
    }
}

@Composable
fun ErrorField(viewModel: LoginViewModel) {
    Text(
        text = viewModel.error.value,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Red,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
}


@Composable
fun SignInWithEmailButton(buttonWidth: Dp, emailLoginClick: () -> Unit) {
    OutlinedButton(
        onClick = { emailLoginClick() },
        modifier = Modifier.width(buttonWidth),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(R.color.fui_bgEmail),
            contentColor = colorResource(R.color.white)
        )
    ) {
        SignInButtonRow(iconId = R.drawable.fui_ic_mail_white_24dp, buttonTextId = R.string.sign_in_with_email)
    }
}

@Composable
fun SignInWithGoogleButton(buttonWidth: Dp, viewModel: LoginViewModel) {
    val context = LocalContext.current
    val token = stringResource(R.string.default_web_client_id)

    val launcher = registerGoogleActivityResultLauncher(viewModel)

    OutlinedButton(
        modifier = Modifier.width(buttonWidth),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(R.color.fui_bgGoogle),
            contentColor = MaterialTheme.colors.onSurface
        ),
        onClick = {
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(token)
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(context, signInOptions)
            launcher.launch(googleSignInClient.signInIntent)
        }
    ) {
        SignInButtonRow(iconId = R.drawable.fui_ic_googleg_color_24dp, buttonTextId = R.string.sign_in_with_google)
    }
}

@Composable
fun registerGoogleActivityResultLauncher(viewModel: LoginViewModel): ManagedActivityResultLauncher<Intent, ActivityResult> {
    // Callback
    return rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            viewModel.signInWithGoogleToken(account.idToken!!)
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
        }
    }
}

@Composable
fun SignInWithFacebookButton(buttonWidth: Dp, viewModel: LoginViewModel) {
    val callbackManager = CallbackManager.Factory.create()
    val context = LocalContext.current as ActivityResultRegistryOwner

    registerFacebookCallback(callbackManager, viewModel)

    OutlinedButton(
        modifier = Modifier.width(buttonWidth),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = colorResource(R.color.fui_bgFacebook),
            contentColor = colorResource(R.color.white)
        ),
        onClick = {
            val permissions = listOf("email", "public_profile")
            LoginManager.getInstance().logIn(context, callbackManager, permissions)
        }
    ) {
        SignInButtonRow(iconId = R.drawable.fui_ic_facebook_white_22dp, buttonTextId = R.string.sign_in_with_facebook)
    }
}

fun registerFacebookCallback(callbackManager: CallbackManager, viewModel: LoginViewModel) {
    LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
        override fun onSuccess(result: LoginResult) {
            Log.d(TAG, "facebook:onSuccess:$result")
            viewModel.signInWithFacebookToken(result.accessToken)
        }
        // TODO onCancel and on error should show some error box
        override fun onCancel() {
            Log.d(TAG, "facebook:onCancel")
        }
        override fun onError(error: FacebookException) {
            viewModel.setError(error.localizedMessage ?: "Unknown error")
            Log.d(TAG, "facebook:onError", error)
        }
    })
}

@Composable
fun SignInButtonRow(@DrawableRes iconId: Int, @StringRes buttonTextId: Int) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp)
    ) {
        LoginButtonIcon(iconId)
        LoginButtonText(buttonTextId)
    }
}

@Composable
fun LoginButtonIcon(@DrawableRes painterResourceId: Int) {
    Icon(
        tint = Color.Unspecified,
        painter = painterResource(painterResourceId),
        contentDescription = null
    )
}

@Composable
fun LoginButtonText(@StringRes stringResourceId: Int) {
    Text(
        text = stringResource(stringResourceId),
        textAlign = TextAlign.Start,
        style = MaterialTheme.typography.button,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            .fillMaxWidth()
    )
}