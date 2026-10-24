package org.nitri.opentopo.nearby

import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.nitri.opentopo.R
import org.nitri.opentopo.nearby.entity.NearbyItem
import org.nitri.opentopo.util.DistanceCalculator
import kotlin.math.roundToInt

internal fun sortNearbyItems(
    items: List<NearbyItem>, latitude: Double, longitude: Double
): List<NearbyItem> = items.sortedBy {
    // Retain the previous whole-metre ordering without modifying Room entities.
    DistanceCalculator.distance(latitude, longitude, it.lat, it.lon).roundToInt()
}

@Composable
internal fun NearbyScreen(
    items: List<NearbyItem>,
    onArticleClick: (NearbyItem) -> Unit,
    onMapClick: (NearbyItem) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(8.dp)) {
            itemsIndexed(items, key = { _, item -> item.pageid ?: "room:${item.id}" }) { index, item ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onArticleClick(item) }
                            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NearbyThumbnail(item.thumbnail)
                        Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                            Text(
                                item.title.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(item.description.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { onMapClick(item) }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                painterResource(R.drawable.ic_map),
                                contentDescription = stringResource(R.string.nearby_show_on_map),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (index < items.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun NearbyThumbnail(url: String?) {
    val context = LocalContext.current
    val picasso = remember(context) { NearbyImages.getPicasso(context) }
    val normalizedUrl = normalizeNearbyThumbnailUrl(url)
    AndroidView(
        modifier = Modifier.size(60.dp),
        factory = { imageContext ->
            ImageView(imageContext).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        },
        update = { view ->
            // A null URL also replaces any previous request with the placeholder.
            picasso.load(normalizedUrl)
                .placeholder(R.drawable.ic_place)
                .error(R.drawable.ic_place)
                .resize(60, 60)
                .centerCrop()
                .into(view)
        },
        onRelease = { view -> picasso.cancelRequest(view) }
    )
}
