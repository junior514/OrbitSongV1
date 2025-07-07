package com.sise.orbitsongv1.utils;

public class Constants {
    // URLs del servidor
    public static final String BASE_URL = "http://10.0.2.2:8080/";
    // Para dispositivo real, usar la IP de tu PC en la red local:
    // public static final String BASE_URL = "http://192.168.1.XXX:8080/";

    // Endpoints
    public static final String LOGIN_ENDPOINT = "api/auth/login";
    public static final String REGISTER_ENDPOINT = "api/auth/register";
    public static final String VERIFY_TOKEN_ENDPOINT = "api/auth/verify";

    // SharedPreferences
    public static final String PREFS_NAME = "OrbitSongPrefs";
    public static final String TOKEN_KEY = "auth_token";
    public static final String USER_KEY = "user_key"; // Para compatibilidad
    public static final String USER_EMAIL_KEY = "user_email";
    public static final String USER_NAME_KEY = "user_name";
    public static final String USER_ID_KEY = "user_id";

    // Códigos de respuesta HTTP
    public static final int HTTP_OK = 200;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_INTERNAL_ERROR = 500;

    // Timeouts (en segundos)
    public static final int CONNECT_TIMEOUT = 30;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;
}