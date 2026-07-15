package com.example.vehicledata

import android.os.Build.VERSION.SDK_INT
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import kotlinx.coroutines.launch

// ==========================================
// THEME
// ==========================================
@Composable
fun VehicleDataTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFE53935),
            background = Color.Black,
            surface = Color(0xFF1E1E1E)
        ),
        content = content
    )
}

// ==========================================
// NAVIGATION & LAYOUT
// ==========================================
@Composable
fun MainAppScreen(viewModel: VehicleViewModel, authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()
    
    val isLoggedIn = authState is AuthUiState.Success
    
    var showSplash by remember { mutableStateOf(false) }
    var actualLoggedIn by remember { mutableStateOf(isLoggedIn) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && !actualLoggedIn) { // Transition from logged out to logged in
            showSplash = true
            kotlinx.coroutines.delay(1000) // Wait for fade in
            actualLoggedIn = true // Swap screens underneath safely
            kotlinx.coroutines.delay(1500) // Wait for GIF to finish and dashboard to load
            showSplash = false
        } else if (!isLoggedIn) { // Transition from logged in to logged out
            actualLoggedIn = false
        }
    }

    LaunchedEffect(actualLoggedIn) {
        if (actualLoggedIn) {
            navController.navigate("dashboard") {
                popUpTo(0) // Clear back stack completely
            }
        } else {
            navController.navigate("login") {
                popUpTo(0)
            }
        }
    }
    
    val startDestination = if (actualLoggedIn) "dashboard" else "login"

    Box(modifier = Modifier.fillMaxSize()) {
        SharedBackground {
            NavHost(navController = navController, startDestination = startDestination) {
                composable("login") {
                    LoginScreen(viewModel = authViewModel, navController = navController)
                }
                composable("otp") {
                    OtpScreen(viewModel = authViewModel, navController = navController)
                }
                composable("dashboard") {
                    DashboardScreenWithDrawer(viewModel, authViewModel, navController)
                }
            }
        }
        
        // Splash Overlay
        androidx.compose.animation.AnimatedVisibility(
            visible = showSplash,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.EaseInOutSine)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(1500, easing = androidx.compose.animation.core.EaseInOutSine)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val context = LocalContext.current
                val imageLoader = remember {
                    ImageLoader.Builder(context)
                        .components {
                            if (SDK_INT >= 28) {
                                add(ImageDecoderDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                        }
                        .build()
                }
                AsyncImage(
                    model = R.drawable.openinggif,
                    contentDescription = "Splash",
                    imageLoader = imageLoader,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun SharedBackground(content: @Composable BoxScope.() -> Unit) {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = R.drawable.bg_anim_3,
            contentDescription = "Animated Background",
            imageLoader = imageLoader,
            modifier = Modifier.fillMaxSize().scale(1.1f),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier.fillMaxSize().background(Color(0x66000000)))
        content()
    }
}

@Composable
fun DashboardScreenWithDrawer(
    viewModel: VehicleViewModel,
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val myInfo by viewModel.myInfo.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchData()
        viewModel.fetchMyInfo()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProfileDrawerContent(
                myInfoData = myInfo,
                onCloseDrawer = {
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        VehicleScreen(
            viewModel = viewModel, 
            onOpenDrawer = {
                scope.launch { drawerState.open() }
            }
        )
    }
}

// ==========================================
// AUTH SCREENS
// ==========================================
@Composable
fun LoginScreen(viewModel: AuthViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsState()
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.OtpSent) {
            navController.navigate("otp") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to u4me",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number", color = Color(0xD9FFFFFF)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0x80FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = Color(0xD9FFFFFF)) },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0x80FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.submitCredentials(mobile, password) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    enabled = mobile.isNotBlank() && password.isNotBlank() && uiState !is AuthUiState.Loading
                ) {
                    if (uiState is AuthUiState.Loading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("LOGIN", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                
                if (uiState is AuthUiState.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = (uiState as AuthUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun OtpScreen(viewModel: AuthViewModel, navController: NavController) {
    val uiState by viewModel.uiState.collectAsState()
    var otp by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        // Navigation to dashboard is handled globally by MainAppScreen to orchestrate the splash transition
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Verify OTP",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = Color.White),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it },
                    label = { Text("Enter OTP", color = Color(0xD9FFFFFF)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0x80FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel.submitOtp(otp) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    enabled = otp.isNotBlank() && uiState !is AuthUiState.Loading
                ) {
                    if (uiState is AuthUiState.Loading) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                    } else {
                        Text("VERIFY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                if (uiState is AuthUiState.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = (uiState as AuthUiState.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ==========================================
// PROFILE DRAWER
// ==========================================
@Composable
fun ProfileDrawerContent(myInfoData: MyInfoData?, onCloseDrawer: () -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF1E1E1E),
        drawerContentColor = Color.White,
        modifier = Modifier.width(340.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .systemBarsPadding()
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (myInfoData != null) {
                myInfoData.displayName.takeIf { !it.isNullOrBlank() }?.let { ProfileItem(label = "Name", value = it) }
                myInfoData.mobileNumber.takeIf { !it.isNullOrBlank() }?.let { ProfileItem(label = "Mobile", value = it) }
                myInfoData.email.takeIf { !it.isNullOrBlank() }?.let { ProfileItem(label = "Email", value = it) }
                myInfoData.state.takeIf { !it.isNullOrBlank() }?.let { ProfileItem(label = "State", value = it) }
                myInfoData.district.takeIf { !it.isNullOrBlank() }?.let { ProfileItem(label = "District", value = it) }
                myInfoData.pinCode.takeIf { !it.isNullOrBlank() }?.let { ProfileItem(label = "PIN Code", value = it) }
                myInfoData.address.takeIf { !it.isNullOrBlank() }?.let { ProfileItem(label = "Address", value = it) }
                myInfoData.rsaTel.takeIf { !it.isNullOrBlank() }?.let { ProfileItem(label = "RSA Telephone", value = it) }
                myInfoData.callTel.takeIf { !it.isNullOrBlank() }?.let { ProfileItem(label = "Customer Care", value = it) }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (!myInfoData.vehicleList.isNullOrEmpty()) {
                    Text(
                        text = "Vehicles",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    myInfoData.vehicleList.forEach { vehicle ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = vehicle.vehicleName ?: vehicle.modelName ?: "Unknown Vehicle",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                vehicle.make.takeIf { !it.isNullOrBlank() }?.let { VehicleItem("Make", it) }
                                vehicle.vinPlainText.takeIf { !it.isNullOrBlank() }?.let { VehicleItem("VIN", it) }
                                vehicle.registrationNumber.takeIf { !it.isNullOrBlank() }?.let { VehicleItem("Reg. No", it) }
                                vehicle.color.takeIf { !it.isNullOrBlank() }?.let { VehicleItem("Color", it) }
                                vehicle.variant.takeIf { !it.isNullOrBlank() }?.let { VehicleItem("Variant", it) }
                                vehicle.batterySpecification.takeIf { !it.isNullOrBlank() }?.let { VehicleItem("Battery", it) }
                                vehicle.motorPower.takeIf { !it.isNullOrBlank() }?.let { VehicleItem("Motor Power", it) }
                                vehicle.odometer?.let { VehicleItem("Odometer", "$it km") }
                                vehicle.purchaseDate.takeIf { !it.isNullOrBlank() }?.let { VehicleItem("Purchased", it) }
                                vehicle.originalWarrantyEndDate.takeIf { !it.isNullOrBlank() }?.let { VehicleItem("Warranty Ends", it) }
                                vehicle.insuranceExpiry.takeIf { !it.isNullOrBlank() }?.let { VehicleItem("Insurance Expires", it) }
                                vehicle.primaryUserName.takeIf { !it.isNullOrBlank() }?.let { VehicleItem("Primary User", it) }
                            }
                        }
                    }
                }
            } else {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(text = label, color = Color(0x80FFFFFF), fontSize = 12.sp)
        Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun VehicleItem(label: String, value: String) {
    Row(
        modifier = Modifier.padding(bottom = 4.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xD9FFFFFF), fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(text = value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.5f))
    }
}

// ==========================================
// VEHICLE DASHBOARD SCREENS
// ==========================================
@Composable
fun VehicleScreen(viewModel: VehicleViewModel, onOpenDrawer: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 24.dp)
        ) {
            IconButton(onClick = onOpenDrawer) {
                Text("☰", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Battery Health Diagnostics",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = Color.White),
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            )
            LikeSection(viewModel)
        }

        when (val state = uiState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is UiState.Success -> {
                VehicleDataDashboard(data = state.data)
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.fetchData() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleDataDashboard(data: VehicleDataModel) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { DataCard(title = "Battery SOH", value = "${data.hvBattSOH}%") }
        item { DataCard(title = "Battery Efficiency", value = "${data.hvBattEffy}%") }
        item { DataCard(title = "Drive Energy", value = "${formatLargeEnergy(data.energyConsumptionDrive)} kWh") }
        item { DataCard(title = "HVAC Energy", value = "${formatLargeEnergy(data.energyConsumptionHVAC)} kWh") }
        item { DataCard(title = "AUX Energy", value = "${formatLargeEnergy(data.energyConsumptionAUX)} kWh") }
        item { DataCard(title = "Batt Care Energy", value = "${formatLargeEnergy(data.energyConsumptionBattCare)} kWh") }
        item { DataCard(title = "Regenerated", value = "${formatLargeEnergy(data.energyRegnted)} kWh") }
        item { DataCard(title = "Total Out", value = "${formatLargeEnergy(data.totalEnergyConsumption)} kWh") }
        item { DataCard(title = "AC Charges", value = "${data.chargeCountAC}") }
        item { DataCard(title = "DC Charges", value = "${data.chargeCountDC}") }
        item { DataCard(title = "Total Charges", value = "${data.totalChrgCount}") }
        item { DataCard(title = "AC Energy In", value = "${formatSmallEnergy(data.energyInputAC)} kWh") }
        item { DataCard(title = "DC Energy In", value = "${formatSmallEnergy(data.energyInputDC)} kWh") }
        item { DataCard(title = "Total In", value = "${formatSmallEnergy(data.totalEnergyInput)} kWh") }
    }
}

@Composable
fun DataCard(title: String, value: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x33FFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xD9FFFFFF), fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            )
        }
    }
}

fun formatLargeEnergy(value: Double): String = String.format("%.2f", value / 1_000_000.0)
fun formatSmallEnergy(value: Double): String = String.format("%.1f", value)

@Composable
fun LikeSection(viewModel: VehicleViewModel) {
    val likes by viewModel.likesCount.collectAsState()
    val hasLiked by viewModel.hasLiked.collectAsState()
    
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f)
    )

    val buttonAlpha by animateFloatAsState(
        targetValue = if (hasLiked) 0f else 1f,
        animationSpec = tween(durationMillis = 2500, easing = LinearOutSlowInEasing)
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (hasLiked) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, delayMillis = 1500)
    )

    Box(contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(buttonAlpha)
        ) {
            Button(
                onClick = { if (!hasLiked) viewModel.postLike() },
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x4DFFFFFF)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.scale(scale),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (likes > 0) "❤️ $likes Likes" else "❤️ 1 Like",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Text(
                text = "1 Like = 1 User",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xB3FFFFFF), fontSize = 10.sp),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Text(
            text = "You liked this! ❤️",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xD9FFFFFF), fontWeight = FontWeight.SemiBold),
            modifier = Modifier.alpha(textAlpha)
        )
    }
}
