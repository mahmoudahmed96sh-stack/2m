package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.DebtDirection
import com.example.data.model.PaymentMethod
import com.example.data.model.RepairStatus
import com.example.data.model.SaleLineItem
import com.example.data.model.StaffMember
import com.example.data.model.StaffRole
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private fun defaultStaffInputs(): List<StaffSetupInput> =
    listOf(
      StaffSetupInput(0, "المالك", StaffRole.ADMIN, "1111"),
      StaffSetupInput(1, "موظف 1", StaffRole.STAFF, "2222"),
      StaffSetupInput(2, "موظف 2", StaffRole.STAFF, "3333"),
      StaffSetupInput(3, "موظف 3", StaffRole.STAFF, "4444"),
    )

  private fun actorAdmin(storeId: String, memberUids: List<String>) =
    StaffMember(
      id = "staff_0",
      storeId = storeId,
      memberUids = memberUids,
      name = "المالك",
      role = StaffRole.ADMIN.value,
      pinHash = "0123456789abcdef0123456789abcdef",
      isActive = true,
      slotIndex = 0L,
    )

  @Test
  fun createStoreWorkspace_and_recordSale_and_repair_and_settleDebt_succeeds() = runBlocking {
    val ownerEmail = "owner_${UUID.randomUUID().toString().take(6)}@2mstore.test"
    val ownerUid = signInTestUser(ownerEmail)
    val repo = StoreRepository(firestore, auth)

    val storeId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createStoreWorkspace("2m Store", defaultStaffInputs()).getOrThrow()
      }
    assertTrue(storeId.isNotBlank())

    val memberUids = listOf(ownerUid)
    val actor = actorAdmin(storeId, memberUids)

    val productId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.upsertProduct(
            storeId = storeId,
            memberUids = memberUids,
            existingProduct = null,
            name = "شاشة آيفون 15 برو",
            category = "شاشات",
            sellingPriceEgp = 4500.0,
            costPriceEgp = 3100.0,
            stockQty = 5L,
            lowStockThreshold = 2L,
            actor = actor,
          )
          .getOrThrow()
      }

    val products =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeProducts(storeId).first { list -> list.any { it.id == productId } }
      }
    val savedProduct = products.first { it.id == productId }
    assertEquals(5L, savedProduct.stockQty)

    // Record partial-credit sale (creates receivable debt automatically & decrements stock)
    val saleResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.recordSale(
            storeId = storeId,
            memberUids = memberUids,
            lineItems =
              listOf(
                SaleLineItem(
                  productId = productId,
                  name = savedProduct.name,
                  qty = 1L,
                  unitSellEgp = 4500.0,
                  unitCostEgp = 3100.0,
                  isCustom = false,
                )
              ),
            catalogProducts = mapOf(productId to savedProduct),
            paymentMethod = PaymentMethod.CREDIT,
            paidAmountEgp = 2000.0,
            customerName = "أحمد علي",
            customerPhone = "01001234567",
            actor = actor,
          )
          .getOrThrow()
      }
    assertEquals(4500.0, saleResult.totalAmountEgp, 0.01)

    val updatedProducts =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeProducts(storeId).first { list ->
          list.firstOrNull { it.id == productId }?.stockQty == 4L
        }
      }
    assertEquals(4L, updatedProducts.first { it.id == productId }.stockQty)

    // Verify automatic receivable debt created and settle installment
    val debts =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeDebts(storeId).first { list -> list.isNotEmpty() }
      }
    val debt = debts.first()
    assertEquals(DebtDirection.RECEIVABLE.value, debt.direction)
    assertEquals(2500.0, debt.remainingAmountEgp, 0.01)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.settleDebtInstallment(
          storeId = storeId,
          memberUids = memberUids,
          debt = debt,
          installmentAmountEgp = 1000.0,
          note = "دفعة أولى",
          actor = actor,
        )
        .getOrThrow()
    }

    val updatedDebt =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeDebts(storeId).first { list ->
          list.firstOrNull { it.id == debt.id }?.remainingAmountEgp == 1500.0
        }
      }
    assertEquals(1500.0, updatedDebt.first { it.id == debt.id }.remainingAmountEgp, 0.01)

    // Create repair ticket and advance status
    val ticket =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createRepairTicket(
            storeId = storeId,
            memberUids = memberUids,
            customerName = "محمود حسن",
            customerPhone = "01112223334",
            deviceModel = "Samsung S24 Ultra",
            issueDescription = "تغيير باغة شاشة",
            estimatedCostEgp = 2200.0,
            partsCostEgp = 1200.0,
            paidAmountEgp = 500.0,
            assignedTech = actor,
            scheduledDateKey = "2026-11-14",
            actor = actor,
          )
          .getOrThrow()
      }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.updateRepairStatus(
          storeId = storeId,
          memberUids = memberUids,
          ticket = ticket,
          newStatus = RepairStatus.READY_FOR_PICKUP,
          actor = actor,
        )
        .getOrThrow()
    }

    val updatedRepairs =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeRepairs(storeId).first { list ->
          list.firstOrNull { it.id == ticket.id }?.status == RepairStatus.READY_FOR_PICKUP.value
        }
      }
    assertEquals(RepairStatus.READY_FOR_PICKUP.value, updatedRepairs.first { it.id == ticket.id }.status)
  }

  @Test
  fun crossUserAccess_nonMember_failsWithPermissionDenied() = runBlocking {
    val ownerEmail = "owner_${UUID.randomUUID().toString().take(6)}@2mstore.test"
    val outsiderEmail = "outsider_${UUID.randomUUID().toString().take(6)}@2mstore.test"
    val ownerUid = signInTestUser(ownerEmail)
    val repo = StoreRepository(firestore, auth)

    val storeId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createStoreWorkspace("2m Store", defaultStaffInputs()).getOrThrow()
      }
    val actor = actorAdmin(storeId, listOf(ownerUid))
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.upsertProduct(
          storeId = storeId,
          memberUids = listOf(ownerUid),
          existingProduct = null,
          name = "بطارية أصلية",
          category = "بطاريات",
          sellingPriceEgp = 900.0,
          costPriceEgp = 550.0,
          stockQty = 3L,
          lowStockThreshold = 1L,
          actor = actor,
        )
        .getOrThrow()
    }

    // Switch to outsider Google account
    signInTestUser(outsiderEmail)
    val outsiderRepo = StoreRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { outsiderRepo.observeStoreWorkspace(storeId).first() }
      fail("Expected PERMISSION_DENIED when reading another store workspace")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
  }

  @Test
  fun observeProducts_unauthenticatedUser_failsWithIllegalStateOrPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = StoreRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeProducts("any_store").first() }
      fail("Expected unauthenticated access to fail")
    } catch (e: Exception) {
      assertTrue(
        e is IllegalStateException ||
          e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
      )
    }
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
