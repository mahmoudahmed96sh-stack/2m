package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class StaffRole(val value: String, val arabicLabel: String) {
  ADMIN("ADMIN", "مدير (صلاحية كاملة)"),
  STAFF("STAFF", "موظف"),
  ;

  companion object {
    fun fromValue(raw: String?): StaffRole =
      entries.firstOrNull { it.value == raw } ?: STAFF
  }
}

enum class PaymentMethod(val value: String, val arabicLabel: String) {
  CASH("CASH", "نقدي"),
  TRANSFER("TRANSFER", "تحويل / إنستاباي"),
  CREDIT("CREDIT", "آجل / جزئي"),
  ;

  companion object {
    fun fromValue(raw: String?): PaymentMethod =
      entries.firstOrNull { it.value == raw } ?: CASH
  }
}

enum class RepairStatus(val value: String, val arabicLabel: String) {
  PENDING("PENDING", "قيد الانتظار"),
  IN_PROGRESS("IN_PROGRESS", "جاري العمل"),
  READY_FOR_PICKUP("READY_FOR_PICKUP", "جاهز للاستلام"),
  DELIVERED("DELIVERED", "تم التسليم"),
  ;

  fun nextStatus(): RepairStatus =
    when (this) {
      PENDING -> IN_PROGRESS
      IN_PROGRESS -> READY_FOR_PICKUP
      READY_FOR_PICKUP -> DELIVERED
      DELIVERED -> DELIVERED
    }

  companion object {
    fun fromValue(raw: String?): RepairStatus =
      entries.firstOrNull { it.value == raw } ?: PENDING
  }
}

enum class DebtDirection(val value: String, val arabicLabel: String) {
  RECEIVABLE("RECEIVABLE", "لينا (على العملاء)"),
  PAYABLE("PAYABLE", "علينا (للموردين)"),
  ;

  companion object {
    fun fromValue(raw: String?): DebtDirection =
      entries.firstOrNull { it.value == raw } ?: RECEIVABLE
  }
}

enum class DebtStatus(val value: String, val arabicLabel: String) {
  OPEN("OPEN", "مفتوح"),
  PARTIAL("PARTIAL", "سداد جزئي"),
  SETTLED("SETTLED", "خالص"),
  ;

  companion object {
    fun fromValue(raw: String?): DebtStatus =
      entries.firstOrNull { it.value == raw } ?: OPEN
  }
}

enum class ActivityActionType(val value: String, val arabicLabel: String) {
  SALE("SALE", "عملية بيع"),
  REPAIR_CREATE("REPAIR_CREATE", "استلام جهاز صيانة"),
  REPAIR_STATUS("REPAIR_STATUS", "تحديث حالة صيانة"),
  DEBT_ADD("DEBT_ADD", "تسجيل دين"),
  DEBT_PAYMENT("DEBT_PAYMENT", "تحصيل / سداد دفعة"),
  PRODUCT_EDIT("PRODUCT_EDIT", "تحديث المخزون"),
  EXPENSE_ADD("EXPENSE_ADD", "تسجيل مصروف"),
  STAFF_UPDATE("STAFF_UPDATE", "تحديث فريق العمل"),
  ;

  companion object {
    fun fromValue(raw: String?): ActivityActionType =
      entries.firstOrNull { it.value == raw } ?: SALE
  }
}

@IgnoreExtraProperties
data class UserWorkspaceLink(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String? = null,
  val displayName: String? = null,
  val activeStoreId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class StoreInvite(
  @DocumentId val id: String = "",
  val storeId: String = "",
  val storeName: String = "",
  val ownerUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class StoreWorkspace(
  @DocumentId val id: String = "",
  val storeName: String = "2m Store",
  val ownerUid: String = "",
  val joinCode: String = "",
  val memberUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class StaffMember(
  @DocumentId val id: String = "",
  val storeId: String = "",
  val memberUids: List<String> = emptyList(),
  val name: String = "",
  val role: String = StaffRole.STAFF.value,
  val pinHash: String = "",
  val isActive: Boolean = true,
  val slotIndex: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val roleEnum: StaffRole
    get() = StaffRole.fromValue(role)

  val isAdmin: Boolean
    get() = roleEnum == StaffRole.ADMIN
}

@IgnoreExtraProperties
data class Product(
  @DocumentId val id: String = "",
  val storeId: String = "",
  val memberUids: List<String> = emptyList(),
  val name: String = "",
  val category: String = "",
  val sellingPriceEgp: Double = 0.0,
  val costPriceEgp: Double = 0.0,
  val stockQty: Long = 0L,
  val lowStockThreshold: Long = 3L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val isLowStock: Boolean
    get() = stockQty <= lowStockThreshold
}

@IgnoreExtraProperties
data class SaleLineItem(
  val productId: String = "",
  val name: String = "",
  val qty: Long = 1L,
  val unitSellEgp: Double = 0.0,
  val unitCostEgp: Double = 0.0,
  val isCustom: Boolean = false,
) {
  val lineTotalEgp: Double
    get() = unitSellEgp * qty

  val lineCostEgp: Double
    get() = unitCostEgp * qty

  val lineProfitEgp: Double
    get() = lineTotalEgp - lineCostEgp

  fun toMap(): Map<String, Any> =
    mapOf(
      "productId" to productId,
      "name" to name,
      "qty" to qty,
      "unitSellEgp" to unitSellEgp,
      "unitCostEgp" to unitCostEgp,
      "isCustom" to isCustom,
    )
}

@IgnoreExtraProperties
data class SaleTransaction(
  @DocumentId val id: String = "",
  val storeId: String = "",
  val memberUids: List<String> = emptyList(),
  val invoiceNumber: String = "",
  val itemsSummary: String = "",
  val items: List<SaleLineItem> = emptyList(),
  val totalAmountEgp: Double = 0.0,
  val totalCostEgp: Double = 0.0,
  val netProfitEgp: Double = 0.0,
  val paymentMethod: String = PaymentMethod.CASH.value,
  val paidAmountEgp: Double = 0.0,
  val customerName: String = "",
  val customerPhone: String = "",
  val staffId: String = "",
  val staffName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  val paymentMethodEnum: PaymentMethod
    get() = PaymentMethod.fromValue(paymentMethod)

  val remainingDebtEgp: Double
    get() = (totalAmountEgp - paidAmountEgp).coerceAtLeast(0.0)
}

@IgnoreExtraProperties
data class RepairTicket(
  @DocumentId val id: String = "",
  val storeId: String = "",
  val memberUids: List<String> = emptyList(),
  val ticketNumber: String = "",
  val customerName: String = "",
  val customerPhone: String = "",
  val deviceModel: String = "",
  val issueDescription: String = "",
  val estimatedCostEgp: Double = 0.0,
  val partsCostEgp: Double = 0.0,
  val paidAmountEgp: Double = 0.0,
  val assignedTechId: String = "",
  val assignedTechName: String = "",
  val status: String = RepairStatus.PENDING.value,
  val scheduledDateKey: String = "",
  val createdByStaffName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val statusEnum: RepairStatus
    get() = RepairStatus.fromValue(status)

  val remainingAmountEgp: Double
    get() = (estimatedCostEgp - paidAmountEgp).coerceAtLeast(0.0)
}

@IgnoreExtraProperties
data class DebtRecord(
  @DocumentId val id: String = "",
  val storeId: String = "",
  val memberUids: List<String> = emptyList(),
  val direction: String = DebtDirection.RECEIVABLE.value,
  val partyName: String = "",
  val phone: String = "",
  val description: String = "",
  val originalAmountEgp: Double = 0.0,
  val remainingAmountEgp: Double = 0.0,
  val dueDateMillis: Long = 0L,
  val status: String = DebtStatus.OPEN.value,
  val sourceSaleId: String? = null,
  val createdByStaffName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val directionEnum: DebtDirection
    get() = DebtDirection.fromValue(direction)

  val statusEnum: DebtStatus
    get() = DebtStatus.fromValue(status)
}

@IgnoreExtraProperties
data class DebtPayment(
  @DocumentId val id: String = "",
  val storeId: String = "",
  val memberUids: List<String> = emptyList(),
  val debtId: String = "",
  val direction: String = DebtDirection.RECEIVABLE.value,
  val partyName: String = "",
  val amountEgp: Double = 0.0,
  val staffId: String = "",
  val staffName: String = "",
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ExpenseEntry(
  @DocumentId val id: String = "",
  val storeId: String = "",
  val memberUids: List<String> = emptyList(),
  val title: String = "",
  val category: String = "",
  val amountEgp: Double = 0.0,
  val staffId: String = "",
  val staffName: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ActivityLogEntry(
  @DocumentId val id: String = "",
  val storeId: String = "",
  val memberUids: List<String> = emptyList(),
  val staffId: String = "",
  val staffName: String = "",
  val staffRole: String = StaffRole.STAFF.value,
  val actionType: String = ActivityActionType.SALE.value,
  val summary: String = "",
  val amountEgp: Double = 0.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  val actionTypeEnum: ActivityActionType
    get() = ActivityActionType.fromValue(actionType)
}
