package com.sise.orbitsongv1.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.sise.orbitsongv1.R;
import com.sise.orbitsongv1.adapters.SongAdapter;
import com.sise.orbitsongv1.models.Song;
import com.sise.orbitsongv1.models.SpotifySearchResponse;
import com.sise.orbitsongv1.services.MusicPlayerService;
import com.sise.orbitsongv1.services.RetrofitClient;
import com.sise.orbitsongv1.utils.Constants;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity
        implements SongAdapter.OnSongClickListener, MusicPlayerService.MusicPlayerListener {

    private static final String TAG = "HomeActivity";

    private TextView tvWelcome;
    private RecyclerView recyclerViewSongs;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabLoadMusic;
    private TextView tvEmptyState;

    private SongAdapter songAdapter;
    private String authToken;
    private boolean isLoading = false;
    private MusicPlayerService musicPlayer;
    private Handler progressHandler;
    private Runnable progressRunnable;

    // Variables para búsqueda en tiempo real
    private Handler searchHandler;
    private Runnable searchRunnable;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupRecyclerView();
        setupWelcomeMessage();
        getAuthToken();
        setupListeners();
        setupMusicPlayer();
        setupSearchHandler();

        // 🔍 DEBUG: Cambiar temporalmente para probar
        loadAllSongsDebug(); // ← DEBUG: Usar este método temporalmente
        // loadSongsFromDatabase(); // ← Comentado temporalmente
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        recyclerViewSongs = findViewById(R.id.recycler_view_songs);
        progressBar = findViewById(R.id.progress_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        fabLoadMusic = findViewById(R.id.fab_load_music);
        tvEmptyState = findViewById(R.id.tv_empty_state);
    }

    private void setupRecyclerView() {
        songAdapter = new SongAdapter(this);
        songAdapter.setOnSongClickListener(this);
        recyclerViewSongs.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSongs.setAdapter(songAdapter);
    }

    private void setupWelcomeMessage() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String username = prefs.getString(Constants.USER_KEY, "Usuario");
        tvWelcome.setText("¡Buen día, " + username + "!");
    }

    // ========================================
    // MÉTODOS DE AUTENTICACIÓN Y NAVEGACIÓN
    // ========================================

    private void logout() {
        if (musicPlayer != null) {
            musicPlayer.stop();
        }

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void getAuthToken() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString(Constants.TOKEN_KEY, null);
        if (token != null) {
            authToken = "Bearer " + token;
            Log.d(TAG, "🔍 DEBUG: Token obtenido: " + authToken.substring(0, 20) + "...");
        } else {
            Log.e(TAG, "🔍 DEBUG: No se encontró token, redirigiendo a login");
            logout();
        }
    }

    private void handleUnauthorized() {
        showError("Sesión expirada. Por favor, inicia sesión de nuevo.");
        logout();
    }

    // ========================================
    // CONFIGURACIÓN INICIAL
    // ========================================

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // 🔍 DEBUG: Cambiar temporalmente
            loadAllSongsDebug(); // ← DEBUG
            // if (currentSearchQuery.isEmpty()) {
            //     loadSongsFromDatabase(); // 🆕 Cargar de BD por defecto
            // } else {
            //     searchInDatabase(currentSearchQuery); // 🆕 Buscar en BD por defecto
            // }
        });

        fabLoadMusic.setOnClickListener(v -> {
            // 🔍 DEBUG: Cambiar temporalmente para probar debug
            loadAllSongsDebug(); // ← DEBUG: Usar este método temporalmente
            // loadAllSpotifyMusic(); // ← Comentado temporalmente
        });
    }

    // Configurar manejador de búsqueda con delay
    private void setupSearchHandler() {
        searchHandler = new Handler(Looper.getMainLooper());
    }

    private void setupMusicPlayer() {
        musicPlayer = MusicPlayerService.getInstance();
        musicPlayer.setListener(this);

        progressHandler = new Handler(Looper.getMainLooper());
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicPlayer.isPlaying()) {
                    int currentPosition = musicPlayer.getCurrentPosition();
                    int duration = musicPlayer.getDuration();
                    onProgressUpdate(currentPosition, duration);
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
    }

    // ========================================
    // 🔍 MÉTODOS DEBUG
    // ========================================

    // 🔍 MÉTODO DEBUG: Cargar TODAS las canciones sin filtros
    private void loadAllSongsDebug() {
        if (isLoading) return;

        showLoading(true);
        showEmptyState(false);
        currentSearchQuery = "";

        Log.d(TAG, "🔍 DEBUG: Cargando TODAS las canciones (sin filtros)...");
        Log.d(TAG, "🔍 DEBUG: URL base: " + RetrofitClient.BASE_URL);
        Log.d(TAG, "🔍 DEBUG: Token: " + (authToken != null ? "Sí" : "No"));

        RetrofitClient.getInstance().getApiService().getAllSongsDebug(authToken)
                .enqueue(new Callback<List<Song>>() {
                    @Override
                    public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);

                        Log.d(TAG, "🔍 DEBUG: Response code: " + response.code());
                        Log.d(TAG, "🔍 DEBUG: Response successful: " + response.isSuccessful());
                        Log.d(TAG, "🔍 DEBUG: Request URL: " + call.request().url());

                        if (response.isSuccessful() && response.body() != null) {
                            List<Song> songs = response.body();
                            Log.d(TAG, "🔍 DEBUG: TODAS las canciones recibidas: " + songs.size());

                            // Debug: Mostrar información de las primeras canciones
                            for (int i = 0; i < Math.min(5, songs.size()); i++) {
                                Song song = songs.get(i);
                                Log.d(TAG, "🔍 DEBUG: Canción " + (i+1) + ": " + song.getNombre());
                                Log.d(TAG, "🔍 DEBUG: Artistas: " + song.getArtistasString());
                                Log.d(TAG, "🔍 DEBUG: Preview URL: " + song.getPreviewUrl());
                                Log.d(TAG, "🔍 DEBUG: Has Preview: " + song.hasPreview());
                                Log.d(TAG, "🔍 DEBUG: ID: " + song.getId());
                                Log.d(TAG, "🔍 DEBUG: SpotifyId: " + song.getSpotifyId());
                            }

                            if (songs.isEmpty()) {
                                showEmptyStateWithMessage("🔍 DEBUG: La base de datos está completamente vacía\n\n¡Usa el botón ➕ para cargar música!");
                                showError("DEBUG: No hay canciones en la base de datos");
                            } else {
                                songAdapter.setSongs(songs);
                                showEmptyState(false);
                                showSuccess("🔍 DEBUG: " + songs.size() + " canciones cargadas (TODAS, sin filtros)");
                            }
                        } else {
                            Log.e(TAG, "🔍 DEBUG: Error en respuesta. Código: " + response.code());
                            try {
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                                Log.e(TAG, "🔍 DEBUG: Error body: " + errorBody);
                            } catch (Exception e) {
                                Log.e(TAG, "🔍 DEBUG: Error leyendo error body", e);
                            }
                            handleApiError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Song>> call, Throwable t) {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, "🔍 DEBUG: Error de conexión completo", t);
                        Log.e(TAG, "🔍 DEBUG: Error message: " + t.getMessage());
                        Log.e(TAG, "🔍 DEBUG: Error class: " + t.getClass().getSimpleName());
                        showError("DEBUG - Error de conexión: " + t.getMessage());
                        showEmptyStateWithMessage("🔍 DEBUG: Error de conexión completo\n\n" + t.getMessage());
                    }
                });
    }

    // ========================================
    // MÉTODOS PRINCIPALES - CARGA DE DATOS
    // ========================================

    // 🆕 MÉTODO PRINCIPAL - Cargar canciones de la base de datos
    private void loadSongsFromDatabase() {
        if (isLoading) return;

        showLoading(true);
        showEmptyState(false);
        currentSearchQuery = "";

        Log.d(TAG, "📋 Cargando canciones desde la base de datos...");

        RetrofitClient.getInstance().getApiService().getSongsWithPreview(authToken)
                .enqueue(new Callback<List<Song>>() {
                    @Override
                    public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Song> songs = response.body();
                            Log.d(TAG, "✅ Canciones cargadas desde BD: " + songs.size());

                            if (songs.isEmpty()) {
                                showEmptyStateWithMessage("🎵 No hay canciones en tu biblioteca\n\n¡Presiona el botón ➕ para cargar música desde Spotify!");
                                showError("No se encontraron canciones en la base de datos");
                            } else {
                                songAdapter.setSongs(songs);
                                showEmptyState(false);
                                showSuccess("📋 " + songs.size() + " canciones cargadas desde tu biblioteca");
                            }
                        } else {
                            Log.e(TAG, "❌ Error al cargar canciones de la BD. Código: " + response.code());
                            handleApiError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Song>> call, Throwable t) {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, "❌ Error de conexión al cargar canciones de BD", t);
                        showError("Error de conexión: " + t.getMessage());
                        showEmptyStateWithMessage("❌ Error de conexión\n\n¡Revisa tu internet e intenta de nuevo!");
                    }
                });
    }

    // 🆕 Buscar en la base de datos local
    private void searchInDatabase(String query) {
        if (query.trim().isEmpty()) {
            loadSongsFromDatabase();
            return;
        }

        if (isLoading) return;

        showLoading(true);
        showEmptyState(false);
        currentSearchQuery = query;

        Log.d(TAG, "🔍 Buscando en la base de datos: " + query);

        RetrofitClient.getInstance().getApiService().searchSongsByNameWithPreview(authToken, query)
                .enqueue(new Callback<List<Song>>() {
                    @Override
                    public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Song> songs = response.body();
                            Log.d(TAG, "✅ Resultados de búsqueda en BD: " + songs.size());

                            if (songs.isEmpty()) {
                                showEmptyStateWithMessage("😕 No se encontraron canciones en tu biblioteca para:\n\"" + query + "\"\n\n💡 Intenta cargar más música desde Spotify usando el botón ➕");
                                Log.d(TAG, "⚠️ Sin resultados en BD para: " + query);
                            } else {
                                songAdapter.setSongs(songs);
                                showEmptyState(false);
                                showSuccess("📋 " + songs.size() + " canciones encontradas en tu biblioteca para: \"" + query + "\"");
                            }
                        } else {
                            Log.e(TAG, "❌ Error en búsqueda de BD. Código: " + response.code());
                            handleApiError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Song>> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "❌ Error de conexión en búsqueda de BD", t);
                        showError("Error de conexión en búsqueda: " + t.getMessage());
                        showEmptyStateWithMessage("❌ Error de conexión\n\nRevisa tu internet e intenta de nuevo");
                    }
                });
    }

    // 🆕 Método mejorado para cargar música desde Spotify y guardarla
    private void loadAllSpotifyMusic() {
        if (isLoading) return;

        showLoading(true);
        showEmptyState(false);

        Log.d(TAG, "📥 Cargando y guardando música desde Spotify...");

        RetrofitClient.getInstance().getApiService().loadAllSongs(authToken)
                .enqueue(new Callback<SpotifySearchResponse>() {
                    @Override
                    public void onResponse(Call<SpotifySearchResponse> call, Response<SpotifySearchResponse> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            SpotifySearchResponse spotifyResponse = response.body();

                            if (spotifyResponse.isSuccessWithResults()) {
                                List<Song> songs = spotifyResponse.getCanciones();
                                Log.d(TAG, "✅ Música guardada desde Spotify: " + songs.size());

                                showSuccess("✅ " + songs.size() + " canciones cargadas y guardadas desde Spotify!");

                                // Después de cargar, refrescar la vista con datos de la BD
                                loadAllSongsDebug(); // 🔍 DEBUG: Cambiar temporalmente
                                // loadSongsFromDatabase();
                            } else {
                                showError(spotifyResponse.getMessage() != null ?
                                        spotifyResponse.getMessage() : "No se pudieron cargar canciones desde Spotify");
                                showEmptyStateWithMessage("😕 No se encontró música\n\n¡Intenta buscar algo específico!");
                            }
                        } else {
                            Log.e(TAG, "❌ Error al cargar música de Spotify. Código: " + response.code());
                            handleApiError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<SpotifySearchResponse> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "❌ Error de conexión con Spotify", t);
                        showError("Error de conexión con Spotify: " + t.getMessage());
                    }
                });
    }

    // ========================================
    // MÉTODOS DE BÚSQUEDA EN SPOTIFY
    // ========================================

    // 🆕 Buscar música en tiempo real desde Spotify
    private void searchSpotifyRealTime(String query) {
        if (query.trim().isEmpty()) {
            loadSongsFromDatabase();
            return;
        }

        if (isLoading) return;

        showLoading(true);
        showEmptyState(false);
        currentSearchQuery = query;

        Log.d(TAG, "🔍 Buscando en tiempo real en Spotify: " + query);

        RetrofitClient.getInstance().getApiService().searchSpotifyRealTimeWithPreview(authToken, query)
                .enqueue(new Callback<SpotifySearchResponse>() {
                    @Override
                    public void onResponse(Call<SpotifySearchResponse> call, Response<SpotifySearchResponse> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            SpotifySearchResponse spotifyResponse = response.body();

                            if (spotifyResponse.isSuccessWithResults()) {
                                List<Song> songs = spotifyResponse.getCanciones();
                                Log.d(TAG, "✅ Resultados de búsqueda: " + songs.size());

                                songAdapter.setSongs(songs);
                                showEmptyState(false);
                                showSuccess("🔍 " + songs.size() + " canciones encontradas para: \"" + query + "\"");
                            } else {
                                showEmptyStateWithMessage("😕 No se encontraron canciones reproducibles para:\n\"" + query + "\"\n\n💡 Intenta con otros términos");
                                Log.d(TAG, "⚠️ Sin resultados para: " + query);
                            }
                        } else {
                            Log.e(TAG, "❌ Error en búsqueda. Código: " + response.code());
                            handleApiError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<SpotifySearchResponse> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "❌ Error de conexión en búsqueda", t);
                        showError("Error de conexión en búsqueda: " + t.getMessage());
                        showEmptyStateWithMessage("❌ Error de conexión\n\nRevisa tu internet e intenta de nuevo");
                    }
                });
    }

    // Cargar recomendaciones desde Spotify
    private void loadSpotifyRecommendations() {
        if (isLoading) return;

        showLoading(true);
        showEmptyState(false);
        currentSearchQuery = "";

        Log.d(TAG, "🎵 Cargando recomendaciones desde Spotify...");

        RetrofitClient.getInstance().getApiService().getSpotifyRecommendations(authToken)
                .enqueue(new Callback<SpotifySearchResponse>() {
                    @Override
                    public void onResponse(Call<SpotifySearchResponse> call, Response<SpotifySearchResponse> response) {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            SpotifySearchResponse spotifyResponse = response.body();

                            if (spotifyResponse.isSuccessWithResults()) {
                                List<Song> songs = spotifyResponse.getCanciones();
                                Log.d(TAG, "✅ Recomendaciones recibidas: " + songs.size());

                                songAdapter.setSongs(songs);
                                showEmptyState(false);
                                showSuccess("🎵 " + songs.size() + " recomendaciones cargadas desde Spotify");
                            } else {
                                showEmptyStateWithMessage("🔍 ¡Busca música en Spotify para comenzar!\n\nUsa la barra de búsqueda de arriba");
                                showError(spotifyResponse.getMessage() != null ?
                                        spotifyResponse.getMessage() : "No hay recomendaciones disponibles");
                            }
                        } else {
                            Log.e(TAG, "❌ Error al cargar recomendaciones. Código: " + response.code());
                            handleApiError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<SpotifySearchResponse> call, Throwable t) {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, "❌ Error de conexión al cargar recomendaciones", t);
                        showError("Error de conexión: " + t.getMessage());
                        showEmptyStateWithMessage("❌ Error de conexión\n\n¡Intenta buscar música manualmente!");
                    }
                });
    }

    // ========================================
    // MÉTODOS DE UI Y UTILIDADES
    // ========================================

    private void handleApiError(int code) {
        if (code == 401) {
            handleUnauthorized();
        } else {
            showError("Error del servidor (Código: " + code + ")");
            showEmptyStateWithMessage("❌ Error del servidor\n\nCódigo: " + code);
        }
    }

    private void showLoading(boolean show) {
        isLoading = show;
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        fabLoadMusic.setEnabled(!show);
    }

    private void showEmptyState(boolean show) {
        tvEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewSongs.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyStateWithMessage(String message) {
        showEmptyState(true);
        tvEmptyState.setText(message);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ========================================
    // LISTENERS DE CANCIONES Y REPRODUCTOR
    // ========================================

    @Override
    public void onSongClick(Song song) {
        Log.d(TAG, "Canción seleccionada: " + song.getNombre());
        // ✅ NUEVO: Abrir reproductor de música
        openMusicPlayer(song);
    }

    @Override
    public void onSongPlayClick(Song song) {
        Log.d(TAG, "Reproducir canción: " + song.getNombre());

        if (musicPlayer.getCurrentSong() != null &&
                musicPlayer.getCurrentSong().getId().equals(song.getId()) &&
                musicPlayer.isPlaying()) {
            musicPlayer.pause();
        }
        else if (musicPlayer.getCurrentSong() != null &&
                musicPlayer.getCurrentSong().getId().equals(song.getId()) &&
                !musicPlayer.isPlaying()) {
            musicPlayer.resume();
        }
        else {
            // ✅ OPCIÓN: También abrir reproductor al hacer clic en play
            openMusicPlayer(song);
            // musicPlayer.playSong(song); // ← Comentado para abrir reproductor
        }
    }

    private void openMusicPlayer(Song song) {
        if (song == null) {
            showError("❌ Error: No se pudo cargar la información de la canción");
            return;
        }

        // 🔧 TEMPORAL: Comentar la validación de preview para testing
        // if (!song.hasPreview()) {
        //     showError("❌ Esta canción no tiene preview disponible para reproducir");
        //     return;
        // }

        try {
            Intent intent = new Intent(this, MusicPlayerActivity.class);
            intent.putExtra(MusicPlayerActivity.EXTRA_SONG, song);
            startActivity(intent);
            Log.d(TAG, "✅ Abriendo reproductor para: " + song.getNombre());

            // 🔍 DEBUG: Mostrar información del preview
            if (song.hasPreview()) {
                Log.d(TAG, "🎵 Canción con preview: " + song.getPreviewUrl());
            } else {
                Log.w(TAG, "⚠️ Canción SIN preview, pero abriendo reproductor para testing");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al abrir reproductor", e);
            showError("❌ Error al abrir el reproductor: " + e.getMessage());
        }
    }
    private void showSongInfo(Song song) {
        String info = "🎵 " + song.getNombre() + "\n" +
                "🎤 " + song.getArtistasString() + "\n" +
                "💿 " + (song.getAlbum() != null ? song.getAlbum() : "Álbum desconocido") + "\n" +
                "⏱️ " + song.getDuracionFormatted();

        Snackbar.make(recyclerViewSongs, info, Snackbar.LENGTH_LONG)
                .setAction("REPRODUCIR", v -> musicPlayer.playSong(song))
                .show();
    }

    // ========================================
    // IMPLEMENTACIÓN DE MusicPlayerListener
    // ========================================

    @Override
    public void onPlaybackStarted(Song song) {
        runOnUiThread(() -> {
            showSuccess("▶️ Reproduciendo: " + song.getNombre());
            songAdapter.notifyDataSetChanged();
            progressHandler.post(progressRunnable);
        });
    }

    @Override
    public void onPlaybackPaused() {
        runOnUiThread(() -> {
            showSuccess("⏸️ Reproducción pausada");
            songAdapter.notifyDataSetChanged();
            progressHandler.removeCallbacks(progressRunnable);
        });
    }

    @Override
    public void onPlaybackStopped() {
        runOnUiThread(() -> {
            showSuccess("⏹️ Reproducción detenida");
            songAdapter.notifyDataSetChanged();
            progressHandler.removeCallbacks(progressRunnable);
        });
    }

    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            showSuccess("✅ Reproducción completada");
            songAdapter.notifyDataSetChanged();
            progressHandler.removeCallbacks(progressRunnable);
        });
    }

    @Override
    public void onPlaybackError(String error) {
        runOnUiThread(() -> {
            showError("❌ " + error);
            progressHandler.removeCallbacks(progressRunnable);
        });
    }

    @Override
    public void onProgressUpdate(int currentPosition, int duration) {
        if (duration > 0) {
            int progress = (currentPosition * 100) / duration;
            Log.d(TAG, "Progreso: " + progress + "% (" + currentPosition + "/" + duration + ")");
        }
    }

    // ========================================
    // MENÚS Y NAVIGATION
    // ========================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // 🆕 Configurar SearchView para buscar primero en BD
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        if (searchView != null) {
            searchView.setQueryHint("🔍 Buscar en tu biblioteca...");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (!query.trim().isEmpty()) {
                        searchInDatabase(query); // 🆕 Buscar en BD primero
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Cancelar búsqueda anterior
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }

                    if (newText.trim().isEmpty()) {
                        loadAllSongsDebug(); // 🔍 DEBUG: Cambiar temporalmente
                        // loadSongsFromDatabase(); // ← Comentado temporalmente
                        return true;
                    }

                    if (newText.length() > 2) {
                        // Buscar automáticamente después de 800ms de inactividad
                        searchRunnable = () -> searchInDatabase(newText); // 🆕 Buscar en BD
                        searchHandler.postDelayed(searchRunnable, 800);
                    }
                    return true;
                }
            });
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            logout();
            return true;
        } else if (id == R.id.action_refresh) {
            // 🔍 DEBUG: Cambiar temporalmente
            loadAllSongsDebug();
            // if (currentSearchQuery.isEmpty()) {
            //     loadSongsFromDatabase();
            // } else {
            //     searchInDatabase(currentSearchQuery);
            // }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ========================================
    // LIFECYCLE METHODS
    // ========================================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
        // Limpiar handler de búsqueda
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
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
    // ✅ MÉTODO NUEVO: Buscar en Spotify con preview
    private void searchSpotifyWithPreview(String query) {
        if (query.trim().isEmpty()) {
            loadAllSongsDebug(); // Usar tu método existente
            return;
        }

        if (isLoading) return;

        showLoading(true);
        showEmptyState(false);
        currentSearchQuery = query;

        Log.d(TAG, "🔍 Buscando en Spotify CON PREVIEW: " + query);

        RetrofitClient.getInstance().getApiService().searchSpotifyRealTimeWithPreview(authToken, query)
                .enqueue(new Callback<SpotifySearchResponse>() {
                    @Override
                    public void onResponse(Call<SpotifySearchResponse> call, Response<SpotifySearchResponse> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            SpotifySearchResponse spotifyResponse = response.body();

                            if (spotifyResponse.isSuccessWithResults()) {
                                List<Song> songs = spotifyResponse.getCanciones();
                                Log.d(TAG, "✅ Canciones de Spotify con preview para '" + query + "': " + songs.size());

                                // Debug: Verificar que las canciones tengan preview
                                int songsWithPreview = 0;
                                for (Song song : songs) {
                                    if (song.hasPreview()) {
                                        songsWithPreview++;
                                        Log.d(TAG, "🎵 Con preview: " + song.getNombre() + " - " + song.getArtistasString());
                                    }
                                }
                                Log.d(TAG, "🎵 Total con preview: " + songsWithPreview + "/" + songs.size());

                                if (songs.isEmpty()) {
                                    showEmptyStateWithMessage("😕 No se encontraron canciones reproducibles para:\n\"" + query + "\"\n\n💡 Intenta con: 'Bad Bunny', 'Taylor Swift', 'The Weeknd'");
                                } else {
                                    songAdapter.setSongs(songs);
                                    showEmptyState(false);
                                    showSuccess("🔍 " + songs.size() + " canciones de Spotify encontradas para: \"" + query + "\"");
                                }
                            } else {
                                showEmptyStateWithMessage("😕 No se encontraron canciones para:\n\"" + query + "\"\n\n💡 Intenta con otros términos");
                                Log.d(TAG, "⚠️ Sin resultados en Spotify para: " + query);
                            }
                        } else {
                            Log.e(TAG, "❌ Error en búsqueda de Spotify. Código: " + response.code());
                            handleApiError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<SpotifySearchResponse> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "❌ Error de conexión en búsqueda de Spotify", t);
                        showError("Error de conexión en búsqueda: " + t.getMessage());
                        showEmptyStateWithMessage("❌ Error de conexión\n\nRevisa tu internet e intenta de nuevo");
                    }
                });
    }

    // ✅ MÉTODO NUEVO: Cargar música popular con preview desde Spotify
    private void loadPopularMusicWithPreview() {
        if (isLoading) return;

        showLoading(true);
        showEmptyState(false);

        Log.d(TAG, "🎵 Cargando música popular con preview desde Spotify...");

        // Buscar música de Bad Bunny que suele tener preview
        RetrofitClient.getInstance().getApiService().searchSpotifyRealTimeWithPreview(authToken, "bad bunny")
                .enqueue(new Callback<SpotifySearchResponse>() {
                    @Override
                    public void onResponse(Call<SpotifySearchResponse> call, Response<SpotifySearchResponse> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            SpotifySearchResponse spotifyResponse = response.body();

                            if (spotifyResponse.isSuccessWithResults()) {
                                List<Song> songs = spotifyResponse.getCanciones();
                                Log.d(TAG, "✅ Música popular con preview cargada: " + songs.size());

                                if (songs.isEmpty()) {
                                    showEmptyStateWithMessage("😕 No se encontró música popular con preview\n\n💡 Intenta buscar manualmente 'Bad Bunny' o 'Taylor Swift'");
                                } else {
                                    songAdapter.setSongs(songs);
                                    showEmptyState(false);
                                    showSuccess("🎵 " + songs.size() + " canciones populares cargadas desde Spotify");

                                    // Debug: Mostrar las primeras canciones cargadas
                                    for (int i = 0; i < Math.min(3, songs.size()); i++) {
                                        Song song = songs.get(i);
                                        Log.d(TAG, "🎵 Música popular " + (i+1) + ": " + song.getNombre() + " - " + song.getArtistasString());
                                        Log.d(TAG, "   🔗 Preview: " + song.getPreviewUrl());
                                    }
                                }
                            } else {
                                showError("No se pudo cargar música popular desde Spotify");
                                showEmptyStateWithMessage("😕 No se pudo cargar música popular\n\n🔍 Intenta buscar manualmente");
                            }
                        } else {
                            Log.e(TAG, "❌ Error al cargar música popular. Código: " + response.code());
                            handleApiError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<SpotifySearchResponse> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "❌ Error de conexión al cargar música popular", t);
                        showError("Error de conexión: " + t.getMessage());
                    }
                });
    }
}