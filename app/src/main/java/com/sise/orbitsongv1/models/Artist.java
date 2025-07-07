package com.sise.orbitsongv1.models;

import com.google.gson.annotations.SerializedName;

public class Artist {
    @SerializedName("id")
    private Long id;

    @SerializedName("spotifyArtistId")
    private String spotifyArtistId;

    @SerializedName("nombre")
    private String nombre;

    @SerializedName("imagenUrl")
    private String imagenUrl;

    @SerializedName("generos")
    private String generos;

    @SerializedName("popularidad")
    private Integer popularidad;

    @SerializedName("seguidores")
    private Integer seguidores;

    @SerializedName("spotifyUrl")
    private String spotifyUrl;

    // Constructor vacío
    public Artist() {
    }

    // Constructor con datos básicos
    public Artist(String spotifyArtistId, String nombre) {
        this.spotifyArtistId = spotifyArtistId;
        this.nombre = nombre;
    }

    // Constructor completo
    public Artist(Long id, String spotifyArtistId, String nombre, String imagenUrl,
                  String generos, Integer popularidad, Integer seguidores, String spotifyUrl) {
        this.id = id;
        this.spotifyArtistId = spotifyArtistId;
        this.nombre = nombre;
        this.imagenUrl = imagenUrl;
        this.generos = generos;
        this.popularidad = popularidad;
        this.seguidores = seguidores;
        this.spotifyUrl = spotifyUrl;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSpotifyArtistId() {
        return spotifyArtistId;
    }

    public void setSpotifyArtistId(String spotifyArtistId) {
        this.spotifyArtistId = spotifyArtistId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getGeneros() {
        return generos;
    }

    public void setGeneros(String generos) {
        this.generos = generos;
    }

    public Integer getPopularidad() {
        return popularidad;
    }

    public void setPopularidad(Integer popularidad) {
        this.popularidad = popularidad;
    }

    public Integer getSeguidores() {
        return seguidores;
    }

    public void setSeguidores(Integer seguidores) {
        this.seguidores = seguidores;
    }

    public String getSpotifyUrl() {
        return spotifyUrl;
    }

    public void setSpotifyUrl(String spotifyUrl) {
        this.spotifyUrl = spotifyUrl;
    }

    // Métodos de utilidad

    /**
     * Verifica si el artista es popular (popularidad > 50)
     */
    public boolean isPopular() {
        return popularidad != null && popularidad > 50;
    }

    /**
     * Formatea el número de seguidores de manera legible
     */
    public String getSeguidoresFormatted() {
        if (seguidores == null || seguidores <= 0) {
            return "Sin datos";
        }

        if (seguidores >= 1_000_000) {
            return String.format("%.1fM", seguidores / 1_000_000.0);
        } else if (seguidores >= 1_000) {
            return String.format("%.1fK", seguidores / 1_000.0);
        } else {
            return seguidores.toString();
        }
    }

    /**
     * Verifica si tiene imagen disponible
     */
    public boolean hasImage() {
        return imagenUrl != null && !imagenUrl.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "Artist{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", spotifyArtistId='" + spotifyArtistId + '\'' +
                ", popularidad=" + popularidad +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Artist)) return false;
        Artist artist = (Artist) o;
        return spotifyArtistId != null && spotifyArtistId.equals(artist.spotifyArtistId);
    }

    @Override
    public int hashCode() {
        return spotifyArtistId != null ? spotifyArtistId.hashCode() : 0;
    }
}