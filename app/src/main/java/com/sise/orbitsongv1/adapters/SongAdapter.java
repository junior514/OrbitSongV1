package com.sise.orbitsongv1.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.sise.orbitsongv1.R;
import com.sise.orbitsongv1.models.Song;
import com.sise.orbitsongv1.services.MusicPlayerService;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private Context context;
    private List<Song> songs;
    private OnSongClickListener listener;
    private MusicPlayerService musicPlayer;

    public interface OnSongClickListener {
        void onSongClick(Song song);
        void onSongPlayClick(Song song);
    }

    public SongAdapter(Context context) {
        this.context = context;
        this.songs = new ArrayList<>();
        this.musicPlayer = MusicPlayerService.getInstance();
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs != null ? songs : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnSongClickListener(OnSongClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public class SongViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivAlbumCover;
        private TextView tvSongTitle;
        private TextView tvArtistName;
        private TextView tvAlbumName;
        private TextView tvDuration;
        private MaterialButton btnPlay;
        private ImageButton btnFavorite;
        private Chip chipPreview;
        private TextView tvPopularity;
        private View layoutPopularity;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);

            ivAlbumCover = itemView.findViewById(R.id.iv_album_cover);
            tvSongTitle = itemView.findViewById(R.id.tv_song_title);
            tvArtistName = itemView.findViewById(R.id.tv_artist_name);
            tvAlbumName = itemView.findViewById(R.id.tv_album_name);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            btnPlay = itemView.findViewById(R.id.btn_play);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
            chipPreview = itemView.findViewById(R.id.chip_preview);
            tvPopularity = itemView.findViewById(R.id.tv_popularity);
            layoutPopularity = itemView.findViewById(R.id.layout_popularity);
        }

        public void bind(Song song) {
            // Información básica
            tvSongTitle.setText(song.getNombre() != null ? song.getNombre() : "Canción sin nombre");
            tvArtistName.setText(song.getArtistasString());
            tvAlbumName.setText(song.getAlbum() != null ? song.getAlbum() : "Álbum desconocido");
            tvDuration.setText(song.getDuracionFormatted());

            // Cargar imagen del álbum
            loadAlbumImage(song);

            // Configurar estado del botón de reproducir
            configurePlayButton(song);

            // Configurar botón de favoritos
            configureFavoriteButton(song);

            // Mostrar indicador de preview
            configurePreviewIndicator(song);

            // Mostrar popularidad
            configurePopularityIndicator(song);

            // Configurar listeners
            setupClickListeners(song);
        }

        private void loadAlbumImage(Song song) {
            if (song.getImagenUrl() != null && !song.getImagenUrl().trim().isEmpty()) {
                RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .placeholder(R.drawable.gradient_album_placeholder)
                        .error(R.drawable.gradient_album_placeholder)
                        .diskCacheStrategy(DiskCacheStrategy.ALL);

                Glide.with(context)
                        .load(song.getImagenUrl())
                        .apply(options)
                        .into(ivAlbumCover);
            } else {
                ivAlbumCover.setImageResource(R.drawable.gradient_album_placeholder);
            }
        }

        private void configurePlayButton(Song song) {
            // Verificar si esta canción se está reproduciendo actualmente
            boolean isCurrentSong = musicPlayer.getCurrentSong() != null &&
                    musicPlayer.getCurrentSong().getId() != null &&
                    musicPlayer.getCurrentSong().getId().equals(song.getId());

            boolean isPlaying = musicPlayer.isPlaying();

            if (isCurrentSong && isPlaying) {
                btnPlay.setIcon(context.getDrawable(R.drawable.ic_pause));
                btnPlay.setContentDescription("Pausar");
            } else {
                btnPlay.setIcon(context.getDrawable(R.drawable.ic_play_arrow));
                btnPlay.setContentDescription("Reproducir");
            }

            // Verificar si tiene preview disponible
            if (!song.hasPreview()) {
                btnPlay.setEnabled(false);
                btnPlay.setAlpha(0.5f);
            } else {
                btnPlay.setEnabled(true);
                btnPlay.setAlpha(1.0f);
            }
        }

        private void configureFavoriteButton(Song song) {
            // TODO: Implementar lógica de favoritos
            // Por ahora, mostrar el botón vacío
            btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            btnFavorite.setSelected(false);
        }

        private void configurePreviewIndicator(Song song) {
            if (song.hasPreview()) {
                chipPreview.setVisibility(View.VISIBLE);
                chipPreview.setText("Preview 30s");
            } else {
                chipPreview.setVisibility(View.GONE);
            }
        }

        private void configurePopularityIndicator(Song song) {
            if (song.getPopularidad() != null && song.getPopularidad() > 0) {
                layoutPopularity.setVisibility(View.VISIBLE);
                tvPopularity.setText(String.format("%.0f%%", song.getPopularidad()));
            } else {
                layoutPopularity.setVisibility(View.GONE);
            }
        }

        private void setupClickListeners(Song song) {
            // Click en toda la card
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSongClick(song);
                }
            });

            // Click en botón de reproducir
            btnPlay.setOnClickListener(v -> {
                if (song.hasPreview() && listener != null) {
                    listener.onSongPlayClick(song);
                }
            });

            // Click en botón de favoritos
            btnFavorite.setOnClickListener(v -> {
                // TODO: Implementar funcionalidad de favoritos
                toggleFavorite(song);
            });
        }

        private void toggleFavorite(Song song) {
            // Animación temporal del botón
            boolean isSelected = btnFavorite.isSelected();
            btnFavorite.setSelected(!isSelected);

            if (!isSelected) {
                btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                // TODO: Agregar a favoritos en el backend
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                // TODO: Quitar de favoritos en el backend
            }
        }
    }

    // Método para actualizar un elemento específico cuando cambie el estado de reproducción
    public void updatePlayingState() {
        notifyDataSetChanged();
    }

    // Método para obtener la posición de una canción
    public int getPositionForSong(Song song) {
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).getId() != null && songs.get(i).getId().equals(song.getId())) {
                return i;
            }
        }
        return -1;
    }

    // Método para agregar canciones incrementalmente
    public void addSongs(List<Song> newSongs) {
        if (newSongs != null && !newSongs.isEmpty()) {
            int startPosition = songs.size();
            songs.addAll(newSongs);
            notifyItemRangeInserted(startPosition, newSongs.size());
        }
    }

    // Método para limpiar la lista
    public void clearSongs() {
        int size = songs.size();
        songs.clear();
        notifyItemRangeRemoved(0, size);
    }

    // Método para obtener todas las canciones
    public List<Song> getAllSongs() {
        return new ArrayList<>(songs);
    }

    // Método para verificar si hay canciones
    public boolean isEmpty() {
        return songs.isEmpty();
    }
}