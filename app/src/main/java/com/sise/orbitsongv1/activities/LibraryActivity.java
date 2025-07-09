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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.sise.orbitsongv1.R;
import com.sise.orbitsongv1.adapters.SongAdapter;
import com.sise.orbitsongv1.models.Song;
import com.sise.orbitsongv1.services.MusicPlayerService;
import com.sise.orbitsongv1.services.RetrofitClient;
import com.sise.orbitsongv1.utils.Constants;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryActivity extends AppCompatActivity
        implements SongAdapter.OnSongClickListener, MusicPlayerService.MusicPlayerListener {

    private static final String TAG = "LibraryActivity";

    // UI Components
    private Toolbar toolbar;
    private RecyclerView recyclerViewSongs;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyState;
    private TextView tvLibraryStats;
    private FloatingActionButton fabSearch;

    // Mini Player Components
    private View miniPlayerCard;
    private ImageView miniPlayerAlbumArt;
    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private ImageButton miniPlayerPlayPause;
    private ImageButton miniPlayerClose;

    // Logic Variables
    private SongAdapter songAdapter;
    private String authToken;
    private boolean isLoading = false;
    private MusicPlayerService musicPlayer;
    private Handler progressHandler;
    private Runnable progressRunnable;

    // Search Variables
    private Handler searchHandler;
    private Runnable searchRunnable;
    private String currentSearchQuery = "";
    private List<Song> allSongs; // Lista completa para filtrado local

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        initViews();
        setupToolbar();
        setupRecyclerView();
        getAuthToken();
        setupListeners();
        setupMusicPlayer();
        setupSearchHandler();

        // Cargar todas las canciones al iniciar
        loadAllSongsFromDatabase();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewSongs = findViewById(R.id.recycler_view_songs);
        progressBar = findViewById(R.id.progress_bar);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        tvLibraryStats = findViewById(R.id.tv_library_stats);
        fabSearch = findViewById(R.id.fab_search);

        // Mini Player
        miniPlayerCard = findViewById(R.id.mini_player_card);
        miniPlayerAlbumArt = findViewById(R.id.mini_player_album_art);
        miniPlayerTitle = findViewById(R.id.mini_player_title);
        miniPlayerArtist = findViewById(R.id.mini_player_artist);
        miniPlayerPlayPause = findViewById(R.id.mini_player_play_pause);
        miniPlayerClose = findViewById(R.id.mini_player_close);

        setupMiniPlayerListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("📚 Mi Biblioteca Musical");
        }
    }

    private void setupRecyclerView() {
        songAdapter = new SongAdapter(this);
        songAdapter.setOnSongClickListener(this);
        recyclerViewSongs.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSongs.setAdapter(songAdapter);
    }

    private void getAuthToken() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        String token = prefs.getString(Constants.TOKEN_KEY, null);
        if (token != null) {
            authToken = "Bearer " + token;
            Log.d(TAG, "✅ Token obtenido correctamente");
        } else {
            Log.e(TAG, "❌ No se encontró token");
            showError("Error de autenticación");
            finish();
        }
    }

    private void setupListeners() {
        swipeRefreshLayout.setOnRefreshListener(this::loadAllSongsFromDatabase);

        fabSearch.setOnClickListener(v -> {
            // Abrir diálogo de búsqueda avanzada
            showAdvancedSearchDialog();
        });
    }

    private void setupMusicPlayer() {
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
    }

    private void setupSearchHandler() {
        searchHandler = new Handler(Looper.getMainLooper());
    }

    private void setupMiniPlayerListeners() {
        if (miniPlayerCard == null) return;

        miniPlayerCard.setOnClickListener(v -> {
            Song currentSong = musicPlayer.getCurrentSong();
            if (currentSong != null) {
                openMusicPlayer(currentSong);
            }
        });

        if (miniPlayerPlayPause != null) {
            miniPlayerPlayPause.setOnClickListener(v -> {
                if (musicPlayer.isPlaying()) {
                    musicPlayer.pause();
                } else {
                    musicPlayer.resume();
                }
            });
        }

        if (miniPlayerClose != null) {
            miniPlayerClose.setOnClickListener(v -> {
                hideMiniPlayer();
                musicPlayer.stop();
            });
        }
    }

    // ========================================
    // MÉTODOS PRINCIPALES - CARGA DE DATOS
    // ========================================

    private void loadAllSongsFromDatabase() {
        if (isLoading) return;

        showLoading(true);
        showEmptyState(false);
        currentSearchQuery = "";

        Log.d(TAG, "📋 Cargando TODAS las canciones desde la base de datos...");

        // Usar el endpoint /todas que no filtra por preview
        RetrofitClient.getInstance().getApiService().getAllSongs(authToken)
                .enqueue(new Callback<List<Song>>() {
                    @Override
                    public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            allSongs = response.body();
                            Log.d(TAG, "✅ TODAS las canciones cargadas: " + allSongs.size());

                            if (allSongs.isEmpty()) {
                                showEmptyStateWithMessage(
                                        "📚 Tu biblioteca está vacía\n\n" +
                                                "Regresa al inicio para cargar música desde Spotify"
                                );
                            } else {
                                songAdapter.setSongs(allSongs);
                                showEmptyState(false);
                                updateLibraryStats(allSongs);
                                showSuccess("📚 " + allSongs.size() + " canciones en tu biblioteca");
                            }
                        } else {
                            Log.e(TAG, "❌ Error al cargar canciones. Código: " + response.code());
                            handleApiError(response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Song>> call, Throwable t) {
                        showLoading(false);
                        swipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, "❌ Error de conexión", t);
                        showError("Error de conexión: " + t.getMessage());
                        showEmptyStateWithMessage("❌ Error de conexión\n\nVerifica tu internet e intenta de nuevo");
                    }
                });
    }

    private void filterSongsLocally(String query) {
        if (allSongs == null || allSongs.isEmpty()) {
            loadAllSongsFromDatabase();
            return;
        }

        if (query.trim().isEmpty()) {
            songAdapter.setSongs(allSongs);
            updateLibraryStats(allSongs);
            currentSearchQuery = "";
            return;
        }

        currentSearchQuery = query;
        String queryLower = query.toLowerCase().trim();

        List<Song> filteredSongs = allSongs.stream()
                .filter(song ->
                        song.getNombre().toLowerCase().contains(queryLower) ||
                                song.getArtistasString().toLowerCase().contains(queryLower) ||
                                (song.getAlbum() != null && song.getAlbum().toLowerCase().contains(queryLower))
                )
                .collect(java.util.stream.Collectors.toList());

        songAdapter.setSongs(filteredSongs);
        updateLibraryStats(filteredSongs);

        if (filteredSongs.isEmpty()) {
            showEmptyStateWithMessage(
                    "🔍 No se encontraron resultados para:\n\"" + query + "\"\n\n" +
                            "💡 Intenta con otros términos o limpia la búsqueda"
            );
        } else {
            showEmptyState(false);
            showToast("🔍 " + filteredSongs.size() + " resultado(s) encontrado(s)");
        }
    }

    // ========================================
    // MÉTODOS DE UI Y UTILIDADES
    // ========================================

    private void updateLibraryStats(List<Song> songs) {
        if (tvLibraryStats == null || songs == null) return;

        long songsWithPreview = songs.stream().filter(Song::hasPreview).count();
        long totalDuration = songs.stream()
                .filter(song -> song.getDuracion() != null)
                .mapToLong(Song::getDuracion)
                .sum();

        String stats = String.format(
                "🎵 %d canciones • 🎧 %d reproducibles • ⏱️ %s total",
                songs.size(),
                songsWithPreview,
                formatDuration(totalDuration)
        );

        tvLibraryStats.setText(stats);
        tvLibraryStats.setVisibility(View.VISIBLE);
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

    private void showAdvancedSearchDialog() {
        String[] searchOptions = {
                "🎵 Por nombre de canción",
                "🎤 Por artista",
                "💿 Por álbum",
                "🎧 Solo con preview",
                "🔄 Mostrar todas"
        };

        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("🔍 Buscar en tu biblioteca");
        builder.setItems(searchOptions, (dialog, which) -> {
            switch (which) {
                case 0:
                case 1:
                case 2:
                    promptSearchInput(which);
                    break;
                case 3:
                    filterOnlyWithPreview();
                    break;
                case 4:
                    loadAllSongsFromDatabase();
                    break;
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void promptSearchInput(int searchType) {
        String[] hints = {"canción", "artista", "álbum"};
        String hint = "Escribe " + hints[searchType] + "...";

        android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(hint);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔍 Buscar por " + hints[searchType])
                .setView(input)
                .setPositiveButton("Buscar", (dialog, which) -> {
                    String query = input.getText().toString().trim();
                    if (!query.isEmpty()) {
                        filterSongsLocally(query);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();

        input.requestFocus();
    }

    private void filterOnlyWithPreview() {
        if (allSongs == null || allSongs.isEmpty()) {
            loadAllSongsFromDatabase();
            return;
        }

        List<Song> songsWithPreview = allSongs.stream()
                .filter(Song::hasPreview)
                .collect(java.util.stream.Collectors.toList());

        songAdapter.setSongs(songsWithPreview);
        updateLibraryStats(songsWithPreview);
        showToast("🎧 " + songsWithPreview.size() + " canciones reproducibles");

        if (songsWithPreview.isEmpty()) {
            showEmptyStateWithMessage(
                    "🎧 No hay canciones reproducibles\n\n" +
                            "Las canciones necesitan tener preview de Spotify para reproducirse"
            );
        } else {
            showEmptyState(false);
        }
    }

    private void handleApiError(int code) {
        if (code == 401) {
            showError("Sesión expirada");
            finish();
        } else {
            showError("Error del servidor (Código: " + code + ")");
            showEmptyStateWithMessage("❌ Error del servidor\n\nIntenta de nuevo más tarde");
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        isLoading = show;
        fabSearch.setEnabled(!show);
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

    // ========================================
    // MINI REPRODUCTOR
    // ========================================

    private void showMiniPlayer(Song song) {
        if (miniPlayerCard == null || song == null) return;

        if (miniPlayerTitle != null) {
            miniPlayerTitle.setText(song.getNombre());
        }
        if (miniPlayerArtist != null) {
            miniPlayerArtist.setText(song.getArtistasString());
        }

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

        updateMiniPlayerPlayButton();

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
    }

    private void hideMiniPlayer() {
        if (miniPlayerCard == null) return;

        miniPlayerCard.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(300)
                .withEndAction(() -> miniPlayerCard.setVisibility(View.GONE))
                .start();
    }

    private void updateMiniPlayerPlayButton() {
        if (miniPlayerPlayPause == null) return;

        if (musicPlayer.isPlaying()) {
            miniPlayerPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            miniPlayerPlayPause.setImageResource(R.drawable.ic_play_arrow);
        }
    }

    // ========================================
    // LISTENERS DE CANCIONES
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
            songAdapter.notifyDataSetChanged();
            progressHandler.post(progressRunnable);
            showMiniPlayer(song);
            updateMiniPlayerPlayButton();
        });
    }

    @Override
    public void onPlaybackPaused() {
        runOnUiThread(() -> {
            songAdapter.notifyDataSetChanged();
            progressHandler.removeCallbacks(progressRunnable);
            updateMiniPlayerPlayButton();
        });
    }

    @Override
    public void onPlaybackStopped() {
        runOnUiThread(() -> {
            songAdapter.notifyDataSetChanged();
            progressHandler.removeCallbacks(progressRunnable);
            hideMiniPlayer();
        });
    }

    @Override
    public void onPlaybackCompleted() {
        runOnUiThread(() -> {
            songAdapter.notifyDataSetChanged();
            progressHandler.removeCallbacks(progressRunnable);
            hideMiniPlayer();
        });
    }

    @Override
    public void onPlaybackError(String error) {
        runOnUiThread(() -> {
            showError("❌ " + error);
            progressHandler.removeCallbacks(progressRunnable);
            hideMiniPlayer();
        });
    }

    @Override
    public void onProgressUpdate(int currentPosition, int duration) {
        // Actualizar progreso si es necesario
    }

    // ========================================
    // MENÚS Y NAVEGACIÓN
    // ========================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_library, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setQueryHint("🔍 Buscar en tu biblioteca...");
                searchView.setMaxWidth(Integer.MAX_VALUE);

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        filterSongsLocally(query);
                        searchView.clearFocus();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if (searchRunnable != null) {
                            searchHandler.removeCallbacks(searchRunnable);
                        }

                        if (newText.trim().isEmpty()) {
                            filterSongsLocally("");
                            return true;
                        }

                        if (newText.length() > 2) {
                            searchRunnable = () -> filterSongsLocally(newText);
                            searchHandler.postDelayed(searchRunnable, 500);
                        }
                        return true;
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_filter) {
            showAdvancedSearchDialog();
            return true;
        } else if (id == R.id.action_refresh) {
            loadAllSongsFromDatabase();
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