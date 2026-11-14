package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.StaffMember
import com.example.data.model.StoreWorkspace
import com.example.ui.components.ReceiptPreviewDialog
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.SubtleGoldBorder
import com.example.ui.theme.WarmMuted
import com.example.ui.viewmodel.StoreMainViewModel

enum class StoreTab(
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  DASHBOARD("الرئيسية", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
  POS("المبيعات", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale, "nav_pos"),
  REPAIRS("الصيانة", Icons.Filled.Build, Icons.Outlined.Build, "nav_repairs"),
  DEBTS("الدفتر", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_debts"),
  ADMIN("الإدارة", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "nav_admin"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoMStoreShell(
  store: StoreWorkspace,
  activeStaff: StaffMember,
  staffList: List<StaffMember>,
  mainViewModel: StoreMainViewModel,
  onSwitchStaffPin: () -> Unit,
  onSignOutGoogle: () -> Unit,
) {
  var currentTab by remember { mutableStateOf(StoreTab.DASHBOARD) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  val metrics by mainViewModel.dashboardMetrics.collectAsStateWithLifecycle()
  val productsState by mainViewModel.productsState.collectAsStateWithLifecycle()
  val salesState by mainViewModel.salesState.collectAsStateWithLifecycle()
  val repairsState by mainViewModel.repairsState.collectAsStateWithLifecycle()
  val debtsState by mainViewModel.debtsState.collectAsStateWithLifecycle()
  val debtPaymentsState by mainViewModel.debtPaymentsState.collectAsStateWithLifecycle()
  val expensesState by mainViewModel.expensesState.collectAsStateWithLifecycle()
  val activityState by mainViewModel.activityLogsState.collectAsStateWithLifecycle()
  val cartItems by mainViewModel.cartItems.collectAsStateWithLifecycle()
  val activeReceipt by mainViewModel.activeReceipt.collectAsStateWithLifecycle()
  val feedbackMsg by mainViewModel.feedbackMessage.collectAsStateWithLifecycle()
  val isSubmitting by mainViewModel.isSubmitting.collectAsStateWithLifecycle()

  LaunchedEffect(feedbackMsg) {
    val msg = feedbackMsg
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      mainViewModel.clearFeedback()
    }
  }

  BackHandler(enabled = currentTab != StoreTab.DASHBOARD) {
    currentTab = StoreTab.DASHBOARD
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MatteBlack)) {
    val useRail = maxWidth >= 600.dp

    Scaffold(
      containerColor = MatteBlack,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = {
        SnackbarHost(snackbarHostState) { data ->
          Snackbar(
            snackbarData = data,
            containerColor = ElevatedSurface,
            contentColor = BrightGold,
            shape = RoundedCornerShape(12.dp),
          )
        }
      },
      topBar = {
        TopAppBar(
          colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface),
          title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Image(
                painter = painterResource(id = R.drawable.img_store_logo),
                contentDescription = "شعار 2m Store",
                modifier =
                  Modifier.size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MetallicGold, RoundedCornerShape(10.dp)),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = store.storeName,
                  style = MaterialTheme.typography.titleMedium,
                  color = BrightGold,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = currentTab.label,
                  style = MaterialTheme.typography.labelSmall,
                  color = WarmMuted,
                )
              }
            }
          },
          actions = {
            Surface(
              modifier =
                Modifier.padding(end = 12.dp)
                  .minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(50))
                  .border(1.dp, SubtleGoldBorder, RoundedCornerShape(50))
                  .clickable(onClick = onSwitchStaffPin)
                  .testTag("topbar_staff_chip"),
              color = ElevatedSurface,
              shape = RoundedCornerShape(50),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier = Modifier.size(8.dp).clip(CircleShape).background(MetallicGold)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "${activeStaff.name} (${if (activeStaff.isAdmin) "مدير" else "موظف"})",
                  style = MaterialTheme.typography.labelMedium,
                  color = IvoryText,
                  fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.SwapHoriz,
                  contentDescription = "تبديل الموظف",
                  tint = BrightGold,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          },
        )
      },
      bottomBar = {
        if (!useRail) {
          NavigationBar(containerColor = CardSurface, tonalElevation = 0.dp) {
            StoreTab.entries.forEach { tab ->
              val selected = currentTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { currentTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag(tab.testTag),
                colors =
                  NavigationBarItemDefaults.colors(
                    selectedIconColor = MatteBlack,
                    selectedTextColor = BrightGold,
                    indicatorColor = MetallicGold,
                    unselectedIconColor = WarmMuted,
                    unselectedTextColor = WarmMuted,
                  ),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useRail) {
          NavigationRail(
            containerColor = CardSurface,
            modifier = Modifier.fillMaxHeight(),
          ) {
            StoreTab.entries.forEach { tab ->
              val selected = currentTab == tab
              NavigationRailItem(
                selected = selected,
                onClick = { currentTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                  )
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag(tab.testTag),
                colors =
                  NavigationRailItemDefaults.colors(
                    selectedIconColor = MatteBlack,
                    selectedTextColor = BrightGold,
                    indicatorColor = MetallicGold,
                    unselectedIconColor = WarmMuted,
                    unselectedTextColor = WarmMuted,
                  ),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          when (currentTab) {
            StoreTab.DASHBOARD ->
              DashboardScreen(
                activeStaff = activeStaff,
                metrics = metrics,
                repairsState = repairsState,
                activityState = activityState,
                onCycleRepairStatus = { ticket, newStatus ->
                  mainViewModel.updateRepairStatus(store.memberUids, ticket, newStatus, activeStaff)
                },
                onOpenRepairsTab = { currentTab = StoreTab.REPAIRS },
                onOpenPosTab = { currentTab = StoreTab.POS },
                onOpenDebtsTab = { currentTab = StoreTab.DEBTS },
                onOpenActivityTab = { currentTab = StoreTab.ADMIN },
              )

            StoreTab.POS ->
              PosAndSalesScreen(
                activeStaff = activeStaff,
                metrics = metrics,
                productsState = productsState,
                salesState = salesState,
                cartItems = cartItems,
                isSubmitting = isSubmitting,
                onAddCatalogProduct = { mainViewModel.addCatalogProductToCart(it) },
                onAddCustomItem = { name, qty, sell, cost ->
                  mainViewModel.addCustomItemToCart(name, qty, sell, cost)
                },
                onUpdateCartQty = { idx, delta, map ->
                  mainViewModel.updateCartItemQty(idx, delta, map)
                },
                onClearCart = { mainViewModel.clearCart() },
                onSubmitCheckout = { method, paid, cName, cPhone ->
                  mainViewModel.submitSaleCheckout(
                    memberUids = store.memberUids,
                    paymentMethod = method,
                    paidAmountEgp = paid,
                    customerName = cName,
                    customerPhone = cPhone,
                    actor = activeStaff,
                  )
                },
                onUpsertProduct = { existing, name, cat, sell, cost, qty, thresh ->
                  mainViewModel.upsertProduct(
                    memberUids = store.memberUids,
                    existing = existing,
                    name = name,
                    category = cat,
                    sellingPrice = sell,
                    costPrice = cost,
                    stockQty = qty,
                    lowStockThreshold = thresh,
                    actor = activeStaff,
                  )
                },
                onOpenReceipt = { mainViewModel.showReceipt(it) },
              )

            StoreTab.REPAIRS ->
              RepairsScreen(
                activeStaff = activeStaff,
                staffList = staffList,
                repairsState = repairsState,
                isSubmitting = isSubmitting,
                onCreateTicket = { cName, cPhone, model, issue, est, parts, paid, tech, dateKey ->
                  mainViewModel.createRepairTicket(
                    memberUids = store.memberUids,
                    customerName = cName,
                    customerPhone = cPhone,
                    deviceModel = model,
                    issueDescription = issue,
                    estimatedCostEgp = est,
                    partsCostEgp = parts,
                    paidAmountEgp = paid,
                    assignedTech = tech,
                    scheduledDateKey = dateKey,
                    actor = activeStaff,
                  )
                },
                onUpdateStatus = { ticket, newStatus ->
                  mainViewModel.updateRepairStatus(store.memberUids, ticket, newStatus, activeStaff)
                },
                onOpenReceipt = { mainViewModel.showReceipt(it) },
              )

            StoreTab.DEBTS ->
              DebtsScreen(
                debtsState = debtsState,
                paymentsState = debtPaymentsState,
                isSubmitting = isSubmitting,
                onCreateDebt = { dir, name, phone, desc, amt, days ->
                  mainViewModel.createDebtRecord(
                    memberUids = store.memberUids,
                    direction = dir,
                    partyName = name,
                    phone = phone,
                    description = desc,
                    amountEgp = amt,
                    dueDaysFromNow = days,
                    actor = activeStaff,
                  )
                },
                onSettleInstallment = { debt, amount, note ->
                  mainViewModel.settleDebtInstallment(
                    memberUids = store.memberUids,
                    debt = debt,
                    installmentAmountEgp = amount,
                    note = note,
                    actor = activeStaff,
                  )
                },
              )

            StoreTab.ADMIN ->
              AdminTeamScreen(
                store = store,
                activeStaff = activeStaff,
                staffList = staffList,
                productsState = productsState,
                expensesState = expensesState,
                activityState = activityState,
                onUpsertProduct = { existing, name, cat, sell, cost, qty, thresh ->
                  mainViewModel.upsertProduct(
                    memberUids = store.memberUids,
                    existing = existing,
                    name = name,
                    category = cat,
                    sellingPrice = sell,
                    costPrice = cost,
                    stockQty = qty,
                    lowStockThreshold = thresh,
                    actor = activeStaff,
                  )
                },
                onDeleteProduct = { prod ->
                  mainViewModel.deleteProduct(store.memberUids, prod, activeStaff)
                },
                onAddExpense = { title, cat, amount ->
                  mainViewModel.addExpense(store.memberUids, title, cat, amount, activeStaff)
                },
                onUpdateStaffMember = { staff, newName, newPin, active ->
                  mainViewModel.updateStaffMember(
                    memberUids = store.memberUids,
                    staff = staff,
                    newName = newName,
                    newPin = newPin,
                    isActive = active,
                    actor = activeStaff,
                  )
                },
                onSwitchStaffPin = onSwitchStaffPin,
                onSignOutGoogle = onSignOutGoogle,
                onShowToast = { msg ->
                  scope.launch { snackbarHostState.showSnackbar(msg) }
                },
              )
          }
        }
      }
    }
  }

  activeReceipt?.let { payload ->
    ReceiptPreviewDialog(
      payload = payload,
      onDismiss = { mainViewModel.dismissReceipt() },
    )
  }
}
