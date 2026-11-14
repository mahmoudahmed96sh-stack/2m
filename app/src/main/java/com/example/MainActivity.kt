package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOut
import com.example.data.repository.StoreRepository
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.screens.GoogleAuthScreen
import com.example.ui.screens.StaffPinSelectionScreen
import com.example.ui.screens.StoreOnboardingScreen
import com.example.ui.screens.TwoMStoreShell
import com.example.ui.theme.BrightGold
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.TwoMStoreTheme
import com.example.ui.theme.WarmMuted
import com.example.ui.viewmodel.StoreMainViewModel
import com.example.ui.viewmodel.StoreSessionViewModel
import com.example.ui.viewmodel.WorkspaceGateState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    setContent {
      TwoMStoreTheme(forceRtl = true) {
        AppRoot()
      }
    }
  }
}

@Composable
private fun AppRoot() {
  val auth = remember { Firebase.auth }
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    GoogleAuthScreen(onAuthSuccess = {})
  } else {
    key(user.uid) {
      AuthenticatedStoreGate(currentUserId = user.uid)
    }
  }
}

@Composable
private fun AuthenticatedStoreGate(currentUserId: String) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val sessionViewModel: StoreSessionViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val dbId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            StoreSessionViewModel(StoreRepository(db, Firebase.auth), currentUserId)
          }
        },
    )

  val gateState by sessionViewModel.gateState.collectAsStateWithLifecycle()
  val activeStaff by sessionViewModel.activeStaff.collectAsStateWithLifecycle()
  val actionError by sessionViewModel.actionError.collectAsStateWithLifecycle()
  val isBusy by sessionViewModel.isBusy.collectAsStateWithLifecycle()

  val onSignOut: () -> Unit = {
    sessionViewModel.lockStaffSession()
    signOut(credentialManager, scope)
  }

  when (val state = gateState) {
    WorkspaceGateState.Loading -> {
      LuxurySplashLoading()
    }

    WorkspaceGateState.NeedsStoreSetup -> {
      StoreOnboardingScreen(
        isBusy = isBusy,
        errorMessage = actionError,
        onCreateStore = { sName, oName, oPin, s1Name, s1Pin, s2Name, s2Pin, s3Name, s3Pin ->
          sessionViewModel.createStoreWorkspace(
            sName,
            oName,
            oPin,
            s1Name,
            s1Pin,
            s2Name,
            s2Pin,
            s3Name,
            s3Pin,
          )
        },
        onJoinStore = { code -> sessionViewModel.joinStoreByCode(code) },
        onSignOut = onSignOut,
      )
    }

    is WorkspaceGateState.Error -> {
      Box(
        modifier = Modifier.fillMaxSize().background(MatteBlack).padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = state.message, color = BrightGold, style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(16.dp))
          GoldOutlinedButton(text = "تسجيل خروج", onClick = onSignOut)
        }
      }
    }

    is WorkspaceGateState.Ready -> {
      val unlocked = activeStaff
      if (unlocked == null) {
        StaffPinSelectionScreen(
          store = state.store,
          staffList = state.staffList,
          errorMessage = actionError,
          onUnlockStaff = { member, pin -> sessionViewModel.unlockStaff(member, pin) },
          onSignOut = onSignOut,
        )
      } else {
        key("${currentUserId}_${state.store.id}") {
          val mainViewModel: StoreMainViewModel =
            viewModel(
              key = "${currentUserId}_${state.store.id}",
              factory =
                viewModelFactory {
                  initializer {
                    val app = checkNotNull(this[APPLICATION_KEY])
                    val dbId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(dbId)
                    StoreMainViewModel(StoreRepository(db, Firebase.auth), state.store.id)
                  }
                },
            )

          TwoMStoreShell(
            store = state.store,
            activeStaff = unlocked,
            staffList = state.staffList,
            mainViewModel = mainViewModel,
            onSwitchStaffPin = { sessionViewModel.lockStaffSession() },
            onSignOutGoogle = onSignOut,
          )
        }
      }
    }
  }
}

@Composable
private fun LuxurySplashLoading() {
  Box(
    modifier = Modifier.fillMaxSize().background(MatteBlack),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_store_logo),
        contentDescription = "2m Store",
        modifier =
          Modifier.size(88.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, MetallicGold, RoundedCornerShape(20.dp)),
      )
      Spacer(modifier = Modifier.height(16.dp))
      CircularProgressIndicator(color = MetallicGold)
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "جاري مزامنة بيانات 2m Store...",
        style = MaterialTheme.typography.bodyMedium,
        color = WarmMuted,
      )
    }
  }
}
