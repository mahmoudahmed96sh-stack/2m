package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DebtDirection
import com.example.data.model.DebtPayment
import com.example.data.model.DebtRecord
import com.example.data.model.DebtStatus
import com.example.receipt.ReceiptGenerator
import com.example.ui.components.DebtStatusPill
import com.example.ui.components.GoldBorderCard
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.SubtleGoldBorder
import com.example.ui.theme.WarmMuted
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebtsScreen(
  debtsState: UiState<List<DebtRecord>>,
  paymentsState: UiState<List<DebtPayment>>,
  isSubmitting: Boolean,
  onCreateDebt: (DebtDirection, String, String, String, Double, Int) -> Unit,
  onSettleInstallment: (DebtRecord, Double, String) -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var showAddDialog by remember { mutableStateOf(false) }
  var settlingDebt by remember { mutableStateOf<DebtRecord?>(null) }

  val context = LocalContext.current
  val allDebts = (debtsState as? UiState.Success)?.data ?: emptyList()
  val allPayments = (paymentsState as? UiState.Success)?.data ?: emptyList()
  val activeDirection =
    if (selectedTab == 0) DebtDirection.RECEIVABLE else DebtDirection.PAYABLE

  val filteredDebts =
    remember(allDebts, activeDirection) {
      allDebts.filter { it.directionEnum == activeDirection }
    }
  val totalRemaining =
    filteredDebts.filter { it.statusEnum != DebtStatus.SETTLED }.sumOf { it.remainingAmountEgp }

  Column(modifier = Modifier.fillMaxSize()) {
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
        text = { Text("لينا (على العملاء)") },
        modifier = Modifier.testTag("debts_tab_receivables"),
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("علينا (للموردين)") },
        modifier = Modifier.testTag("debts_tab_payables"),
      )
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text =
                  if (activeDirection == DebtDirection.RECEIVABLE) "إجمالي المبالغ المستحقة لنا"
                  else "إجمالي المبالغ المطلوبة للموردين",
                style = MaterialTheme.typography.labelLarge,
                color = WarmMuted,
              )
              Text(
                text = ReceiptGenerator.formatEgp(totalRemaining),
                style = MaterialTheme.typography.headlineMedium,
                color = if (activeDirection == DebtDirection.RECEIVABLE) BrightGold else CrimsonAlert,
                fontWeight = FontWeight.Bold,
              )
            }
            GoldPrimaryButton(
              text = "إضافة حساب",
              icon = Icons.Default.Add,
              onClick = { showAddDialog = true },
              testTag = "add_debt_btn",
            )
          }
        }
      }

      if (filteredDebts.isEmpty()) {
        item {
          GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
            Text(
              text =
                if (activeDirection == DebtDirection.RECEIVABLE) "لا توجد مديونيات عملاء مسجلة حالياً."
                else "لا توجد مستحقات لموردين مسجلة حالياً.",
              style = MaterialTheme.typography.bodyMedium,
              color = WarmMuted,
            )
          }
        }
      } else {
        items(filteredDebts, key = { it.id }) { debt ->
          val now = System.currentTimeMillis()
          val isOverdue =
            debt.statusEnum != DebtStatus.SETTLED && debt.dueDateMillis > 0L && debt.dueDateMillis < now
          val dueStr =
            if (debt.dueDateMillis > 0L) {
              SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(debt.dueDateMillis))
            } else "غير محدد"
          val history = allPayments.filter { it.debtId == debt.id }

          GoldBorderCard(highlighted = isOverdue, modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = debt.partyName,
                  style = MaterialTheme.typography.titleMedium,
                  color = IvoryText,
                  fontWeight = FontWeight.Bold,
                )
                if (debt.description.isNotBlank()) {
                  Text(
                    text = debt.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmMuted,
                  )
                }
                Text(
                  text = "الاستحقاق: $dueStr • سجل بواسطة: ${debt.createdByStaffName}",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (isOverdue) CrimsonAlert else WarmMuted,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              DebtStatusPill(status = debt.statusEnum, isOverdue = isOverdue)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "المتبقي: ${ReceiptGenerator.formatEgp(debt.remainingAmountEgp)}",
                  style = MaterialTheme.typography.titleMedium,
                  color = BrightGold,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "الأصل: ${ReceiptGenerator.formatEgp(debt.originalAmountEgp)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = WarmMuted,
                )
              }

              if (debt.phone.isNotBlank()) {
                Surface(
                  modifier =
                    Modifier.clip(RoundedCornerShape(8.dp))
                      .border(1.dp, SubtleGoldBorder, RoundedCornerShape(8.dp))
                      .clickable {
                        context.startActivity(
                          Intent(Intent.ACTION_DIAL, Uri.parse("tel:${debt.phone}"))
                        )
                      },
                  color = ElevatedSurface,
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Default.Phone,
                      contentDescription = "اتصال",
                      tint = EmeraldProfit,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(debt.phone, style = MaterialTheme.typography.labelMedium, color = IvoryText)
                  }
                }
              }
            }

            if (history.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              Text("سجل الدفعات:", style = MaterialTheme.typography.labelMedium, color = MetallicGold)
              history.take(3).forEach { pay ->
                val pDate =
                  pay.createdAt?.toDate()?.let {
                    SimpleDateFormat("MM/dd", Locale.US).format(it)
                  } ?: "الآن"
                Text(
                  text = "• ${ReceiptGenerator.formatEgp(pay.amountEgp)} ($pDate) - ${pay.staffName}",
                  style = MaterialTheme.typography.labelSmall,
                  color = WarmMuted,
                )
              }
            }

            if (debt.statusEnum != DebtStatus.SETTLED) {
              Spacer(modifier = Modifier.height(10.dp))
              GoldPrimaryButton(
                text = "سداد دفعة / تصفية الحساب",
                icon = Icons.Default.Payments,
                onClick = { settlingDebt = debt },
                modifier = Modifier.fillMaxWidth(),
                testTag = "settle_debt_btn_${debt.id}",
              )
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    CreateDebtDialog(
      defaultDirection = activeDirection,
      isSubmitting = isSubmitting,
      onDismiss = { showAddDialog = false },
      onConfirm = { dir, name, phone, desc, amt, days ->
        onCreateDebt(dir, name, phone, desc, amt, days)
        showAddDialog = false
      },
    )
  }

  settlingDebt?.let { debt ->
    SettleInstallmentDialog(
      debt = debt,
      isSubmitting = isSubmitting,
      onDismiss = { settlingDebt = null },
      onConfirm = { amount, note ->
        onSettleInstallment(debt, amount, note)
        settlingDebt = null
      },
    )
  }
}

@Composable
private fun CreateDebtDialog(
  defaultDirection: DebtDirection,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (DebtDirection, String, String, String, Double, Int) -> Unit,
) {
  var direction by remember { mutableStateOf(defaultDirection) }
  var partyName by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var dueDaysText by remember { mutableStateOf("7") }

  Dialog(onDismissRequest = onDismiss) {
    GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "تسجيل حساب جديد",
        style = MaterialTheme.typography.titleLarge,
        color = BrightGold,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(10.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DebtDirection.entries.forEach { dir ->
          val selected = direction == dir
          Surface(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .border(
                  1.dp,
                  if (selected) MetallicGold else SubtleGoldBorder,
                  RoundedCornerShape(10.dp),
                )
                .clickable { direction = dir },
            color = if (selected) MetallicGold.copy(alpha = 0.2f) else CardSurface,
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
              Text(
                text = if (dir == DebtDirection.RECEIVABLE) "لينا" else "علينا",
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) BrightGold else WarmMuted,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      LuxuryTextField(
        value = partyName,
        onValueChange = { partyName = it },
        label = if (direction == DebtDirection.RECEIVABLE) "اسم العميل" else "اسم المورد / التاجر",
      )
      Spacer(modifier = Modifier.height(8.dp))
      LuxuryTextField(
        value = phone,
        onValueChange = { phone = it },
        label = "رقم الهاتف",
        keyboardType = KeyboardType.Phone,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LuxuryTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = "المبلغ (ج.م)",
          keyboardType = KeyboardType.Decimal,
          modifier = Modifier.weight(1.3f),
        )
        LuxuryTextField(
          value = dueDaysText,
          onValueChange = { dueDaysText = it },
          label = "استحقاق (أيام)",
          keyboardType = KeyboardType.Number,
          modifier = Modifier.weight(1f),
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      LuxuryTextField(value = description, onValueChange = { description = it }, label = "التفاصيل / الملاحظات")
      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldOutlinedButton(text = "إلغاء", onClick = onDismiss, modifier = Modifier.weight(1f))
        GoldPrimaryButton(
          text = if (isSubmitting) "جاري الحفظ..." else "حفظ",
          enabled = !isSubmitting && partyName.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
          onClick = {
            onConfirm(
              direction,
              partyName,
              phone,
              description,
              amountText.toDoubleOrNull() ?: 0.0,
              dueDaysText.toIntOrNull()?.coerceAtLeast(0) ?: 7,
            )
          },
          modifier = Modifier.weight(1f),
          testTag = "confirm_create_debt_btn",
        )
      }
    }
  }
}

@Composable
private fun SettleInstallmentDialog(
  debt: DebtRecord,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (Double, String) -> Unit,
) {
  var amountText by remember { mutableStateOf(debt.remainingAmountEgp.toInt().toString()) }
  var note by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "سداد دفعة — ${debt.partyName}",
        style = MaterialTheme.typography.titleLarge,
        color = BrightGold,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "المبلغ المتبقي: ${ReceiptGenerator.formatEgp(debt.remainingAmountEgp)}",
        style = MaterialTheme.typography.bodyMedium,
        color = IvoryText,
      )
      Spacer(modifier = Modifier.height(10.dp))
      LuxuryTextField(
        value = amountText,
        onValueChange = { amountText = it },
        label = "قيمة الدفعة (ج.م)",
        keyboardType = KeyboardType.Decimal,
      )
      TextButton(
        onClick = { amountText = debt.remainingAmountEgp.toInt().toString() },
      ) {
        Text("تصفية المبلغ بالكامل (${ReceiptGenerator.formatEgp(debt.remainingAmountEgp)})", color = EmeraldProfit)
      }
      LuxuryTextField(value = note, onValueChange = { note = it }, label = "ملاحظة (اختياري)")
      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldOutlinedButton(text = "إلغاء", onClick = onDismiss, modifier = Modifier.weight(1f))
        GoldPrimaryButton(
          text = if (isSubmitting) "جاري التسجيل..." else "تأكيد السداد",
          enabled = !isSubmitting && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
          onClick = { onConfirm(amountText.toDoubleOrNull() ?: 0.0, note) },
          modifier = Modifier.weight(1.2f),
          testTag = "confirm_settle_debt_btn",
        )
      }
    }
  }
}
