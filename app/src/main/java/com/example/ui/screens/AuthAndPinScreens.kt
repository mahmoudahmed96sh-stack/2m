package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.onGoogleSignInClicked
import com.example.data.model.StaffMember
import com.example.data.model.StoreWorkspace
import com.example.ui.components.GoldBorderCard
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.SubtleGoldBorder
import com.example.ui.theme.WarmMuted

@Composable
fun GoogleAuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(MatteBlack, Color(0xFF16130B), MatteBlack)))
        .safeDrawingPadding()
        .padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_store_logo),
        contentDescription = "شعار متجر 2m Store",
        contentScale = ContentScale.Crop,
        modifier =
          Modifier.size(124.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(1.5.dp, MetallicGold, RoundedCornerShape(26.dp)),
      )
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = "2m Store",
        style = MaterialTheme.typography.displayLarge,
        color = BrightGold,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "نظام الإدارة المتكامل للمبيعات والصيانة والديون",
        style = MaterialTheme.typography.titleMedium,
        color = IvoryText,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(24.dp))

      GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
        FeatureBullet(Icons.Default.CloudSync, "مزامنة لحظية فورية عبر 4 أجهزة (المالك + 3 موظفين)")
        Spacer(modifier = Modifier.height(12.dp))
        FeatureBullet(Icons.Default.Storefront, "نقطة بيع سريعة وحساب صافي الربح والمصروفات بالجنيه المصري")
        Spacer(modifier = Modifier.height(12.dp))
        FeatureBullet(Icons.Default.Build, "متابعة صيانة اليوم والديون (لينا / علينا) والفواتير عبر واتساب")
      }

      Spacer(modifier = Modifier.height(24.dp))

      if (!errorMsg.isNullOrBlank()) {
        Text(
          text = errorMsg ?: "",
          color = CrimsonAlert,
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(bottom = 12.dp),
        )
      }

      GoldPrimaryButton(
        text = if (isLoading) "جاري تسجيل الدخول..." else "تسجيل الدخول بحساب Google",
        enabled = !isLoading,
        icon = Icons.Default.Lock,
        modifier = Modifier.fillMaxWidth(),
        testTag = "google_sign_in_button",
        onClick = {
          isLoading = true
          errorMsg = null
          onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
              isLoading = false
              onAuthSuccess()
            },
            onAuthError = { msg ->
              isLoading = false
              errorMsg = msg
            },
            scope = scope,
            onAuthCancelled = { isLoading = false },
          )
        },
      )

      if (isLoading) {
        Spacer(modifier = Modifier.height(12.dp))
        CircularProgressIndicator(color = MetallicGold, modifier = Modifier.size(28.dp))
      }
    }
  }
}

@Composable
private fun FeatureBullet(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
    Box(
      modifier =
        Modifier.size(36.dp)
          .clip(CircleShape)
          .background(MetallicGold.copy(alpha = 0.14f))
          .border(1.dp, SubtleGoldBorder, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = BrightGold, modifier = Modifier.size(18.dp))
    }
    Spacer(modifier = Modifier.width(12.dp))
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = IvoryText)
  }
}

@Composable
fun StoreOnboardingScreen(
  isBusy: Boolean,
  errorMessage: String?,
  onCreateStore: (
    storeName: String,
    ownerName: String,
    ownerPin: String,
    staff1Name: String,
    staff1Pin: String,
    staff2Name: String,
    staff2Pin: String,
    staff3Name: String,
    staff3Pin: String,
  ) -> Unit,
  onJoinStore: (String) -> Unit,
  onSignOut: () -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var storeName by remember { mutableStateOf("2m Store") }
  var ownerName by remember { mutableStateOf("المالك") }
  var ownerPin by remember { mutableStateOf("1111") }
  var staff1Name by remember { mutableStateOf("موظف 1") }
  var staff1Pin by remember { mutableStateOf("2222") }
  var staff2Name by remember { mutableStateOf("موظف 2") }
  var staff2Pin by remember { mutableStateOf("3333") }
  var staff3Name by remember { mutableStateOf("موظف 3") }
  var staff3Pin by remember { mutableStateOf("4444") }
  var joinCode by remember { mutableStateOf("") }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(MatteBlack)
        .safeDrawingPadding()
        .padding(16.dp),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "إعداد مساحة عمل 2m Store",
          style = MaterialTheme.typography.headlineMedium,
          color = BrightGold,
          fontWeight = FontWeight.Bold,
        )
        TextButton(onClick = onSignOut, modifier = Modifier.minimumInteractiveComponentSize()) {
          Text("خروج", color = WarmMuted)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = CardSurface,
        contentColor = BrightGold,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = MetallicGold,
          )
        },
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("إنشاء متجر جديد") },
          modifier = Modifier.testTag("tab_create_store"),
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("الانضمام بكود المتجر") },
          modifier = Modifier.testTag("tab_join_store"),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (!errorMessage.isNullOrBlank()) {
        Text(
          text = errorMessage,
          color = CrimsonAlert,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(bottom = 12.dp),
        )
      }

      if (selectedTab == 0) {
        GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "إعداد المالك و 3 حسابات للموظفين",
            style = MaterialTheme.typography.titleMedium,
            color = BrightGold,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(12.dp))
          LuxuryTextField(
            value = storeName,
            onValueChange = { storeName = it },
            label = "اسم المتجر",
          )
          Spacer(modifier = Modifier.height(10.dp))
          StaffPinSetupRow(
            title = "المالك (صلاحية كاملة)",
            name = ownerName,
            onNameChange = { ownerName = it },
            pin = ownerPin,
            onPinChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) ownerPin = it },
          )
          StaffPinSetupRow(
            title = "موظف 1",
            name = staff1Name,
            onNameChange = { staff1Name = it },
            pin = staff1Pin,
            onPinChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) staff1Pin = it },
          )
          StaffPinSetupRow(
            title = "موظف 2",
            name = staff2Name,
            onNameChange = { staff2Name = it },
            pin = staff2Pin,
            onPinChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) staff2Pin = it },
          )
          StaffPinSetupRow(
            title = "موظف 3",
            name = staff3Name,
            onNameChange = { staff3Name = it },
            pin = staff3Pin,
            onPinChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) staff3Pin = it },
          )
          Spacer(modifier = Modifier.height(16.dp))
          GoldPrimaryButton(
            text = if (isBusy) "جاري الإنشاء..." else "إنشاء متجر 2m والبدء",
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
            testTag = "create_store_submit",
            onClick = {
              onCreateStore(
                storeName,
                ownerName,
                ownerPin,
                staff1Name,
                staff1Pin,
                staff2Name,
                staff2Pin,
                staff3Name,
                staff3Pin,
              )
            },
          )
        }
      } else {
        GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "ربط جهاز موظف بمتجر قائم",
            style = MaterialTheme.typography.titleMedium,
            color = BrightGold,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "أدخل كود الدعوة المكون من 6 أحرف من شاشة الإدارة في جهاز المالك للمزامنة الفورية.",
            style = MaterialTheme.typography.bodyMedium,
            color = WarmMuted,
          )
          Spacer(modifier = Modifier.height(14.dp))
          LuxuryTextField(
            value = joinCode,
            onValueChange = { joinCode = it.uppercase() },
            label = "كود المتجر (مثال: 2MA9F1)",
          )
          Spacer(modifier = Modifier.height(16.dp))
          GoldPrimaryButton(
            text = if (isBusy) "جاري الربط..." else "الانضمام للمتجر",
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth(),
            testTag = "join_store_submit",
            onClick = { onJoinStore(joinCode) },
          )
        }
      }
    }
  }
}

@Composable
private fun StaffPinSetupRow(
  title: String,
  name: String,
  onNameChange: (String) -> Unit,
  pin: String,
  onPinChange: (String) -> Unit,
) {
  Spacer(modifier = Modifier.height(8.dp))
  Text(text = title, style = MaterialTheme.typography.labelMedium, color = MetallicGold)
  Spacer(modifier = Modifier.height(4.dp))
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    LuxuryTextField(
      value = name,
      onValueChange = onNameChange,
      label = "الاسم",
      modifier = Modifier.weight(1.4f),
    )
    LuxuryTextField(
      value = pin,
      onValueChange = onPinChange,
      label = "PIN (4 أرقام)",
      keyboardType = KeyboardType.NumberPassword,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
fun StaffPinSelectionScreen(
  store: StoreWorkspace,
  staffList: List<StaffMember>,
  errorMessage: String?,
  onUnlockStaff: (StaffMember, String) -> Boolean,
  onSignOut: () -> Unit,
) {
  var selectedStaff by remember { mutableStateOf<StaffMember?>(null) }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(MatteBlack, ElevatedSurface, MatteBlack)))
        .safeDrawingPadding()
        .padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_store_logo),
        contentDescription = "2m Store",
        modifier =
          Modifier.size(78.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, MetallicGold, RoundedCornerShape(18.dp)),
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = store.storeName,
        style = MaterialTheme.typography.headlineLarge,
        color = BrightGold,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "اختر حسابك وأدخل الرقم السري (4 أرقام) للمتابعة",
        style = MaterialTheme.typography.bodyMedium,
        color = WarmMuted,
      )
      Spacer(modifier = Modifier.height(20.dp))

      val rows = staffList.filter { it.isActive }.chunked(2)
      rows.forEach { pair ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          pair.forEach { member ->
            GoldBorderCard(
              highlighted = member.isAdmin,
              onClick = { selectedStaff = member },
              modifier = Modifier.weight(1f).testTag("staff_card_${member.slotIndex}"),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Box(
                  modifier =
                    Modifier.size(48.dp)
                      .clip(CircleShape)
                      .background(MetallicGold.copy(alpha = 0.16f))
                      .border(1.dp, MetallicGold, CircleShape),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = if (member.isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                    contentDescription = member.name,
                    tint = BrightGold,
                    modifier = Modifier.size(24.dp),
                  )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = member.name,
                  style = MaterialTheme.typography.titleMedium,
                  color = IvoryText,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = if (member.isAdmin) "مدير النظام" else "موظف",
                  style = MaterialTheme.typography.labelMedium,
                  color = if (member.isAdmin) BrightGold else WarmMuted,
                )
              }
            }
          }
          if (pair.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }

      Spacer(modifier = Modifier.height(10.dp))
      GoldOutlinedButton(
        text = "تسجيل خروج من حساب Google",
        icon = Icons.Default.Group,
        onClick = onSignOut,
      )
    }
  }

  selectedStaff?.let { staff ->
    PinPadDialog(
      staff = staff,
      errorMessage = errorMessage,
      onDismiss = { selectedStaff = null },
      onSubmitPin = { pin ->
        if (onUnlockStaff(staff, pin)) {
          selectedStaff = null
        }
      },
    )
  }
}

@Composable
fun PinPadDialog(
  staff: StaffMember,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onSubmitPin: (String) -> Unit,
) {
  var enteredPin by remember(staff.id) { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier =
        Modifier.fillMaxWidth()
          .border(1.2.dp, MetallicGold, RoundedCornerShape(22.dp)),
      color = MatteBlack,
      shape = RoundedCornerShape(22.dp),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "الرقم السري لـ ${staff.name}",
          style = MaterialTheme.typography.titleLarge,
          color = BrightGold,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = if (staff.isAdmin) "دخول بصلاحية المدير" else "دخول بصلاحية الموظف",
          style = MaterialTheme.typography.bodySmall,
          color = WarmMuted,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 4 PIN dots
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          repeat(4) { idx ->
            val filled = idx < enteredPin.length
            Box(
              modifier =
                Modifier.size(18.dp)
                  .clip(CircleShape)
                  .background(if (filled) BrightGold else CardSurface)
                  .border(1.5.dp, MetallicGold, CircleShape)
            )
          }
        }

        if (!errorMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(text = errorMessage, color = CrimsonAlert, style = MaterialTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.height(18.dp))

        val keys =
          listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("مسح", "0", "⌫"),
          )

        keys.forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            row.forEach { key ->
              Surface(
                modifier =
                  Modifier.weight(1f)
                    .heightIn(min = 54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, SubtleGoldBorder, RoundedCornerShape(14.dp))
                    .clickable {
                      when (key) {
                        "مسح" -> enteredPin = ""
                        "⌫" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                        else -> {
                          if (enteredPin.length < 4) {
                            val next = enteredPin + key
                            enteredPin = next
                            if (next.length == 4) {
                              onSubmitPin(next)
                              enteredPin = ""
                            }
                          }
                        }
                      }
                    }
                    .testTag("pin_key_$key"),
                color = ElevatedSurface,
                shape = RoundedCornerShape(14.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  if (key == "⌫") {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.Backspace,
                      contentDescription = "حذف",
                      tint = BrightGold,
                    )
                  } else {
                    Text(
                      text = key,
                      style = MaterialTheme.typography.titleLarge,
                      color = if (key == "مسح") WarmMuted else IvoryText,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
          Text("إلغاء", color = WarmMuted)
        }
      }
    }
  }
}

@Composable
fun LuxuryTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  keyboardType: KeyboardType = KeyboardType.Text,
  singleLine: Boolean = true,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    singleLine = singleLine,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    colors =
      OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MetallicGold,
        unfocusedBorderColor = SubtleGoldBorder,
        focusedLabelColor = BrightGold,
        unfocusedLabelColor = WarmMuted,
        focusedTextColor = IvoryText,
        unfocusedTextColor = IvoryText,
        cursorColor = BrightGold,
        focusedContainerColor = CardSurface,
        unfocusedContainerColor = CardSurface,
      ),
  )
}
