package com.example.simple_agent_android.ui

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simple_agent_android.ui.theme.*
import com.example.simple_agent_android.data.FeedbackCategory
import com.example.simple_agent_android.data.FeedbackRequest
import com.example.simple_agent_android.network.FeedbackApi
import com.example.simple_agent_android.utils.DeviceInfoUtils
import com.example.simple_agent_android.utils.ImagePickerUtils
import com.example.simple_agent_android.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val feedbackApi = remember { FeedbackApi() }
    
    // Get state from ViewModel (persisted across navigation)
    val userName = viewModel.feedbackUserName.value
    val feedbackText = viewModel.feedbackText.value
    val selectedCategoryName = viewModel.feedbackCategory.value
    val screenshots = viewModel.feedbackScreenshots.value
    
    // Convert category name back to enum
    val selectedCategory = FeedbackCategory.values().find { it.displayName == selectedCategoryName } ?: FeedbackCategory.BUG
    
    // Local state for UI only
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var submitStatus by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64Image = ImagePickerUtils.convertImageToBase64(context, it)
            base64Image?.let { screenshot ->
                viewModel.addFeedbackScreenshot(screenshot)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReagentBlack)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        // Header Section
        Text(
            text = "Send Feedback",
            style = MaterialTheme.typography.headlineMedium,
            color = ReagentWhite,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Help us improve Simple Agent",
            style = MaterialTheme.typography.bodyMedium,
            color = ReagentGray,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // User Information Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = ReagentDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ReagentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                OutlinedTextField(
                    value = userName,
                    onValueChange = viewModel::updateFeedbackUserName,
                    label = { 
                        Text(
                            "Your Name",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    placeholder = {
                        Text(
                            "Enter your name",
                            color = ReagentGray.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ReagentBlue,
                        unfocusedBorderColor = ReagentGray,
                        cursorColor = ReagentBlue,
                        focusedLabelColor = ReagentBlue,
                        unfocusedLabelColor = ReagentGray
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Feedback Content Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = ReagentDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Feedback,
                        contentDescription = null,
                        tint = ReagentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Feedback Details",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ReagentGreen,
                            unfocusedBorderColor = ReagentGray,
                            focusedLabelColor = ReagentGreen,
                            unfocusedLabelColor = ReagentGray
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(ReagentDark)
                    ) {
                        FeedbackCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        category.displayName,
                                        color = ReagentWhite
                                    ) 
                                },
                                onClick = {
                                    viewModel.updateFeedbackCategory(category.displayName)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Feedback Text
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = viewModel::updateFeedbackText,
                    label = { 
                        Text(
                            "Your Feedback",
                            style = MaterialTheme.typography.bodyMedium
                        ) 
                    },
                    placeholder = {
                        Text(
                            "Describe your issue, suggestion, or feedback...",
                            color = ReagentGray.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 4,
                    maxLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ReagentGreen,
                        unfocusedBorderColor = ReagentGray,
                        cursorColor = ReagentGreen,
                        focusedLabelColor = ReagentGreen,
                        unfocusedLabelColor = ReagentGray
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Screenshots Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = ReagentDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = ReagentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Screenshots (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = "Select screenshots from your gallery to help us understand your feedback better",
                    style = MaterialTheme.typography.bodySmall,
                    color = ReagentGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Button(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ReagentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Select Screenshot",
                        color = ReagentWhite,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (screenshots.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Selected Screenshots:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ReagentWhite,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    screenshots.forEachIndexed { index, _ ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = ReagentBlack),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Screenshot ${index + 1}",
                                    color = ReagentGray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                IconButton(
                                    onClick = { viewModel.removeFeedbackScreenshot(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove screenshot",
                                        tint = ReagentStatusOffline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Submit Button
        Button(
            onClick = {
                if (userName.isBlank() || feedbackText.isBlank()) {
                    submitStatus = "Please fill in all required fields"
                    isSuccess = false
                    return@Button
                }
                
                scope.launch {
                    isLoading = true
                    submitStatus = null
                    
                    val feedbackRequest = FeedbackRequest(
                        user_name = userName,
                        feedback = feedbackText,
                        category = selectedCategory.displayName,
                        app_version = DeviceInfoUtils.getAppVersion(context),
                        device_info = DeviceInfoUtils.getDeviceInfo(context),
                        screenshots = screenshots
                    )
                    
                    val result = feedbackApi.sendFeedback(feedbackRequest)
                    
                    isLoading = false
                    
                    result.fold(
                        onSuccess = { message ->
                            submitStatus = message
                            isSuccess = true
                            // Clear form on success
                            viewModel.clearFeedbackForm()
                        },
                        onFailure = { error ->
                            submitStatus = "Failed to send feedback: ${error.message}"
                            isSuccess = false
                        }
                    )
                }
            },
            enabled = !isLoading && userName.isNotBlank() && feedbackText.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ReagentGreen,
                disabledContainerColor = ReagentGray.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = ReagentBlack,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sending...", color = ReagentBlack, fontWeight = FontWeight.Medium)
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = ReagentBlack
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Send Feedback",
                    color = ReagentBlack,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        // Status Message
        submitStatus?.let { status ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSuccess) ReagentGreen.copy(alpha = 0.1f) else ReagentStatusOffline.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isSuccess) ReagentGreen else ReagentStatusOffline,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = status,
                        color = if (isSuccess) ReagentGreen else ReagentStatusOffline,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
} 