package com.sise.orbitsongv1.services;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
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

    public interface MusicPlayerListener {
        void onPlaybackStarted(Song song);
        void onPlaybackPaused();
        void onPlaybackStopped();
        void onPlaybackCompleted();
        void onPlaybackError(String error);
        void onProgressUpdate(int currentPosition, int duration);
    }

    private MusicPlayerService() {
        initializeMediaPlayer();
    }

    public static synchronized MusicPlayerService getInstance() {
        if (instance == null) {
            instance = new MusicPlayerService();
        }
        return instance;
    }

    public void setListener(MusicPlayerListener listener) {
        this.listener = listener;
    }

    private void initializeMediaPlayer() {
        if (mediaPlayer != null) {
            release();
        }

        mediaPlayer = new MediaPlayer();

        // Configurar atributos de audio
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        mediaPlayer.setAudioAttributes(audioAttributes);

        // Configurar listeners
        mediaPlayer.setOnCompletionListener(mp -> {
            isPlaying = false;
            if (listener != null) {
                listener.onPlaybackCompleted();
            }
            Log.d(TAG, "Reproducción completada");
        });

        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            isPlaying = false;
            String errorMsg = "Error de reproducción: " + what + ", " + extra;
            Log.e(TAG, errorMsg);
            if (listener != null) {
                listener.onPlaybackError(errorMsg);
            }
            return true;
        });

        mediaPlayer.setOnPreparedListener(mp -> {
            Log.d(TAG, "MediaPlayer preparado");
            mp.start();
            isPlaying = true;
            if (listener != null) {
                listener.onPlaybackStarted(currentSong);
            }
        });
    }

    public void playSong(Song song) {
        if (song == null) {
            if (listener != null) {
                listener.onPlaybackError("Canción no válida");
            }
            return;
        }

        // Verificar si hay preview URL
        String previewUrl = song.getPreviewUrl();
        if (previewUrl == null || previewUrl.trim().isEmpty()) {
            if (listener != null) {
                listener.onPlaybackError("🚫 Preview no disponible para: " + song.getNombre() +
                        "\n💡 Prueba con otras canciones que tengan preview");
            }
            return;
        }

        Log.d(TAG, "Reproduciendo: " + song.getNombre() + " - URL: " + previewUrl);

        try {
            // Detener reproducción actual si existe
            stop();

            // Reinicializar MediaPlayer
            initializeMediaPlayer();

            currentSong = song;

            // Configurar nueva canción
            mediaPlayer.setDataSource(previewUrl);
            mediaPlayer.prepareAsync(); // Preparar de forma asíncrona

        } catch (IOException e) {
            Log.e(TAG, "Error al configurar MediaPlayer", e);
            if (listener != null) {
                listener.onPlaybackError("❌ Error al cargar: " + song.getNombre() +
                        "\n🔄 Intenta de nuevo o prueba otra canción");
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Estado ilegal del MediaPlayer", e);
            initializeMediaPlayer();
            if (listener != null) {
                listener.onPlaybackError("🔄 Error interno del reproductor\nIntenta de nuevo");
            }
        }
    }

    public void pause() {
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.pause();
                isPlaying = false;
                if (listener != null) {
                    listener.onPlaybackPaused();
                }
                Log.d(TAG, "Reproducción pausada");
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error al pausar", e);
            }
        }
    }

    public void resume() {
        if (mediaPlayer != null && !isPlaying) {
            try {
                mediaPlayer.start();
                isPlaying = true;
                if (listener != null) {
                    listener.onPlaybackStarted(currentSong);
                }
                Log.d(TAG, "Reproducción reanudada");
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error al reanudar", e);
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
                if (listener != null) {
                    listener.onPlaybackStopped();
                }
                Log.d(TAG, "Reproducción detenida");
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error al detener", e);
            }
        }
    }

    public void seekTo(int position) {
        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.seekTo(position);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error al hacer seek", e);
            }
        }
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPlaying) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error al obtener posición actual", e);
            }
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getDuration();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error al obtener duración", e);
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

    public void release() {
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
                Log.e(TAG, "Error al liberar MediaPlayer", e);
            }
        }
    }
}