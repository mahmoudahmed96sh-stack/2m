package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.PinHasher
import com.example.data.firebase.unwrapFirestoreException
import com.example.data.model.StaffMember
import com.example.data.model.StaffRole
import com.example.data.model.StoreWorkspace
import com.example.data.repository.StaffSetupInput
import com.example.data.repository.StoreRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WorkspaceGateState {
  data object Loading : WorkspaceGateState

  data object NeedsStoreSetup : WorkspaceGateState

  data class Ready(
    val store: StoreWorkspace,
    val staffList: List<StaffMember>,
  ) : WorkspaceGateState

  data class Error(val message: String) : WorkspaceGateState
}

@OptIn(ExperimentalCoroutinesApi::class)
class StoreSessionViewModel(
  private val repository: StoreRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _activeStaff = MutableStateFlow<StaffMember?>(null)
  val activeStaff: StateFlow<StaffMember?> = _activeStaff.asStateFlow()

  private val _actionError = MutableStateFlow<String?>(null)
  val actionError: StateFlow<String?> = _actionError.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private var lastSyncedMembersHash: Int = 0

  val gateState: StateFlow<WorkspaceGateState> =
    repository
      .observeUserWorkspaceLink(currentUserId)
      .flatMapLatest { link ->
        val storeId = link?.activeStoreId
        if (storeId.isNullOrBlank()) {
          flowOf<WorkspaceGateState>(WorkspaceGateState.NeedsStoreSetup)
        } else {
          repository.observeStoreWorkspace(storeId).flatMapLatest { store ->
            if (store == null) {
              flowOf<WorkspaceGateState>(WorkspaceGateState.NeedsStoreSetup)
            } else {
              // Sync memberUids across subcollections whenever store membership grows
              val membersHash = store.memberUids.sorted().hashCode()
              if (store.memberUids.size > 1 && membersHash != lastSyncedMembersHash) {
                lastSyncedMembersHash = membersHash
                viewModelScope.launch {
                  repository.syncMemberUidsAcrossStore(store.id, store.memberUids)
                }
              }
              repository.observeStaffMembers(store.id).map { staff ->
                // Refresh active staff reference if already unlocked
                val currentActive = _activeStaff.value
                if (currentActive != null) {
                  val refreshed = staff.firstOrNull { it.id == currentActive.id }
                  if (refreshed != null && refreshed.isActive) {
                    _activeStaff.value = refreshed
                  } else {
                    _activeStaff.value = null
                  }
                }
                WorkspaceGateState.Ready(store, staff)
              }
            }
          }
        }
      }
      .catch { error ->
        val fsErr = error.unwrapFirestoreException()
        if (fsErr == null && error is CancellationException) throw error
        val root = fsErr ?: error
        Log.w(TAG, "Error observing store session", root)
        emit(WorkspaceGateState.Error(root.message ?: "تعذر تحميل بيانات المتجر"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = WorkspaceGateState.Loading,
      )

  fun clearError() {
    _actionError.value = null
  }

  fun createStoreWorkspace(
    storeName: String,
    ownerName: String,
    ownerPin: String,
    staff1Name: String,
    staff1Pin: String,
    staff2Name: String,
    staff2Pin: String,
    staff3Name: String,
    staff3Pin: String,
  ) {
    if (ownerPin.length != 4 || !ownerPin.all { it.isDigit() }) {
      _actionError.value = "الرقم السري للمالك يجب أن يتكون من 4 أرقام"
      return
    }
    val pins = listOf(ownerPin, staff1Pin, staff2Pin, staff3Pin)
    if (pins.any { it.length != 4 || !it.all { ch -> ch.isDigit() } }) {
      _actionError.value = "جميع الأرقام السرية يجب أن تتكون من 4 أرقام"
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val inputs =
        listOf(
          StaffSetupInput(0, ownerName.ifBlank { "المالك" }, StaffRole.ADMIN, ownerPin),
          StaffSetupInput(1, staff1Name.ifBlank { "موظف 1" }, StaffRole.STAFF, staff1Pin),
          StaffSetupInput(2, staff2Name.ifBlank { "موظف 2" }, StaffRole.STAFF, staff2Pin),
          StaffSetupInput(3, staff3Name.ifBlank { "موظف 3" }, StaffRole.STAFF, staff3Pin),
        )
      val result = repository.createStoreWorkspace(storeName, inputs)
      _isBusy.value = false
      result.onFailure { err ->
        _actionError.value = err.message ?: "فشل إنشاء المتجر"
      }
    }
  }

  fun joinStoreByCode(joinCode: String) {
    if (joinCode.isBlank()) {
      _actionError.value = "يرجى إدخال كود المتجر"
      return
    }
    viewModelScope.launch {
      _isBusy.value = true
      _actionError.value = null
      val result = repository.joinStoreByCode(joinCode)
      _isBusy.value = false
      result.onFailure { err ->
        _actionError.value = err.message ?: "تعذر الانضمام للمتجر بهذا الكود"
      }
    }
  }

  fun unlockStaff(staff: StaffMember, pin: String): Boolean {
    if (!staff.isActive) {
      _actionError.value = "هذا الحساب معطّل حالياً"
      return false
    }
    return if (PinHasher.verifyPin(pin, staff.pinHash)) {
      _activeStaff.value = staff
      _actionError.value = null
      true
    } else {
      _actionError.value = "الرقم السري غير صحيح"
      false
    }
  }

  fun lockStaffSession() {
    _activeStaff.value = null
    _actionError.value = null
  }

  private companion object {
    const val TAG = "StoreSessionVM"
  }
}
