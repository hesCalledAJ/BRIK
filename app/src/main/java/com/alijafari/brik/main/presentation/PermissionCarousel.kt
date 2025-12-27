package com.alijafari.brik.main.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alijafari.brik.R
import com.alijafari.brik.main.domain.model.PermissionRequirement

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PermissionCarousel(
    requirements: List<PermissionRequirement>,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { requirements.size })

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Setup Required",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        HorizontalPager(
            state = pagerState,
            pageSpacing = 16.dp,
        ) { page ->
            val item = requirements[page]
            PermissionCard(item)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .height(8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(requirements.size) { iteration ->
                val color =
                    if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.3f
                    )
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(6.dp)
                )
            }
        }
    }
}

@Composable
fun PermissionCard(requirement: PermissionRequirement) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { requirement.onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(requirement.icon),
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    requirement.title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    requirement.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(painterResource(R.drawable.ic_arrow_right), null)
        }
    }
}