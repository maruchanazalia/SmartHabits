package com.example.events.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.events.data.api.HabitsService
import com.example.events.data.api.UserService
import com.example.events.data.model.Habit
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import java.io.File
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.core.content.FileProvider
import coil.compose.rememberImagePainter
import com.example.events.data.api.AuthService
import com.example.events.data.model.HabitItem
import com.example.events.data.model.HabitList
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.events.data.model.Location


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    habitsService: HabitsService,
    userService: UserService,
    onNavigateToHabits: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var habits by remember { mutableStateOf<List<Habit>>(emptyList()) }

    var showCreateHabitDialog by remember {
        mutableStateOf(false)
    }

    var selectedHabit by remember { mutableStateOf<Habit?>(null) }
    var showEditItemDialog = remember { mutableStateOf(false) }

    Button(onClick = { onNavigateToHabits?.invoke() }) {
        Text("Ir a Hábitos")
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            habits = habitsService.fetchHabits()
        }
    }

    fun updateHabit(updatedHabit: Habit) {
        habits = habits.map { habit ->
            if (habit.id == updatedHabit.id) {
                updatedHabit
            } else {
                habit
            }
        }
    }

    fun deleteHabit(habitId: Int) {
        habits = habits.filter { it.id != habitId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Hábitos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateHabitDialog = true }) {
                Icon(Icons.Filled.Add, "Crear Hábito")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            items(habits) { habit ->
                HabitCard(
                    habit = habit,
                    onHabitModified = { updatedHabit ->
                        updateHabit(updatedHabit)
                    },
                    onDeleteHabit = { habitToDelete ->
                        deleteHabit(habitToDelete.id)
                    },
                    habitsService = habitsService,
                    userService = userService,
                    authService = AuthService(),
                    onShowEditItemDialog = {
                        showEditItemDialog.value = true
                        selectedHabit = habit
                    }
                )
            }
        }

        if (showCreateHabitDialog) {
            CreateHabitDialog(
                onDismissRequest = { showCreateHabitDialog = false },
                onHabitCreated = { newHabit ->
                    habits = habits + newHabit
                    showCreateHabitDialog = false
                },
                habitsService = habitsService,
                userService = userService,
                authService = AuthService()
            )
        }

        if (showEditItemDialog.value && selectedHabit != null) {
            AddItemDialog(
                eventId = selectedHabit!!.id,
                onDismissRequest = {
                    showEditItemDialog.value = false
                    selectedHabit = null
                },
                onItemAdded = { newItem ->
                    val updatedHabit = selectedHabit!!.copy(
                        habitList = selectedHabit!!.habitList.copy(
                            items = selectedHabit!!.habitList.items + newItem
                        )
                    )
                    updateHabit(updatedHabit)
                    showEditItemDialog.value = false
                    selectedHabit = null
                },
                habitsService = habitsService,
                userService = userService
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CreateHabitDialog(
    onDismissRequest: () -> Unit,
    onHabitCreated: (Habit) -> Unit,
    habitsService: HabitsService,
    userService: UserService,
    authService: AuthService
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var organizerId by remember { mutableStateOf("1") }
    var participantIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var allUsers by remember { mutableStateOf<List<com.example.events.data.model.User>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var recordedAudioUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    var showParticipantDialog by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var showCameraPermissionRationale by remember { mutableStateOf(false) }
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    var showAudioPermissionRationale by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            allUsers = userService.fetchAllUsers()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            selectedImageUris = uris
        }
    )

    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempImageUri != null) {
                selectedImageUris = selectedImageUris + tempImageUri!!
            }
        }
    )

    fun createImageUri(context: Context): Uri {
        val imagesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HabitsImages")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val image = File(imagesDir, "habit_image_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            image
        )
    }

    fun launchCamera() {
        if (cameraPermissionState.status.isGranted) {
            tempImageUri = createImageUri(context)
            cameraLauncher.launch(tempImageUri!!)
        } else {
            if (cameraPermissionState.status.shouldShowRationale) {
                showCameraPermissionRationale = true
            } else {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    val recorder = remember { MediaRecorder() }
    var audioFilePath by remember { mutableStateOf("") }

    fun startRecording() {
        if (audioPermissionState.status.isGranted) {
            val audioDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "HabitsAudio")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
            val audioFile = File(audioDir, "habit_audio_${System.currentTimeMillis()}.mp3")
            audioFilePath = audioFile.absolutePath

            try {
                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFilePath)
                    prepare()
                    start()
                    isRecording = true
                }
            } catch (e: Exception) {
                Log.e("AudioRecord", "prepare() failed: ${e.message}")
                recorder.reset()
                recorder.release()
                isRecording = false
            }
        } else {
            if (audioPermissionState.status.shouldShowRationale) {
                showAudioPermissionRationale = true
            } else {
                audioPermissionState.launchPermissionRequest()
            }
        }
    }

    fun stopRecording() {
        recorder.apply {
            if (isRecording) {
                try {
                    stop()
                    release()
                } catch (e: Exception) {
                    Log.e("AudioRecord", "stop() failed: ${e.message}")
                } finally {
                    isRecording = false
                    recordedAudioUri = Uri.fromFile(File(audioFilePath))
                }
            }
        }
    }

    if (showAudioPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showAudioPermissionRationale = false },
            title = { Text("Permiso de micrófono necesario") },
            text = { Text("Necesitamos permiso para acceder al micrófono para que puedas grabar notas de audio para el hábito.") },
            confirmButton = {
                Button(onClick = {
                    showAudioPermissionRationale = false
                    audioPermissionState.launchPermissionRequest()
                }) {
                    Text("Entendido")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioPermissionRationale = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Crear Nuevo Hábito",
                    style = MaterialTheme.typography.headlineSmall
                )
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Hábito") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ubicación") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Hora (HH:MM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = organizerId,
                    onValueChange = { organizerId = it },
                    label = { Text("ID del Organizador") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { showParticipantDialog = true }) {
                    Text("Seleccionar Participantes")
                }
                if (participantIds.isNotEmpty()) {
                    Text("Participantes seleccionados: ${participantIds.joinToString(", ")}")
                }

                Text(text = "Imágenes Adjuntas")
                Row {
                    Button(onClick = {
                        imagePickerLauncher.launch(arrayOf("image/*"))
                    }) {
                        Text("Seleccionar Imágenes")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        launchCamera()
                    }) {
                        Text("Tomar Foto")
                    }
                }
                LazyRow {
                    items(selectedImageUris) { uri ->
                        Image(
                            painter = rememberImagePainter(data = uri),
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier
                                .size(80.dp)
                                .padding(4.dp)
                        )
                    }
                }

                Text(text = "Nota de Audio")
                Row {
                    Button(onClick = {
                        if (isRecording) {
                            stopRecording()
                        } else {
                            startRecording()
                        }
                    }) {
                        Text(if (isRecording) "Detener Grabación" else "Grabar Audio")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (recordedAudioUri != null) {
                        Text("Audio grabado: ${recordedAudioUri?.lastPathSegment}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val newHabit = Habit(
                            id = 0,
                            name = name,
                            description = description,
                            location = Location(0, 0.0, 0.0, location),
                            daysOfWeek = emptyList(),
                            timeOfDay = time,
                            audioNote = null,
                            images = emptyList(),
                            createdAt = "",
                            updatedAt = "",
                            frequency = "Daily",
                            habitList = HabitList(0, "Lista de Hábitos", null, emptyList()) // Proporciona un valor para habitList (puedes ajustarlo)
                        )

                        coroutineScope.launch {
                            val habitId = habitsService.createHabit(newHabit)

                            if (habitId != null) {
                                selectedImageUris.forEach { uri ->
                                    habitsService.uploadImage(habitId, uri, context)
                                }
                                if (recordedAudioUri != null) {
                                    habitsService.uploadAudio(habitId, recordedAudioUri!!)
                                }
                                onHabitCreated(newHabit.copy(id = habitId))
                                onDismissRequest()
                            } else {
                                println("Error al crear el hábito")
                            }
                        }
                    }) {
                        Text("Crear Hábito")
                    }
                }
            }
        }
    }

    if (showParticipantDialog) {
        AlertDialog(
            onDismissRequest = { showParticipantDialog = false },
            title = { Text("Seleccionar Participantes") },
            text = {
                Column {
                    allUsers.forEach { user ->
                        var isChecked by remember { mutableStateOf(participantIds.contains(user.id)) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    isChecked = !isChecked
                                    if (isChecked) {
                                        participantIds = participantIds + user.id
                                    } else {
                                        participantIds = participantIds - user.id
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null
                            )
                            Text("${user.firstName} ${user.lastName} (ID: ${user.id})")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showParticipantDialog = false }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showParticipantDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showCameraPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionRationale = false },
            title = { Text("Permiso de cámara necesario") },
            text = { Text("Necesitamos permiso para acceder a la cámara para que puedas tomar fotos para el hábito.") },
            confirmButton = {
                Button(onClick = {
                    showCameraPermissionRationale = false
                    cameraPermissionState.launchPermissionRequest()
                }) {
                    Text("Entendido")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionRationale = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AudioPlayerComponent(audioUrl: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(audioUrl)))
            prepare()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        isPlaying = false
                    }
                }
            })
        }
    }

    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray.copy(alpha = 0.1f))
            .padding(8.dp)
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    exoPlayer.pause()
                    isPlaying = false
                } else {
                    exoPlayer.play()
                    isPlaying = true
                }
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Nota de audio",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ItemListComponent(itemList: com.example.events.data.model.HabitList) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Text(
            text = itemList.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        itemList.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        itemList.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (item.status) {
                        "completed" -> Icons.Default.CheckCircle
                        "pending" -> Icons.Default.CheckCircle
                        else -> Icons.Default.Warning
                    },
                    contentDescription = "Estado del ítem",
                    tint = when (item.status) {
                        "completed" -> Color.Green
                        "pending" -> Color.Green
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = item.responsible.firstName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    eventId: Int,
    onDismissRequest: () -> Unit,
    onItemAdded: (HabitItem) -> Unit,
    habitsService: HabitsService,
    userService: UserService
) {
    var itemName by remember { mutableStateOf("") }
    var responsibleIdText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Añadir Nuevo Ítem",
                    style = MaterialTheme.typography.headlineSmall
                )
                TextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Nombre del Ítem") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = responsibleIdText,
                    onValueChange = { responsibleIdText = it },
                    label = { Text("ID del Responsable") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        coroutineScope.launch {
                            val responsibleId = responsibleIdText.toIntOrNull()
                            if (responsibleId != null) {
                                val newItem = habitsService.createItem(
                                    eventId = eventId,
                                    itemName = itemName,
                                    responsibleId = responsibleId
                                )

                                if (newItem != null) {
                                    onItemAdded(newItem)
                                    onDismissRequest()
                                } else {
                                    println("Error al crear el ítem")
                                }
                            } else {
                                println("ID del responsable inválido")
                            }
                        }
                    }) {
                        Text("Añadir Ítem")
                    }
                }
            }
        }
    }
}

@Composable
fun HabitCard(
    habit: Habit,
    onHabitModified: (Habit) -> Unit,
    habitsService: HabitsService,
    userService: UserService,
    authService: AuthService,
    onShowEditItemDialog: () -> Unit,
    onDeleteHabit: (Habit) -> Unit
) {
    var expandedState by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { expandedState = !expandedState }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = habit.name, style = MaterialTheme.typography.headlineSmall)
            if (expandedState) {
                Text(text = habit.description ?: "")

                Row {
                    Button(onClick = {
                        coroutineScope.launch {
                            habitsService.deleteHabit(habit.id)
                            onDeleteHabit(habit)
                        }
                    }) {
                        Text("Eliminar")
                    }
                    Button(onClick = {
                        onShowEditItemDialog()
                    }) {
                        Text("Añadir Item")
                    }
                }
            }
        }
    }
}