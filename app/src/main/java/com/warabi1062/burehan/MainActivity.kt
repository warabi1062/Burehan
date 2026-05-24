package com.warabi1062.burehan

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.warabi1062.burehan.ui.theme.BurehanTheme
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OpenCVLoader.initLocal()
        enableEdgeToEdge()
        setContent {
            BurehanTheme {
                BurehanApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurehanApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("ブレハン") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (!hasPermission) {
                PermissionRequest { permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES) }
            } else {
                if (state.isScanning) {
                    ScanProgress(state.scannedCount, state.totalCount)
                }
                if (state.photos.isEmpty() && !state.isScanning) {
                    StartScanPrompt { viewModel.startScan() }
                } else {
                    PhotoGrid(state.photos)
                }
            }
        }
    }
}

@Composable
private fun PermissionRequest(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "写真へのアクセス権限が必要です",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Button(onClick = onRequest) {
            Text("権限を許可する")
        }
    }
}

@Composable
private fun StartScanPrompt(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "端末内の写真をスキャンして\nシャープネスを判定します",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Button(onClick = onStart) {
            Text("スキャン開始")
        }
    }
}

@Composable
private fun ScanProgress(scanned: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            "スキャン中… $scanned / $total",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        LinearProgressIndicator(
            progress = { if (total > 0) scanned.toFloat() / total else 0f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PhotoGrid(photos: List<PhotoItem>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(photos, key = { it.uri.toString() }) { photo ->
            PhotoThumbnail(photo)
        }
    }
}

@Composable
private fun PhotoThumbnail(photo: PhotoItem) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = "${photo.score}",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .background(
                    color = scoreColor(photo.score).copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun scoreColor(score: Int): Color = when {
    score <= 30 -> Color(0xFFE53935)
    score <= 60 -> Color(0xFFFFA726)
    else -> Color(0xFF43A047)
}
