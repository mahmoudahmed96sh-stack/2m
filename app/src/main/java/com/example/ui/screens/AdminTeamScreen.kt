package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ActivityLogEntry
import com.example.data.model.ExpenseEntry
import com.example.data.model.Product
import com.example.data.model.StaffMember
import com.example.data.model.StoreWorkspace
import com.example.receipt.ReceiptGenerator
import com.example.ui.components.GoldBorderCard
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.WarmMuted
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminTeamScreen(
  store: StoreWorkspace,
  activeStaff: StaffMember,
  staffList: List<StaffMember>,
  productsState: UiState<List<Product>>,
  expensesState: UiState<List<ExpenseEntry>>,
  activityState: UiState<List<ActivityLogEntry>>,
  onUpsertProduct: (Product?, String, String, Double, Double, Long, Long) -> Unit,
  onDeleteProduct: (Product) -> Unit,
  onAddExpense: (String, String, Double) -> Unit,
  onUpdateStaffMember: (StaffMember, String, String?, Boolean) -> Unit,
  onSwitchStaffPin: () -> Unit,
  onSignOutGoogle: () -> Unit,
  onShowToast: (String) -> Unit,
) {
  var selectedSubTab by remember { mutableIntStateOf(0) }
  var editingProduct by remember { mutableStateOf<Product?>(null) }
  var showAddProduct by remember { mutableStateOf(false) }
  var showAddExpense by remember { mutableStateOf(false) }
  var editingStaff by remember { mutableStateOf<StaffMember?>(null) }

  val clipboard = LocalClipboardManager.current
  val products = (productsState as? UiState.Success)?.data ?: emptyList()
  val expenses = (expensesState as? UiState.Success)?.data ?: emptyList()
  val activities = (activityState as? UiState.Success)?.data ?: emptyList()

  Column(modifier = Modifier.fillMaxSize()) {
    TabRow(
      selectedTabIndex = selectedSubTab,
      containerColor = CardSurface,
      contentColor = BrightGold,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
          color = MetallicGold,
        )
      },
    ) {
      Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("النشاط") })
      Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("المخزون") })
      Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, text = { Text("المصاريف") })
      Tab(selected = selectedSubTab == 3, onClick = { selectedSubTab = 3 }, text = { Text("الفريق") })
    }

    when (selectedSubTab) {
      0 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (activities.isEmpty()) {
            item {
              GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
                Text("لا يوجد نشاط مسجل بعد.", color = WarmMuted)
              }
            }
          } else {
            items(activities, key = { it.id }) { log -> ActivityLogRowCard(log = log) }
          }
        }
      }

      1 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          item {
            GoldPrimaryButton(
              text = "إضافة منتج جديد للمخزون",
              icon = Icons.Default.Add,
              onClick = { showAddProduct = true },
              modifier = Modifier.fillMaxWidth(),
            )
          }
          items(products, key = { it.id }) { prod ->
            GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(prod.name, style = MaterialTheme.typography.titleMedium, color = IvoryText, fontWeight = FontWeight.Bold)
                  Text(
                    text = "بيع: ${ReceiptGenerator.formatEgp(prod.sellingPriceEgp)} • المخزون: ${prod.stockQty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrightGold,
                  )
                  if (activeStaff.isAdmin) {
                    Text(
                      text = "التكلفة: ${ReceiptGenerator.formatEgp(prod.costPriceEgp)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = WarmMuted,
                    )
                  }
                }
                Row {
                  IconButton(
                    onClick = { editingProduct = prod },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MetallicGold)
                  }
                  if (activeStaff.isAdmin) {
                    IconButton(
                      onClick = { onDeleteProduct(prod) },
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = CrimsonAlert)
                    }
                  }
                }
              }
            }
          }
        }
      }

      2 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          if (!activeStaff.isAdmin) {
            item {
              GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Lock, contentDescription = null, tint = BrightGold)
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    "سجل المصروفات وحسابات الأرباح مخصص لصلاحية المدير فقط.",
                    color = IvoryText,
                    style = MaterialTheme.typography.bodyMedium,
                  )
                }
              }
            }
          } else {
            item {
              GoldPrimaryButton(
                text = "تسجيل مصروف جديد (إيجار / كهرباء / رواتب)",
                icon = Icons.Default.Add,
                onClick = { showAddExpense = true },
                modifier = Modifier.fillMaxWidth(),
              )
            }
            items(expenses, key = { it.id }) { exp ->
              val dStr =
                exp.createdAt?.toDate()?.let {
                  SimpleDateFormat("yyyy/MM/dd", Locale.US).format(it)
                } ?: "اليوم"
              GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text(exp.title, style = MaterialTheme.typography.titleSmall, color = IvoryText, fontWeight = FontWeight.Bold)
                    Text("${exp.category} • $dStr", style = MaterialTheme.typography.labelSmall, color = WarmMuted)
                  }
                  Text(
                    text = ReceiptGenerator.formatEgp(exp.amountEgp),
                    style = MaterialTheme.typography.titleMedium,
                    color = CrimsonAlert,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
      }

      3 -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "كود ربط الأجهزة الأربعة (Store Join Code)",
                style = MaterialTheme.typography.labelLarge,
                color = WarmMuted,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = store.joinCode,
                  style = MaterialTheme.typography.headlineMedium,
                  color = BrightGold,
                  fontWeight = FontWeight.Bold,
                )
                GoldOutlinedButton(
                  text = "نسخ الكود",
                  icon = Icons.Default.ContentCopy,
                  onClick = {
                    clipboard.setText(AnnotatedString(store.joinCode))
                    onShowToast("تم نسخ كود المتجر: ${store.joinCode}")
                  },
                )
              }
              Text(
                text = "الأجهزة المتصلة حالياً: ${store.memberUids.size} جهاز",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldProfit,
              )
            }
          }

          items(staffList, key = { it.id }) { member ->
            GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = IvoryText,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${member.roleEnum.arabicLabel} • ${if (member.isActive) "نشط" else "معطّل"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (member.isAdmin) BrightGold else WarmMuted,
                  )
                }
                if (activeStaff.isAdmin) {
                  IconButton(
                    onClick = { editingStaff = member },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل الموظف", tint = MetallicGold)
                  }
                }
              }
            }
          }

          item {
            GoldOutlinedButton(
              text = "تبديل الموظف النشط (PIN)",
              icon = Icons.Default.Group,
              onClick = onSwitchStaffPin,
              modifier = Modifier.fillMaxWidth(),
              testTag = "switch_staff_pin_btn",
            )
            Spacer(modifier = Modifier.height(10.dp))
            GoldOutlinedButton(
              text = "تسجيل خروج من حساب Google",
              icon = Icons.AutoMirrored.Filled.Logout,
              onClick = onSignOutGoogle,
              modifier = Modifier.fillMaxWidth(),
              testTag = "sign_out_google_btn",
            )
          }
        }
      }
    }
  }

  if (showAddProduct || editingProduct != null) {
    ProductEditorDialog(
      existing = editingProduct,
      onDismiss = {
        showAddProduct = false
        editingProduct = null
      },
      onConfirm = { name, cat, sell, cost, qty, threshold ->
        onUpsertProduct(editingProduct, name, cat, sell, cost, qty, threshold)
        showAddProduct = false
        editingProduct = null
      },
    )
  }

  if (showAddExpense) {
    AddExpenseDialog(
      onDismiss = { showAddExpense = false },
      onConfirm = { title, cat, amount ->
        onAddExpense(title, cat, amount)
        showAddExpense = false
      },
    )
  }

  editingStaff?.let { staff ->
    EditStaffDialog(
      staff = staff,
      onDismiss = { editingStaff = null },
      onConfirm = { newName, newPin, active ->
        onUpdateStaffMember(staff, newName, newPin, active)
        editingStaff = null
      },
    )
  }
}

@Composable
private fun AddExpenseDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, Double) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf("تشغيل") }
  var amountText by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "تسجيل مصروف جديد",
        style = MaterialTheme.typography.titleLarge,
        color = BrightGold,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(10.dp))
      LuxuryTextField(value = title, onValueChange = { title = it }, label = "بيان المصروف")
      Spacer(modifier = Modifier.height(8.dp))
      LuxuryTextField(value = category, onValueChange = { category = it }, label = "البند (إيجار / كهرباء / رواتب)")
      Spacer(modifier = Modifier.height(8.dp))
      LuxuryTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = "المبلغ (ج.م)",
        keyboardType = KeyboardType.Decimal,
      )
      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldOutlinedButton(text = "إلغاء", onClick = onDismiss, modifier = Modifier.weight(1f))
        GoldPrimaryButton(
          text = "حفظ",
          enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
          onClick = { onConfirm(title, category, amountText.toDoubleOrNull() ?: 0.0) },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun EditStaffDialog(
  staff: StaffMember,
  onDismiss: () -> Unit,
  onConfirm: (String, String?, Boolean) -> Unit,
) {
  var name by remember { mutableStateOf(staff.name) }
  var newPin by remember { mutableStateOf("") }
  var isActive by remember { mutableStateOf(staff.isActive) }

  Dialog(onDismissRequest = onDismiss) {
    GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "تعديل بيانات ${staff.name}",
        style = MaterialTheme.typography.titleLarge,
        color = BrightGold,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(10.dp))
      LuxuryTextField(value = name, onValueChange = { name = it }, label = "الاسم")
      Spacer(modifier = Modifier.height(8.dp))
      LuxuryTextField(
        value = newPin,
        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
        label = "تغيير الرقم السري (4 أرقام - اتركه فارغاً للإبقاء)",
        keyboardType = KeyboardType.NumberPassword,
      )
      if (!staff.isAdmin) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("الحساب نشط", color = IvoryText)
          Switch(
            checked = isActive,
            onCheckedChange = { isActive = it },
            colors =
              SwitchDefaults.colors(
                checkedThumbColor = MatteBlack,
                checkedTrackColor = MetallicGold,
              ),
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldOutlinedButton(text = "إلغاء", onClick = onDismiss, modifier = Modifier.weight(1f))
        GoldPrimaryButton(
          text = "حفظ",
          enabled = name.isNotBlank() && (newPin.isEmpty() || newPin.length == 4),
          onClick = { onConfirm(name, newPin.takeIf { it.length == 4 }, isActive) },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}
