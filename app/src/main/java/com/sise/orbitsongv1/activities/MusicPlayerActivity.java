package com.sise.orbitsongv1.activities;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
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
        loadSongFromIntent();

        // ✅ NUEVO: Verificar configuración de audio
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
    }

    private void setupListeners() {
        // Play/Pause button
        fabPlayPause.setOnClickListener(v -> togglePlayPause());

        // Botones de navegación
        btnPrevious.setOnClickListener(v -> {
            // TODO: Implementar canción anterior
            showToast("Función de canción anterior no implementada aún");
        });

        btnNext.setOnClickListener(v -> {
            // TODO: Implementar canción siguiente
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
                showToast("Error: No se pudo cargar la canción");
                finish();
            }
        } else {
            Log.e(TAG, "❌ Intent no contiene información de la canción");
            showToast("Error: No se especificó qué canción reproducir");
            finish();
        }
    }

    // ✅ MÉTODO NUEVO: Verificar configuración de audio
    private void checkAudioSettings() {
        try {
            // Verificar volumen del sistema
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

            Log.d(TAG, "🔊 DEBUG Audio:");
            Log.d(TAG, "   📊 Volumen actual: " + currentVolume + "/" + maxVolume);
            Log.d(TAG, "   🎵 Stream música activo: " + (currentVolume > 0));

            // Si el volumen está muy bajo, avisar al usuario
            if (currentVolume == 0) {
                showToast("⚠️ El volumen está silenciado. Sube el volumen para escuchar música.");
                Log.w(TAG, "⚠️ Volumen en 0, el usuario no escuchará nada");
            } else if (currentVolume < maxVolume * 0.3) {
                showToast("🔊 El volumen está bajo. Considera subirlo para mejor experiencia.");
                Log.w(TAG, "⚠️ Volumen bajo: " + currentVolume + "/" + maxVolume);
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
                    showToast("⚠️ Dispositivo en modo vibración");
                    break;
                case AudioManager.RINGER_MODE_NORMAL:
                    Log.d(TAG, "✅ Dispositivo en modo normal");
                    break;
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error verificando configuración de audio", e);
        }
    }

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

    // ✅ MÉTODO MEJORADO: playSong con debug completo
    private void playSong(Song song) {
        // 🔍 DEBUG: Mostrar información detallada
        Log.d(TAG, "🔍 DEBUG playSong - Información completa:");
        Log.d(TAG, "   📝 Nombre: " + song.getNombre());
        Log.d(TAG, "   🎤 Artistas: " + song.getArtistasString());
        Log.d(TAG, "   🔗 Preview URL: " + song.getPreviewUrl());
        Log.d(TAG, "   ✅ Has Preview: " + song.hasPreview());

        if (!song.hasPreview()) {
            showToast("⚠️ Esta canción no tiene preview, mostrando interfaz de demostración");
            Log.w(TAG, "⚠️ Canción sin preview: " + song.getNombre());

            updatePlayPauseButton(true);
            showToast("🎵 Modo demostración - Canción: " + song.getNombre());
            return;
        }

        // 🔍 DEBUG: Verificar URL de preview
        String previewUrl = song.getPreviewUrl();
        if (previewUrl != null) {
            Log.d(TAG, "🔍 DEBUG: Preview URL completa: " + previewUrl);

            // Verificar si es una URL válida
            if (previewUrl.startsWith("http://") || previewUrl.startsWith("https://")) {
                Log.d(TAG, "✅ Preview URL parece válida");
            } else {
                Log.w(TAG, "⚠️ Preview URL no parece válida: " + previewUrl);
            }
        }

        Log.d(TAG, "🎵 Enviando canción al MusicPlayerService...");
        musicPlayer.playSong(song);

        // 🔍 DEBUG: Verificar estado del reproductor después de llamar playSong
        Log.d(TAG, "🔍 DEBUG: MusicPlayer isPlaying después de playSong: " + musicPlayer.isPlaying());
    }

    // ✅ MÉTODO CORREGIDO: togglePlayPause - Manejar modo demostración
    private void togglePlayPause() {
        if (currentSong == null) {
            showToast("❌ No hay canción cargada");
            return;
        }

        // 🔧 AGREGADO: Si no tiene preview, simular toggle
        if (!currentSong.hasPreview()) {
            boolean isCurrentlyPlaying = fabPlayPause.getContentDescription().equals("Pausar");
            updatePlayPauseButton(!isCurrentlyPlaying);

            if (!isCurrentlyPlaying) {
                showToast("▶️ Simulando reproducción: " + currentSong.getNombre());
                // Opcional: Simular progreso para demo
                simulateProgress();
            } else {
                showToast("⏸️ Simulando pausa");
                // Detener simulación de progreso
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

    // ✅ MÉTODO NUEVO: Simular progreso para canciones sin preview
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
        // TODO: Implementar modo repetir
        showToast("Función de repetir no implementada aún");
    }

    private void toggleShuffle() {
        // TODO: Implementar modo aleatorio
        showToast("Función de aleatorio no implementada aún");
    }

    private void toggleFavorite() {
        // TODO: Implementar favoritos
        showToast("Función de favoritos no implementada aún");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
    }
}