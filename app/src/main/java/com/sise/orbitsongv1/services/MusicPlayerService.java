package com.sise.orbitsongv1.services;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import com.sise.orbitsongv1.models.Song;

import java.io.IOException;

public class MusicPlayerService {
    private static final String TAG = "MusicPlayerService";
    private static MusicPlayerService instance;
    private MediaPlayer mediaPlayer;
    private Song currentSong;
    private boolean isPlaying = false;
    private MusicPlayerListener listener;

    // ✅ NUEVOS: Gestión de Audio Focus
    private Context context;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private PowerManager.WakeLock wakeLock;

    public interface MusicPlayerListener {
        void onPlaybackStarted(Song song);
        void onPlaybackPaused();
        void onPlaybackStopped();
        void onPlaybackCompleted();
        void onPlaybackError(String error);
        void onProgressUpdate(int currentPosition, int duration);
    }

    private MusicPlayerService() {
        // Constructor privado - no inicializar MediaPlayer aquí
    }

    public static synchronized MusicPlayerService getInstance() {
        if (instance == null) {
            instance = new MusicPlayerService();
        }
        return instance;
    }

    // ✅ NUEVO: Método para inicializar con contexto
    public void initialize(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        initializeMediaPlayer();
        setupAudioFocus();
        Log.d(TAG, "✅ MusicPlayerService inicializado con contexto");
    }

    public void setListener(MusicPlayerListener listener) {
        this.listener = listener;
    }

    // ✅ NUEVO: Configurar Audio Focus
    private void setupAudioFocus() {
        if (audioManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this::onAudioFocusChange)
                    .build();
        }
        Log.d(TAG, "✅ Audio Focus configurado");
    }

    // ✅ NUEVO: Manejar cambios de Audio Focus
    private void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "🔊 Audio Focus cambió: " + focusChange);
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d(TAG, "🔊 Audio Focus ganado - reanudando");
                if (!isPlaying && mediaPlayer != null) {
                    resume();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                Log.d(TAG, "🔊 Audio Focus perdido - pausando");
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                Log.d(TAG, "🔊 Audio Focus perdido temporalmente - pausando");
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.d(TAG, "🔊 Audio Focus perdido - bajando volumen");
                // Podrías bajar el volumen aquí si quisieras
                break;
        }
    }

    // ✅ NUEVO: Solicitar Audio Focus
    private boolean requestAudioFocus() {
        if (audioManager == null) {
            Log.w(TAG, "⚠️ AudioManager es null");
            return true; // Continuar sin audio focus
        }

        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                result = audioManager.requestAudioFocus(audioFocusRequest);
            } else {
                Log.w(TAG, "⚠️ AudioFocusRequest es null");
                return true;
            }
        } else {
            result = audioManager.requestAudioFocus(
                    this::onAudioFocusChange,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );
        }

        boolean granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        Log.d(TAG, "🔊 Audio Focus " + (granted ? "concedido" : "denegado"));
        return granted;
    }

    // ✅ MEJORADO: initializeMediaPlayer con mejor configuración
    private void initializeMediaPlayer() {
        Log.d(TAG, "🔧 Inicializando MediaPlayer...");

        if (mediaPlayer != null) {
            release();
        }

        try {
            mediaPlayer = new MediaPlayer();

            // ✅ CONFIGURACIÓN DE AUDIO MEJORADA
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build();
            mediaPlayer.setAudioAttributes(audioAttributes);

            // ✅ CONFIGURACIÓN ADICIONAL
            mediaPlayer.setScreenOnWhilePlaying(false);

            // ✅ WAKE LOCK para mantener CPU activa durante reproducción
            if (context != null) {
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OrbitSong::MusicPlayerWakeLock");
                }
            }

            // ✅ LISTENERS MEJORADOS CON MEJOR LOGGING
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "✅ MediaPlayer PREPARADO - Iniciando reproducción");
                try {
                    mp.start();
                    isPlaying = true;

                    // Activar wake lock
                    if (wakeLock != null && !wakeLock.isHeld()) {
                        wakeLock.acquire(10*60*1000L /*10 minutos*/);
                    }

                    if (listener != null) {
                        listener.onPlaybackStarted(currentSong);
                    }
                    Log.d(TAG, "✅ Reproducción INICIADA exitosamente");

                    // Debug de estado del MediaPlayer
                    logMediaPlayerState();

                } catch (Exception e) {
                    Log.e(TAG, "❌ Error al iniciar reproducción después de prepared", e);
                    if (listener != null) {
                        listener.onPlaybackError("Error al iniciar: " + e.getMessage());
                    }
                }
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "✅ Reproducción completada");
                isPlaying = false;
                releaseWakeLock();
                if (listener != null) {
                    listener.onPlaybackCompleted();
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                isPlaying = false;
                releaseWakeLock();
                String detailedError = getDetailedError(what, extra);
                Log.e(TAG, "❌ ERROR MediaPlayer: " + detailedError);
                if (listener != null) {
                    listener.onPlaybackError(detailedError);
                }
                return true;
            });

            mediaPlayer.setOnInfoListener((mp, what, extra) -> {
                Log.d(TAG, "ℹ️ MediaPlayer Info: what=" + what + ", extra=" + extra);
                return false;
            });

            Log.d(TAG, "✅ MediaPlayer inicializado correctamente");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al inicializar MediaPlayer", e);
            if (listener != null) {
                listener.onPlaybackError("Error de inicialización: " + e.getMessage());
            }
        }
    }

    // ✅ NUEVO: Logging del estado del MediaPlayer
    private void logMediaPlayerState() {
        if (mediaPlayer == null) return;

        try {
            Log.d(TAG, "📊 Estado MediaPlayer:");
            Log.d(TAG, "   🔄 isPlaying: " + mediaPlayer.isPlaying());
            Log.d(TAG, "   ⏱️ Duración: " + mediaPlayer.getDuration() + "ms");
            Log.d(TAG, "   📍 Posición: " + mediaPlayer.getCurrentPosition() + "ms");

            // Verificar volumen del sistema
            if (audioManager != null) {
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                Log.d(TAG, "   🔊 Volumen sistema: " + currentVolume + "/" + maxVolume);

                if (currentVolume == 0) {
                    Log.w(TAG, "⚠️ ¡VOLUMEN EN 0! - El usuario no escuchará nada");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "No se pudo obtener estado del MediaPlayer", e);
        }
    }

    // ✅ NUEVO: Obtener errores detallados
    private String getDetailedError(int what, int extra) {
        String error = "Error desconocido";

        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                error = "Servidor de medios falló";
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                error = "Error desconocido de medios";
                break;
        }

        switch (extra) {
            case MediaPlayer.MEDIA_ERROR_IO:
                error += " - Error de conexión/red";
                break;
            case MediaPlayer.MEDIA_ERROR_MALFORMED:
                error += " - Archivo corrupto";
                break;
            case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                error += " - Formato no soportado";
                break;
            case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                error += " - Conexión agotó tiempo";
                break;
        }

        return error + " (what=" + what + ", extra=" + extra + ")";
    }

    // ✅ MEJORADO: playSong con validaciones exhaustivas
    public void playSong(Song song) {
        Log.d(TAG, "🎵 === INICIANDO PLAYSONG ===");

        if (song == null) {
            Log.e(TAG, "❌ Canción es null");
            if (listener != null) {
                listener.onPlaybackError("Canción no válida");
            }
            return;
        }

        String previewUrl = song.getPreviewUrl();
        Log.d(TAG, "🔍 VALIDACIONES:");
        Log.d(TAG, "   📝 Canción: " + song.getNombre());
        Log.d(TAG, "   🎤 Artista: " + song.getArtistasString());
        Log.d(TAG, "   🔗 Preview URL: " + previewUrl);
        Log.d(TAG, "   📏 URL length: " + (previewUrl != null ? previewUrl.length() : "null"));
        Log.d(TAG, "   ✅ Has Preview: " + song.hasPreview());

        // ✅ VALIDACIÓN 1: URL no nula ni vacía
        if (previewUrl == null || previewUrl.trim().isEmpty()) {
            Log.e(TAG, "❌ Preview URL es null o vacía");
            if (listener != null) {
                listener.onPlaybackError("🚫 Esta canción no tiene preview disponible\n💡 Prueba con otra canción");
            }
            return;
        }

        // ✅ VALIDACIÓN 2: URL debe ser HTTPS
        if (!previewUrl.startsWith("https://")) {
            Log.e(TAG, "❌ Preview URL no es HTTPS: " + previewUrl);
            if (listener != null) {
                listener.onPlaybackError("🔒 URL de preview no es segura\n💡 Solo se permiten URLs HTTPS");
            }
            return;
        }

        // ✅ VALIDACIÓN 3: URL debe ser de Spotify
        if (!previewUrl.contains("scdn.co")) {
            Log.w(TAG, "⚠️ URL no parece ser de Spotify: " + previewUrl);
        }

        // ✅ VALIDACIÓN 4: Solicitar Audio Focus
        if (!requestAudioFocus()) {
            Log.w(TAG, "⚠️ No se pudo obtener Audio Focus, pero continuando...");
        }

        try {
            Log.d(TAG, "🎵 Configurando MediaPlayer...");

            // Detener cualquier reproducción actual
            stop();

            // Reinicializar MediaPlayer
            initializeMediaPlayer();

            currentSong = song;

            Log.d(TAG, "🔄 Configurando DataSource: " + previewUrl);
            mediaPlayer.setDataSource(previewUrl);

            Log.d(TAG, "🔄 Preparando MediaPlayer de forma asíncrona...");
            mediaPlayer.prepareAsync();

            Log.d(TAG, "✅ MediaPlayer configurado, esperando onPrepared...");

        } catch (IOException e) {
            Log.e(TAG, "❌ IOException al configurar MediaPlayer", e);
            Log.e(TAG, "   🔗 URL problemática: " + previewUrl);
            if (listener != null) {
                listener.onPlaybackError("❌ Error de conexión\n🔄 Verifica tu internet e intenta de nuevo");
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "❌ IllegalStateException en MediaPlayer", e);
            initializeMediaPlayer();
            if (listener != null) {
                listener.onPlaybackError("🔄 Error interno del reproductor\nIntenta de nuevo");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error inesperado al configurar MediaPlayer", e);
            if (listener != null) {
                listener.onPlaybackError("❌ Error inesperado: " + e.getMessage());
            }
        }
    }

    public void pause() {
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.pause();
                isPlaying = false;
                releaseWakeLock();
                if (listener != null) {
                    listener.onPlaybackPaused();
                }
                Log.d(TAG, "⏸️ Reproducción pausada");
            } catch (IllegalStateException e) {
                Log.e(TAG, "❌ Error al pausar", e);
            }
        }
    }

    public void resume() {
        if (mediaPlayer != null && !isPlaying) {
            try {
                if (requestAudioFocus()) {
                    mediaPlayer.start();
                    isPlaying = true;
                    acquireWakeLock();
                    if (listener != null) {
                        listener.onPlaybackStarted(currentSong);
                    }
                    Log.d(TAG, "▶️ Reproducción reanudada");
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "❌ Error al reanudar", e);
            }
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            try {
                if (isPlaying) {
                    mediaPlayer.stop();
                }
                isPlaying = false;
                currentSong = null;
                releaseWakeLock();
                abandonAudioFocus();
                if (listener != null) {
                    listener.onPlaybackStopped();
                }
                Log.d(TAG, "⏹️ Reproducción detenida");
            } catch (IllegalStateException e) {
                Log.e(TAG, "❌ Error al detener", e);
            }
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.seekTo(position);
                Log.d(TAG, "⏩ Seek a posición: " + position + "ms");
            } catch (IllegalStateException e) {
                Log.e(TAG, "❌ Error al hacer seek", e);
            }
        }
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPlaying) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                Log.e(TAG, "❌ Error al obtener posición actual", e);
            }
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getDuration();
            } catch (IllegalStateException e) {
                Log.e(TAG, "❌ Error al obtener duración", e);
            }
        }
        return 0;
    }

    public boolean isPlaying() {
        return isPlaying && mediaPlayer != null;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    // ✅ NUEVO: Gestión de Wake Lock
    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10*60*1000L /*10 minutos*/);
            Log.d(TAG, "🔋 Wake Lock adquirido");
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "🔋 Wake Lock liberado");
        }
    }

    // ✅ NUEVO: Abandonar Audio Focus
    private void abandonAudioFocus() {
        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                audioManager.abandonAudioFocus(this::onAudioFocusChange);
            }
            Log.d(TAG, "🔊 Audio Focus abandonado");
        }
    }

    public void release() {
        Log.d(TAG, "🧹 Liberando recursos del MediaPlayer...");

        if (mediaPlayer != null) {
            try {
                if (isPlaying) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
                isPlaying = false;
                currentSong = null;
            } catch (IllegalStateException e) {
                Log.e(TAG, "❌ Error al liberar MediaPlayer", e);
            }
        }

        releaseWakeLock();
        abandonAudioFocus();

        Log.d(TAG, "✅ Recursos liberados correctamente");
    }

    // ✅ NUEVO: Método de testing para URLs específicas
    public void testUrl(String testUrl) {
        Log.d(TAG, "🧪 TESTING URL: " + testUrl);

        MediaPlayer testPlayer = new MediaPlayer();
        try {
            testPlayer.setDataSource(testUrl);
            testPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "✅ TEST URL EXITOSO: " + testUrl);
                mp.start();

                // Detener después de 3 segundos
                new android.os.Handler().postDelayed(() -> {
                    try {
                        mp.stop();
                        mp.release();
                        Log.d(TAG, "🧪 Test completado y limpiado");
                    } catch (Exception e) {
                        Log.e(TAG, "Error limpiando test", e);
                    }
                }, 3000);
            });
            testPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "❌ TEST URL FALLÓ: " + testUrl + " - what=" + what + ", extra=" + extra);
                mp.release();
                return true;
            });
            testPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "❌ TEST URL EXCEPCIÓN: " + testUrl, e);
        }
    }
    // ✅ AGREGAR AL FINAL DE MusicPlayerService.java

    /**
     * 🧪 MÉTODO DE TESTING: Probar con URL conocida que funciona
     */
    public void testWithKnownWorkingUrl() {
        Log.d(TAG, "🧪 === TESTING CON URL CONOCIDA ===");

        // URL de testing que definitivamente funciona
        String testUrl = "https://www.soundjay.com/misc/sounds/bell-ringing-05.mp3";

        Song testSong = new Song();
        testSong.setId("test-audio");
        testSong.setNombre("🧪 Test Audio - Campana");
        testSong.setArtistas(java.util.Collections.singletonList("Sistema de Prueba"));
        testSong.setPreviewUrl(testUrl);
        testSong.setDuracion(5000); // 5 segundos

        Log.d(TAG, "🧪 Reproduciendo URL de prueba que funciona: " + testUrl);
        playSong(testSong);
    }

    /**
     * 🧪 MÉTODO DE TESTING: Probar conectividad básica
     */
    public void testBasicConnectivity() {
        Log.d(TAG, "🧪 === TEST CONECTIVIDAD BÁSICA ===");

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("https://www.google.com");
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                connection.disconnect();

                Log.d(TAG, "✅ Conectividad básica OK - Google responde: " + responseCode);

                // Ahora probar URL de Spotify típica
                testSpotifyConnectivity();

            } catch (Exception e) {
                Log.e(TAG, "❌ Test conectividad básica falló", e);
            }
        }).start();
    }

    private void testSpotifyConnectivity() {
        try {
            // URL típica de Spotify preview (ejemplo genérico)
            String spotifyTestUrl = "https://p.scdn.co/mp3-preview/test";
            java.net.URL url = new java.net.URL(spotifyTestUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            Log.d(TAG, "🎵 Conectividad Spotify: " + responseCode);

        } catch (Exception e) {
            Log.d(TAG, "⚠️ Conectividad Spotify: " + e.getMessage());
        }
    }

    /**
     * 🧪 MÉTODO DE TESTING: Verificar configuración de audio del dispositivo
     */
    public void testDeviceAudioConfig() {
        Log.d(TAG, "🧪 === TEST CONFIGURACIÓN AUDIO ===");

        if (audioManager == null) {
            Log.e(TAG, "❌ AudioManager es null");
            return;
        }

        try {
            // Verificar volumen actual
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            float volumePercent = maxVolume > 0 ? (float) currentVolume / maxVolume * 100 : 0;

            Log.d(TAG, "🔊 Configuración de Audio:");
            Log.d(TAG, "   📊 Volumen actual: " + currentVolume + "/" + maxVolume + " (" + String.format("%.1f%%", volumePercent) + ")");

            // Verificar modo de sonido
            int ringerMode = audioManager.getRingerMode();
            String modeStr = "DESCONOCIDO";
            switch (ringerMode) {
                case AudioManager.RINGER_MODE_NORMAL: modeStr = "NORMAL"; break;
                case AudioManager.RINGER_MODE_VIBRATE: modeStr = "VIBRACIÓN"; break;
                case AudioManager.RINGER_MODE_SILENT: modeStr = "SILENCIOSO"; break;
            }
            Log.d(TAG, "   📱 Modo del dispositivo: " + modeStr);

            // Verificar audio focus
            boolean canRequestFocus = requestAudioFocus();
            Log.d(TAG, "   🔊 Puede obtener Audio Focus: " + canRequestFocus);

            // Verificar auriculares
            boolean hasWiredHeadset = audioManager.isWiredHeadsetOn();
            boolean hasBluetoothA2dp = audioManager.isBluetoothA2dpOn();
            Log.d(TAG, "   🎧 Auriculares conectados: " + hasWiredHeadset);
            Log.d(TAG, "   📶 Bluetooth audio: " + hasBluetoothA2dp);

            // Estado general
            if (currentVolume == 0) {
                Log.w(TAG, "⚠️ PROBLEMA: Volumen en 0 - usuario no escuchará nada");
            } else if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                Log.w(TAG, "⚠️ PROBLEMA: Dispositivo en modo silencioso");
            } else if (currentVolume < maxVolume * 0.3) {
                Log.w(TAG, "⚠️ AVISO: Volumen bajo (" + String.format("%.1f%%", volumePercent) + ")");
            } else {
                Log.d(TAG, "✅ Configuración de audio parece correcta");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error verificando configuración de audio", e);
        }
    }

    /**
     * 🧪 MÉTODO PRINCIPAL DE TESTING: Ejecutar todos los tests
     */
    public void runCompleteTest() {
        Log.d(TAG, "🧪 === INICIANDO TEST COMPLETO ===");

        testDeviceAudioConfig();

        new android.os.Handler().postDelayed(() -> {
            testBasicConnectivity();
        }, 1000);

        new android.os.Handler().postDelayed(() -> {
            testWithKnownWorkingUrl();
        }, 3000);

        Log.d(TAG, "🧪 Test completo programado - revisa logs en los próximos 5 segundos");
    }
}