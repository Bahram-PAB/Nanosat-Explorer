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
import androidx.compose.material.icons.filled.*
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
import com.example.ui.GeminiSearchState
import com.example.ui.SatelliteViewModel
import com.example.ui.theme.*
import com.example.api.GeminiClient

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
    val satellites by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedUnit by viewModel.selectedUnitSize.collectAsStateWithLifecycle()
    val selectedWeight by viewModel.selectedWeightRange.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val selectedMissionType by viewModel.selectedMissionType.collectAsStateWithLifecycle()
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsStateWithLifecycle()
    val countries by viewModel.availableCountries.collectAsStateWithLifecycle()
    
    val selectedSatellite by viewModel.selectedSatellite.collectAsStateWithLifecycle()
    val geminiState by viewModel.geminiSearchState.collectAsStateWithLifecycle()
    
    var showFilterDialog by remember { mutableStateOf(false) }
    var geminiSearchInput by remember { mutableStateOf("") }
    var showGeminiSearchDialog by remember { mutableStateOf(false) }

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
                navigationIcon = {
                    IconButton(onClick = { showGeminiSearchDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Gemini Query",
                            tint = SolidPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAllFilters() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Filters",
                            tint = if (isAnyFilterApplied) SolidPrimary else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SpaceBackground
                )
            )
        },
        containerColor = SpaceBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Search Box & Filter Trigger Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("satellite_search_bar"),
                        placeholder = {
                            Text(
                                "نام ماهواره، کشور یا سازمان...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = SolidPrimary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SolidPrimary,
                            unfocusedBorderColor = SearchBackground,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedContainerColor = SearchBackground,
                            unfocusedContainerColor = SearchBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Filter Button
                    Box {
                        Badge(
                            containerColor = SolidPrimary,
                            contentColor = SpaceBackground,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .alpha(if (isAnyFilterApplied) 1f else 0f)
                        ) {
                            var count = 0
                            if (selectedUnit != null) count++
                            if (selectedWeight != null) count++
                            if (selectedStatus != null) count++
                            if (selectedCountry != null) count++
                            if (showOnlyFavorites) count++
                            Text(count.toString(), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { showFilterDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAnyFilterApplied) DarkAccent else SearchBackground,
                                contentColor = if (isAnyFilterApplied) SolidPrimary else SecondaryText
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(12.dp),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Filter Options",
                                tint = if (isAnyFilterApplied) SolidPrimary else SecondaryText
                            )
                        }
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
                                FilterChipIndicator(label = translateStatus(selectedStatus!!)) {
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
                                FilterChipIndicator(label = selectedMissionType!!) {
                                    viewModel.selectedMissionType.value = null
                                }
                            }
                        }
                        if (showOnlyFavorites) {
                            item {
                                FilterChipIndicator(label = "★ نشان شده‌ها") {
                                    viewModel.showOnlyFavorites.value = false
                                }
                            }
                        }
                    }
                }

                // Live Results stats bar (Persian labels)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "یافت شده: ${satellites.size} مورد",
                        color = SecondaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "مرجع داده: nanosats.eu",
                        color = SecondaryText,
                        fontSize = 11.sp
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
                                "ماهواره‌ای با این مشخصات در کاتالوگ محلی یافت نشد.",
                                color = SecondaryText,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    geminiSearchInput = searchQuery
                                    showGeminiSearchDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SolidPrimary,
                                    contentColor = SpaceBackground
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Search, "AI Search")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جستجوی زنده در nanosats.eu با هوش مصنوعی")
                            }
                        }
                    }
                } else {
                    // Grid / List Display of satellites
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(satellites) { satellite ->
                            SatelliteCard(satellite = satellite, onSelect = {
                                viewModel.selectSatellite(satellite)
                            })
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
                        onClose = { viewModel.selectSatellite(null) },
                        onToggleFavorite = { viewModel.toggleFavorite(satellite) },
                        onDelete = {
                            viewModel.deleteSatellite(satellite)
                            Toast.makeText(context, "از پایگاه داده حذف شد", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Dialog for Advanced Filter parameters configuration
            if (showFilterDialog) {
                FilterDialog(
                    onDismiss = { showFilterDialog = false },
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

            // Dialog for Gemini dynamic database index search
            if (showGeminiSearchDialog) {
                GeminiSearchDialog(
                    onDismiss = {
                        showGeminiSearchDialog = false
                        viewModel.resetGeminiState()
                    },
                    inputValue = geminiSearchInput,
                    onInputValueChange = { geminiSearchInput = it },
                    geminiState = geminiState,
                    onTriggerSearch = { viewModel.searchOnlineSatellite(geminiSearchInput) }
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
fun SatelliteCard(satellite: Satellite, onSelect: () -> Unit) {
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
            // Header: Name & Type U size badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = satellite.name,
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

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
                overflow = TextOverflow.Ellipsis
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
                        text = translateStatus(satellite.status),
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
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (satellite.isCustom) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete custom import",
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
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = if (satellite.isFavorite) Color.Yellow else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Satellite Title & Status Block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(statusColor)
                        )
                        Text(
                            text = "وضعیت: " + translateStatus(satellite.status),
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
                        "مشخصات فنی و فیزیکی",
                        color = SolidPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecItemBox(modifier = Modifier.weight(1f), title = "اندازه بدنه", valString = satellite.unitSize, icon = Icons.Default.Info)
                            SpecItemBox(modifier = Modifier.weight(1f), title = "وزن کل (جرم)", valString = "${satellite.weightKg} kg", icon = Icons.Default.Build)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecItemBox(modifier = Modifier.weight(1f), title = "کشور سازنده", valString = satellite.launchCountry, icon = Icons.Default.LocationOn)
                            SpecItemBox(modifier = Modifier.weight(1f), title = "تاریخ پرتاب", valString = satellite.launchDate, icon = Icons.Default.DateRange)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecItemBox(modifier = Modifier.weight(1f), title = "سازمان / اپراتور", valString = satellite.launchAgency, icon = Icons.Default.Home)
                            SpecItemBox(modifier = Modifier.weight(1f), title = "نوع ماموریت", valString = satellite.missionType, icon = Icons.Default.Star)
                        }
                    }
                }

                // Mission Objectives Section
                item {
                    HorizontalDivider(color = LightBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "هدف اصلی ماموریت",
                        color = SolidPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = satellite.missionObjective,
                        color = SecondaryText,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                }

                // Detailed Description Context
                item {
                    HorizontalDivider(color = LightBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "جزئیات و درباره ماهواره",
                        color = SolidPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = satellite.description,
                        color = SecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = SecondaryText)
                    }
                    Text(
                        "فیلترهای پیشرفته",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = SolidPrimary
                    )
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
                    Text("نمایش نشان شده‌ها (★)", color = PrimaryText, fontSize = 14.sp)
                }

                // 1. Satellite Unit Filter
                FilterCategoryTitle("اندازه ماهواره (یونیت CubeSat)")
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
                FilterCategoryTitle("محدوده وزن ماهواره")
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
                FilterCategoryTitle("وضعیت مداری")
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
                                    text = translateStatus(stat),
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
                                    text = translateStatus(stat),
                                    color = if (isSel) SpaceBackground else PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 4. Country of Origin (Dynamic List & Scroll)
                FilterCategoryTitle("کشور سازنده / پرتاب‌کننده")
                if (countriesList.isEmpty()) {
                    Text("کشور فعالی ثبت نشده است", color = Color.Gray, fontSize = 11.sp)
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
                FilterCategoryTitle("نوع ماموریت")
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
                                    text = mt,
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
                                    text = mt,
                                    color = if (isSel) SpaceBackground else PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

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
                        Text("پاک کردن همه", color = FailureRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = SolidPrimary, contentColor = Color.White),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("اعمال فیلترها", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterCategoryTitle(title: String) {
    Text(
        text = title,
        color = Color.LightGray,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        textAlign = TextAlign.Right
    )
}

// Dialog for Gemini API deep parsing of nanosats.eu database
@Composable
fun GeminiSearchDialog(
    onDismiss: () -> Unit,
    inputValue: String,
    onInputValueChange: (String) -> Unit,
    geminiState: GeminiSearchState,
    onTriggerSearch: () -> Unit
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Dismiss", tint = SecondaryText)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Star, "AI", tint = SolidPrimary)
                        Text(
                            "جستجوی زنده با هوش مصنوعی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SolidPrimary
                        )
                    }
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                if (!GeminiClient.isApiKeyConfigured()) {
                    // Help note for missing key
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FailureRed.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, FailureRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "کلید API یافت نشد!",
                                color = FailureRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                            Text(
                                "برای جستجوی هوشمند، باید کلید خود را با نام GEMINI_API_KEY در بخش Secrets در Google AI Studio وارد کنید.",
                                color = SecondaryText,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                } else {
                    Text(
                        "اگر نام ماهواره یا سازمان فضایی در کاتالوگ نیست، بنویسید تا جمینای مستقیماً مشخصات آن را استخراج و برای شما وارد کند:",
                        fontSize = 12.sp,
                        color = SecondaryText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )

                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = onInputValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("مثلاً: Capella-3 یا AlSat-2 ...", color = Color.Gray, fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SolidPrimary,
                            unfocusedBorderColor = SearchBackground,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedContainerColor = SearchBackground,
                            unfocusedContainerColor = SearchBackground
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Operational progress UI
                    AnimatedContent(targetState = geminiState) { state ->
                        when (state) {
                            is GeminiSearchState.Idle -> {
                                Button(
                                    onClick = onTriggerSearch,
                                    enabled = inputValue.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = SolidPrimary, contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Search, "Search Online")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("شروع جستجوی هوشمند جدید", fontWeight = FontWeight.Bold)
                                }
                            }
                            is GeminiSearchState.Loading -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(color = SolidPrimary)
                                    Text(
                                        "درحال اسکن دیتابیس nanosats.eu و استخراج اطلاعات ماهواره...",
                                        fontSize = 11.sp,
                                        color = SolidPrimary,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            is GeminiSearchState.Success -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ActiveGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, "Success", tint = ActiveGreen)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "با موفقیت اضافه شد!",
                                            color = ActiveGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Right
                                        )
                                        Text(
                                            "ماهواره ${state.satellite.name} با مشخصات استخراج شده به کاتالوگ شما متصل شد.",
                                            color = SecondaryText,
                                            fontSize = 11.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }
                            }
                            is GeminiSearchState.Error -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(FailureRed.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                        .border(1.dp, FailureRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "خطا در دریافت اطلاعات",
                                        color = FailureRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )
                                    Text(
                                        state.message,
                                        color = SecondaryText,
                                        fontSize = 11.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = onTriggerSearch,
                                        colors = ButtonDefaults.buttonColors(containerColor = FailureRed, contentColor = Color.White),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("تلاش مجدد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Translate orbit status to beautifully styled Persian text description
fun translateStatus(status: String): String {
    return when (status.lowercase()) {
        "orbiting" -> "فعال در مدار"
        "de-orbiting", "de-orbited" -> "خارج‌شده از مدار"
        "decayed" -> "سوخته در جو"
        "launch failure" -> "شکست در پرتاب"
        else -> status
    }
}

// Style different status colors uniformly over the applet UI
fun statusToColor(status: String): Color {
    return when (status.lowercase()) {
        "orbiting" -> ActiveGreen
        "de-orbiting", "de-orbited" -> DeorbitedSlate
        "decayed" -> DecayedOrange
        "launch failure" -> FailureRed
        else -> Color.Gray
    }
}
