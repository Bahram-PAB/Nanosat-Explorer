package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.Satellite
import com.example.data.missionType
import com.example.ui.SatelliteViewModel
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SatelliteViewModel = viewModel()) {
    val context = LocalContext.current
    val isEnglish by viewModel.isEnglish.collectAsStateWithLifecycle()
    val satellites by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedUnit by viewModel.selectedUnitSize.collectAsStateWithLifecycle()
    val selectedWeight by viewModel.selectedWeightRange.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val selectedMissionType by viewModel.selectedMissionType.collectAsStateWithLifecycle()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsStateWithLifecycle()
    val countries by viewModel.availableCountries.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalSatelliteCount.collectAsStateWithLifecycle()
    
    val selectedSatellite by viewModel.selectedSatellite.collectAsStateWithLifecycle()
    
    var showFilterDialog by remember { mutableStateOf(false) }

    // Multi-criteria checking
    val isAnyFilterApplied = selectedUnit != null || selectedWeight != null || 
            selectedStatus != null || selectedCountry != null || showOnlyFavorites || selectedMissionType != null

    // Back handling - closes details if open, else exits
    BackHandler(enabled = selectedSatellite != null) {
        viewModel.selectSatellite(null)
    }

    // Main Scaffold
    Scaffold(
        topBar = {
            if (selectedSatellite == null) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Index",
                                tint = SolidPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = "NANOSAT FINDER",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp,
                                color = PrimaryText
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.clearAllFilters() }) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = "Reset Filters",
                                tint = if (isAnyFilterApplied) SolidPrimary else Color.Gray
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = SpaceBackground
                    )
                )
            }
        },
        containerColor = SpaceBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(SearchBackground, RoundedCornerShape(12.dp))
                        .clickable { showFilterDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val filterIconBlock = @Composable {
                        Box {
                            if (isAnyFilterApplied) {
                                Badge(
                                    containerColor = SolidPrimary,
                                    contentColor = SpaceBackground,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-6).dp)
                                ) {
                                    var count = 0
                                    if (selectedUnit != null) count++
                                    if (selectedWeight != null) count++
                                    if (selectedStatus != null) count++
                                    if (selectedCountry != null) count++
                                    if (showOnlyFavorites) count++
                                    if (selectedMissionType != null) count++
                                    Text(count.toString(), fontWeight = FontWeight.Bold)
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = if (isEnglish) "Advanced Filters" else "فیلترهای پیشرفته",
                                tint = if (isAnyFilterApplied) SolidPrimary else SecondaryText,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (isEnglish) {
                        Text(
                            text = "Advanced Filters",
                            color = PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        filterIconBlock()
                    } else {
                        filterIconBlock()
                        Text(
                            text = "فیلترهای پیشرفته",
                            color = PrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Horizontal Active Filter badging indicators
                if (isAnyFilterApplied) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (selectedUnit != null) {
                            item {
                                FilterChipIndicator(label = selectedUnit!!) {
                                    viewModel.selectedUnitSize.value = null
                                }
                            }
                        }
                        if (selectedWeight != null) {
                            item {
                                FilterChipIndicator(label = selectedWeight!!) {
                                    viewModel.selectedWeightRange.value = null
                                }
                            }
                        }
                        if (selectedStatus != null) {
                            item {
                                FilterChipIndicator(label = translateStatus(selectedStatus!!, isEnglish)) {
                                    viewModel.selectedStatus.value = null
                                }
                            }
                        }
                        if (selectedCountry != null) {
                            item {
                                FilterChipIndicator(label = selectedCountry!!) {
                                    viewModel.selectedCountry.value = null
                                }
                            }
                        }
                        if (selectedMissionType != null) {
                            item {
                                FilterChipIndicator(label = translateMissionType(selectedMissionType!!, isEnglish)) {
                                    viewModel.selectedMissionType.value = null
                                }
                            }
                        }
                        if (showOnlyFavorites) {
                            item {
                                FilterChipIndicator(label = if (isEnglish) "★ Bookmarked" else "★ نشان شده‌ها") {
                                    viewModel.showOnlyFavorites.value = false
                                }
                            }
                        }
                    }
                }

                // Live Results stats bar (Bilingual labels)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Found: ${satellites.size} items" else "یافت شده: ${satellites.size} مورد",
                        color = SecondaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = if (isEnglish) "Total Satellites: $totalCount" else "تعداد کل ماهواره‌ها: $totalCount",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // If list is empty, display illustrative empty state
                if (satellites.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Empty Satellites",
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = if (isEnglish) {
                                    "No satellites matching these specifications found in the local catalog."
                                } else {
                                    "ماهواره‌ای با این مشخصات در کاتالوگ محلی یافت نشد."
                                },
                                color = SecondaryText,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Single column List Display of satellites
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(satellites) { satellite ->
                            SatelliteCard(
                                satellite = satellite,
                                isEnglish = isEnglish,
                                onSelect = { viewModel.selectSatellite(satellite) },
                                onToggleFavorite = { viewModel.toggleFavorite(satellite) }
                            )
                        }
                    }
                }

                // Footer Divider
                HorizontalDivider(
                    color = Color.Gray.copy(alpha = 0.15f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Footer Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side (icons)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                        IconButton(
                            onClick = { uriHandler.openUri("https://github.com/Bahram-PAB") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_github),
                                contentDescription = "GitHub",
                                tint = SolidPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { uriHandler.openUri("https://www.linkedin.com/in/bahram-pouralibaba-1a992239") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_linkedin),
                                contentDescription = "LinkedIn",
                                tint = SolidPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Right side: Version & Language toggle icon with appropriate spacing
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Version 4.2" else "نسخه ۴.۲",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        IconButton(
                            onClick = { viewModel.toggleLanguage() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = if (isEnglish) "Switch Language" else "تغییر زبان",
                                tint = SolidPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Foreground slide-in Satellite Details View Sheet
            AnimatedVisibility(
                visible = selectedSatellite != null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                selectedSatellite?.let { satellite ->
                    SatelliteDetailView(
                        satellite = satellite,
                        isEnglish = isEnglish,
                        onClose = { viewModel.selectSatellite(null) },
                        onToggleFavorite = { viewModel.toggleFavorite(satellite) },
                        onDelete = {
                            viewModel.deleteSatellite(satellite)
                            val deleteText = if (isEnglish) "Removed from local database" else "از پایگاه داده حذف شد"
                            Toast.makeText(context, deleteText, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Dialog for Advanced Filter parameters configuration
            if (showFilterDialog) {
                FilterDialog(
                    onDismiss = { showFilterDialog = false },
                    isEnglish = isEnglish,
                    countriesList = countries,
                    selectedUnit = selectedUnit,
                    onSelectUnit = { viewModel.selectedUnitSize.value = it },
                    selectedWeight = selectedWeight,
                    onSelectWeight = { viewModel.selectedWeightRange.value = it },
                    selectedStatus = selectedStatus,
                    onSelectStatus = { viewModel.selectedStatus.value = it },
                    selectedCountry = selectedCountry,
                    onSelectCountry = { viewModel.selectedCountry.value = it },
                    selectedMissionType = selectedMissionType,
                    onSelectMissionType = { viewModel.selectedMissionType.value = it },
                    showFavorites = showOnlyFavorites,
                    onToggleFavorites = { viewModel.showOnlyFavorites.value = it },
                    onReset = { viewModel.clearAllFilters() }
                )
            }


        }
    }
}

// Visual layout helper for filter indicators
@Composable
fun FilterChipIndicator(label: String, onRemove: () -> Unit) {
    Surface(
        color = DarkAccent,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SolidPrimary.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, color = SolidPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove filter",
                tint = SolidPrimary,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove() }
            )
        }
    }
}

@Composable
fun SatelliteCard(satellite: Satellite, isEnglish: Boolean, onSelect: () -> Unit, onToggleFavorite: () -> Unit) {
    val statusColor = statusToColor(satellite.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("satellite_item_${satellite.id}"),
        colors = CardDefaults.cardColors(containerColor = SpaceCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LightBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Name & Favorite button & Type U size badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Determine order or alignment dynamically. Standard LTR name on Left, badge on Right
                Text(
                    text = satellite.name,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                )

                // Favorite Toggle Star Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (satellite.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isEnglish) "Toggle Favorite" else "نشان کردن",
                        tint = if (satellite.isFavorite) Color.Yellow else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(SolidPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = satellite.unitSize,
                        color = SolidPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Organisation / Operator
            Text(
                text = satellite.launchAgency,
                color = SecondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Specs line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nation Country Indicator pill
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = satellite.launchCountry,
                        color = Color.White,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Operational Status Colored Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(statusColor)
                    )
                    Text(
                        text = translateStatus(satellite.status, isEnglish),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SatelliteDetailView(
    satellite: Satellite,
    isEnglish: Boolean,
    onClose: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = statusToColor(satellite.status)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Action Bar & Title Block (No image)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpaceCard)
                    .padding(bottom = 20.dp)
            ) {
                // Action Bar: Close, Favorite, Delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side details actions: Delete custom and Toggle Favorite
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (satellite.isCustom) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = if (isEnglish) "Delete" else "حذف",
                                    tint = FailureRed
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        ) {
                            Icon(
                                imageVector = if (satellite.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (isEnglish) "Favorite" else "نشان کردن",
                                tint = if (satellite.isFavorite) Color.Yellow else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Right side: Straight right-pointing arrow to return to Home (or Left back arrow for English LTR)
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    ) {
                        Icon(
                            imageVector = if (isEnglish) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.ArrowForward,
                            contentDescription = if (isEnglish) "Back to Home" else "برگشت به خانه",
                            tint = Color.White
                        )
                    }
                }

                // Satellite Title & Status Block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                    ) {
                        if (isEnglish) {
                            Text(
                                text = satellite.name,
                                color = PrimaryText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp,
                                fontFamily = FontFamily.SansSerif,
                                modifier = Modifier.weight(1f)
                            )
                            if (satellite.isCustom) {
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, SolidPrimary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("AI SYNC", color = SolidPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            if (satellite.isCustom) {
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, SolidPrimary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("AI SYNC", color = SolidPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = satellite.name,
                                color = PrimaryText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp,
                                fontFamily = FontFamily.SansSerif,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isEnglish) Arrangement.Start else Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) {
                                "Status: " + translateStatus(satellite.status, true)
                            } else {
                                "وضعیت: " + translateStatus(satellite.status, false)
                            },
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Specs grid & descriptions
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Specifications Grid Section
                item {
                    Text(
                        text = if (isEnglish) "Technical and Physical Specifications" else "مشخصات فنی و فیزیکی",
                        color = SolidPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecItemBox(
                                modifier = Modifier.weight(1f),
                                title = if (isEnglish) "Unit Size" else "اندازه بدنه",
                                valString = satellite.unitSize,
                                icon = Icons.Default.Info
                            )
                            SpecItemBox(
                                modifier = Modifier.weight(1f),
                                title = if (isEnglish) "Total Mass" else "وزن کل (جرم)",
                                valString = "${satellite.weightKg} kg",
                                icon = Icons.Default.Build
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecItemBox(
                                modifier = Modifier.weight(1f),
                                title = if (isEnglish) "Country" else "کشور سازنده",
                                valString = satellite.launchCountry,
                                icon = Icons.Default.LocationOn
                            )
                            SpecItemBox(
                                modifier = Modifier.weight(1f),
                                title = if (isEnglish) "Launch Date" else "تاریخ پرتاب",
                                valString = satellite.launchDate,
                                icon = Icons.Default.DateRange
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecItemBox(
                                modifier = Modifier.weight(1f),
                                title = if (isEnglish) "Agency/Operator" else "سازمان / اپراتور",
                                valString = satellite.launchAgency,
                                icon = Icons.Default.Home
                            )
                            SpecItemBox(
                                modifier = Modifier.weight(1f),
                                title = if (isEnglish) "Mission Type" else "نوع ماموریت",
                                valString = translateMissionType(satellite.missionType, isEnglish),
                                icon = Icons.Default.Star
                            )
                        }
                    }
                }

                // Mission Objectives Section
                item {
                    HorizontalDivider(color = LightBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isEnglish) "Primary Mission Objective" else "هدف اصلی ماموریت",
                        color = SolidPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = satellite.missionObjective,
                        color = SecondaryText,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )
                }

                // Detailed Description Context
                item {
                    HorizontalDivider(color = LightBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isEnglish) "Details and About Satellite" else "جزئیات و درباره ماهواره",
                        color = SolidPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = satellite.description,
                        color = SecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun SpecItemBox(modifier: Modifier = Modifier, title: String, valString: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = modifier
            .background(SpaceCard, RoundedCornerShape(12.dp))
            .border(1.dp, LightBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = SolidPrimary, modifier = Modifier.size(20.dp))
        Column {
            Text(title, color = SecondaryText, fontSize = 10.sp)
            Text(valString, color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// Dialog for multi filters choices
@Composable
fun FilterDialog(
    onDismiss: () -> Unit,
    isEnglish: Boolean,
    countriesList: List<String>,
    selectedUnit: String?,
    onSelectUnit: (String?) -> Unit,
    selectedWeight: String?,
    onSelectWeight: (String?) -> Unit,
    selectedStatus: String?,
    onSelectStatus: (String?) -> Unit,
    selectedCountry: String?,
    onSelectCountry: (String?) -> Unit,
    selectedMissionType: String?,
    onSelectMissionType: (String?) -> Unit,
    showFavorites: Boolean,
    onToggleFavorites: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SpaceCard,
            contentColor = PrimaryText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = if (isEnglish) Alignment.Start else Alignment.End
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEnglish) {
                        Text(
                            text = "Advanced Filters",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = SolidPrimary
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close", tint = SecondaryText)
                        }
                    } else {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close", tint = SecondaryText)
                        }
                        Text(
                            text = "فیلترهای پیشرفته",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = SolidPrimary
                        )
                    }
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                // Favorite Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleFavorites(!showFavorites) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEnglish) {
                        Text(text = "Show Bookmarked (★)", color = PrimaryText, fontSize = 14.sp)
                        Switch(
                            checked = showFavorites,
                            onCheckedChange = { onToggleFavorites(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SolidPrimary,
                                checkedTrackColor = SolidPrimary.copy(alpha = 0.4f),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = LightBorder
                            )
                        )
                    } else {
                        Switch(
                            checked = showFavorites,
                            onCheckedChange = { onToggleFavorites(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SolidPrimary,
                                checkedTrackColor = SolidPrimary.copy(alpha = 0.4f),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = LightBorder
                            )
                        )
                        Text(text = "نمایش نشان شده‌ها (★)", color = PrimaryText, fontSize = 14.sp)
                    }
                }

                // 1. Satellite Unit Filter
                FilterCategoryTitle(
                    title = if (isEnglish) "Satellite Size (CubeSat Unit)" else "اندازه ماهواره (یونیت CubeSat)",
                    isEnglish = isEnglish
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("1U", "2U", "3U", "6U", "12U", "Other").forEach { u ->
                        val isSel = selectedUnit == u
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) SolidPrimary else SearchBackground)
                                .border(1.dp, if (isSel) SolidPrimary else LightBorder, RoundedCornerShape(6.dp))
                                .clickable { onSelectUnit(if (isSel) null else u) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = u,
                                color = if (isSel) SpaceBackground else PrimaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 2. Mass/Weight Range
                FilterCategoryTitle(
                    title = if (isEnglish) "Satellite Weight Range" else "محدوده وزن ماهواره",
                    isEnglish = isEnglish
                )
                val limits = listOf("Micro (< 1.5kg)", "Light (1.5 - 5kg)", "Medium (5 - 20kg)", "Heavy (> 20kg)")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        limits.take(2).forEach { w ->
                            val isSel = selectedWeight == w
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) SolidPrimary else SearchBackground)
                                    .border(1.dp, if (isSel) SolidPrimary else LightBorder, RoundedCornerShape(6.dp))
                                    .clickable { onSelectWeight(if (isSel) null else w) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = w,
                                    color = if (isSel) SpaceBackground else PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        limits.drop(2).forEach { w ->
                            val isSel = selectedWeight == w
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) SolidPrimary else SearchBackground)
                                    .border(1.dp, if (isSel) SolidPrimary else LightBorder, RoundedCornerShape(6.dp))
                                    .clickable { onSelectWeight(if (isSel) null else w) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = w,
                                    color = if (isSel) SpaceBackground else PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3. Status
                FilterCategoryTitle(
                    title = if (isEnglish) "Orbital Status" else "وضعیت مداری",
                    isEnglish = isEnglish
                )
                val statuses = listOf("Orbiting", "De-orbited", "Decayed", "Launch Failure")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        statuses.take(2).forEach { stat ->
                            val isSel = selectedStatus == stat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) SolidPrimary else SearchBackground)
                                    .border(1.dp, if (isSel) SolidPrimary else LightBorder, RoundedCornerShape(6.dp))
                                    .clickable { onSelectStatus(if (isSel) null else stat) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = translateStatus(stat, isEnglish),
                                    color = if (isSel) SpaceBackground else PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        statuses.drop(2).forEach { stat ->
                            val isSel = selectedStatus == stat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) SolidPrimary else SearchBackground)
                                    .border(1.dp, if (isSel) SolidPrimary else LightBorder, RoundedCornerShape(6.dp))
                                    .clickable { onSelectStatus(if (isSel) null else stat) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = translateStatus(stat, isEnglish),
                                    color = if (isSel) SpaceBackground else PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 4. Country of Origin (Dynamic List & Scroll)
                FilterCategoryTitle(
                    title = if (isEnglish) "Country of Origin/Launcher" else "کشور سازنده / پرتاب‌کننده",
                    isEnglish = isEnglish
                )
                if (countriesList.isEmpty()) {
                    Text(
                        text = if (isEnglish) "No active countries found" else "کشور فعالی ثبت نشده است",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(countriesList) { country ->
                            val isSel = selectedCountry == country
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) SolidPrimary else SearchBackground)
                                    .border(1.dp, if (isSel) SolidPrimary else LightBorder, RoundedCornerShape(6.dp))
                                    .clickable { onSelectCountry(if (isSel) null else country) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = country,
                                    color = if (isSel) SpaceBackground else PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 5. Mission Type
                FilterCategoryTitle(
                    title = if (isEnglish) "Mission Type" else "نوع ماموریت",
                    isEnglish = isEnglish
                )
                val missionTypes = listOf("علمی و تحقیقاتی", "تست فناوری", "سنجش از دور", "ارتباطات و مخابرات")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        missionTypes.take(2).forEach { mt ->
                            val isSel = selectedMissionType == mt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) SolidPrimary else SearchBackground)
                                    .border(1.dp, if (isSel) SolidPrimary else LightBorder, RoundedCornerShape(6.dp))
                                    .clickable { onSelectMissionType(if (isSel) null else mt) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = translateMissionType(mt, isEnglish),
                                    color = if (isSel) SpaceBackground else PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        missionTypes.drop(2).forEach { mt ->
                            val isSel = selectedMissionType == mt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) SolidPrimary else SearchBackground)
                                    .border(1.dp, if (isSel) SolidPrimary else LightBorder, RoundedCornerShape(6.dp))
                                    .clickable { onSelectMissionType(if (isSel) null else mt) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = translateMissionType(mt, isEnglish),
                                    color = if (isSel) SpaceBackground else PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = {
                            onReset()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isEnglish) "Clear All" else "پاک کردن همه",
                            color = FailureRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = SolidPrimary, contentColor = Color.White),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Apply Filters" else "اعمال فیلترها",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterCategoryTitle(title: String, isEnglish: Boolean = false) {
    Text(
        text = title,
        color = Color.LightGray,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Right
    )
}



// Translate orbit status to beautifully styled Persian or English text description
fun translateStatus(status: String, isEnglish: Boolean = false): String {
    val low = status.lowercase()
    if (isEnglish) {
        return when {
            low.contains("orbiting") && low.contains("operational") -> "Orbiting (Operational)"
            low.contains("orbiting") -> "Orbiting"
            low.contains("reentered") && low.contains("was operational") -> "Reentered (Was Operational)"
            low.contains("reentered") && low.contains("operational") -> "Reentered (Operational)"
            low.contains("reentered") -> "Reentered (Decayed)"
            low.contains("decayed") -> "Decayed"
            low.contains("launch failure") -> "Launch Failure"
            low.contains("no signal") -> "No Signal"
            low.contains("was operational") -> "Inactive (Was Operational)"
            low.contains("operational") -> "Operational"
            else -> status
        }
    }
    return when {
        low.contains("orbiting") && low.contains("operational") -> "فعال در مدار"
        low.contains("orbiting") -> "مستقر در مدار"
        low.contains("reentered") && low.contains("was operational") -> "سوخته در جو (قبلاً فعال)"
        low.contains("reentered") && low.contains("operational") -> "سوخته در جو (عملیاتی)"
        low.contains("reentered") -> "خارج‌شده از جو (سوخته)"
        low.contains("decayed") -> "سوخته در جو"
        low.contains("launch failure") -> "شکست در پرتاب"
        low.contains("no signal") -> "بدون سیگنال"
        low.contains("was operational") -> "غیرفعال (قبلاً عملیاتی)"
        low.contains("operational") -> "عملیاتی فعال"
        else -> status
    }
}

fun translateMissionType(missionType: String, isEnglish: Boolean): String {
    if (!isEnglish) return missionType
    return when (missionType) {
        "علمی و تحقیقاتی" -> "Scientific/Research"
        "تست فناوری" -> "Technology Demo"
        "سنجش از دور" -> "Remote Sensing"
        "ارتباطات و مخابرات" -> "Communications"
        else -> missionType
    }
}

// Style different status colors uniformly over the applet UI
fun statusToColor(status: String): Color {
    val low = status.lowercase()
    return when {
        low.contains("orbiting") || low.contains("operational") && !low.contains("was") -> ActiveGreen
        low.contains("reentered") || low.contains("decayed") -> DecayedOrange
        low.contains("no signal") || low.contains("lost") -> DeorbitedSlate
        low.contains("launch failure") -> FailureRed
        else -> Color.Gray
    }
}
