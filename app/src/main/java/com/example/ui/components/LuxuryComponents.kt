package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DebtStatus
import com.example.data.model.RepairStatus
import com.example.receipt.ReceiptGenerator
import com.example.receipt.ReceiptPayload
import com.example.ui.theme.AmberProgress
import com.example.ui.theme.BrightGold
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.HairlineGold
import com.example.ui.theme.IvoryText
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.SkyReady
import com.example.ui.theme.SubtleGoldBorder
import com.example.ui.theme.WarmMuted

@Composable
fun GoldBorderCard(
  modifier: Modifier = Modifier,
  highlighted: Boolean = false,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = RoundedCornerShape(16.dp)
  val borderColor = if (highlighted) MetallicGold else SubtleGoldBorder
  val bgBrush =
    Brush.verticalGradient(
      colors =
        if (highlighted) listOf(Color(0xFF242015), CardSurface)
        else listOf(ElevatedSurface, CardSurface)
    )

  Surface(
    modifier =
      modifier
        .clip(shape)
        .background(bgBrush)
        .border(width = if (highlighted) 1.3.dp else 1.dp, color = borderColor, shape = shape)
        .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base },
    color = Color.Transparent,
    shape = shape,
  ) {
    Column(modifier = Modifier.padding(16.dp), content = content)
  }
}

@Composable
fun KpiSummaryCard(
  title: String,
  value: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  accentColor: Color = BrightGold,
  subtitle: String? = null,
  isLocked: Boolean = false,
  testTag: String = "",
) {
  GoldBorderCard(
    modifier = modifier.let { if (testTag.isNotBlank()) it.testTag(testTag) else it },
    highlighted = true,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = WarmMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Box(
        modifier =
          Modifier.size(34.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.14f))
            .border(1.dp, accentColor.copy(alpha = 0.45f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = if (isLocked) Icons.Default.Lock else icon,
          contentDescription = title,
          tint = accentColor,
          modifier = Modifier.size(18.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(10.dp))
    if (isLocked) {
      Text(
        text = "للمدير فقط",
        style = MaterialTheme.typography.titleMedium,
        color = WarmMuted,
        fontWeight = FontWeight.Bold,
      )
    } else {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        color = IvoryText,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    if (!subtitle.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = accentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun RepairStatusPill(
  status: RepairStatus,
  modifier: Modifier = Modifier,
) {
  val (color, bg) =
    when (status) {
      RepairStatus.PENDING -> WarmMuted to Color(0xFF282520)
      RepairStatus.IN_PROGRESS -> AmberProgress to AmberProgress.copy(alpha = 0.16f)
      RepairStatus.READY_FOR_PICKUP -> SkyReady to SkyReady.copy(alpha = 0.16f)
      RepairStatus.DELIVERED -> EmeraldProfit to EmeraldProfit.copy(alpha = 0.16f)
    }
  Surface(
    modifier = modifier.border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(50)),
    color = bg,
    shape = RoundedCornerShape(50),
  ) {
    Text(
      text = status.arabicLabel,
      style = MaterialTheme.typography.labelMedium,
      color = color,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun DebtStatusPill(
  status: DebtStatus,
  isOverdue: Boolean = false,
) {
  val (label, color) =
    when {
      status == DebtStatus.SETTLED -> "خالص" to EmeraldProfit
      isOverdue -> "متأخر" to CrimsonAlert
      status == DebtStatus.PARTIAL -> "سداد جزئي" to AmberProgress
      else -> "مفتوح" to MetallicGold
    }
  Surface(
    modifier = Modifier.border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(50)),
    color = color.copy(alpha = 0.14f),
    shape = RoundedCornerShape(50),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = color,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun GoldPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  testTag: String = "",
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier =
      modifier
        .heightIn(min = 48.dp)
        .let { if (testTag.isNotBlank()) it.testTag(testTag) else it },
    colors =
      ButtonDefaults.buttonColors(
        containerColor = MetallicGold,
        contentColor = MatteBlack,
        disabledContainerColor = HairlineGold.copy(alpha = 0.4f),
        disabledContentColor = WarmMuted,
      ),
    shape = RoundedCornerShape(12.dp),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
  ) {
    if (icon != null) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun GoldOutlinedButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  testTag: String = "",
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    modifier =
      modifier
        .heightIn(min = 48.dp)
        .let { if (testTag.isNotBlank()) it.testTag(testTag) else it },
    border = BorderStroke(1.dp, if (enabled) MetallicGold else SubtleGoldBorder),
    colors =
      ButtonDefaults.outlinedButtonColors(
        contentColor = BrightGold,
        disabledContentColor = WarmMuted,
      ),
    shape = RoundedCornerShape(12.dp),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
  ) {
    if (icon != null) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
  }
}

@Composable
fun ReceiptPreviewDialog(
  payload: ReceiptPayload,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val bitmap: Bitmap = remember(payload) { ReceiptGenerator.renderReceiptBitmap(context, payload) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier =
        Modifier.fillMaxWidth()
          .padding(16.dp)
          .border(1.2.dp, MetallicGold, RoundedCornerShape(20.dp)),
      color = MatteBlack,
      shape = RoundedCornerShape(20.dp),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "معاينة الفاتورة الإلكترونية",
            style = MaterialTheme.typography.titleMedium,
            color = BrightGold,
            fontWeight = FontWeight.Bold,
          )
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("close_receipt_dialog"),
          ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = IvoryText)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(max = 420.dp)
              .verticalScroll(rememberScrollState())
              .clip(RoundedCornerShape(12.dp))
              .border(1.dp, SubtleGoldBorder, RoundedCornerShape(12.dp))
        ) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "فاتورة 2m Store",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          GoldPrimaryButton(
            text = "مشاركة واتساب",
            icon = Icons.Default.Share,
            onClick = { ReceiptGenerator.shareReceiptViaWhatsApp(context, payload) },
            modifier = Modifier.weight(1f),
            testTag = "share_whatsapp_button",
          )
          GoldOutlinedButton(
            text = "طباعة",
            icon = Icons.Default.Print,
            onClick = { ReceiptGenerator.printReceipt(context, payload) },
            modifier = Modifier.weight(1f),
            testTag = "print_receipt_button",
          )
        }
      }
    }
  }
}
