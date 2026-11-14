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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.RepairStatus
import com.example.data.model.RepairTicket
import com.example.data.model.StaffMember
import com.example.receipt.ReceiptGenerator
import com.example.receipt.ReceiptPayload
import com.example.ui.components.GoldBorderCard
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.RepairStatusPill
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.SubtleGoldBorder
import com.example.ui.theme.WarmMuted
import com.example.ui.viewmodel.StoreMainViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun RepairsScreen(
  activeStaff: StaffMember,
  staffList: List<StaffMember>,
  repairsState: UiState<List<RepairTicket>>,
  isSubmitting: Boolean,
  onCreateTicket: (
    customerName: String,
    customerPhone: String,
    deviceModel: String,
    issueDescription: String,
    estimatedCostEgp: Double,
    partsCostEgp: Double,
    paidAmountEgp: Double,
    assignedTech: StaffMember,
    scheduledDateKey: String,
  ) -> Unit,
  onUpdateStatus: (RepairTicket, RepairStatus) -> Unit,
  onOpenReceipt: (ReceiptPayload) -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }
  var showCreateDialog by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val todayKey = StoreMainViewModel.todayDateKey()
  val allRepairs = (repairsState as? UiState.Success)?.data ?: emptyList()

  val filteredRepairs =
    remember(allRepairs, selectedTab, searchQuery, todayKey) {
      val base =
        if (selectedTab == 0) allRepairs.filter { it.scheduledDateKey == todayKey }
        else allRepairs
      if (searchQuery.isBlank()) base
      else {
        base.filter {
          it.customerName.contains(searchQuery, true) ||
            it.customerPhone.contains(searchQuery, true) ||
            it.deviceModel.contains(searchQuery, true) ||
            it.ticketNumber.contains(searchQuery, true)
        }
      }
    }

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
        text = { Text("صيانة اليوم (${allRepairs.count { it.scheduledDateKey == todayKey }})") },
        modifier = Modifier.testTag("repairs_tab_today"),
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("كل الأجهزة (${allRepairs.size})") },
        modifier = Modifier.testTag("repairs_tab_all"),
      )
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        GoldPrimaryButton(
          text = "استلام جهاز صيانة جديد",
          icon = Icons.Default.Add,
          onClick = { showCreateDialog = true },
          modifier = Modifier.fillMaxWidth(),
          testTag = "add_repair_ticket_btn",
        )
      }

      item {
        LuxuryTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          label = "ابحث بالاسم أو رقم الهاتف أو موديل الجهاز...",
        )
      }

      if (filteredRepairs.isEmpty()) {
        item {
          GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = MetallicGold)
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text =
                  if (selectedTab == 0) "لا توجد أجهزة مجدولة لصيانة اليوم."
                  else "لا توجد تذاكر صيانة مطابقة.",
                style = MaterialTheme.typography.bodyMedium,
                color = WarmMuted,
              )
            }
          }
        }
      } else {
        items(filteredRepairs, key = { it.id }) { ticket ->
          GoldBorderCard(highlighted = ticket.statusEnum == RepairStatus.READY_FOR_PICKUP) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.Top,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${ticket.ticketNumber} • ${ticket.deviceModel}",
                  style = MaterialTheme.typography.titleMedium,
                  color = BrightGold,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "العميل: ${ticket.customerName}",
                  style = MaterialTheme.typography.bodyMedium,
                  color = IvoryText,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = "العطل: ${ticket.issueDescription}",
                  style = MaterialTheme.typography.bodySmall,
                  color = WarmMuted,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "الفني المسؤول: ${ticket.assignedTechName} • موعد: ${ticket.scheduledDateKey}",
                  style = MaterialTheme.typography.labelMedium,
                  color = IvoryText,
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              RepairStatusPill(status = ticket.statusEnum)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                modifier =
                  Modifier.clip(RoundedCornerShape(8.dp))
                    .border(1.dp, SubtleGoldBorder, RoundedCornerShape(8.dp))
                    .clickable {
                      val dialIntent =
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ticket.customerPhone}"))
                      context.startActivity(dialIntent)
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
                  Text(
                    text = ticket.customerPhone,
                    style = MaterialTheme.typography.labelMedium,
                    color = IvoryText,
                  )
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "التكلفة: ${ReceiptGenerator.formatEgp(ticket.estimatedCostEgp)}",
                  style = MaterialTheme.typography.titleSmall,
                  color = BrightGold,
                  fontWeight = FontWeight.Bold,
                )
                if (ticket.remainingAmountEgp > 0.01) {
                  Text(
                    text = "متبقي: ${ReceiptGenerator.formatEgp(ticket.remainingAmountEgp)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmMuted,
                  )
                }
              }
            }

            // Status quick-toggle bar
            if (ticket.statusEnum != RepairStatus.DELIVERED || activeStaff.isAdmin) {
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                RepairStatus.entries.forEach { st ->
                  val selected = ticket.statusEnum == st
                  Surface(
                    modifier =
                      Modifier.weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                          1.dp,
                          if (selected) MetallicGold else SubtleGoldBorder,
                          RoundedCornerShape(8.dp),
                        )
                        .clickable { if (!selected) onUpdateStatus(ticket, st) },
                    color = if (selected) MetallicGold.copy(alpha = 0.2f) else CardSurface,
                  ) {
                    Box(
                      contentAlignment = Alignment.Center,
                      modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                    ) {
                      Text(
                        text =
                          when (st) {
                            RepairStatus.PENDING -> "انتظار"
                            RepairStatus.IN_PROGRESS -> "يعمل"
                            RepairStatus.READY_FOR_PICKUP -> "جاهز"
                            RepairStatus.DELIVERED -> "سُلّم"
                          },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) BrightGold else WarmMuted,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            GoldOutlinedButton(
              text = "إيصال الصيانة ومشاركته",
              icon = Icons.AutoMirrored.Filled.ReceiptLong,
              onClick = { onOpenReceipt(ReceiptPayload.RepairReceipt(ticket)) },
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
    }
  }

  if (showCreateDialog) {
    CreateRepairDialog(
      staffList = staffList.filter { it.isActive },
      defaultTech = activeStaff,
      isSubmitting = isSubmitting,
      onDismiss = { showCreateDialog = false },
      onConfirm = { cName, cPhone, model, issue, estCost, partsCost, paid, tech, dateKey ->
        onCreateTicket(cName, cPhone, model, issue, estCost, partsCost, paid, tech, dateKey)
        showCreateDialog = false
      },
    )
  }
}

@Composable
private fun CreateRepairDialog(
  staffList: List<StaffMember>,
  defaultTech: StaffMember,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, String, Double, Double, Double, StaffMember, String) -> Unit,
) {
  var customerName by remember { mutableStateOf("") }
  var customerPhone by remember { mutableStateOf("") }
  var deviceModel by remember { mutableStateOf("") }
  var issueDescription by remember { mutableStateOf("") }
  var estimatedCostText by remember { mutableStateOf("") }
  var partsCostText by remember { mutableStateOf("0") }
  var paidText by remember { mutableStateOf("0") }
  var selectedTech by remember { mutableStateOf(defaultTech) }
  val todayKey = StoreMainViewModel.todayDateKey()

  Dialog(onDismissRequest = onDismiss) {
    GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
          text = "استلام جهاز صيانة جديد",
          style = MaterialTheme.typography.titleLarge,
          color = BrightGold,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        LuxuryTextField(value = customerName, onValueChange = { customerName = it }, label = "اسم العميل")
        Spacer(modifier = Modifier.height(8.dp))
        LuxuryTextField(
          value = customerPhone,
          onValueChange = { customerPhone = it },
          label = "رقم الهاتف",
          keyboardType = KeyboardType.Phone,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LuxuryTextField(
          value = deviceModel,
          onValueChange = { deviceModel = it },
          label = "موديل الجهاز (مثال: iPhone 14 Pro)",
        )
        Spacer(modifier = Modifier.height(8.dp))
        LuxuryTextField(
          value = issueDescription,
          onValueChange = { issueDescription = it },
          label = "وصف العطل",
          singleLine = false,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          LuxuryTextField(
            value = estimatedCostText,
            onValueChange = { estimatedCostText = it },
            label = "السعر للعميل (ج.م)",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
          )
          LuxuryTextField(
            value = paidText,
            onValueChange = { paidText = it },
            label = "مدفوع مقدم",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.weight(1f),
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LuxuryTextField(
          value = partsCostText,
          onValueChange = { partsCostText = it },
          label = "تكلفة قطع الغيار (ج.م)",
          keyboardType = KeyboardType.Decimal,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text("الفني المسؤول:", style = MaterialTheme.typography.labelLarge, color = MetallicGold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          staffList.forEach { member ->
            val selected = selectedTech.id == member.id
            Surface(
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .border(
                    1.dp,
                    if (selected) MetallicGold else SubtleGoldBorder,
                    RoundedCornerShape(8.dp),
                  )
                  .clickable { selectedTech = member },
              color = if (selected) MetallicGold.copy(alpha = 0.2f) else CardSurface,
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                  text = member.name,
                  style = MaterialTheme.typography.labelSmall,
                  color = if (selected) BrightGold else WarmMuted,
                  maxLines = 1,
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          GoldOutlinedButton(text = "إلغاء", onClick = onDismiss, modifier = Modifier.weight(1f))
          GoldPrimaryButton(
            text = if (isSubmitting) "جاري الحفظ..." else "حفظ التذكرة",
            enabled =
              !isSubmitting &&
                customerName.isNotBlank() &&
                customerPhone.isNotBlank() &&
                deviceModel.isNotBlank() &&
                issueDescription.isNotBlank(),
            onClick = {
              onConfirm(
                customerName,
                customerPhone,
                deviceModel,
                issueDescription,
                estimatedCostText.toDoubleOrNull() ?: 0.0,
                partsCostText.toDoubleOrNull() ?: 0.0,
                paidText.toDoubleOrNull() ?: 0.0,
                selectedTech,
                todayKey,
              )
            },
            modifier = Modifier.weight(1.2f),
            testTag = "confirm_create_repair_btn",
          )
        }
      }
    }
  }
}
