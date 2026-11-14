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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.SaleLineItem
import com.example.data.model.SaleTransaction
import com.example.data.model.StaffMember
import com.example.receipt.ReceiptGenerator
import com.example.receipt.ReceiptPayload
import com.example.ui.components.GoldBorderCard
import com.example.ui.components.GoldOutlinedButton
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.theme.AmberProgress
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.SubtleGoldBorder
import com.example.ui.theme.WarmMuted
import com.example.ui.viewmodel.DashboardMetrics
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PosAndSalesScreen(
  activeStaff: StaffMember,
  metrics: DashboardMetrics,
  productsState: UiState<List<Product>>,
  salesState: UiState<List<SaleTransaction>>,
  cartItems: List<SaleLineItem>,
  isSubmitting: Boolean,
  onAddCatalogProduct: (Product) -> Unit,
  onAddCustomItem: (String, Long, Double, Double) -> Unit,
  onUpdateCartQty: (Int, Long, Map<String, Product>) -> Unit,
  onClearCart: () -> Unit,
  onSubmitCheckout: (PaymentMethod, Double, String, String) -> Unit,
  onUpsertProduct: (Product?, String, String, Double, Double, Long, Long) -> Unit,
  onOpenReceipt: (ReceiptPayload) -> Unit,
) {
  var selectedSubTab by remember { mutableIntStateOf(0) }
  var showCustomItemDialog by remember { mutableStateOf(false) }
  var showCheckoutDialog by remember { mutableStateOf(false) }
  var showAddProductDialog by remember { mutableStateOf(false) }

  val products = (productsState as? UiState.Success)?.data ?: emptyList()
  val sales = (salesState as? UiState.Success)?.data ?: emptyList()
  val catalogMap = remember(products) { products.associateBy { it.id } }

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
      Tab(
        selected = selectedSubTab == 0,
        onClick = { selectedSubTab = 0 },
        text = { Text("نقطة البيع (POS)") },
        modifier = Modifier.testTag("pos_tab_register"),
      )
      Tab(
        selected = selectedSubTab == 1,
        onClick = { selectedSubTab = 1 },
        text = { Text("سجل المبيعات والأرباح") },
        modifier = Modifier.testTag("pos_tab_history"),
      )
    }

    if (selectedSubTab == 0) {
      var searchQuery by remember { mutableStateOf("") }
      var selectedCategory by remember { mutableStateOf("الكل") }
      val categories =
        remember(products) {
          listOf("الكل") + products.map { it.category.ifBlank { "عام" } }.distinct()
        }
      val filteredProducts =
        remember(products, searchQuery, selectedCategory) {
          products.filter { prod ->
            val matchCat =
              selectedCategory == "الكل" || prod.category.ifBlank { "عام" } == selectedCategory
            val matchQuery =
              searchQuery.isBlank() || prod.name.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
          }
        }

      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              GoldOutlinedButton(
                text = "+ صنف يدوي",
                onClick = { showCustomItemDialog = true },
                modifier = Modifier.weight(1f),
                testTag = "pos_custom_item_btn",
              )
              GoldOutlinedButton(
                text = "+ للكتالوج",
                icon = Icons.Default.Inventory2,
                onClick = { showAddProductDialog = true },
                modifier = Modifier.weight(1f),
                testTag = "pos_add_catalog_btn",
              )
            }
          }

          item {
            LuxuryTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              label = "ابحث عن منتج أو إكسسوار...",
            )
          }

          if (categories.size > 1) {
            item {
              LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                  FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) },
                    colors =
                      FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MetallicGold,
                        selectedLabelColor = MatteBlack,
                        containerColor = CardSurface,
                        labelColor = IvoryText,
                      ),
                  )
                }
              }
            }
          }

          if (filteredProducts.isEmpty()) {
            item {
              GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MetallicGold,
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text =
                      if (products.isEmpty()) "لا توجد أصناف في الكتالوج بعد. أضف أصنافاً للكتالوج أو استخدم البيع اليدوي السريع."
                      else "لا توجد نتائج مطابقة للبحث.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmMuted,
                  )
                }
              }
            }
          } else {
            items(filteredProducts, key = { it.id }) { prod ->
              GoldBorderCard(
                modifier = Modifier.fillMaxWidth().testTag("pos_product_${prod.id}"),
                onClick = { onAddCatalogProduct(prod) },
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = prod.name,
                      style = MaterialTheme.typography.titleMedium,
                      color = IvoryText,
                      fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                      Text(
                        text = prod.category.ifBlank { "عام" },
                        style = MaterialTheme.typography.labelMedium,
                        color = WarmMuted,
                      )
                      val stockColor =
                        when {
                          prod.stockQty <= 0L -> CrimsonAlert
                          prod.isLowStock -> AmberProgress
                          else -> EmeraldProfit
                        }
                      Text(
                        text =
                          if (prod.stockQty <= 0L) "نفذت الكمية"
                          else "المخزون: ${prod.stockQty}",
                        style = MaterialTheme.typography.labelMedium,
                        color = stockColor,
                        fontWeight = FontWeight.SemiBold,
                      )
                    }
                    if (activeStaff.isAdmin) {
                      Text(
                        text = "التكلفة: ${ReceiptGenerator.formatEgp(prod.costPriceEgp)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmMuted,
                      )
                    }
                  }

                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = ReceiptGenerator.formatEgp(prod.sellingPriceEgp),
                      style = MaterialTheme.typography.titleMedium,
                      color = BrightGold,
                      fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                      color = MetallicGold.copy(alpha = 0.16f),
                      shape = RoundedCornerShape(8.dp),
                      modifier =
                        Modifier.border(1.dp, SubtleGoldBorder, RoundedCornerShape(8.dp)),
                    ) {
                      Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        Icon(
                          imageVector = Icons.Default.AddShoppingCart,
                          contentDescription = "إضافة للسلة",
                          tint = BrightGold,
                          modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("أضف", style = MaterialTheme.typography.labelMedium, color = BrightGold)
                      }
                    }
                  }
                }
              }
            }
          }

          if (cartItems.isNotEmpty()) {
            item {
              Text(
                text = "سلة المبيعات الحالية (${cartItems.sumOf { it.qty }} قطعة)",
                style = MaterialTheme.typography.titleMedium,
                color = BrightGold,
                fontWeight = FontWeight.Bold,
              )
            }
            itemsIndexed(cartItems) { index, item ->
              GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = item.name,
                      style = MaterialTheme.typography.titleSmall,
                      color = IvoryText,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text =
                        "${ReceiptGenerator.formatEgp(item.unitSellEgp)} × ${item.qty} = ${ReceiptGenerator.formatEgp(item.lineTotalEgp)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = BrightGold,
                    )
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = { onUpdateCartQty(index, -1L, catalogMap) },
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Icon(
                        imageVector =
                          if (item.qty <= 1L) Icons.Default.DeleteOutline else Icons.Default.Remove,
                        contentDescription = "إنقاص",
                        tint = if (item.qty <= 1L) CrimsonAlert else IvoryText,
                      )
                    }
                    Text(
                      text = "${item.qty}",
                      style = MaterialTheme.typography.titleMedium,
                      color = IvoryText,
                      fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                      onClick = { onUpdateCartQty(index, 1L, catalogMap) },
                      modifier = Modifier.minimumInteractiveComponentSize(),
                    ) {
                      Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "زيادة",
                        tint = BrightGold,
                      )
                    }
                  }
                }
              }
            }
          }
          item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Docked Checkout Bar when cart has items
        if (cartItems.isNotEmpty()) {
          val cartTotal = cartItems.sumOf { it.lineTotalEgp }
          val cartProfit = cartItems.sumOf { it.lineProfitEgp }
          Surface(
            modifier =
              Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.2.dp, MetallicGold, RoundedCornerShape(16.dp)),
            color = ElevatedSurface,
            shape = RoundedCornerShape(16.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "الإجمالي: ${ReceiptGenerator.formatEgp(cartTotal)}",
                  style = MaterialTheme.typography.titleMedium,
                  color = BrightGold,
                  fontWeight = FontWeight.Bold,
                )
                if (activeStaff.isAdmin) {
                  Text(
                    text = "صافي ربح الفاتورة: ${ReceiptGenerator.formatEgp(cartProfit)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldProfit,
                  )
                }
              }
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClearCart) {
                  Text("تفريغ", color = CrimsonAlert)
                }
                GoldPrimaryButton(
                  text = "إتمام البيع",
                  icon = Icons.Default.ShoppingCartCheckout,
                  onClick = { showCheckoutDialog = true },
                  testTag = "pos_open_checkout_btn",
                )
              }
            }
          }
        }
      }
    } else {
      // Sales & Profit History Sub-Tab
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "ملخص الشهر الحالي",
              style = MaterialTheme.typography.titleMedium,
              color = BrightGold,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text("إيراد الشهر", style = MaterialTheme.typography.labelMedium, color = WarmMuted)
                Text(
                  ReceiptGenerator.formatEgp(metrics.monthlySalesEgp),
                  style = MaterialTheme.typography.titleLarge,
                  color = IvoryText,
                  fontWeight = FontWeight.Bold,
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text("صافي ربح الشهر", style = MaterialTheme.typography.labelMedium, color = WarmMuted)
                Text(
                  if (activeStaff.isAdmin) ReceiptGenerator.formatEgp(metrics.monthlyNetProfitEgp)
                  else "للمدير فقط",
                  style = MaterialTheme.typography.titleLarge,
                  color = if (activeStaff.isAdmin) EmeraldProfit else WarmMuted,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
            if (activeStaff.isAdmin && metrics.monthlyExpensesEgp > 0) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "مصاريف الشهر المخصومة: ${ReceiptGenerator.formatEgp(metrics.monthlyExpensesEgp)}",
                style = MaterialTheme.typography.labelSmall,
                color = AmberProgress,
              )
            }
          }
        }

        if (sales.isEmpty()) {
          item {
            GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
              Text("لا توجد فواتير مبيعات مسجلة حتى الآن.", color = WarmMuted)
            }
          }
        } else {
          items(sales, key = { it.id }) { sale ->
            val timeStr =
              sale.createdAt?.toDate()?.let {
                SimpleDateFormat("MM/dd hh:mm a", Locale.US).format(it)
              } ?: "الآن"
            GoldBorderCard(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${sale.invoiceNumber} • ${sale.paymentMethodEnum.arabicLabel}",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrightGold,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = sale.itemsSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IvoryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "بواسطة: ${sale.staffName} • $timeStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmMuted,
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                  Text(
                    text = ReceiptGenerator.formatEgp(sale.totalAmountEgp),
                    style = MaterialTheme.typography.titleMedium,
                    color = IvoryText,
                    fontWeight = FontWeight.Bold,
                  )
                  if (activeStaff.isAdmin) {
                    Text(
                      text = "ربح: ${ReceiptGenerator.formatEgp(sale.netProfitEgp)}",
                      style = MaterialTheme.typography.labelMedium,
                      color = EmeraldProfit,
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
              GoldOutlinedButton(
                text = "عرض ومشاركة الفاتورة",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                onClick = { onOpenReceipt(ReceiptPayload.SaleReceipt(sale)) },
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
      }
    }
  }

  if (showCustomItemDialog) {
    CustomItemEntryDialog(
      onDismiss = { showCustomItemDialog = false },
      onConfirm = { name, qty, sell, cost ->
        onAddCustomItem(name, qty, sell, cost)
        showCustomItemDialog = false
      },
    )
  }

  if (showCheckoutDialog) {
    CheckoutDialog(
      cartTotal = cartItems.sumOf { it.lineTotalEgp },
      isSubmitting = isSubmitting,
      onDismiss = { showCheckoutDialog = false },
      onConfirm = { method, paid, custName, custPhone ->
        onSubmitCheckout(method, paid, custName, custPhone)
        showCheckoutDialog = false
      },
    )
  }

  if (showAddProductDialog) {
    ProductEditorDialog(
      existing = null,
      onDismiss = { showAddProductDialog = false },
      onConfirm = { name, cat, sell, cost, qty, threshold ->
        onUpsertProduct(null, name, cat, sell, cost, qty, threshold)
        showAddProductDialog = false
      },
    )
  }
}

@Composable
private fun CustomItemEntryDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, Long, Double, Double) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var qtyText by remember { mutableStateOf("1") }
  var sellText by remember { mutableStateOf("") }
  var costText by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "إضافة صنف يدوي سريع",
        style = MaterialTheme.typography.titleLarge,
        color = BrightGold,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(12.dp))
      LuxuryTextField(value = name, onValueChange = { name = it }, label = "اسم الصنف أو الخدمة")
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LuxuryTextField(
          value = qtyText,
          onValueChange = { qtyText = it },
          label = "الكمية",
          keyboardType = KeyboardType.Number,
          modifier = Modifier.weight(1f),
        )
        LuxuryTextField(
          value = sellText,
          onValueChange = { sellText = it },
          label = "سعر البيع (ج.م)",
          keyboardType = KeyboardType.Decimal,
          modifier = Modifier.weight(1.3f),
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      LuxuryTextField(
        value = costText,
        onValueChange = { costText = it },
        label = "سعر التكلفة (ج.م)",
        keyboardType = KeyboardType.Decimal,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldOutlinedButton(text = "إلغاء", onClick = onDismiss, modifier = Modifier.weight(1f))
        GoldPrimaryButton(
          text = "إضافة للسلة",
          enabled = name.isNotBlank() && (sellText.toDoubleOrNull() ?: -1.0) >= 0.0,
          onClick = {
            onConfirm(
              name,
              qtyText.toLongOrNull()?.coerceAtLeast(1L) ?: 1L,
              sellText.toDoubleOrNull() ?: 0.0,
              costText.toDoubleOrNull() ?: 0.0,
            )
          },
          modifier = Modifier.weight(1f),
          testTag = "confirm_custom_item_btn",
        )
      }
    }
  }
}

@Composable
private fun CheckoutDialog(
  cartTotal: Double,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (PaymentMethod, Double, String, String) -> Unit,
) {
  var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var paidText by remember { mutableStateOf(cartTotal.toInt().toString()) }
  var customerName by remember { mutableStateOf("") }
  var customerPhone by remember { mutableStateOf("") }

  Dialog(onDismissRequest = onDismiss) {
    GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "إتمام عملية البيع",
        style = MaterialTheme.typography.titleLarge,
        color = BrightGold,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "الإجمالي المطلوب: ${ReceiptGenerator.formatEgp(cartTotal)}",
        style = MaterialTheme.typography.titleMedium,
        color = IvoryText,
      )
      Spacer(modifier = Modifier.height(12.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PaymentMethod.entries.forEach { method ->
          val selected = paymentMethod == method
          Surface(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .border(
                  1.dp,
                  if (selected) MetallicGold else SubtleGoldBorder,
                  RoundedCornerShape(10.dp),
                )
                .clickable {
                  paymentMethod = method
                  if (method != PaymentMethod.CREDIT) {
                    paidText = cartTotal.toInt().toString()
                  } else {
                    paidText = "0"
                  }
                },
            color = if (selected) MetallicGold.copy(alpha = 0.2f) else CardSurface,
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            ) {
              Text(
                text = method.arabicLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) BrightGold else WarmMuted,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      LuxuryTextField(
        value = paidText,
        onValueChange = { paidText = it },
        label = "المبلغ المدفوع الآن (ج.م)",
        keyboardType = KeyboardType.Decimal,
      )
      val paidVal = paidText.toDoubleOrNull() ?: 0.0
      val remaining = (cartTotal - paidVal).coerceAtLeast(0.0)
      if (remaining > 0.01) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "سيتم تسجيل ${ReceiptGenerator.formatEgp(remaining)} تلقائياً في دفتر (لينا)",
          style = MaterialTheme.typography.labelMedium,
          color = AmberProgress,
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      LuxuryTextField(
        value = customerName,
        onValueChange = { customerName = it },
        label = if (remaining > 0.01) "اسم العميل (مطلوب للآجل)" else "اسم العميل (اختياري)",
      )
      Spacer(modifier = Modifier.height(8.dp))
      LuxuryTextField(
        value = customerPhone,
        onValueChange = { customerPhone = it },
        label = "رقم الهاتف (اختياري)",
        keyboardType = KeyboardType.Phone,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldOutlinedButton(text = "إلغاء", onClick = onDismiss, modifier = Modifier.weight(1f))
        GoldPrimaryButton(
          text = if (isSubmitting) "جاري الحفظ..." else "تأكيد وإصدار الفاتورة",
          enabled = !isSubmitting && (remaining <= 0.01 || customerName.isNotBlank()),
          onClick = { onConfirm(paymentMethod, paidVal, customerName, customerPhone) },
          modifier = Modifier.weight(1.3f),
          testTag = "confirm_checkout_btn",
        )
      }
    }
  }
}

@Composable
fun ProductEditorDialog(
  existing: Product?,
  onDismiss: () -> Unit,
  onConfirm: (String, String, Double, Double, Long, Long) -> Unit,
) {
  var name by remember { mutableStateOf(existing?.name ?: "") }
  var category by remember { mutableStateOf(existing?.category ?: "إكسسوارات") }
  var sellText by remember { mutableStateOf(existing?.sellingPriceEgp?.toInt()?.toString() ?: "") }
  var costText by remember { mutableStateOf(existing?.costPriceEgp?.toInt()?.toString() ?: "") }
  var stockText by remember { mutableStateOf(existing?.stockQty?.toString() ?: "5") }
  var thresholdText by remember { mutableStateOf(existing?.lowStockThreshold?.toString() ?: "2") }

  Dialog(onDismissRequest = onDismiss) {
    GoldBorderCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
      Text(
        text = if (existing == null) "إضافة صنف للكتالوج" else "تعديل صنف",
        style = MaterialTheme.typography.titleLarge,
        color = BrightGold,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(10.dp))
      LuxuryTextField(value = name, onValueChange = { name = it }, label = "اسم المنتج")
      Spacer(modifier = Modifier.height(8.dp))
      LuxuryTextField(value = category, onValueChange = { category = it }, label = "التصنيف (شاشات / بطاريات / جرابات...)")
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LuxuryTextField(
          value = sellText,
          onValueChange = { sellText = it },
          label = "سعر البيع",
          keyboardType = KeyboardType.Decimal,
          modifier = Modifier.weight(1f),
        )
        LuxuryTextField(
          value = costText,
          onValueChange = { costText = it },
          label = "سعر التكلفة",
          keyboardType = KeyboardType.Decimal,
          modifier = Modifier.weight(1f),
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LuxuryTextField(
          value = stockText,
          onValueChange = { stockText = it },
          label = "الكمية المتوفرة",
          keyboardType = KeyboardType.Number,
          modifier = Modifier.weight(1f),
        )
        LuxuryTextField(
          value = thresholdText,
          onValueChange = { thresholdText = it },
          label = "حد التنبيه",
          keyboardType = KeyboardType.Number,
          modifier = Modifier.weight(1f),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GoldOutlinedButton(text = "إلغاء", onClick = onDismiss, modifier = Modifier.weight(1f))
        GoldPrimaryButton(
          text = "حفظ",
          enabled = name.isNotBlank() && (sellText.toDoubleOrNull() ?: -1.0) >= 0.0,
          onClick = {
            onConfirm(
              name,
              category,
              sellText.toDoubleOrNull() ?: 0.0,
              costText.toDoubleOrNull() ?: 0.0,
              stockText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
              thresholdText.toLongOrNull()?.coerceAtLeast(0L) ?: 2L,
            )
          },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}
