package com.sise.orbitsongv1.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SpotifySearchResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("query")
    private String query;

    @SerializedName("total")
    private int total;

    @SerializedName("canciones")
    private List<Song> canciones;

    // Constructor vacío
    public SpotifySearchResponse() {
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getQuery() {
        return query;
    }

    public int getTotal() {
        return total;
    }

    public List<Song> getCanciones() {
        return canciones;
    }

    // Setters
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void setCanciones(List<Song> canciones) {
        this.canciones = canciones;
    }

    // Métodos de utilidad
    public boolean hasCanciones() {
        return canciones != null && !canciones.isEmpty();
    }

    public boolean isSuccessWithResults() {
        return success && hasCanciones();
    }
}