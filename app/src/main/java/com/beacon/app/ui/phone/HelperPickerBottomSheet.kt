package com.beacon.app.ui.phone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.theme.BeaconCardDark
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconOnDark
import com.beacon.domain.helper.TrustedHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelperPickerBottomSheet(
    helpers: List<TrustedHelper>,
    onDismiss: () -> Unit,
    onPick: (TrustedHelper) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp)) {
            Text("Choose a helper")
            helpers.forEach { helper ->
                BentoCard(
                    title = helper.displayName,
                    value = helper.phoneNumber,
                    onClick = { onPick(helper) },
                    containerColor = BeaconCardDark,
                    contentColor = BeaconOnDark,
                    minHeight = BeaconDimens.bentoWideMinHeight,
                    modifier = Modifier.fillMaxWidth(),
                    contentDescription = "Select ${helper.displayName}",
                )
            }
        }
    }
}
