package com.ironlogic.veyra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ironlogic.veyra.ui.theme.VeyraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VeyraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VeyraHome()
                }
            }
        }
    }
}

@Composable
private fun VeyraHome() {
    val isTabletLandscape = LocalConfiguration.current.screenWidthDp >= 600

    if (isTabletLandscape) {
        TabletLibraryShell()
    } else {
        PhoneLibraryShell()
    }
}

@Composable
private fun PhoneLibraryShell() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.background)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Header()
        ContinueWatchingCard(modifier = Modifier.fillMaxWidth())
        SectionTitle("Your library")
        EmptyLibraryCard(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.weight(1f))
        FloatingDock()
    }
}

@Composable
private fun TabletLibraryShell() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(VeyraColors.background)
            .padding(28.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Column(
            modifier = Modifier.width(230.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "VEYRA",
                color = VeyraColors.accent,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
            )
            Text("Library", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("History", color = VeyraColors.muted)
            Text("Settings", color = VeyraColors.muted)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header()
            ContinueWatchingCard(modifier = Modifier.fillMaxWidth())
            SectionTitle("Your library")
            EmptyLibraryCard(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Good evening",
            color = VeyraColors.muted,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = "Stay with your stories.",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ContinueWatchingCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(176.dp)
            .background(VeyraColors.surface, RoundedCornerShape(24.dp))
            .padding(20.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("CONTINUE WATCHING", color = VeyraColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Your next memory will appear here.", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Veyra remembers where you stopped.", color = VeyraColors.muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyLibraryCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(160.dp)
            .background(VeyraColors.surface.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Your library is quiet.", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text("Grant video access to bring your local collection into Veyra.", color = VeyraColors.muted)
        }
    }
}

@Composable
private fun FloatingDock() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeyraColors.surface, RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Text("Library", color = VeyraColors.accent, fontWeight = FontWeight.SemiBold)
        Text("History", color = VeyraColors.muted)
        Text("Settings", color = VeyraColors.muted)
    }
}

private object VeyraColors {
    val background = Color(0xFF111014)
    val surface = Color(0xFF211E25)
    val muted = Color(0xFFA9A2AE)
    val accent = Color(0xFFFF9E5E)
}
