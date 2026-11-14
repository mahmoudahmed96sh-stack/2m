package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.unwrapFirestoreException
import com.example.data.model.ActivityLogEntry
import com.example.data.model.DebtDirection
import com.example.data.model.DebtPayment
import com.example.data.model.DebtRecord
import com.example.data.model.DebtStatus
import com.example.data.model.ExpenseEntry
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.RepairStatus
import com.example.data.model.RepairTicket
import com.example.data.model.SaleLineItem
import com.example.data.model.SaleTransaction
import com.example.data.model.StaffMember
import com.example.data.repository.StoreRepository
import com.example.receipt.ReceiptPayload
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

data class DashboardMetrics(
  val todaySalesEgp: Double = 0.0,
  val todayNetProfitEgp: Double = 0.0,
  val monthlySalesEgp: Double = 0.0,
  val monthlyNetProfitEgp: Double = 0.0,
  val todayRepairsActiveCount: Int = 0,
  val totalReceivablesEgp: Double = 0.0,
  val totalPayablesEgp: Double = 0.0,
  val todayExpensesEgp: Double = 0.0,
  val monthlyExpensesEgp: Double = 0.0,
)

class StoreMainViewModel(
  private val repository: StoreRepository,
  private val storeId: String,
) : ViewModel() {

  val productsState: StateFlow<UiState<List<Product>>> =
    repository.observeProducts(storeId).toUiStateFlow("products")

  val salesState: StateFlow<UiState<List<SaleTransaction>>> =
    repository.observeSales(storeId).toUiStateFlow("sales")

  val repairsState: StateFlow<UiState<List<RepairTicket>>> =
    repository.observeRepairs(storeId).toUiStateFlow("repairs")

  val debtsState: StateFlow<UiState<List<DebtRecord>>> =
    repository.observeDebts(storeId).toUiStateFlow("debts")

  val debtPaymentsState: StateFlow<UiState<List<DebtPayment>>> =
    repository.observeDebtPayments(storeId).toUiStateFlow("debt_payments")

  val expensesState: StateFlow<UiState<List<ExpenseEntry>>> =
    repository.observeExpenses(storeId).toUiStateFlow("expenses")

  val activityLogsState: StateFlow<UiState<List<ActivityLogEntry>>> =
    repository.observeActivityLogs(storeId).toUiStateFlow("activity_logs")

  val dashboardMetrics: StateFlow<DashboardMetrics> =
    combine(salesState, repairsState, debtsState, expensesState) { salesUi, repairsUi, debtsUi, expensesUi ->
      val sales = (salesUi as? UiState.Success)?.data ?: emptyList()
      val repairs = (repairsUi as? UiState.Success)?.data ?: emptyList()
      val debts = (debtsUi as? UiState.Success)?.data ?: emptyList()
      val expenses = (expensesUi as? UiState.Success)?.data ?: emptyList()

      val todayKey = todayDateKey()
      val monthKey = currentMonthKey()

      val todaySales = sales.filter { dateKeyOf(it.createdAt?.toDate()) == todayKey }
      val monthSales = sales.filter { monthKeyOf(it.createdAt?.toDate()) == monthKey }
      val todayExpenses = expenses.filter { dateKeyOf(it.createdAt?.toDate()) == todayKey }
      val monthExpenses = expenses.filter { monthKeyOf(it.createdAt?.toDate()) == monthKey }

      val todaySalesTotal = todaySales.sumOf { it.totalAmountEgp }
      val todayGrossProfit = todaySales.sumOf { it.netProfitEgp }
      val todayExpTotal = todayExpenses.sumOf { it.amountEgp }

      val monthSalesTotal = monthSales.sumOf { it.totalAmountEgp }
      val monthGrossProfit = monthSales.sumOf { it.netProfitEgp }
      val monthExpTotal = monthExpenses.sumOf { it.amountEgp }

      val todayActiveRepairs =
        repairs.count {
          it.scheduledDateKey == todayKey && it.statusEnum != RepairStatus.DELIVERED
        }

      val receivables =
        debts
          .filter {
            it.directionEnum == DebtDirection.RECEIVABLE && it.statusEnum != DebtStatus.SETTLED
          }
          .sumOf { it.remainingAmountEgp }

      val payables =
        debts
          .filter {
            it.directionEnum == DebtDirection.PAYABLE && it.statusEnum != DebtStatus.SETTLED
          }
          .sumOf { it.remainingAmountEgp }

      DashboardMetrics(
        todaySalesEgp = todaySalesTotal,
        todayNetProfitEgp = todayGrossProfit - todayExpTotal,
        monthlySalesEgp = monthSalesTotal,
        monthlyNetProfitEgp = monthGrossProfit - monthExpTotal,
        todayRepairsActiveCount = todayActiveRepairs,
        totalReceivablesEgp = receivables,
        totalPayablesEgp = payables,
        todayExpensesEgp = todayExpTotal,
        monthlyExpensesEgp = monthExpTotal,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = DashboardMetrics(),
    )

  private val _cartItems = MutableStateFlow<List<SaleLineItem>>(emptyList())
  val cartItems: StateFlow<List<SaleLineItem>> = _cartItems.asStateFlow()

  private val _activeReceipt = MutableStateFlow<ReceiptPayload?>(null)
  val activeReceipt: StateFlow<ReceiptPayload?> = _activeReceipt.asStateFlow()

  private val _feedbackMessage = MutableStateFlow<String?>(null)
  val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

  private val _isSubmitting = MutableStateFlow(false)
  val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

  fun clearFeedback() {
    _feedbackMessage.value = null
  }

  fun showReceipt(payload: ReceiptPayload) {
    _activeReceipt.value = payload
  }

  fun dismissReceipt() {
    _activeReceipt.value = null
  }

  fun addCatalogProductToCart(product: Product) {
    if (product.stockQty <= 0L) {
      _feedbackMessage.value = "نفذت كمية ${product.name} من المخزون"
      return
    }
    val current = _cartItems.value.toMutableList()
    val index = current.indexOfFirst { !it.isCustom && it.productId == product.id }
    if (index >= 0) {
      val existing = current[index]
      if (existing.qty >= product.stockQty) {
        _feedbackMessage.value = "الحد الأقصى المتاح في المخزون هو ${product.stockQty}"
        return
      }
      current[index] = existing.copy(qty = existing.qty + 1L)
    } else {
      current.add(
        SaleLineItem(
          productId = product.id,
          name = product.name,
          qty = 1L,
          unitSellEgp = product.sellingPriceEgp,
          unitCostEgp = product.costPriceEgp,
          isCustom = false,
        )
      )
    }
    _cartItems.value = current
  }

  fun addCustomItemToCart(name: String, qty: Long, sellPrice: Double, costPrice: Double) {
    if (name.isBlank() || qty <= 0L || sellPrice < 0.0 || costPrice < 0.0) {
      _feedbackMessage.value = "يرجى إدخال بيانات الصنف بشكل صحيح"
      return
    }
    val current = _cartItems.value.toMutableList()
    current.add(
      SaleLineItem(
        productId = "custom_${System.currentTimeMillis()}",
        name = name.trim(),
        qty = qty,
        unitSellEgp = sellPrice,
        unitCostEgp = costPrice,
        isCustom = true,
      )
    )
    _cartItems.value = current
  }

  fun updateCartItemQty(index: Int, delta: Long, catalogMap: Map<String, Product>) {
    val current = _cartItems.value.toMutableList()
    if (index !in current.indices) return
    val item = current[index]
    val newQty = item.qty + delta
    if (newQty <= 0L) {
      current.removeAt(index)
    } else {
      if (!item.isCustom) {
        val maxStock = catalogMap[item.productId]?.stockQty ?: Long.MAX_VALUE
        if (newQty > maxStock) {
          _feedbackMessage.value = "الكمية المتاحة في المخزون: $maxStock"
          return
        }
      }
      current[index] = item.copy(qty = newQty)
    }
    _cartItems.value = current
  }

  fun clearCart() {
    _cartItems.value = emptyList()
  }

  fun submitSaleCheckout(
    memberUids: List<String>,
    paymentMethod: PaymentMethod,
    paidAmountEgp: Double,
    customerName: String,
    customerPhone: String,
    actor: StaffMember,
  ) {
    val items = _cartItems.value
    if (items.isEmpty()) {
      _feedbackMessage.value = "السلة فارغة، أضف أصنافاً أولاً"
      return
    }
    val catalogList = (productsState.value as? UiState.Success)?.data ?: emptyList()
    val catalogMap = catalogList.associateBy { it.id }

    viewModelScope.launch {
      _isSubmitting.value = true
      val res =
        repository.recordSale(
          storeId = storeId,
          memberUids = memberUids,
          lineItems = items,
          catalogProducts = catalogMap,
          paymentMethod = paymentMethod,
          paidAmountEgp = paidAmountEgp,
          customerName = customerName,
          customerPhone = customerPhone,
          actor = actor,
        )
      _isSubmitting.value = false
      res
        .onSuccess { sale ->
          _cartItems.value = emptyList()
          _activeReceipt.value = ReceiptPayload.SaleReceipt(sale)
          _feedbackMessage.value = "تم تسجيل الفاتورة ${sale.invoiceNumber} بنجاح"
        }
        .onFailure { err ->
          _feedbackMessage.value = err.message ?: "تعذر حفظ الفاتورة"
        }
    }
  }

  fun upsertProduct(
    memberUids: List<String>,
    existing: Product?,
    name: String,
    category: String,
    sellingPrice: Double,
    costPrice: Double,
    stockQty: Long,
    lowStockThreshold: Long,
    actor: StaffMember,
  ) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val res =
        repository.upsertProduct(
          storeId = storeId,
          memberUids = memberUids,
          existingProduct = existing,
          name = name,
          category = category,
          sellingPriceEgp = sellingPrice,
          costPriceEgp = costPrice,
          stockQty = stockQty,
          lowStockThreshold = lowStockThreshold,
          actor = actor,
        )
      _isSubmitting.value = false
      res
        .onSuccess { _feedbackMessage.value = "تم حفظ الصنف في المخزون" }
        .onFailure { _feedbackMessage.value = it.message ?: "فشل حفظ الصنف" }
    }
  }

  fun deleteProduct(memberUids: List<String>, product: Product, actor: StaffMember) {
    viewModelScope.launch {
      repository
        .deleteProduct(storeId, memberUids, product, actor)
        .onSuccess { _feedbackMessage.value = "تم حذف الصنف" }
        .onFailure { _feedbackMessage.value = it.message ?: "تعذر حذف الصنف" }
    }
  }

  fun createRepairTicket(
    memberUids: List<String>,
    customerName: String,
    customerPhone: String,
    deviceModel: String,
    issueDescription: String,
    estimatedCostEgp: Double,
    partsCostEgp: Double,
    paidAmountEgp: Double,
    assignedTech: StaffMember,
    scheduledDateKey: String,
    actor: StaffMember,
  ) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val res =
        repository.createRepairTicket(
          storeId = storeId,
          memberUids = memberUids,
          customerName = customerName,
          customerPhone = customerPhone,
          deviceModel = deviceModel,
          issueDescription = issueDescription,
          estimatedCostEgp = estimatedCostEgp,
          partsCostEgp = partsCostEgp,
          paidAmountEgp = paidAmountEgp,
          assignedTech = assignedTech,
          scheduledDateKey = scheduledDateKey,
          actor = actor,
        )
      _isSubmitting.value = false
      res
        .onSuccess { ticket ->
          _activeReceipt.value = ReceiptPayload.RepairReceipt(ticket)
          _feedbackMessage.value = "تم تسجيل تذكرة الصيانة ${ticket.ticketNumber}"
        }
        .onFailure { _feedbackMessage.value = it.message ?: "فشل تسجيل الصيانة" }
    }
  }

  fun updateRepairStatus(
    memberUids: List<String>,
    ticket: RepairTicket,
    newStatus: RepairStatus,
    actor: StaffMember,
  ) {
    viewModelScope.launch {
      repository
        .updateRepairStatus(storeId, memberUids, ticket, newStatus, actor)
        .onSuccess { _feedbackMessage.value = "تم تحديث حالة ${ticket.deviceModel} إلى ${newStatus.arabicLabel}" }
        .onFailure { _feedbackMessage.value = it.message ?: "تعذر تحديث الحالة" }
    }
  }

  fun createDebtRecord(
    memberUids: List<String>,
    direction: DebtDirection,
    partyName: String,
    phone: String,
    description: String,
    amountEgp: Double,
    dueDaysFromNow: Int,
    actor: StaffMember,
  ) {
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dueDaysFromNow) }
    viewModelScope.launch {
      _isSubmitting.value = true
      val res =
        repository.createDebtRecord(
          storeId = storeId,
          memberUids = memberUids,
          direction = direction,
          partyName = partyName,
          phone = phone,
          description = description,
          amountEgp = amountEgp,
          dueDateMillis = cal.timeInMillis,
          actor = actor,
        )
      _isSubmitting.value = false
      res
        .onSuccess { _feedbackMessage.value = "تم تسجيل الحساب في الدفتر" }
        .onFailure { _feedbackMessage.value = it.message ?: "فشل تسجيل الحساب" }
    }
  }

  fun settleDebtInstallment(
    memberUids: List<String>,
    debt: DebtRecord,
    installmentAmountEgp: Double,
    note: String,
    actor: StaffMember,
  ) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val res =
        repository.settleDebtInstallment(
          storeId = storeId,
          memberUids = memberUids,
          debt = debt,
          installmentAmountEgp = installmentAmountEgp,
          note = note,
          actor = actor,
        )
      _isSubmitting.value = false
      res
        .onSuccess { _feedbackMessage.value = "تم تسجيل السداد وتحديث الحساب بنجاح" }
        .onFailure { _feedbackMessage.value = it.message ?: "فشل تسجيل السداد" }
    }
  }

  fun addExpense(
    memberUids: List<String>,
    title: String,
    category: String,
    amountEgp: Double,
    actor: StaffMember,
  ) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val res = repository.addExpense(storeId, memberUids, title, category, amountEgp, actor)
      _isSubmitting.value = false
      res
        .onSuccess { _feedbackMessage.value = "تم تسجيل المصروف" }
        .onFailure { _feedbackMessage.value = it.message ?: "فشل تسجيل المصروف" }
    }
  }

  fun updateStaffMember(
    memberUids: List<String>,
    staff: StaffMember,
    newName: String,
    newPin: String?,
    isActive: Boolean,
    actor: StaffMember,
  ) {
    viewModelScope.launch {
      _isSubmitting.value = true
      val res =
        repository.updateStaffMember(
          storeId = storeId,
          memberUids = memberUids,
          staff = staff,
          newName = newName,
          newPin = newPin,
          isActive = isActive,
          actor = actor,
        )
      _isSubmitting.value = false
      res
        .onSuccess { _feedbackMessage.value = "تم تحديث بيانات الموظف" }
        .onFailure { _feedbackMessage.value = it.message ?: "تعذر تحديث الموظف" }
    }
  }

  private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
    this.map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val fsError = error.unwrapFirestoreException()
        if (fsError == null && error is CancellationException) throw error
        val root = fsError ?: error
        Log.w(TAG, "Error observing $label", root)
        emit(UiState.Error(root.message ?: "تعذر تحميل $label"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading,
      )

  companion object {
    private const val TAG = "StoreMainVM"

    fun todayDateKey(): String =
      SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun currentMonthKey(): String =
      SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

    private fun dateKeyOf(date: Date?): String {
      if (date == null) return todayDateKey()
      return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
    }

    private fun monthKeyOf(date: Date?): String {
      if (date == null) return currentMonthKey()
      return SimpleDateFormat("yyyy-MM", Locale.US).format(date)
    }
  }
}
