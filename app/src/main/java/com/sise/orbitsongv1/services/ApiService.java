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
    // 🆕 ENDPOINTS PARA LISTAR CANCIONES DE LA BD
    // ===============================================

    /**
     * 📋 Obtener todas las canciones guardadas en la base de datos
     */
    @GET("api/canciones")
    @Headers("Content-Type: application/json")
    Call<List<Song>> getSongs(@Header("Authorization") String authToken);

    /**
     * 🎵 Obtener solo canciones con preview de la base de datos
     */
    @GET("api/canciones/con-preview")
    @Headers("Content-Type: application/json")
    Call<List<Song>> getSongsWithPreview(@Header("Authorization") String authToken);

    /**
     * 🔍 DEBUG: Obtener TODAS las canciones sin filtros
     */
    @GET("api/canciones/todas")
    @Headers("Content-Type: application/json")
    Call<List<Song>> getAllSongsDebug(@Header("Authorization") String authToken);

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
     * 🎵 Buscar canciones por nombre con preview en la base de datos
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
    // ENDPOINTS DE SPOTIFY - BÚSQUEDA Y GUARDADO EN BD
    // ===============================================

    /**
     * 📥 Cargar y guardar nuevas canciones desde Spotify
     */
    @POST("api/spotify/canciones/cargar-todas")
    @Headers("Content-Type: application/json")
    Call<SpotifySearchResponse> loadAllSongs(@Header("Authorization") String authToken);

    @GET("api/spotify/buscar")
    @Headers("Content-Type: application/json")
    Call<List<Song>> searchSpotifyMusic(
            @Header("Authorization") String authToken,
            @Query("nombre") String searchTerm
    );

    // ===============================================
    // 🚀 NUEVOS: ENDPOINTS DE BÚSQUEDA EN TIEMPO REAL
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
     * 🎵 Buscar canciones con preview en tiempo real desde Spotify
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
}