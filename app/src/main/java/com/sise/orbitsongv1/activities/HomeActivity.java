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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
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

    // Variables de UI principales
    private TextView tvWelcome;
    private RecyclerView recyclerViewSongs;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabLoadMusic;
    private TextView tvEmptyState;

    // Variables del mini reproductor
    private View miniPlayerCard;
    private ImageView miniPlayerAlbumArt;
    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private ImageButton miniPlayerPlayPause;
    private ImageButton miniPlayerClose;

    // Variables de estadísticas
    private LinearLayout layoutQuickStats;
    private TextView tvTotalSongs;
    private TextView tvWithPreview;

    // Variables de lógica
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

    // Estados de la app
    private enum AppState {
        LOADING, LOADED_WITH_DATA, LOADED_EMPTY, ERROR
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupRecyclerView();
        setupWelcomeMessage();
        getAuthToken();
        setupListeners();
        setupMusicPlayerEnhanced();
        setupSearchHandler();

        // Cargar canciones al iniciar
        loadSongsFromDatabaseEnhanced();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        recyclerViewSongs = findViewById(R.id.recycler_view_songs);
        progressBar = findViewById(R.id.progress_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        fabLoadMusic = findViewById(R.id.fab_load_music);
        tvEmptyState = findViewById(R.id.tv_empty_state);

        // Inicializar mini reproductor
        initMiniPlayerViews();
    }

    private void initMiniPlayerViews() {
        miniPlayerCard = findViewById(R.id.mini_player_card);
        miniPlayerAlbumArt = findViewById(R.id.mini_player_album_art);
        miniPlayerTitle = findViewById(R.id.mini_player_title);
        miniPlayerArtist = findViewById(R.id.mini_player_artist);
        miniPlayerPlayPause = findViewById(R.id.mini_player_play_pause);
        miniPlayerClose = findViewById(R.id.mini_player_close);

        // Views de estadísticas
        layoutQuickStats = findViewById(R.id.layout_quick_stats);
        tvTotalSongs = findViewById(R.id.tv_total_songs);
        tvWithPreview = findViewById(R.id.tv_with_preview);

        setupMiniPlayerListeners();
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
            Log.d(TAG, "✅ Token obtenido correctamente");
        } else {
            Log.e(TAG, "❌ No se encontró token, redirigiendo a login");
            logout();
        }
    }

    private void handleUnauthorized() {
        showError("Sesión expirada. Por favor, inicia sesión de nuevo.");
        logout();
    }

    // ✅ NUEVO: Método para abrir LibraryActivity
    private void openLibraryActivity() {
        try {
            Intent intent = new Intent(this, LibraryActivity.class);
            startActivity(intent);
            Log.d(TAG, "✅ Navegando a LibraryActivity - Mi Biblioteca Musical");
            showToast("📚 Abriendo tu biblioteca musical...");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error al abrir LibraryActivity", e);
            showError("Error al abrir la biblioteca: " + e.getMessage());
        }
    }

    // ========================================
    // CONFIGURACIÓN INICIAL
    // ========================================

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (currentSearchQuery.isEmpty()) {
                loadSongsFromDatabaseEnhanced();
            } else {
                searchInDatabase(currentSearchQuery);
            }
        });

        fabLoadMusic.setOnClickListener(v -> showLoadMusicOptions());

        // ✅ CORREGIDO: Listeners para las cards de acciones rápidas
        setupActionCards();

        // ✅ CORREGIDO: Listener para botón de actualizar (con manejo de errores)
        try {
            findViewById(R.id.btn_refresh_music).setOnClickListener(v -> {
                loadSongsFromDatabaseEnhanced();
                showToast("🔄 Actualizando biblioteca...");
            });
        } catch (Exception e) {
            Log.w(TAG, "Botón de actualizar no encontrado en el layout actual");
        }
    }

    // ✅ MODIFICADO: Configurar listeners de las cards con navegación a LibraryActivity
    private void setupActionCards() {
        try {
            // Card: Búsqueda en tiempo real
            findViewById(R.id.card_realtime_search).setOnClickListener(v -> {
                promptSpotifySearch();
            });
        } catch (Exception e) {
            Log.w(TAG, "Card realtime search no encontrada");
        }

        try {
            // Card: Tendencias
            findViewById(R.id.card_trending).setOnClickListener(v -> {
                loadSpotifyRecommendations();
                showToast("🔥 Cargando tendencias desde Spotify...");
            });
        } catch (Exception e) {
            Log.w(TAG, "Card trending no encontrada");
        }

        try {
            // ✅ CAMBIO PRINCIPAL: Card Mi Biblioteca ahora navega a LibraryActivity
            findViewById(R.id.card_my_library).setOnClickListener(v -> {
                openLibraryActivity(); // ← CAMBIO AQUÍ: Ahora navega en lugar de cargar canciones
            });
        } catch (Exception e) {
            Log.w(TAG, "Card my library no encontrada");
        }

        try {
            // Card: Cargar Música
            findViewById(R.id.card_load_music).setOnClickListener(v -> {
                showLoadMusicOptions();
            });
        } catch (Exception e) {
            Log.w(TAG, "Card load music no encontrada");
        }

        // ✅ CORREGIDO: Botón de búsqueda en empty state
        setupEmptyStateButton();
    }

    // ✅ CORREGIDO: Método para configurar el empty state dinámicamente
    private void setupEmptyStateButton() {
        try {
            View btnSearchSpotify = findViewById(R.id.btn_search_spotify);
            if (btnSearchSpotify != null) {
                btnSearchSpotify.setOnClickListener(v -> promptSpotifySearch());
            }
        } catch (Exception e) {
            Log.w(TAG, "Botón search spotify no encontrado");
        }
    }

    private void setupSearchHandler() {
        searchHandler = new Handler(Looper.getMainLooper());
    }

    private void setupMusicPlayerEnhanced() {
        musicPlayer = MusicPlayerService.getInstance();
        musicPlayer.initialize(this);
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

        Log.d(TAG, "✅ MusicPlayerService configurado con mini reproductor");
    }

    // ========================================
    // MINI REPRODUCTOR FLOTANTE
    // ========================================

    private void setupMiniPlayerListeners() {
        if (miniPlayerCard == null) return;

        // Click en la card completa para abrir reproductor
        miniPlayerCard.setOnClickListener(v -> {
            Song currentSong = musicPlayer.getCurrentSong();
            if (currentSong != null) {
                openMusicPlayer(currentSong);
            }
        });

        // Botón play/pause
        if (miniPlayerPlayPause != null) {
            miniPlayerPlayPause.setOnClickListener(v -> {
                if (musicPlayer.isPlaying()) {
                    musicPlayer.pause();
                } else {
                    musicPlayer.resume();
                }
            });
        }

        // Botón cerrar
        if (miniPlayerClose != null) {
            miniPlayerClose.setOnClickListener(v -> {
                hideMiniPlayer();
                musicPlayer.stop();
            });
        }
    }

    private void showMiniPlayer(Song song) {
        if (miniPlayerCard == null || song == null) return;

        // Actualizar información
        if (miniPlayerTitle != null) {
            miniPlayerTitle.setText(song.getNombre());
        }
        if (miniPlayerArtist != null) {
            miniPlayerArtist.setText(song.getArtistasString());
        }

        // Cargar imagen del álbum
        if (miniPlayerAlbumArt != null) {
            if (song.getImagenUrl() != null && !song.getImagenUrl().trim().isEmpty()) {
                Glide.with(this)
                        .load(song.getImagenUrl())
                        .placeholder(R.drawable.gradient_album_placeholder)
                        .error(R.drawable.gradient_album_placeholder)
                        .centerCrop()
                        .into(miniPlayerAlbumArt);
            } else {
                miniPlayerAlbumArt.setImageResource(R.drawable.gradient_album_placeholder);
            }
        }

        // Actualizar botón play/pause
        updateMiniPlayerPlayButton();

        // Mostrar con animación
        if (miniPlayerCard.getVisibility() != View.VISIBLE) {
            miniPlayerCard.setVisibility(View.VISIBLE);
            miniPlayerCard.setAlpha(0f);
            miniPlayerCard.setTranslationY(100f);
            miniPlayerCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start();
        }

        Log.d(TAG, "✅ Mini reproductor mostrado para: " + song.getNombre());
    }

    private void hideMiniPlayer() {
        if (miniPlayerCard == null) return;

        miniPlayerCard.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(300)
                .withEndAction(() -> miniPlayerCard.setVisibility(View.GONE))
                .start();

        Log.d(TAG, "🔽 Mini reproductor ocultado");
    }

    private void updateMiniPlayerPlayButton() {
        if (miniPlayerPlayPause == null) return;

        if (musicPlayer.isPlaying()) {
            miniPlayerPlayPause.setImageResource(R.drawable.ic_pause);
            miniPlayerPlayPause.setContentDescription("Pausar");
        } else {
            miniPlayerPlayPause.setImageResource(R.drawable.ic_play_arrow);
            miniPlayerPlayPause.setContentDescription("Reproducir");
        }
    }

    // ========================================
    // ESTADÍSTICAS RÁPIDAS EN EL HEADER
    // ========================================

    private void updateQuickStats(List<Song> songs) {
        if (layoutQuickStats == null || tvTotalSongs == null || tvWithPreview == null) return;

        if (songs == null || songs.isEmpty()) {
            layoutQuickStats.setVisibility(View.GONE);
            return;
        }

        int totalSongs = songs.size();
        long songsWithPreview = songs.stream().filter(Song::hasPreview).count();

        tvTotalSongs.setText(String.format("🎵 %d canciones", totalSongs));
        tvWithPreview.setText(String.format("🎧 %d reproducibles", songsWithPreview));

        // Mostrar con animación
        if (layoutQuickStats.getVisibility() != View.VISIBLE) {
            layoutQuickStats.setVisibility(View.VISIBLE);
            layoutQuickStats.setAlpha(0f);
            layoutQuickStats.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .start();
        }
    }

    // ========================================
    // MÉTODOS PRINCIPALES - CARGA DE DATOS
    // ========================================

    private void loadSongsFromDatabaseEnhanced() {
        if (isLoading) return;

        showLoadingWithAnimation(true);
        showEmptyState(false);
        currentSearchQuery = "";

        Log.d(TAG, "📋 Cargando canciones desde la base de datos...");

        RetrofitClient.getInstance().getApiService().getSongsWithPreview(authToken)
                .enqueue(new Callback<List<Song>>() {
                    @Override
                    public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                        showLoadingWithAnimation(false);
                        swipeRefreshLayout.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Song> songs = response.body();
                            Log.d(TAG, "✅ Canciones cargadas desde BD: " + songs.size());

                            if (songs.isEmpty()) {
                                updateAppState(AppState.LOADED_EMPTY, null);
                                showSnackbarWithAction();
                            } else {
                                updateAppState(AppState.LOADED_WITH_DATA, songs);
                                showSuccess("📋 " + songs.size() + " canciones cargadas desde tu biblioteca");
                                showLibraryStats(songs);
                            }
                        } else {
                            Log.e(TAG, "❌ Error al cargar canciones de la BD. Código: " + response.code());
                            handleApiError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Song>> call, Throwable t) {
                        showLoadingWithAnimation(false);
                        swipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, "❌ Error de conexión al cargar canciones de BD", t);
                        showError("Error de conexión: " + t.getMessage());
                        updateAppState(AppState.ERROR, null);
                    }
                });
    }

    private void searchInDatabase(String query) {
        if (query.trim().isEmpty()) {
            loadSongsFromDatabaseEnhanced();
            return;
        }

        if (isLoading) return;

        showLoadingWithAnimation(true);
        showEmptyState(false);
        currentSearchQuery = query;

        Log.d(TAG, "🔍 Buscando en la base de datos: " + query);

        RetrofitClient.getInstance().getApiService().searchSongsByNameWithPreview(authToken, query)
                .enqueue(new Callback<List<Song>>() {
                    @Override
                    public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                        showLoadingWithAnimation(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Song> songs = response.body();
                            Log.d(TAG, "✅ Resultados de búsqueda en BD: " + songs.size());

                            if (songs.isEmpty()) {
                                showEmptyStateWithMessage(
                                        "😕 No se encontraron canciones en tu biblioteca para:\n" +
                                                "\"" + query + "\"\n\n" +
                                                "💡 Opciones:\n" +
                                                "• Busca en Spotify usando el menú de búsqueda\n" +
                                                "• Carga más música usando el botón ➕\n" +
                                                "• Intenta con otros términos"
                                );
                                showSnackbarSearchSpotify(query);
                            } else {
                                songAdapter.setSongs(songs);
                                showEmptyState(false);
                                updateQuickStats(songs);
                                showSuccess("📋 " + songs.size() + " resultado(s) encontrado(s) para: \"" + query + "\"");
                            }
                        } else {
                            Log.e(TAG, "❌ Error en búsqueda de BD. Código: " + response.code());
                            handleApiError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Song>> call, Throwable t) {
                        showLoadingWithAnimation(false);
                        Log.e(TAG, "❌ Error de conexión en búsqueda de BD", t);
                        showError("Error de conexión en búsqueda: " + t.getMessage());
                        showEmptyStateWithMessage("❌ Error de conexión\n\nRevisa tu internet e intenta de nuevo");
                    }
                });
    }

    private void loadAllSpotifyMusic() {
        if (isLoading) return;

        showLoadingWithAnimation(true);
        showEmptyState(false);

        Log.d(TAG, "📥 Cargando y guardando música desde Spotify...");
        showToast("📥 Descargando música popular desde Spotify...");

        RetrofitClient.getInstance().getApiService().loadAllSongs(authToken)
                .enqueue(new Callback<SpotifySearchResponse>() {
                    @Override
                    public void onResponse(Call<SpotifySearchResponse> call, Response<SpotifySearchResponse> response) {
                        showLoadingWithAnimation(false);

                        if (response.isSuccessful() && response.body() != null) {
                            SpotifySearchResponse spotifyResponse = response.body();

                            if (spotifyResponse.isSuccessWithResults()) {
                                List<Song> songs = spotifyResponse.getCanciones();
                                Log.d(TAG, "✅ Música guardada desde Spotify: " + songs.size());

                                showSuccess("✅ " + songs.size() + " canciones cargadas y guardadas desde Spotify!");
                                loadSongsFromDatabaseEnhanced();
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
                        showLoadingWithAnimation(false);
                        Log.e(TAG, "❌ Error de conexión con Spotify", t);
                        showError("Error de conexión con Spotify: " + t.getMessage());
                    }
                });
    }

    private void searchSpotifyRealTime(String query) {
        if (query.trim().isEmpty()) {
            loadSongsFromDatabaseEnhanced();
            return;
        }

        if (isLoading) return;

        showLoadingWithAnimation(true);
        showEmptyState(false);
        currentSearchQuery = query;

        Log.d(TAG, "🔍 Buscando en tiempo real en Spotify: " + query);
        showToast("🔍 Buscando en Spotify: " + query);

        RetrofitClient.getInstance().getApiService().searchSpotifyRealTimeWithPreview(authToken, query)
                .enqueue(new Callback<SpotifySearchResponse>() {
                    @Override
                    public void onResponse(Call<SpotifySearchResponse> call, Response<SpotifySearchResponse> response) {
                        showLoadingWithAnimation(false);

                        if (response.isSuccessful() && response.body() != null) {
                            SpotifySearchResponse spotifyResponse = response.body();

                            if (spotifyResponse.isSuccessWithResults()) {
                                List<Song> songs = spotifyResponse.getCanciones();
                                Log.d(TAG, "✅ Resultados de Spotify: " + songs.size());

                                songAdapter.setSongs(songs);
                                showEmptyState(false);
                                updateQuickStats(songs);
                                showSuccess("🎵 " + songs.size() + " canciones encontradas en Spotify para: \"" + query + "\"");
                            } else {
                                showEmptyStateWithMessage(
                                        "😕 No se encontraron canciones en Spotify para:\n" +
                                                "\"" + query + "\"\n\n" +
                                                "💡 Intenta con:\n" +
                                                "• 'Bad Bunny'\n" +
                                                "• 'Taylor Swift'\n" +
                                                "• 'The Weeknd'\n" +
                                                "• Nombres de canciones específicas"
                                );
                            }
                        } else {
                            Log.e(TAG, "❌ Error en búsqueda de Spotify. Código: " + response.code());
                            handleApiError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<SpotifySearchResponse> call, Throwable t) {
                        showLoadingWithAnimation(false);
                        Log.e(TAG, "❌ Error de conexión en búsqueda de Spotify", t);
                        showError("Error de conexión en búsqueda de Spotify: " + t.getMessage());
                        showEmptyStateWithMessage("❌ Error de conexión con Spotify\n\nRevisa tu internet e intenta de nuevo");
                    }
                });
    }

    private void loadSpotifyRecommendations() {
        if (isLoading) return;

        showLoadingWithAnimation(true);
        showEmptyState(false);
        currentSearchQuery = "";

        Log.d(TAG, "🎵 Cargando recomendaciones desde Spotify...");
        showToast("🎯 Cargando recomendaciones desde Spotify...");

        RetrofitClient.getInstance().getApiService().getSpotifyRecommendations(authToken)
                .enqueue(new Callback<SpotifySearchResponse>() {
                    @Override
                    public void onResponse(Call<SpotifySearchResponse> call, Response<SpotifySearchResponse> response) {
                        showLoadingWithAnimation(false);
                        swipeRefreshLayout.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            SpotifySearchResponse spotifyResponse = response.body();

                            if (spotifyResponse.isSuccessWithResults()) {
                                List<Song> songs = spotifyResponse.getCanciones();
                                Log.d(TAG, "✅ Recomendaciones recibidas: " + songs.size());

                                songAdapter.setSongs(songs);
                                showEmptyState(false);
                                updateQuickStats(songs);
                                showSuccess("🎯 " + songs.size() + " recomendaciones cargadas desde Spotify");
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
                        showLoadingWithAnimation(false);
                        swipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, "❌ Error de conexión al cargar recomendaciones", t);
                        showError("Error de conexión: " + t.getMessage());
                        showEmptyStateWithMessage("❌ Error de conexión\n\n¡Intenta buscar música manualmente!");
                    }
                });
    }

    // ========================================
    // GESTIÓN DE ESTADOS Y ANIMACIONES
    // ========================================

    private void updateAppState(AppState state, List<Song> songs) {
        switch (state) {
            case LOADING:
                showLoadingWithAnimation(true);
                showEmptyState(false);
                if (layoutQuickStats != null) {
                    layoutQuickStats.setVisibility(View.GONE);
                }
                break;

            case LOADED_WITH_DATA:
                showLoadingWithAnimation(false);
                showEmptyState(false);
                songAdapter.setSongs(songs);
                updateQuickStats(songs);
                animateItemsEntry();
                break;

            case LOADED_EMPTY:
                showLoadingWithAnimation(false);
                showEmptyStateWithMessage(
                        "🎵 Tu biblioteca musical está vacía\n\n" +
                                "¡Presiona el botón ➕ para cargar música desde Spotify!\n\n" +
                                "💡 Prueba buscar: 'Bad Bunny', 'Taylor Swift', 'The Weeknd'"
                );
                if (layoutQuickStats != null) {
                    layoutQuickStats.setVisibility(View.GONE);
                }
                break;

            case ERROR:
                showLoadingWithAnimation(false);
                showEmptyStateWithMessage(
                        "❌ Error de conexión\n\n" +
                                "🔄 Verifica tu internet e intenta de nuevo\n\n" +
                                "📱 Desliza hacia abajo para reintentarlo"
                );
                if (layoutQuickStats != null) {
                    layoutQuickStats.setVisibility(View.GONE);
                }
                break;
        }
    }

    private void animateItemsEntry() {
        if (recyclerViewSongs != null) {
            recyclerViewSongs.scheduleLayoutAnimation();
        }
    }

    private void showLoadingWithAnimation(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setAlpha(0f);
            progressBar.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        } else {
            progressBar.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> progressBar.setVisibility(View.GONE))
                    .start();
        }

        isLoading = show;
        fabLoadMusic.setEnabled(!show);
        invalidateOptionsMenu();
    }

    // ========================================
    // MÉTODOS DE UI Y UTILIDADES
    // ========================================

    // ✅ MODIFICADO: Incluir opción para abrir LibraryActivity
    private void showLoadMusicOptions() {
        String[] options = {
                "📥 Música Popular",
                "🎯 Recomendaciones",
                "🔍 Buscar en Spotify",
                "📚 Ver Mi Biblioteca",  // ← NUEVA OPCIÓN
                "💡 Sugerencias",
                "📊 Ver Estadísticas"
        };

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("🎵 Opciones de Música");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: loadAllSpotifyMusic(); break;
                case 1: loadSpotifyRecommendations(); break;
                case 2: promptSpotifySearch(); break;
                case 3: openLibraryActivity(); break;  // ← NUEVA ACCIÓN
                case 4: showSearchSuggestions(); break;
                case 5:
                    if (!songAdapter.isEmpty()) {
                        showDetailedStats(songAdapter.getAllSongs());
                    } else {
                        showToast("📊 No hay canciones para mostrar estadísticas");
                    }
                    break;
            }
        });
        builder.setNegativeButton("❌ Cancelar", null);
        builder.show();
    }

    private void promptSpotifySearch() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Escribe artista, canción, álbum...");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔍 Buscar en Spotify")
                .setMessage("¿Qué música quieres buscar?")
                .setView(input)
                .setPositiveButton("Buscar", (dialog, which) -> {
                    String query = input.getText().toString().trim();
                    if (!query.isEmpty()) {
                        searchSpotifyRealTime(query);
                    } else {
                        showToast("⚠️ Escribe algo para buscar");
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();

        input.requestFocus();
    }

    private void showSearchSuggestions() {
        String[] suggestions = {
                "Bad Bunny", "Taylor Swift", "The Weeknd", "Dua Lipa",
                "Ed Sheeran", "Drake", "Ariana Grande", "Billie Eilish",
                "reggaeton", "pop", "rock", "bachata", "salsa"
        };

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("💡 Sugerencias de búsqueda");
        builder.setItems(suggestions, (dialog, which) -> {
            String selectedSuggestion = suggestions[which];
            searchSpotifyRealTime(selectedSuggestion);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void showLibraryStats(List<Song> songs) {
        if (songs == null || songs.isEmpty()) return;

        long songsWithPreview = songs.stream().filter(Song::hasPreview).count();
        long totalDuration = songs.stream()
                .filter(song -> song.getDuracion() != null)
                .mapToLong(Song::getDuracion)
                .sum();

        String stats = String.format(
                "📊 Biblioteca: %d canciones • %d con preview • %s total",
                songs.size(),
                songsWithPreview,
                formatDuration(totalDuration)
        );

        Log.d(TAG, stats);

        if (songs.size() > 0) {
            Snackbar.make(recyclerViewSongs, stats, Snackbar.LENGTH_LONG)
                    .setAction("VER DETALLES", v -> showDetailedStats(songs))
                    .show();
        }
    }

    private void showDetailedStats(List<Song> songs) {
        StringBuilder statsBuilder = new StringBuilder();

        // Contar artistas únicos
        long uniqueArtists = songs.stream()
                .flatMap(song -> song.getArtistas().stream())
                .distinct()
                .count();

        // Canción más popular
        Song mostPopular = songs.stream()
                .filter(song -> song.getPopularidad() != null)
                .max((s1, s2) -> Double.compare(s1.getPopularidad(), s2.getPopularidad()))
                .orElse(null);

        // Duración promedio
        double avgDuration = songs.stream()
                .filter(song -> song.getDuracion() != null)
                .mapToInt(Song::getDuracion)
                .average()
                .orElse(0);

        statsBuilder.append("📊 Estadísticas de tu Biblioteca Musical\n\n");
        statsBuilder.append("🎵 Total de canciones: ").append(songs.size()).append("\n");
        statsBuilder.append("🎤 Artistas únicos: ").append(uniqueArtists).append("\n");
        statsBuilder.append("⏱️ Duración promedio: ").append(formatDuration((long)avgDuration)).append("\n");

        if (mostPopular != null) {
            statsBuilder.append("⭐ Más popular: ").append(mostPopular.getNombre())
                    .append(" (").append(String.format("%.0f%%", mostPopular.getPopularidad())).append(")\n");
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📊 Estadísticas")
                .setMessage(statsBuilder.toString())
                .setPositiveButton("Genial", null)
                .show();
    }

    private String formatDuration(long totalMs) {
        if (totalMs <= 0) return "0:00";

        long hours = totalMs / 3600000;
        long minutes = (totalMs % 3600000) / 60000;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    private void handleApiError(int code) {
        if (code == 401) {
            handleUnauthorized();
        } else {
            showError("Error del servidor (Código: " + code + ")");
            showEmptyStateWithMessage("❌ Error del servidor\n\nCódigo: " + code + "\n\nIntenta de nuevo más tarde");
        }
    }

    private void showLoading(boolean show) {
        showLoadingWithAnimation(show);
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSnackbarWithAction() {
        Snackbar.make(recyclerViewSongs, "Tu biblioteca está vacía", Snackbar.LENGTH_LONG)
                .setAction("CARGAR MÚSICA", v -> showLoadMusicOptions())
                .show();
    }

    private void showSnackbarSearchSpotify(String query) {
        Snackbar.make(recyclerViewSongs, "Sin resultados en tu biblioteca", Snackbar.LENGTH_LONG)
                .setAction("BUSCAR EN SPOTIFY", v -> searchSpotifyRealTime(query))
                .show();
    }

    // ========================================
    // LISTENERS DE CANCIONES Y REPRODUCTOR
    // ========================================

    @Override
    public void onSongClick(Song song) {
        Log.d(TAG, "Canción seleccionada: " + song.getNombre());
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
            openMusicPlayer(song);
        }
    }

    private void openMusicPlayer(Song song) {
        if (song == null) {
            showError("❌ Error: No se pudo cargar la información de la canción");
            return;
        }

        try {
            Intent intent = new Intent(this, MusicPlayerActivity.class);
            intent.putExtra(MusicPlayerActivity.EXTRA_SONG, song);
            startActivity(intent);
            Log.d(TAG, "✅ Abriendo reproductor para: " + song.getNombre());

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

    // ========================================
    // IMPLEMENTACIÓN DE MusicPlayerListener
    // ========================================

    @Override
    public void onPlaybackStarted(Song song) {
        runOnUiThread(() -> {
            showSuccess("▶️ Reproduciendo: " + song.getNombre());
            songAdapter.notifyDataSetChanged();
            progressHandler.post(progressRunnable);

            // Mostrar mini reproductor
            showMiniPlayer(song);
            updateMiniPlayerPlayButton();
        });
    }

    @Override
    public void onPlaybackPaused() {
        runOnUiThread(() -> {
            showSuccess("⏸️ Reproducción pausada");
            songAdapter.notifyDataSetChanged();
            progressHandler.removeCallbacks(progressRunnable);

            // Actualizar mini reproductor
            updateMiniPlayerPlayButton();
        });
    }

    @Override
    public void onPlaybackStopped() {
        runOnUiThread(() -> {
            showSuccess("⏹️ Reproducción detenida");
            songAdapter.notifyDataSetChanged();
            progressHandler.removeCallbacks(progressRunnable);

            // Ocultar mini reproductor
            hideMiniPlayer();
        });
    }

    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            showSuccess("✅ Reproducción completada");
            songAdapter.notifyDataSetChanged();
            progressHandler.removeCallbacks(progressRunnable);

            // Ocultar mini reproductor
            hideMiniPlayer();
        });
    }

    @Override
    public void onPlaybackError(String error) {
        runOnUiThread(() -> {
            showError("❌ " + error);
            progressHandler.removeCallbacks(progressRunnable);

            // Ocultar mini reproductor en caso de error
            hideMiniPlayer();
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

        // ✅ CORREGIDO: Configurar SearchView con manejo de errores
        try {
            MenuItem searchItem = menu.findItem(R.id.action_search);
            if (searchItem != null) {
                SearchView searchView = (SearchView) searchItem.getActionView();

                if (searchView != null) {
                    searchView.setQueryHint("🔍 Buscar en biblioteca o Spotify...");
                    searchView.setMaxWidth(Integer.MAX_VALUE);

                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            if (!query.trim().isEmpty()) {
                                searchInDatabase(query);
                            }
                            searchView.clearFocus();
                            return true;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            // Cancelar búsqueda anterior
                            if (searchRunnable != null) {
                                searchHandler.removeCallbacks(searchRunnable);
                            }

                            if (newText.trim().isEmpty()) {
                                loadSongsFromDatabaseEnhanced();
                                return true;
                            }

                            if (newText.length() > 2) {
                                // Búsqueda automática con delay
                                searchRunnable = () -> searchInDatabase(newText);
                                searchHandler.postDelayed(searchRunnable, 800);
                            }
                            return true;
                        }
                    });

                    // Expandir/colapsar SearchView
                    searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                        @Override
                        public boolean onMenuItemActionExpand(MenuItem item) {
                            showToast("💡 Tip: Busca en tu biblioteca o presiona Enter para Spotify");
                            return true;
                        }

                        @Override
                        public boolean onMenuItemActionCollapse(MenuItem item) {
                            if (!currentSearchQuery.isEmpty()) {
                                currentSearchQuery = "";
                                loadSongsFromDatabaseEnhanced();
                            }
                            return true;
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error configurando SearchView: " + e.getMessage());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_logout) {
            confirmLogout();
            return true;
        } else if (id == R.id.action_refresh) {
            handleRefresh();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void confirmLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔐 Cerrar Sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Sí, cerrar", (dialog, which) -> logout())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void handleRefresh() {
        if (currentSearchQuery.isEmpty()) {
            loadSongsFromDatabaseEnhanced();
        } else {
            searchInDatabase(currentSearchQuery);
        }
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
}