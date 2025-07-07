package com.sise.orbitsongv1.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Song implements Serializable {

    private static final long serialVersionUID = 1L;

    @SerializedName("id")
    private String id;

    @SerializedName("nombre")
    private String nombre;

    // ✅ SIMPLIFICADO: Sin TypeAdapter, el backend envía List<String> directamente
    @SerializedName("artistas")
    private List<String> artistas;

    @SerializedName("album")
    private String album;

    @SerializedName("duracion")
    private Integer duracion;

    @SerializedName("imagenUrl")
    private String imagenUrl;

    @SerializedName("previewUrl")
    private String previewUrl;

    @SerializedName("popularidad")
    private Double popularidad;

    @SerializedName("spotifyId")
    private String spotifyId;

    // Constructores
    public Song() {
        this.artistas = new ArrayList<>();
    }

    public Song(String id, String nombre, List<String> artistas, String album,
                Integer duracion, String imagenUrl, String previewUrl,
                Double popularidad, String spotifyId) {
        this.id = id;
        this.nombre = nombre;
        this.artistas = artistas != null ? artistas : new ArrayList<>();
        this.album = album;
        this.duracion = duracion;
        this.imagenUrl = imagenUrl;
        this.previewUrl = previewUrl;
        this.popularidad = popularidad;
        this.spotifyId = spotifyId;
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<String> getArtistas() {
        return artistas != null ? artistas : new ArrayList<>();
    }

    public void setArtistas(List<String> artistas) {
        this.artistas = artistas != null ? artistas : new ArrayList<>();
    }

    // ✅ Método para obtener artistas como string concatenado
    public String getArtistasString() {
        if (artistas == null || artistas.isEmpty()) {
            return "Artista desconocido";
        }

        if (artistas.size() == 1) {
            return artistas.get(0);
        }

        return String.join(", ", artistas);
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public Integer getDuracion() {
        return duracion;
    }

    public void setDuracion(Integer duracion) {
        this.duracion = duracion;
    }

    // ✅ Método para formatear duración
    public String getDuracionFormatted() {
        if (duracion == null || duracion <= 0) {
            return "0:00";
        }

        int minutes = duracion / 60000;
        int seconds = (duracion % 60000) / 1000;
        return String.format("%d:%02d", minutes, seconds);
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    // ✅ Método para verificar si tiene preview
    public boolean hasPreview() {
        return previewUrl != null && !previewUrl.trim().isEmpty();
    }

    public Double getPopularidad() {
        return popularidad;
    }

    public void setPopularidad(Double popularidad) {
        this.popularidad = popularidad;
    }

    public String getSpotifyId() {
        return spotifyId;
    }

    public void setSpotifyId(String spotifyId) {
        this.spotifyId = spotifyId;
    }

    // ✅ Métodos de utilidad
    @Override
    public String toString() {
        return "Song{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", artistas=" + artistas +
                ", album='" + album + '\'' +
                ", duracion=" + duracion +
                ", hasPreview=" + hasPreview() +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Song song = (Song) obj;
        return id != null ? id.equals(song.id) : song.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}