package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityLogEntry
import com.example.data.model.RepairStatus
import com.example.data.model.RepairTicket
import com.example.data.model.StaffMember
import com.example.receipt.ReceiptGenerator
import com.example.ui.components.GoldBorderCard
import com.example.ui.components.KpiSummaryCard
import com.example.ui.components.RepairStatusPill
import com.example.ui.theme.AmberProgress
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.SkyReady
import com.example.ui.theme.SubtleGoldBorder
import com.example.ui.theme.WarmMuted
import com.example.ui.viewmodel.DashboardMetrics
import com.example.ui.viewmodel.StoreMainViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
  activeStaff: StaffMember,
  metrics: DashboardMetrics,
  repairsState: UiState<List<RepairTicket>>,
  activityState: UiState<List<ActivityLogEntry>>,
  onCycleRepairStatus: (RepairTicket, RepairStatus) -> Unit,
  onOpenRepairsTab: () -> Unit,
  onOpenPosTab: () -> Unit,
  onOpenDebtsTab: () -> Unit,
  onOpenActivityTab: () -> Unit,
) {
  val todayKey = StoreMainViewModel.todayDateKey()
  val todayRepairs =
    ((repairsState as? UiState.Success)?.data ?: emptyList())
      .filter { it.scheduledDateKey == todayKey }
      .take(5)
  val recentActivities =
    ((activityState as? UiState.Success)?.data ?: emptyList()).take(6)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      // Live sync banner
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ElevatedSurface)
            .border(1.dp, SubtleGoldBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Sync,
            contentDescription = "مزامنة لحظية",
            tint = EmeraldProfit,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "متصل بالمزامنة اللحظية • ${activeStaff.name}",
            style = MaterialTheme.typography.labelLarge,
            color = IvoryText,
          )
        }
        Text(
          text = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date()),
          style = MaterialTheme.typography.labelMedium,
          color = BrightGold,
        )
      }
    }

    // 5 Gold-Bordered Top Summary Cards
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          KpiSummaryCard(
            title = "مبيعات اليوم",
            value = ReceiptGenerator.formatEgp(metrics.todaySalesEgp),
            icon = Icons.Default.PointOfSale,
            accentColor = BrightGold,
            modifier = Modifier.weight(1f).clickable(onClick = onOpenPosTab),
            testTag = "kpi_today_sales",
          )
          KpiSummaryCard(
            title = "صافي ربح اليوم",
            value = ReceiptGenerator.formatEgp(metrics.todayNetProfitEgp),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            accentColor = EmeraldProfit,
            isLocked = !activeStaff.isAdmin,
            subtitle = if (activeStaff.isAdmin && metrics.todayExpensesEgp > 0) "بعد خصم ${ReceiptGenerator.formatEgp(metrics.todayExpensesEgp)} مصاريف" else null,
            modifier = Modifier.weight(1f),
            testTag = "kpi_today_profit",
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          KpiSummaryCard(
            title = "أجهزة صيانة اليوم",
            value = "${metrics.todayRepairsActiveCount} جهاز",
            icon = Icons.Default.Build,
            accentColor = SkyReady,
            subtitle = "قيد المتابعة اليوم",
            modifier = Modifier.weight(1f).clickable(onClick = onOpenRepairsTab),
            testTag = "kpi_today_repairs",
          )
          KpiSummaryCard(
            title = "إجمالي اللي لينا",
            value = ReceiptGenerator.formatEgp(metrics.totalReceivablesEgp),
            icon = Icons.Default.AccountBalanceWallet,
            accentColor = MetallicGold,
            modifier = Modifier.weight(1f).clickable(onClick = onOpenDebtsTab),
            testTag = "kpi_receivables",
          )
        }

        KpiSummaryCard(
          title = "إجمالي اللي علينا (للموردين)",
          value = ReceiptGenerator.formatEgp(metrics.totalPayablesEgp),
          icon = Icons.Default.Payments,
          accentColor = CrimsonAlert,
          modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDebtsTab),
          testTag = "kpi_payables",
        )
      }
    }

    // Today's Repairs Quick Queue
    item {
      SectionHeaderRow(
        title = "صيانة اليوم السريعة",
        actionLabel = "كل الصيانة",
        onAction = onOpenRepairsTab,
      )
    }

    if (repairsState is UiState.Loading) {
      item {
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator(color = MetallicGold)
        }
      }
    } else if (todayRepairs.isEmpty()) {
      item {
        GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "لا توجد أجهزة مسجلة لصيانة اليوم حتى الآن.",
            style = MaterialTheme.typography.bodyMedium,
            color = WarmMuted,
          )
        }
      }
    } else {
      items(todayRepairs, key = { it.id }) { ticket ->
        GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${ticket.deviceModel} • ${ticket.customerName}",
                style = MaterialTheme.typography.titleSmall,
                color = IvoryText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${ticket.issueDescription} • الفني: ${ticket.assignedTechName}",
                style = MaterialTheme.typography.bodySmall,
                color = WarmMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            RepairStatusPill(status = ticket.statusEnum)
          }

          if (ticket.statusEnum != RepairStatus.DELIVERED) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              listOf(
                RepairStatus.PENDING,
                RepairStatus.IN_PROGRESS,
                RepairStatus.READY_FOR_PICKUP,
                RepairStatus.DELIVERED,
              ).forEach { st ->
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
                      .clickable {
                        if (!selected) onCycleRepairStatus(ticket, st)
                      },
                  color = if (selected) MetallicGold.copy(alpha = 0.2f) else ElevatedSurface,
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
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
        }
      }
    }

    // Live Team Activity Feed
    item {
      SectionHeaderRow(
        title = "سجل نشاط الفريق اللحظي",
        actionLabel = "عرض الكل",
        onAction = onOpenActivityTab,
      )
    }

    if (recentActivities.isEmpty()) {
      item {
        GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "سيظهر هنا كل تحديث أو عملية بيع أو صيانة يقوم بها أي من أعضاء الفريق الأربعة.",
            style = MaterialTheme.typography.bodyMedium,
            color = WarmMuted,
          )
        }
      }
    } else {
      items(recentActivities, key = { it.id }) { log ->
        ActivityLogRowCard(log = log)
      }
    }
  }
}

@Composable
fun SectionHeaderRow(
  title: String,
  actionLabel: String,
  onAction: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = BrightGold,
      fontWeight = FontWeight.Bold,
    )
    TextButton(onClick = onAction, modifier = Modifier.minimumInteractiveComponentSize()) {
      Text(text = actionLabel, color = MetallicGold, style = MaterialTheme.typography.labelLarge)
      Spacer(modifier = Modifier.width(4.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = MetallicGold,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

@Composable
fun ActivityLogRowCard(log: ActivityLogEntry) {
  val timeText =
    log.createdAt?.toDate()?.let {
      SimpleDateFormat("hh:mm a", Locale.US).format(it)
    } ?: "الآن"

  GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier =
            Modifier.size(36.dp)
              .clip(CircleShape)
              .background(MetallicGold.copy(alpha = 0.14f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = BrightGold,
            modifier = Modifier.size(18.dp),
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "${log.staffName} • ${log.actionTypeEnum.arabicLabel}",
            style = MaterialTheme.typography.labelLarge,
            color = BrightGold,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = log.summary,
            style = MaterialTheme.typography.bodySmall,
            color = IvoryText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column(horizontalAlignment = Alignment.End) {
        if (log.amountEgp > 0.0) {
          Text(
            text = ReceiptGenerator.formatEgp(log.amountEgp),
            style = MaterialTheme.typography.labelLarge,
            color = EmeraldProfit,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(text = timeText, style = MaterialTheme.typography.labelSmall, color = WarmMuted)
      }
    }
  }
}
