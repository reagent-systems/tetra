package com.example.simple_agent_android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.simple_agent_android.R
import com.example.simple_agent_android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SidebarDrawer(
    drawerOpen: Boolean,
    onDrawerOpen: () -> Unit,
    onDrawerClose: () -> Unit,
    selectedScreen: String,
    onSelectScreen: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Sync drawerOpen prop with drawerState
    LaunchedEffect(drawerOpen) {
        if (drawerOpen && !drawerState.isOpen) drawerState.open()
        else if (!drawerOpen && drawerState.isOpen) drawerState.close()
    }
    ModalNavigationDrawer(
        drawerContent = {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(260.dp)
                    .background(ReagentBlack)
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.Start
            ) {
                DrawerItem(
                    label = stringResource(R.string.nav_home),
                    selected = selectedScreen == "home",
                    onClick = { onSelectScreen("home"); onDrawerClose() }
                )
                DrawerItem(
                    label = stringResource(R.string.nav_debug),
                    selected = selectedScreen == "debug",
                    onClick = { onSelectScreen("debug"); onDrawerClose() }
                )
                DrawerItem(
                    label = stringResource(R.string.nav_settings),
                    selected = selectedScreen == "settings",
                    onClick = { onSelectScreen("settings"); onDrawerClose() }
                )
                DrawerItem(
                    label = stringResource(R.string.nav_feedback),
                    selected = selectedScreen == "feedback",
                    onClick = { onSelectScreen("feedback"); onDrawerClose() }
                )
                DrawerItem(
                    label = stringResource(R.string.nav_about),
                    selected = selectedScreen == "about",
                    onClick = { onSelectScreen("about"); onDrawerClose() }
                )
            }
        },
        drawerState = drawerState,
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar with hamburger
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReagentBlack)
                        .height(56.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    IconButton(
                        onClick = {
                            if (!drawerState.isOpen) {
                                scope.launch { drawerState.open() }
                                onDrawerOpen()
                            } else {
                                scope.launch { drawerState.close() }
                                onDrawerClose()
                            }
                        },
                        modifier = Modifier.padding(start = 8.dp, top = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.sidebar_open),
                            tint = ReagentGreen
                        )
                    }
                }
                // Main content below top bar
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    content()
                }
            }
        }
    )
}

@Composable
fun DrawerItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) ReagentGreen else ReagentWhite
    Text(
        text = label,
        color = color,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 24.dp)
            .clickable { onClick() },
        style = MaterialTheme.typography.titleMedium
    )
} 