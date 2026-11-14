package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.PinHasher
import com.example.data.firebase.handleFirestoreError
import com.example.data.firebase.unwrapFirestoreException
import com.example.data.model.ActivityActionType
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
import com.example.data.model.StaffRole
import com.example.data.model.StoreInvite
import com.example.data.model.StoreWorkspace
import com.example.data.model.UserWorkspaceLink
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class StaffSetupInput(
  val slotIndex: Int,
  val name: String,
  val role: StaffRole,
  val pin: String,
)

class StoreRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    FirebaseAuth.getInstance(),
  )

  private fun requireUid(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً")

  private fun storeDoc(storeId: String) = db.collection("stores").document(storeId)

  fun observeUserWorkspaceLink(uid: String): Flow<UserWorkspaceLink?> =
    db.collection("users")
      .document(uid)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(
            UserWorkspaceLink::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      }
      .catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        if (auth.currentUser?.uid == uid) {
          handleFirestoreError(firestoreError ?: error, OperationType.GET, "users/$uid")
        }
        throw error
      }

  fun observeStoreWorkspace(storeId: String): Flow<StoreWorkspace?> =
    storeDoc(storeId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(
            StoreWorkspace::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      }
      .catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        if (auth.currentUser != null) {
          handleFirestoreError(firestoreError ?: error, OperationType.GET, "stores/$storeId")
        }
        throw error
      }

  fun observeStaffMembers(storeId: String): Flow<List<StaffMember>> =
    observeMemberScopedCollection(
      storeId = storeId,
      subcollection = "staff",
      clazz = StaffMember::class.java,
    ).map { list -> list.sortedBy { it.slotIndex } }

  fun observeProducts(storeId: String): Flow<List<Product>> =
    observeMemberScopedCollection(
      storeId = storeId,
      subcollection = "products",
      clazz = Product::class.java,
    ).map { list -> list.sortedBy { it.name } }

  fun observeSales(storeId: String): Flow<List<SaleTransaction>> =
    observeMemberScopedCollection(
      storeId = storeId,
      subcollection = "sales",
      clazz = SaleTransaction::class.java,
    ).map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }

  fun observeRepairs(storeId: String): Flow<List<RepairTicket>> =
    observeMemberScopedCollection(
      storeId = storeId,
      subcollection = "repairs",
      clazz = RepairTicket::class.java,
    ).map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }

  fun observeDebts(storeId: String): Flow<List<DebtRecord>> =
    observeMemberScopedCollection(
      storeId = storeId,
      subcollection = "debts",
      clazz = DebtRecord::class.java,
    ).map { list -> list.sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L } }

  fun observeDebtPayments(storeId: String): Flow<List<DebtPayment>> =
    observeMemberScopedCollection(
      storeId = storeId,
      subcollection = "debt_payments",
      clazz = DebtPayment::class.java,
    ).map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }

  fun observeExpenses(storeId: String): Flow<List<ExpenseEntry>> =
    observeMemberScopedCollection(
      storeId = storeId,
      subcollection = "expenses",
      clazz = ExpenseEntry::class.java,
    ).map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }

  fun observeActivityLogs(storeId: String): Flow<List<ActivityLogEntry>> =
    observeMemberScopedCollection(
      storeId = storeId,
      subcollection = "activity_logs",
      clazz = ActivityLogEntry::class.java,
    ).map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L }.take(120) }

  private fun <T : Any> observeMemberScopedCollection(
    storeId: String,
    subcollection: String,
    clazz: Class<T>,
  ): Flow<List<T>> = flow {
    val uid = requireUid()
    val path = "stores/$storeId/$subcollection"
    emitAll(
      storeDoc(storeId)
        .collection(subcollection)
        .whereArrayContains("memberUids", uid)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = error.unwrapFirestoreException()
          if (firestoreError == null && error is CancellationException) throw error
          if (auth.currentUser?.uid == uid) {
            handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  suspend fun createStoreWorkspace(
    storeName: String,
    staffInputs: List<StaffSetupInput>,
  ): Result<String> {
    val uid = requireUid()
    val currentUser = auth.currentUser
    val storeId = "store_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val joinCode = "2M${UUID.randomUUID().toString().replace("-", "").uppercase().take(4)}"
    val memberUids = listOf(uid)

    return try {
      val batch = db.batch()
      val storePayload =
        mapOf(
          "storeName" to storeName.trim().ifBlank { "2m Store" },
          "ownerUid" to uid,
          "joinCode" to joinCode,
          "memberUids" to memberUids,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      batch.set(storeDoc(storeId), storePayload)

      val invitePayload =
        mapOf(
          "storeId" to storeId,
          "storeName" to storeName.trim().ifBlank { "2m Store" },
          "ownerUid" to uid,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      batch.set(db.collection("store_invites").document(joinCode), invitePayload)

      val userPayload =
        mapOf(
          "userId" to uid,
          "email" to (currentUser?.email ?: ""),
          "displayName" to (currentUser?.displayName ?: ""),
          "activeStoreId" to storeId,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      batch.set(db.collection("users").document(uid), userPayload)

      staffInputs.take(4).forEach { staff ->
        val staffId = "staff_${staff.slotIndex}"
        val staffPayload =
          mapOf(
            "storeId" to storeId,
            "memberUids" to memberUids,
            "name" to staff.name.trim(),
            "role" to staff.role.value,
            "pinHash" to PinHasher.hashPin(staff.pin),
            "isActive" to true,
            "slotIndex" to staff.slotIndex,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        batch.set(storeDoc(storeId).collection("staff").document(staffId), staffPayload)
      }

      batch.commit().await()
      Result.success(storeId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "stores/$storeId")
      Result.failure(e)
    }
  }

  suspend fun joinStoreByCode(joinCodeRaw: String): Result<String> {
    val uid = requireUid()
    val currentUser = auth.currentUser
    val code = joinCodeRaw.trim().uppercase()
    if (code.isBlank()) {
      return Result.failure(IllegalArgumentException("يرجى إدخال كود المتجر"))
    }
    return try {
      val inviteSnap = db.collection("store_invites").document(code).get().await()
      val invite =
        inviteSnap.toObject(StoreInvite::class.java)
          ?: return Result.failure(IllegalArgumentException("كود المتجر غير صحيح"))
      val storeId = invite.storeId

      storeDoc(storeId)
        .update(
          mapOf(
            "memberUids" to FieldValue.arrayUnion(uid),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      val userDoc = db.collection("users").document(uid)
      val existingUser = userDoc.get().await()
      if (existingUser.exists()) {
        userDoc
          .update(
            mapOf(
              "activeStoreId" to storeId,
              "email" to (currentUser?.email ?: ""),
              "displayName" to (currentUser?.displayName ?: ""),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        userDoc
          .set(
            mapOf(
              "userId" to uid,
              "email" to (currentUser?.email ?: ""),
              "displayName" to (currentUser?.displayName ?: ""),
              "activeStoreId" to storeId,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(storeId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "store_invites/$code")
      Result.failure(e)
    }
  }

  /**
   * Keeps subcollection documents readable across newly joined store devices
   * when an existing member device detects new UIDs in StoreWorkspace.memberUids.
   */
  suspend fun syncMemberUidsAcrossStore(storeId: String, memberUids: List<String>) {
    requireUid()
    if (memberUids.isEmpty()) return
    val subcollections =
      listOf("staff", "products", "sales", "repairs", "debts", "debt_payments", "expenses", "activity_logs")
    try {
      val uid = requireUid()
      var batch = db.batch()
      var ops = 0
      for (col in subcollections) {
        val docs =
          storeDoc(storeId)
            .collection(col)
            .whereArrayContains("memberUids", uid)
            .get()
            .await()
        for (doc in docs.documents) {
          val existingMembers =
            (doc.get("memberUids") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
          if (!existingMembers.containsAll(memberUids)) {
            batch.update(doc.reference, mapOf("memberUids" to memberUids))
            ops++
            if (ops >= 400) {
              batch.commit().await()
              batch = db.batch()
              ops = 0
            }
          }
        }
      }
      if (ops > 0) {
        batch.commit().await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, "stores/$storeId")
    }
  }

  suspend fun upsertProduct(
    storeId: String,
    memberUids: List<String>,
    existingProduct: Product?,
    name: String,
    category: String,
    sellingPriceEgp: Double,
    costPriceEgp: Double,
    stockQty: Long,
    lowStockThreshold: Long,
    actor: StaffMember,
  ): Result<String> {
    requireUid()
    val productId =
      existingProduct?.id?.takeIf { it.isNotBlank() }
        ?: "prod_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val docRef = storeDoc(storeId).collection("products").document(productId)

    return try {
      val batch = db.batch()
      if (existingProduct == null) {
        batch.set(
          docRef,
          mapOf(
            "storeId" to storeId,
            "memberUids" to memberUids,
            "name" to name.trim(),
            "category" to category.trim(),
            "sellingPriceEgp" to sellingPriceEgp,
            "costPriceEgp" to costPriceEgp,
            "stockQty" to stockQty,
            "lowStockThreshold" to lowStockThreshold,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      } else {
        batch.update(
          docRef,
          mapOf(
            "name" to name.trim(),
            "category" to category.trim(),
            "sellingPriceEgp" to sellingPriceEgp,
            "costPriceEgp" to costPriceEgp,
            "stockQty" to stockQty,
            "lowStockThreshold" to lowStockThreshold,
            "memberUids" to memberUids,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
      appendActivityLog(
        batch = batch,
        storeId = storeId,
        memberUids = memberUids,
        actor = actor,
        actionType = ActivityActionType.PRODUCT_EDIT,
        summary = "${if (existingProduct == null) "إضافة صنف" else "تحديث صنف"}: ${name.trim()} (المخزون: $stockQty)",
        amountEgp = sellingPriceEgp,
      )
      batch.commit().await()
      Result.success(productId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteProduct(
    storeId: String,
    memberUids: List<String>,
    product: Product,
    actor: StaffMember,
  ): Result<Unit> {
    requireUid()
    val docRef = storeDoc(storeId).collection("products").document(product.id)
    return try {
      val batch = db.batch()
      batch.delete(docRef)
      appendActivityLog(
        batch = batch,
        storeId = storeId,
        memberUids = memberUids,
        actor = actor,
        actionType = ActivityActionType.PRODUCT_EDIT,
        summary = "حذف صنف من الكتالوج: ${product.name}",
        amountEgp = 0.0,
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun recordSale(
    storeId: String,
    memberUids: List<String>,
    lineItems: List<SaleLineItem>,
    catalogProducts: Map<String, Product>,
    paymentMethod: PaymentMethod,
    paidAmountEgp: Double,
    customerName: String,
    customerPhone: String,
    actor: StaffMember,
  ): Result<SaleTransaction> {
    requireUid()
    val saleId = "sale_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val invoiceNumber =
      "INV-${SimpleDateFormat("yyMMdd-HHmm", Locale.US).format(Date())}-${(10..99).random()}"
    val totalAmount = lineItems.sumOf { it.lineTotalEgp }
    val totalCost = lineItems.sumOf { it.lineCostEgp }
    val netProfit = totalAmount - totalCost
    val itemsSummary =
      lineItems.joinToString("، ") { "${it.name} ×${it.qty}" }.take(480)
    val clampedPaid = paidAmountEgp.coerceIn(0.0, totalAmount)
    val remainingDebt = (totalAmount - clampedPaid).coerceAtLeast(0.0)

    val saleRef = storeDoc(storeId).collection("sales").document(saleId)

    return try {
      val batch = db.batch()
      val salePayload =
        mapOf(
          "storeId" to storeId,
          "memberUids" to memberUids,
          "invoiceNumber" to invoiceNumber,
          "itemsSummary" to itemsSummary,
          "items" to lineItems.map { it.toMap() },
          "totalAmountEgp" to totalAmount,
          "totalCostEgp" to totalCost,
          "netProfitEgp" to netProfit,
          "paymentMethod" to paymentMethod.value,
          "paidAmountEgp" to clampedPaid,
          "customerName" to customerName.trim(),
          "customerPhone" to customerPhone.trim(),
          "staffId" to actor.id,
          "staffName" to actor.name,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      batch.set(saleRef, salePayload)

      // Decrement catalog stock safely without going negative
      lineItems
        .filter { !it.isCustom && it.productId.isNotBlank() }
        .groupBy { it.productId }
        .forEach { (productId, grouped) ->
          val totalQty = grouped.sumOf { it.qty }
          val product = catalogProducts[productId]
          if (product != null && product.stockQty > 0L) {
            val decrement = minOf(totalQty, product.stockQty)
            if (decrement > 0L) {
              batch.update(
                storeDoc(storeId).collection("products").document(productId),
                mapOf(
                  "stockQty" to FieldValue.increment(-decrement),
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
              )
            }
          }
        }

      // If remaining balance > 0, automatically log Receivable ("لينا")
      if (remainingDebt > 0.01) {
        val debtId = "debt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val debtRef = storeDoc(storeId).collection("debts").document(debtId)
        val status = if (clampedPaid > 0.0) DebtStatus.PARTIAL else DebtStatus.OPEN
        val dueMillis = System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L
        batch.set(
          debtRef,
          mapOf(
            "storeId" to storeId,
            "memberUids" to memberUids,
            "direction" to DebtDirection.RECEIVABLE.value,
            "partyName" to customerName.trim().ifBlank { "عميل فاتورة $invoiceNumber" },
            "phone" to customerPhone.trim(),
            "description" to "متبقي فاتورة مبيعات $invoiceNumber ($itemsSummary)".take(280),
            "originalAmountEgp" to remainingDebt,
            "remainingAmountEgp" to remainingDebt,
            "dueDateMillis" to dueMillis,
            "status" to status.value,
            "sourceSaleId" to saleId,
            "createdByStaffName" to actor.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }

      appendActivityLog(
        batch = batch,
        storeId = storeId,
        memberUids = memberUids,
        actor = actor,
        actionType = ActivityActionType.SALE,
        summary = "فاتورة $invoiceNumber: $itemsSummary",
        amountEgp = totalAmount,
      )

      batch.commit().await()

      Result.success(
        SaleTransaction(
          id = saleId,
          storeId = storeId,
          memberUids = memberUids,
          invoiceNumber = invoiceNumber,
          itemsSummary = itemsSummary,
          items = lineItems,
          totalAmountEgp = totalAmount,
          totalCostEgp = totalCost,
          netProfitEgp = netProfit,
          paymentMethod = paymentMethod.value,
          paidAmountEgp = clampedPaid,
          customerName = customerName.trim(),
          customerPhone = customerPhone.trim(),
          staffId = actor.id,
          staffName = actor.name,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, saleRef.path)
      Result.failure(e)
    }
  }

  suspend fun createRepairTicket(
    storeId: String,
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
  ): Result<RepairTicket> {
    requireUid()
    val repairId = "rep_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val ticketNumber =
      "REP-${SimpleDateFormat("MMdd", Locale.US).format(Date())}-${(100..999).random()}"
    val docRef = storeDoc(storeId).collection("repairs").document(repairId)

    return try {
      val batch = db.batch()
      batch.set(
        docRef,
        mapOf(
          "storeId" to storeId,
          "memberUids" to memberUids,
          "ticketNumber" to ticketNumber,
          "customerName" to customerName.trim(),
          "customerPhone" to customerPhone.trim(),
          "deviceModel" to deviceModel.trim(),
          "issueDescription" to issueDescription.trim(),
          "estimatedCostEgp" to estimatedCostEgp,
          "partsCostEgp" to partsCostEgp,
          "paidAmountEgp" to paidAmountEgp,
          "assignedTechId" to assignedTech.id,
          "assignedTechName" to assignedTech.name,
          "status" to RepairStatus.PENDING.value,
          "scheduledDateKey" to scheduledDateKey,
          "createdByStaffName" to actor.name,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      appendActivityLog(
        batch = batch,
        storeId = storeId,
        memberUids = memberUids,
        actor = actor,
        actionType = ActivityActionType.REPAIR_CREATE,
        summary = "استلام صيانة $ticketNumber: ${deviceModel.trim()} للعميل ${customerName.trim()}",
        amountEgp = estimatedCostEgp,
      )
      batch.commit().await()
      Result.success(
        RepairTicket(
          id = repairId,
          storeId = storeId,
          memberUids = memberUids,
          ticketNumber = ticketNumber,
          customerName = customerName.trim(),
          customerPhone = customerPhone.trim(),
          deviceModel = deviceModel.trim(),
          issueDescription = issueDescription.trim(),
          estimatedCostEgp = estimatedCostEgp,
          partsCostEgp = partsCostEgp,
          paidAmountEgp = paidAmountEgp,
          assignedTechId = assignedTech.id,
          assignedTechName = assignedTech.name,
          status = RepairStatus.PENDING.value,
          scheduledDateKey = scheduledDateKey,
          createdByStaffName = actor.name,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateRepairStatus(
    storeId: String,
    memberUids: List<String>,
    ticket: RepairTicket,
    newStatus: RepairStatus,
    actor: StaffMember,
  ): Result<Unit> {
    requireUid()
    val docRef = storeDoc(storeId).collection("repairs").document(ticket.id)
    return try {
      val batch = db.batch()
      batch.update(
        docRef,
        mapOf(
          "status" to newStatus.value,
          "memberUids" to memberUids,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      appendActivityLog(
        batch = batch,
        storeId = storeId,
        memberUids = memberUids,
        actor = actor,
        actionType = ActivityActionType.REPAIR_STATUS,
        summary = "تحديث صيانة ${ticket.deviceModel} (${ticket.customerName}) إلى: ${newStatus.arabicLabel}",
        amountEgp = ticket.estimatedCostEgp,
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun createDebtRecord(
    storeId: String,
    memberUids: List<String>,
    direction: DebtDirection,
    partyName: String,
    phone: String,
    description: String,
    amountEgp: Double,
    dueDateMillis: Long,
    actor: StaffMember,
  ): Result<String> {
    requireUid()
    val debtId = "debt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val docRef = storeDoc(storeId).collection("debts").document(debtId)

    return try {
      val batch = db.batch()
      batch.set(
        docRef,
        mapOf(
          "storeId" to storeId,
          "memberUids" to memberUids,
          "direction" to direction.value,
          "partyName" to partyName.trim(),
          "phone" to phone.trim(),
          "description" to description.trim(),
          "originalAmountEgp" to amountEgp,
          "remainingAmountEgp" to amountEgp,
          "dueDateMillis" to dueDateMillis,
          "status" to DebtStatus.OPEN.value,
          "createdByStaffName" to actor.name,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      val prefix = if (direction == DebtDirection.RECEIVABLE) "لينا عند" else "علينا لـ"
      appendActivityLog(
        batch = batch,
        storeId = storeId,
        memberUids = memberUids,
        actor = actor,
        actionType = ActivityActionType.DEBT_ADD,
        summary = "تسجيل حساب ($prefix ${partyName.trim()})",
        amountEgp = amountEgp,
      )
      batch.commit().await()
      Result.success(debtId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun settleDebtInstallment(
    storeId: String,
    memberUids: List<String>,
    debt: DebtRecord,
    installmentAmountEgp: Double,
    note: String,
    actor: StaffMember,
  ): Result<Unit> {
    requireUid()
    val paid = installmentAmountEgp.coerceIn(0.0, debt.remainingAmountEgp)
    if (paid <= 0.0) {
      return Result.failure(IllegalArgumentException("يرجى إدخال مبلغ صحيح"))
    }
    val newRemaining = (debt.remainingAmountEgp - paid).coerceAtLeast(0.0)
    val newStatus = if (newRemaining <= 0.01) DebtStatus.SETTLED else DebtStatus.PARTIAL
    val finalRemaining = if (newStatus == DebtStatus.SETTLED) 0.0 else newRemaining

    val debtRef = storeDoc(storeId).collection("debts").document(debt.id)
    val paymentId = "pay_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val paymentRef = storeDoc(storeId).collection("debt_payments").document(paymentId)

    return try {
      val batch = db.batch()
      batch.update(
        debtRef,
        mapOf(
          "remainingAmountEgp" to finalRemaining,
          "status" to newStatus.value,
          "memberUids" to memberUids,
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        paymentRef,
        mapOf(
          "storeId" to storeId,
          "memberUids" to memberUids,
          "debtId" to debt.id,
          "direction" to debt.direction,
          "partyName" to debt.partyName,
          "amountEgp" to paid,
          "staffId" to actor.id,
          "staffName" to actor.name,
          "note" to note.trim(),
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      val verb = if (debt.directionEnum == DebtDirection.RECEIVABLE) "تحصيل من" else "سداد إلى"
      appendActivityLog(
        batch = batch,
        storeId = storeId,
        memberUids = memberUids,
        actor = actor,
        actionType = ActivityActionType.DEBT_PAYMENT,
        summary = "$verb ${debt.partyName} (${if (newStatus == DebtStatus.SETTLED) "خالص" else "دفعة"})",
        amountEgp = paid,
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, debtRef.path)
      Result.failure(e)
    }
  }

  suspend fun addExpense(
    storeId: String,
    memberUids: List<String>,
    title: String,
    category: String,
    amountEgp: Double,
    actor: StaffMember,
  ): Result<String> {
    requireUid()
    val expenseId = "exp_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val docRef = storeDoc(storeId).collection("expenses").document(expenseId)
    return try {
      val batch = db.batch()
      batch.set(
        docRef,
        mapOf(
          "storeId" to storeId,
          "memberUids" to memberUids,
          "title" to title.trim(),
          "category" to category.trim(),
          "amountEgp" to amountEgp,
          "staffId" to actor.id,
          "staffName" to actor.name,
          "createdAt" to FieldValue.serverTimestamp(),
        ),
      )
      appendActivityLog(
        batch = batch,
        storeId = storeId,
        memberUids = memberUids,
        actor = actor,
        actionType = ActivityActionType.EXPENSE_ADD,
        summary = "مصروف: ${title.trim()}",
        amountEgp = amountEgp,
      )
      batch.commit().await()
      Result.success(expenseId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateStaffMember(
    storeId: String,
    memberUids: List<String>,
    staff: StaffMember,
    newName: String,
    newPin: String?,
    isActive: Boolean,
    actor: StaffMember,
  ): Result<Unit> {
    requireUid()
    val docRef = storeDoc(storeId).collection("staff").document(staff.id)
    return try {
      val updates =
        mutableMapOf<String, Any>(
          "name" to newName.trim(),
          "isActive" to isActive,
          "memberUids" to memberUids,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (!newPin.isNullOrBlank() && newPin.length == 4) {
        updates["pinHash"] = PinHasher.hashPin(newPin)
      }
      val batch = db.batch()
      batch.set(docRef, updates, SetOptions.merge())
      appendActivityLog(
        batch = batch,
        storeId = storeId,
        memberUids = memberUids,
        actor = actor,
        actionType = ActivityActionType.STAFF_UPDATE,
        summary = "تحديث بيانات الموظف: ${newName.trim()}",
        amountEgp = 0.0,
      )
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  private fun appendActivityLog(
    batch: com.google.firebase.firestore.WriteBatch,
    storeId: String,
    memberUids: List<String>,
    actor: StaffMember,
    actionType: ActivityActionType,
    summary: String,
    amountEgp: Double,
  ) {
    val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val logRef = storeDoc(storeId).collection("activity_logs").document(logId)
    batch.set(
      logRef,
      mapOf(
        "storeId" to storeId,
        "memberUids" to memberUids,
        "staffId" to actor.id,
        "staffName" to actor.name,
        "staffRole" to actor.role,
        "actionType" to actionType.value,
        "summary" to summary.take(290),
        "amountEgp" to amountEgp,
        "createdAt" to FieldValue.serverTimestamp(),
      ),
    )
  }
}
