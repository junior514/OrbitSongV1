package com.sise.orbitsongv1.activities;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sise.orbitsongv1.R;
import com.sise.orbitsongv1.models.Song;
import com.sise.orbitsongv1.services.MusicPlayerService;
import com.sise.orbitsongv1.services.RetrofitClient;

import java.util.Collections;

public class MusicPlayerActivity extends AppCompatActivity implements MusicPlayerService.MusicPlayerListener {

    private static final String TAG = "MusicPlayerActivity";
    public static final String EXTRA_SONG = "extra_song";

    // UI Components
    private Toolbar toolbar;
    private ImageView ivAlbumArt;
    private TextView tvSongTitle;
    private TextView tvArtistName;
    private TextView tvAlbumName;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private SeekBar seekBarProgress;
    private FloatingActionButton fabPlayPause;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private ImageButton btnRepeat;
    private ImageButton btnShuffle;
    private ImageButton btnFavorite;
    private TextView tvPopularity;
    private View layoutPopularity;

    // Data
    private Song currentSong;
    private MusicPlayerService musicPlayer;
    private Handler progressHandler;
    private Runnable progressRunnable;
    private boolean isUserSeekingProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        initViews();
        setupToolbar();
        setupMusicPlayer();
        setupListeners();
        setupTestingButtons();

        loadSongFromIntent();
        checkAudioSettings();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        ivAlbumArt = findViewById(R.id.iv_album_art);
        tvSongTitle = findViewById(R.id.tv_song_title);
        tvArtistName = findViewById(R.id.tv_artist_name);
        tvAlbumName = findViewById(R.id.tv_album_name);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        seekBarProgress = findViewById(R.id.seekbar_progress);
        fabPlayPause = findViewById(R.id.fab_play_pause);
        btnPrevious = findViewById(R.id.btn_previous);
        btnNext = findViewById(R.id.btn_next);
        btnRepeat = findViewById(R.id.btn_repeat);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnFavorite = findViewById(R.id.btn_favorite);
        tvPopularity = findViewById(R.id.tv_popularity);
        layoutPopularity = findViewById(R.id.layout_popularity);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Reproductor");
        }
    }

    private void setupMusicPlayer() {
        musicPlayer = MusicPlayerService.getInstance();
        musicPlayer.initialize(this);
        musicPlayer.setListener(this);

        // Configurar handler para actualizar progreso
        progressHandler = new Handler(Looper.getMainLooper());
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicPlayer.isPlaying() && !isUserSeekingProgress) {
                    updateProgress();
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };

        Log.d(TAG, "✅ MusicPlayerService configurado en MusicPlayerActivity");
    }

    // ========================================
    // SETUP DE TESTING BUTTONS
    // ========================================

    private void setupTestingButtons() {
        // Test con long click en el botón de play
        fabPlayPause.setOnLongClickListener(v -> {
            Log.d(TAG, "🧪 Long click PLAY - Ejecutando test con URL conocida");
            musicPlayer.testWithKnownWorkingUrl();
            showToast("🧪 Testing con URL conocida - revisa logs");
            return true;
        });

        // Test de conectividad con long click en botón anterior
        btnPrevious.setOnLongClickListener(v -> {
            Log.d(TAG, "🧪 Long click ANTERIOR - Test de conectividad");
            musicPlayer.testBasicConnectivity();
            showToast("🧪 Testing conectividad - revisa logs");
            return true;
        });

        // Test de configuración de audio con long click en botón siguiente
        btnNext.setOnLongClickListener(v -> {
            Log.d(TAG, "🧪 Long click SIGUIENTE - Test configuración audio");
            musicPlayer.testDeviceAudioConfig();
            showToast("🧪 Testing audio config - revisa logs");
            return true;
        });

        // Test completo con long click en botón de favorito
        btnFavorite.setOnLongClickListener(v -> {
            Log.d(TAG, "🧪 Long click FAVORITO - Test completo");
            musicPlayer.runCompleteTest();
            showToast("🧪 Test completo iniciado - revisa logs");
            return true;
        });

        // Test de backend con long click en botón de repetir
        btnRepeat.setOnLongClickListener(v -> {
            Log.d(TAG, "🧪 Long click REPETIR - Test backend");
            testBackendPreviewStatus();
            showToast("🧪 Testing backend - revisa logs");
            return true;
        });

        // Test de shuffle con long click en botón de aleatorio
        btnShuffle.setOnLongClickListener(v -> {
            Log.d(TAG, "🧪 Long click SHUFFLE - Debug estado actual");
            debugMusicPlayerState();
            showToast("🧪 Debug estado - revisa logs");
            return true;
        });

        Log.d(TAG, "🧪 Testing buttons configurados:");
        Log.d(TAG, "   🎵 Long click PLAY = Test URL conocida");
        Log.d(TAG, "   🔙 Long click ANTERIOR = Test conectividad");
        Log.d(TAG, "   ▶️ Long click SIGUIENTE = Test audio config");
        Log.d(TAG, "   ❤️ Long click FAVORITO = Test completo");
        Log.d(TAG, "   🔄 Long click REPETIR = Test backend");
        Log.d(TAG, "   🔀 Long click SHUFFLE = Debug estado");
    }

    private void setupListeners() {
        // Play/Pause button
        fabPlayPause.setOnClickListener(v -> togglePlayPause());

        // Botones de navegación
        btnPrevious.setOnClickListener(v -> {
            showToast("Función de canción anterior no implementada aún");
        });

        btnNext.setOnClickListener(v -> {
            showToast("Función de canción siguiente no implementada aún");
        });

        // Botones de modo
        btnRepeat.setOnClickListener(v -> toggleRepeat());
        btnShuffle.setOnClickListener(v -> toggleShuffle());
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        // SeekBar
        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateCurrentTimeDisplay(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeekingProgress = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeekingProgress = false;
                int newPosition = seekBar.getProgress();

                // Solo seekTo si la canción tiene preview real
                if (currentSong != null && currentSong.hasPreview()) {
                    musicPlayer.seekTo(newPosition);
                    Log.d(TAG, "Usuario movió progreso a: " + newPosition + "ms");
                }
            }
        });
    }

    private void loadSongFromIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_SONG)) {
            currentSong = (Song) intent.getSerializableExtra(EXTRA_SONG);
            if (currentSong != null) {
                displaySongInfo(currentSong);
                playSong(currentSong);
            } else {
                Log.e(TAG, "❌ No se pudo obtener la canción del intent");
                showToast("Error: No se pudo cargar la información de la canción");
                finish();
            }
        } else {
            Log.e(TAG, "❌ Intent no contiene información de la canción");
            showToast("Error: No se especificó qué canción reproducir");
            finish();
        }
    }

    // ========================================
    // MÉTODOS DE TESTING DEL BACKEND
    // ========================================

    private void testBackendPreviewStatus() {
        Log.d(TAG, "🔍 === TESTING BACKEND PREVIEW STATUS ===");

        // Test 1: Health check del backend
        RetrofitClient.getInstance().getApiService().healthCheck()
                .enqueue(new retrofit2.Callback<Object>() {
                    @Override
                    public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "✅ Backend Health Check OK");
                            Log.d(TAG, "📊 Response: " + response.body());
                            showToast("✅ Backend OK");

                            // Si el backend responde, probar debug de preview
                            testBackendPreviewDebug();
                        } else {
                            Log.e(TAG, "❌ Backend Health Check falló: " + response.code());
                            showToast("❌ Backend error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<Object> call, Throwable t) {
                        Log.e(TAG, "❌ Backend no disponible", t);
                        showToast("❌ Backend no disponible: " + t.getMessage());
                    }
                });
    }

    private void testBackendPreviewDebug() {
        RetrofitClient.getInstance().getApiService().debugPreviewPublic()
                .enqueue(new retrofit2.Callback<Object>() {
                    @Override
                    public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "✅ Backend Preview Debug OK");
                            Log.d(TAG, "📊 Preview Status: " + response.body());
                            showToast("✅ Preview Debug OK - revisa logs");

                            // Test final: múltiples artistas
                            testBackendMultipleArtists();
                        } else {
                            Log.e(TAG, "❌ Preview Debug falló: " + response.code());
                            showToast("❌ Preview Debug error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<Object> call, Throwable t) {
                        Log.e(TAG, "❌ Preview Debug error", t);
                        showToast("❌ Preview Debug error: " + t.getMessage());
                    }
                });
    }

    private void testBackendMultipleArtists() {
        RetrofitClient.getInstance().getApiService().testMultipleArtists()
                .enqueue(new retrofit2.Callback<Object>() {
                    @Override
                    public void onResponse(retrofit2.Call<Object> call, retrofit2.Response<Object> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "✅ Multiple Artists Test OK");
                            Log.d(TAG, "📊 Artists Test: " + response.body());
                            showToast("✅ Artists Test OK - el backend funciona correctamente");
                        } else {
                            Log.e(TAG, "❌ Multiple Artists Test falló: " + response.code());
                            showToast("❌ Artists Test error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<Object> call, Throwable t) {
                        Log.e(TAG, "❌ Multiple Artists Test error", t);
                        showToast("❌ Artists Test error: " + t.getMessage());
                    }
                });
    }

    // ========================================
    // MÉTODOS DE AUDIO Y TESTING
    // ========================================

    private void checkAudioSettings() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            Log.d(TAG, "🔊 DEBUG Audio Completo:");
            Log.d(TAG, "   📊 Volumen actual: " + currentVolume + "/" + maxVolume);
            Log.d(TAG, "   🎵 Stream música activo: " + (currentVolume > 0));

            // Verificar porcentaje de volumen
            float volumePercent = maxVolume > 0 ? (float) currentVolume / maxVolume * 100 : 0;
            Log.d(TAG, "   📈 Porcentaje volumen: " + String.format("%.1f%%", volumePercent));

            // Alertas según el volumen
            if (currentVolume == 0) {
                showToast("⚠️ El volumen está silenciado. Sube el volumen para escuchar música.");
                Log.w(TAG, "⚠️ CRÍTICO: Volumen en 0, el usuario no escuchará nada");
            } else if (currentVolume < maxVolume * 0.3) {
                showToast("🔊 El volumen está bajo (" + currentVolume + "/" + maxVolume + "). Considera subirlo.");
                Log.w(TAG, "⚠️ Volumen bajo: " + currentVolume + "/" + maxVolume);
            } else {
                Log.d(TAG, "✅ Volumen adecuado: " + currentVolume + "/" + maxVolume);
            }

            // Verificar modo de sonido
            int ringerMode = audioManager.getRingerMode();
            switch (ringerMode) {
                case AudioManager.RINGER_MODE_SILENT:
                    Log.w(TAG, "⚠️ Dispositivo en modo silencioso");
                    showToast("⚠️ Dispositivo en modo silencioso");
                    break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    Log.w(TAG, "⚠️ Dispositivo en modo vibración");
                    showToast("⚠️ Dispositivo en modo vibración - sube el volumen");
                    break;
                case AudioManager.RINGER_MODE_NORMAL:
                    Log.d(TAG, "✅ Dispositivo en modo normal");
                    break;
            }

            // Verificar si hay auriculares conectados
            boolean isWiredHeadsetOn = audioManager.isWiredHeadsetOn();
            boolean isBluetoothA2dpOn = audioManager.isBluetoothA2dpOn();
            Log.d(TAG, "   🎧 Auriculares cableados: " + isWiredHeadsetOn);
            Log.d(TAG, "   📶 Bluetooth audio: " + isBluetoothA2dpOn);

            if (isWiredHeadsetOn) {
                showToast("🎧 Auriculares conectados - perfecto para música");
            } else if (isBluetoothA2dpOn) {
                showToast("📶 Audio Bluetooth conectado");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error verificando configuración de audio", e);
            showToast("❌ Error verificando audio: " + e.getMessage());
        }
    }

    private void debugMusicPlayerState() {
        Log.d(TAG, "🔍 === DEBUG ESTADO COMPLETO ===");
        Log.d(TAG, "MusicPlayer:");
        Log.d(TAG, "   ▶️ isPlaying: " + musicPlayer.isPlaying());
        Log.d(TAG, "   🎵 currentSong: " + (musicPlayer.getCurrentSong() != null ? musicPlayer.getCurrentSong().getNombre() : "NULL"));
        Log.d(TAG, "   ⏱️ position: " + musicPlayer.getCurrentPosition());
        Log.d(TAG, "   📏 duration: " + musicPlayer.getDuration());

        // Estado del sistema
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            Log.d(TAG, "Sistema Audio:");
            Log.d(TAG, "   🔊 Volumen: " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) +
                    "/" + audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            Log.d(TAG, "   📱 Modo: " + audioManager.getRingerMode());
            Log.d(TAG, "   🎧 Auriculares: " + audioManager.isWiredHeadsetOn());
            Log.d(TAG, "   📶 Bluetooth: " + audioManager.isBluetoothA2dpOn());
        }

        // Estado de la canción actual
        if (currentSong != null) {
            Log.d(TAG, "Canción Actual:");
            Log.d(TAG, "   📝 Nombre: " + currentSong.getNombre());
            Log.d(TAG, "   🎤 Artistas: " + currentSong.getArtistasString());
            Log.d(TAG, "   🔗 Preview URL: " + currentSong.getPreviewUrl());
            Log.d(TAG, "   ✅ Has Preview: " + currentSong.hasPreview());
            Log.d(TAG, "   ⏱️ Duración: " + currentSong.getDuracion());
        }

        Log.d(TAG, "=== FIN DEBUG ESTADO ===");
    }

    // Verificar accesibilidad de URL
    private void testUrlAccessibility(String url) {
        if (url == null || url.trim().isEmpty()) {
            Log.e(TAG, "❌ URL es null o vacía");
            return;
        }

        Log.d(TAG, "🌐 Verificando accesibilidad de URL: " + url);

        // Test en hilo separado para no bloquear UI
        new Thread(() -> {
            try {
                java.net.URL testUrl = new java.net.URL(url);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) testUrl.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                String contentType = connection.getContentType();
                long contentLength = connection.getContentLengthLong();

                connection.disconnect();

                runOnUiThread(() -> {
                    Log.d(TAG, "🌐 URL Response:");
                    Log.d(TAG, "   📊 Code: " + responseCode);
                    Log.d(TAG, "   📄 Type: " + contentType);
                    Log.d(TAG, "   📏 Length: " + contentLength + " bytes");

                    if (responseCode == 200) {
                        Log.d(TAG, "✅ URL accesible");
                        showToast("✅ URL accesible - Code: " + responseCode);
                    } else {
                        Log.w(TAG, "⚠️ URL respondió con código: " + responseCode);
                        showToast("⚠️ URL código: " + responseCode);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "❌ URL no accesible: " + e.getMessage());
                    showToast("❌ URL no accesible: " + e.getMessage());
                });
            }
        }).start();
    }

    // ========================================
    // MÉTODOS PRINCIPALES
    // ========================================

    private void displaySongInfo(Song song) {
        // Información básica
        tvSongTitle.setText(song.getNombre() != null ? song.getNombre() : "Canción sin nombre");
        tvArtistName.setText(song.getArtistasString());
        tvAlbumName.setText(song.getAlbum() != null ? song.getAlbum() : "Álbum desconocido");

        // Duración total
        if (song.getDuracion() != null && song.getDuracion() > 0) {
            tvTotalTime.setText(song.getDuracionFormatted());
            seekBarProgress.setMax(song.getDuracion());
        } else {
            tvTotalTime.setText("--:--");
            seekBarProgress.setMax(100);
        }

        // Tiempo actual inicialmente en 0
        tvCurrentTime.setText("0:00");
        seekBarProgress.setProgress(0);

        // Popularidad
        if (song.getPopularidad() != null && song.getPopularidad() > 0) {
            layoutPopularity.setVisibility(View.VISIBLE);
            tvPopularity.setText(String.format("%.0f%%", song.getPopularidad()));
        } else {
            layoutPopularity.setVisibility(View.GONE);
        }

        // Cargar imagen del álbum
        loadAlbumArt(song);

        // Actualizar título de la toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(song.getNombre());
        }

        Log.d(TAG, "✅ Información de la canción mostrada: " + song.getNombre());
    }

    private void loadAlbumArt(Song song) {
        if (song.getImagenUrl() != null && !song.getImagenUrl().trim().isEmpty()) {
            RequestOptions options = new RequestOptions()
                    .centerCrop()
                    .placeholder(R.drawable.gradient_album_placeholder)
                    .error(R.drawable.gradient_album_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);

            Glide.with(this)
                    .load(song.getImagenUrl())
                    .apply(options)
                    .into(ivAlbumArt);
        } else {
            ivAlbumArt.setImageResource(R.drawable.gradient_album_placeholder);
        }
    }

    // MÉTODO MEJORADO: playSong con debug completo y test de backend
    private void playSong(Song song) {
        Log.d(TAG, "🎵 === PLAYSONG MEJORADO CON BACKEND TEST ===");

        // Debug de la canción
        Log.d(TAG, "🔍 Song Debug:");
        Log.d(TAG, "   📝 Nombre: " + (song.getNombre() != null ? song.getNombre() : "NULL"));
        Log.d(TAG, "   🎤 Artistas: " + song.getArtistasString());
        Log.d(TAG, "   🔗 Preview URL: " + song.getPreviewUrl());
        Log.d(TAG, "   ✅ Has Preview: " + song.hasPreview());
        Log.d(TAG, "   🆔 ID: " + song.getId());

        if (!song.hasPreview()) {
            Log.w(TAG, "⚠️ Canción sin preview - probando backend");
            showToast("⚠️ Sin preview - probando backend...");

            // Si no tiene preview, probar el backend para verificar si hay problema general
            testBackendPreviewStatus();

            updatePlayPauseButton(true);
            simulateProgress();
            return;
        }

        String previewUrl = song.getPreviewUrl();

        // Debug exhaustivo de la URL
        Log.d(TAG, "🔍 URL Debug completo:");
        Log.d(TAG, "   🔗 URL completa: " + previewUrl);
        Log.d(TAG, "   📏 Longitud: " + (previewUrl != null ? previewUrl.length() : "NULL"));
        Log.d(TAG, "   🔒 Es HTTPS: " + (previewUrl != null && previewUrl.startsWith("https://")));
        Log.d(TAG, "   🎵 Es Spotify: " + (previewUrl != null && previewUrl.contains("scdn.co")));

        // Test de accesibilidad de URL
        if (previewUrl != null && !previewUrl.trim().isEmpty()) {
            testUrlAccessibility(previewUrl);
        }

        Log.d(TAG, "🎵 Enviando al MusicPlayerService...");
        musicPlayer.playSong(song);

        // Verificación después de 3 segundos
        new Handler().postDelayed(() -> {
            Log.d(TAG, "🔍 Verificación después de 3 segundos:");
            Log.d(TAG, "   ▶️ isPlaying: " + musicPlayer.isPlaying());
            Log.d(TAG, "   🎵 currentSong: " + (musicPlayer.getCurrentSong() != null ?
                    musicPlayer.getCurrentSong().getNombre() : "NULL"));

            if (!musicPlayer.isPlaying()) {
                Log.w(TAG, "⚠️ No está reproduciendo - ejecutando test de audio");
                musicPlayer.testDeviceAudioConfig();
                showToast("⚠️ Problema de audio detectado - revisa logs");
            }
        }, 3000);
    }

    private void togglePlayPause() {
        if (currentSong == null) {
            showToast("❌ No hay canción cargada");
            return;
        }

        // Si no tiene preview, simular toggle
        if (!currentSong.hasPreview()) {
            boolean isCurrentlyPlaying = fabPlayPause.getContentDescription().equals("Pausar");
            updatePlayPauseButton(!isCurrentlyPlaying);

            if (!isCurrentlyPlaying) {
                showToast("▶️ Simulando reproducción: " + currentSong.getNombre());
                simulateProgress();
            } else {
                showToast("⏸️ Simulando pausa");
                progressHandler.removeCallbacks(progressRunnable);
            }
            return;
        }

        // Funcionalidad normal para canciones con preview
        if (musicPlayer.isPlaying()) {
            musicPlayer.pause();
        } else if (musicPlayer.getCurrentSong() != null &&
                musicPlayer.getCurrentSong().getId() != null &&
                musicPlayer.getCurrentSong().getId().equals(currentSong.getId())) {
            musicPlayer.resume();
        } else {
            playSong(currentSong);
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (isPlaying) {
            fabPlayPause.setImageResource(R.drawable.ic_pause);
            fabPlayPause.setContentDescription("Pausar");
        } else {
            fabPlayPause.setImageResource(R.drawable.ic_play_arrow);
            fabPlayPause.setContentDescription("Reproducir");
        }
    }

    private void updateProgress() {
        if (musicPlayer.isPlaying()) {
            int currentPosition = musicPlayer.getCurrentPosition();
            int duration = musicPlayer.getDuration();

            seekBarProgress.setProgress(currentPosition);
            updateCurrentTimeDisplay(currentPosition);

            Log.d(TAG, "Progreso actualizado: " + currentPosition + "/" + duration + "ms");
        }
    }

    private void updateCurrentTimeDisplay(int currentPosition) {
        int minutes = currentPosition / 60000;
        int seconds = (currentPosition % 60000) / 1000;
        String timeString = String.format("%d:%02d", minutes, seconds);
        tvCurrentTime.setText(timeString);
    }

    private void simulateProgress() {
        if (currentSong != null && currentSong.getDuracion() != null) {
            progressHandler.post(new Runnable() {
                int simulatedPosition = 0;
                final int maxDuration = currentSong.getDuracion();

                @Override
                public void run() {
                    if (fabPlayPause.getContentDescription().equals("Pausar")) {
                        simulatedPosition += 1000; // Incrementar 1 segundo

                        if (simulatedPosition >= maxDuration) {
                            // Simular fin de canción
                            simulatedPosition = 0;
                            updatePlayPauseButton(false);
                            seekBarProgress.setProgress(0);
                            tvCurrentTime.setText("0:00");
                            showToast("✅ Demostración completada");
                            return;
                        }

                        // Actualizar UI
                        seekBarProgress.setProgress(simulatedPosition);
                        updateCurrentTimeDisplay(simulatedPosition);

                        // Continuar simulación
                        progressHandler.postDelayed(this, 1000);
                    }
                }
            });
        }
    }

    private void toggleRepeat() {
        showToast("Función de repetir no implementada aún");
    }

    private void toggleShuffle() {
        showToast("Función de aleatorio no implementada aún");
    }

    private void toggleFavorite() {
        showToast("Función de favoritos no implementada aún");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ========================================
    // MÉTODOS ADICIONALES DE TESTING
    // ========================================

    // Test directo con MediaPlayer básico
    private void testDirectMediaPlayer() {
        Log.d(TAG, "🧪 === TEST DIRECTO MEDIAPLAYER ===");

        String testUrl = "https://www.soundjay.com/misc/sounds/bell-ringing-05.mp3";

        MediaPlayer testPlayer = new MediaPlayer();

        try {
            Log.d(TAG, "🔧 Configurando MediaPlayer básico...");

            // Configuración mínima
            testPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );

            testPlayer.setDataSource(testUrl);

            testPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "✅ TEST DIRECTO: MediaPlayer preparado");
                try {
                    mp.start();
                    Log.d(TAG, "✅ TEST DIRECTO: Reproducción iniciada");
                    showToast("✅ Test directo: Audio funciona");

                    // Auto-detener después de 3 segundos
                    new Handler().postDelayed(() -> {
                        try {
                            if (mp.isPlaying()) {
                                mp.stop();
                            }
                            mp.release();
                            Log.d(TAG, "🧪 Test directo completado");
                        } catch (Exception e) {
                            Log.e(TAG, "Error limpiando test directo", e);
                        }
                    }, 3000);

                } catch (Exception e) {
                    Log.e(TAG, "❌ TEST DIRECTO: Error al iniciar", e);
                    showToast("❌ Test directo: Error al iniciar");
                    mp.release();
                }
            });

            testPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "❌ TEST DIRECTO: Error - what=" + what + ", extra=" + extra);
                showToast("❌ Test directo: Error " + what + "/" + extra);
                mp.release();
                return true;
            });

            Log.d(TAG, "🔄 Preparando MediaPlayer directo...");
            testPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "❌ TEST DIRECTO: Exception", e);
            showToast("❌ Test directo: Exception - " + e.getMessage());
            testPlayer.release();
        }
    }

    // Método para mostrar menú de testing completo
    private void showTestingMenu() {
        String[] testOptions = {
                "🧪 Test URL Conocida",
                "🔊 Test Configuración Audio",
                "🌐 Test Conectividad",
                "🎵 Test MediaPlayer Directo",
                "🔍 Test Backend Preview",
                "🎤 Test Múltiples Artistas",
                "📊 Debug Estado Completo",
                "🔄 Test Completo (Todo)"
        };

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("🧪 Menú de Testing");
        builder.setItems(testOptions, (dialog, which) -> {
            switch (which) {
                case 0:
                    musicPlayer.testWithKnownWorkingUrl();
                    showToast("🧪 Test URL conocida ejecutándose...");
                    break;
                case 1:
                    musicPlayer.testDeviceAudioConfig();
                    showToast("🔊 Test audio config - revisa logs");
                    break;
                case 2:
                    musicPlayer.testBasicConnectivity();
                    showToast("🌐 Test conectividad - revisa logs");
                    break;
                case 3:
                    testDirectMediaPlayer();
                    showToast("🎵 Test MediaPlayer directo ejecutándose...");
                    break;
                case 4:
                    testBackendPreviewStatus();
                    showToast("🔍 Test backend - revisa logs");
                    break;
                case 5:
                    testBackendMultipleArtists();
                    showToast("🎤 Test artistas - revisa logs");
                    break;
                case 6:
                    debugMusicPlayerState();
                    showToast("📊 Debug estado - revisa logs");
                    break;
                case 7:
                    runCompleteTestSequence();
                    showToast("🔄 Test completo iniciado - revisa logs");
                    break;
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // Secuencia de test completo
    private void runCompleteTestSequence() {
        Log.d(TAG, "🧪 === INICIANDO SECUENCIA DE TEST COMPLETO ===");

        // Test 1: Configuración de audio (inmediato)
        musicPlayer.testDeviceAudioConfig();

        // Test 2: Conectividad básica (1 segundo después)
        new Handler().postDelayed(() -> {
            musicPlayer.testBasicConnectivity();
        }, 1000);

        // Test 3: Backend status (2 segundos después)
        new Handler().postDelayed(() -> {
            testBackendPreviewStatus();
        }, 2000);

        // Test 4: URL conocida (4 segundos después)
        new Handler().postDelayed(() -> {
            musicPlayer.testWithKnownWorkingUrl();
        }, 4000);

        // Test 5: MediaPlayer directo (7 segundos después)
        new Handler().postDelayed(() -> {
            testDirectMediaPlayer();
        }, 7000);

        // Test 6: Debug estado final (10 segundos después)
        new Handler().postDelayed(() -> {
            debugMusicPlayerState();
            showToast("🧪 Secuencia de test completo finalizada");
        }, 10000);

        Log.d(TAG, "🧪 Secuencia programada - resultados en los próximos 10 segundos");
    }

    // Long click en la imagen del álbum para acceder al menú de testing
    private void setupAdvancedTesting() {
        if (ivAlbumArt != null) {
            ivAlbumArt.setOnLongClickListener(v -> {
                showTestingMenu();
                return true;
            });
            Log.d(TAG, "🧪 Testing avanzado: Long click en imagen del álbum");
        }
    }

    // ========================================
    // IMPLEMENTACIÓN DE MusicPlayerListener
    // ========================================

    @Override
    public void onPlaybackStarted(Song song) {
        runOnUiThread(() -> {
            Log.d(TAG, "✅ Reproducción iniciada: " + song.getNombre());
            updatePlayPauseButton(true);
            progressHandler.post(progressRunnable);
            showToast("▶️ Reproduciendo: " + song.getNombre());
        });
    }

    @Override
    public void onPlaybackPaused() {
        runOnUiThread(() -> {
            Log.d(TAG, "⏸️ Reproducción pausada");
            updatePlayPauseButton(false);
            progressHandler.removeCallbacks(progressRunnable);
            showToast("⏸️ Reproducción pausada");
        });
    }

    @Override
    public void onPlaybackStopped() {
        runOnUiThread(() -> {
            Log.d(TAG, "⏹️ Reproducción detenida");
            updatePlayPauseButton(false);
            progressHandler.removeCallbacks(progressRunnable);
            seekBarProgress.setProgress(0);
            tvCurrentTime.setText("0:00");
            showToast("⏹️ Reproducción detenida");
        });
    }

    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            Log.d(TAG, "✅ Reproducción completada");
            updatePlayPauseButton(false);
            progressHandler.removeCallbacks(progressRunnable);
            seekBarProgress.setProgress(0);
            tvCurrentTime.setText("0:00");
            showToast("✅ Reproducción completada");
        });
    }

    @Override
    public void onPlaybackError(String error) {
        runOnUiThread(() -> {
            Log.e(TAG, "❌ Error de reproducción: " + error);
            updatePlayPauseButton(false);
            progressHandler.removeCallbacks(progressRunnable);
            showToast("❌ Error: " + error);

            // Si hay error, ofrecer opciones de testing
            new Handler().postDelayed(() -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("❌ Error de Reproducción")
                        .setMessage("Se detectó un error. ¿Quieres ejecutar tests de diagnóstico?")
                        .setPositiveButton("SÍ, DIAGNOSTICAR", (d, w) -> showTestingMenu())
                        .setNegativeButton("Ahora no", null)
                        .show();
            }, 1000);
        });
    }

    @Override
    public void onProgressUpdate(int currentPosition, int duration) {
        // Este método se llama desde el servicio, pero preferimos manejar el progreso localmente
    }

    // ========================================
    // LIFECYCLE Y NAVIGATION
    // ========================================

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
        Log.d(TAG, "🔄 MusicPlayerActivity destruida");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            progressHandler.post(progressRunnable);
        }

        // Configurar testing avanzado al reanudar
        setupAdvancedTesting();
    }
}