package com.simats.pcos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

// Modern brand colors consistent with existing theme
val PcosPrimaryPink = Color(0xFFEB7DB2)
val PcosDarkPink = Color(0xFFB80257)
val PcosSoftPink = Color(0xFFFDE8F1)
val PcosBackground = Color(0xFFF8FAFC)
val PcosSurface = Color.White
val PcosTextPrimary = Color(0xFF1E293B)
val PcosTextSecondary = Color(0xFF64748B)

/**
 * Reusable ProfileAvatar component with Coil image loading, memory/disk caching,
 * loading indicator, error fallback, and initials generation.
 */
@Composable
fun ProfileAvatar(
    imageUrl: String?,
    name: String = "",
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    borderWidth: Dp = 1.5.dp,
    borderColor: Color = PcosPrimaryPink,
    onClick: (() -> Unit)? = null
) {
    val normalizedUrl = remember(imageUrl) { ImageHelper.normalizeImageUrl(imageUrl) }
    var isError by remember(normalizedUrl) { mutableStateOf(false) }
    var isLoading by remember(normalizedUrl) { mutableStateOf(normalizedUrl != null) }

    val avatarModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(
            if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, CircleShape)
            else Modifier
        )
        .then(
            if (onClick != null) Modifier.clickable(onClick = onClick)
            else Modifier
        )

    Box(
        modifier = avatarModifier.background(PcosSoftPink),
        contentAlignment = Alignment.Center
    ) {
        if (!normalizedUrl.isNullOrEmpty() && !isError) {
            val context = LocalContext.current
            val imageRequest = remember(normalizedUrl) {
                ImageRequest.Builder(context)
                    .data(normalizedUrl)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = if (name.isNotEmpty()) "$name avatar" else "Profile avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onLoading = { isLoading = true },
                onSuccess = {
                    isLoading = false
                    isError = false
                },
                onError = {
                    isLoading = false
                    isError = true
                }
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 0.45f),
                    color = PcosPrimaryPink,
                    strokeWidth = 2.dp
                )
            }
        } else {
            // Fallback avatar: Display initials if available, otherwise Person icon
            val initials = remember(name) {
                val parts = name.trim().split(" ").filter { it.isNotEmpty() }
                when {
                    parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
                    parts.isNotEmpty() && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
                    else -> ""
                }
            }

            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    color = PcosDarkPink,
                    fontSize = (size.value * 0.38f).sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default avatar",
                    tint = PcosPrimaryPink,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
    }
}

/**
 * Modern Material 3 Patient Card with avatar, username, status badge, and arrow.
 */
@Composable
fun PatientCard(
    patient: Patient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                imageUrl = null, // Backend patientlist.php returns usernames; avatar falls back to initials
                name = patient.username,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = patient.username,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PcosTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFECFDF5)
                ) {
                    Text(
                        text = "Active Patient",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF059669),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View patient details",
                    tint = PcosTextSecondary
                )
            }
        }
    }
}

/**
 * Metric summary card for doctor dashboard.
 */
@Composable
fun DashboardSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PcosTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PcosTextSecondary
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = PcosTextSecondary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Standard loading indicator with text.
 */
@Composable
fun LoadingContent(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = PcosPrimaryPink,
                strokeWidth = 3.dp,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = PcosTextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Standard empty state presentation.
 */
@Composable
fun EmptyStateContent(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Inbox,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(PcosSoftPink),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PcosPrimaryPink,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PcosTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                color = PcosTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PcosPrimaryPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = actionText, color = Color.White)
                }
            }
        }
    }
}

/**
 * Standard error state presentation with retry action.
 */
@Composable
fun ErrorStateContent(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Unable to load data",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF991B1B)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = errorMessage,
                fontSize = 13.sp,
                color = Color(0xFF7F1D1D),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry", color = Color.White)
            }
        }
    }
}
