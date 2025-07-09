package com.sise.orbitsongv1.services;

import com.sise.orbitsongv1.models.LoginRequest;
import com.sise.orbitsongv1.models.LoginResponse;
import com.sise.orbitsongv1.models.Song;
import com.sise.orbitsongv1.models.User;
import com.sise.orbitsongv1.models.SpotifySearchResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // ===============================================
    // ENDPOINTS DE AUTENTICACIÓN
    // ===============================================

    @POST("api/auth/login")
    @Headers("Content-Type: application/json")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("api/auth/register")
    @Headers("Content-Type: application/json")
    Call<LoginResponse> register(@Body User user);

    // ===============================================
    // ENDPOINTS DE CANCIONES - BASE DE DATOS
    // ===============================================

    /**
     * 📋 Obtener canciones principales (con preview por defecto)
     */
    @GET("api/canciones")
    @Headers("Content-Type: application/json")
    Call<List<Song>> getSongs(@Header("Authorization") String authToken);

    /**
     * 🎵 PRIORIDAD: Obtener solo canciones CON preview
     */
    @GET("api/canciones/con-preview")
    @Headers("Content-Type: application/json")
    Call<List<Song>> getSongsWithPreview(@Header("Authorization") String authToken);

    /**
     * 📚 Obtener TODAS las canciones sin filtros (incluso sin preview)
     */
    @GET("api/canciones/todas")
    @Headers("Content-Type: application/json")
    Call<List<Song>> getAllSongs(@Header("Authorization") String authToken);

    // ===============================================
    // ENDPOINTS DE BÚSQUEDA EN BASE DE DATOS
    // ===============================================

    /**
     * 🔍 Buscar canciones por nombre en la base de datos
     */
    @GET("api/canciones/buscar/nombre")
    @Headers("Content-Type: application/json")
    Call<List<Song>> searchSongsByName(
            @Header("Authorization") String authToken,
            @Query("nombre") String nombre
    );

    /**
     * 🎵 PRIORIDAD: Buscar canciones por nombre CON preview
     */
    @GET("api/canciones/buscar/nombre/con-preview")
    @Headers("Content-Type: application/json")
    Call<List<Song>> searchSongsByNameWithPreview(
            @Header("Authorization") String authToken,
            @Query("nombre") String nombre
    );

    @GET("api/canciones/buscar/artista")
    @Headers("Content-Type: application/json")
    Call<List<Song>> searchSongsByArtist(
            @Header("Authorization") String authToken,
            @Query("artista") String artista
    );

    @GET("api/canciones/buscar/album")
    @Headers("Content-Type: application/json")
    Call<List<Song>> searchSongsByAlbum(
            @Header("Authorization") String authToken,
            @Query("album") String album
    );

    // ===============================================
    // ENDPOINTS DE SPOTIFY - BÚSQUEDA EN TIEMPO REAL
    // ===============================================

    /**
     * 🔍 Buscar canciones en tiempo real desde Spotify (sin guardar en BD)
     */
    @GET("api/spotify/buscar-tiempo-real")
    @Headers("Content-Type: application/json")
    Call<SpotifySearchResponse> searchSpotifyRealTime(
            @Header("Authorization") String authToken,
            @Query("query") String searchTerm
    );

    /**
     * 🎵 PRIORIDAD: Buscar canciones CON preview en tiempo real
     */
    @GET("api/spotify/buscar-tiempo-real/con-preview")
    @Headers("Content-Type: application/json")
    Call<SpotifySearchResponse> searchSpotifyRealTimeWithPreview(
            @Header("Authorization") String authToken,
            @Query("query") String searchTerm
    );

    /**
     * 🎯 Obtener recomendaciones populares desde Spotify
     */
    @GET("api/spotify/recomendaciones")
    @Headers("Content-Type: application/json")
    Call<SpotifySearchResponse> getSpotifyRecommendations(
            @Header("Authorization") String authToken
    );

    // ===============================================
    // ENDPOINTS DE SPOTIFY - CARGAR Y GUARDAR EN BD
    // ===============================================

    /**
     * 📥 Cargar y guardar canciones generales desde Spotify
     */
    @POST("api/spotify/canciones/cargar-todas")
    @Headers("Content-Type: application/json")
    Call<SpotifySearchResponse> loadAllSongs(@Header("Authorization") String authToken);

    /**
     * 🎵 CLAVE: Cargar canciones CON preview garantizado desde Spotify
     */
    @POST("api/spotify/cargar-con-preview-garantizado")
    @Headers("Content-Type: application/json")
    Call<SpotifySearchResponse> loadSongsWithPreview(@Header("Authorization") String authToken);

    /**
     * 🎧 ALTERNATIVO: Cargar preview público (para testing)
     */
    @POST("api/spotify/cargar-preview-publico")
    @Headers("Content-Type: application/json")
    Call<SpotifySearchResponse> loadSongsWithPreviewPublic();

    // ===============================================
    // ENDPOINTS DE DEBUG Y TESTING
    // ===============================================

    /**
     * 🔍 Debug público - verificar estado del backend
     */
    @GET("api/spotify/debug-preview-public")
    @Headers("Content-Type: application/json")
    Call<Object> debugPreviewPublic();

    /**
     * 🎤 Test múltiples artistas para verificar preview
     */
    @GET("api/spotify/test-multiple-artists")
    @Headers("Content-Type: application/json")
    Call<Object> testMultipleArtists();

    /**
     * 📊 Obtener estadísticas de preview
     */
    @GET("api/spotify/estadisticas-preview")
    @Headers("Content-Type: application/json")
    Call<Object> getPreviewStats(@Header("Authorization") String authToken);

    /**
     * 🧪 Debug de artista específico
     */
    @GET("api/spotify/debug-single-artist")
    @Headers("Content-Type: application/json")
    Call<Object> debugSingleArtist(@Query("artist") String artist);

    /**
     * 🔧 Verificar configuración de Spotify
     */
    @GET("api/spotify/verify-spotify-config")
    @Headers("Content-Type: application/json")
    Call<Object> verifySpotifyConfig();

    /**
     * ❤️ Endpoint de salud del servicio
     */
    @GET("api/spotify/health")
    @Headers("Content-Type: application/json")
    Call<Object> healthCheck();
}