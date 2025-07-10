# 🎵 OrbitSong

OrbitSong es una aplicación Android moderna que te permite descubrir, reproducir y gestionar música a través de la integración con Spotify API. Con una interfaz intuitiva y funcionalidades avanzadas, OrbitSong ofrece una experiencia musical completa.

## 📱 Características Principales

### 🎧 Reproducción de Música
- Mini reproductor flotante con controles básicos
- Reproductor completo con interfaz inmersiva
- Reproducción en tiempo real de previews de Spotify
- Control de reproducción (play, pause, stop)

### 🔍 Búsqueda y Descubrimiento
- Búsqueda en tiempo real en Spotify
- Búsqueda en biblioteca local con filtros
- Recomendaciones personalizadas desde Spotify
- Música popular y tendencias

### 📚 Gestión de Biblioteca
- Biblioteca personal con canciones guardadas
- Estadísticas detalladas de tu colección musical
- Organización por artistas y géneros
- Sincronización con base de datos local

### 🌟 Experiencia de Usuario
- Interfaz moderna con Material Design
- Animaciones fluidas y transiciones
- Modo oscuro/claro (adaptable)
- Navegación intuitiva con bottom navigation

## 🛠️ Tecnologías Utilizadas

### Frontend (Android)
- **Java** - Lenguaje principal
- **Android SDK** - Framework nativo
- **Material Design** - Componentes UI
- **Retrofit** - Cliente HTTP para API REST
- **Glide** - Carga y gestión de imágenes
- **RecyclerView** - Listas eficientes

### Backend & API
- **Spotify Web API** - Integración musical
- **API REST personalizada** - Backend propio
- **Autenticación JWT** - Seguridad de sesiones
- **Base de datos** - Almacenamiento local/remoto

### Arquitectura
- **MVVM** (Model-View-ViewModel)
- **Repository Pattern** - Gestión de datos
- **LiveData** - Observables reactivos
- **Singleton Services** - Servicios compartidos

## 🗄️ Diagrama de Base de Datos
![image](https://github.com/user-attachments/assets/ed7bea84-7b0b-449a-848b-a683f1bcf02f)


### Esquema MySQL

*[Inserta aquí tu diagrama de base de datos MySQL]*

![Diagrama de Base de Datos](ruta/a/tu/diagrama.png)

### Descripción de Tablas

**Usuarios (users)**
- ID, nombre, email, contraseña
- Información de perfil y preferencias

**Canciones (songs)**
- Metadatos de canciones
- Referencias a Spotify API

**Bibliotecas (libraries)**
- Relación usuario-canción
- Estadísticas de reproducción

**Sesiones (sessions)**
- Gestión de autenticación JWT
- Tokens de acceso y refresh

## 📋 Requisitos del Sistema

### Mínimos
- Android 5.0 (API nivel 21) o superior
- 2 GB RAM mínimo
- 100 MB de espacio libre
- Conexión a Internet para funcionalidades de Spotify

### Recomendados
- Android 8.0 (API nivel 26) o superior
- 4 GB RAM o más
- 500 MB de espacio libre
- Conexión WiFi para mejor experiencia

## 🚀 Instalación

### Para Desarrolladores

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/junior514/OrbitSongV1
   cd orbitsong
   ```

2. **Configurar Android Studio**
   - Abrir el proyecto en Android Studio
   - Sync del proyecto con Gradle
   - Configurar SDK de Android (mínimo API 21)

3. **Configurar Spotify API**
   ```java
   // En Constants.java
   public static final String SPOTIFY_CLIENT_ID = "tu_client_id";
   public static final String SPOTIFY_CLIENT_SECRET = "tu_client_secret";
   ```

4. **Ejecutar la aplicación**
   ```bash
   ./gradlew assembleDebug
   ```

### Para Usuarios Finales

1. Descargar el APK desde la sección Releases
2. Habilitar "Fuentes desconocidas" en configuración
3. Instalar el APK
4. Crear cuenta o iniciar sesión

## 📖 Guía de Uso

### Registro e Inicio de Sesión

**Registro:** Completa el formulario con tus datos
- Nombre y apellido
- Email válido
- Contraseña (mínimo 6 caracteres)
- Información opcional: teléfono, fecha de nacimiento, género

**Inicio de Sesión:** Accede con tu email y contraseña

### Exploración Musical

- **Búsqueda:** Usa la barra de búsqueda para encontrar música
- **Biblioteca:** Accede a tu colección personal
- **Recomendaciones:** Descubre nueva música basada en tus gustos
- **Tendencias:** Explora la música más popular

### Reproducción

- **Seleccionar canción:** Toca cualquier canción para reproducir
- **Mini reproductor:** Controla la reproducción desde la pantalla principal
- **Reproductor completo:** Toca el mini reproductor para vista completa
- **Controles:** Play/Pause, siguiente, anterior, progreso

## 🏗️ Arquitectura del Proyecto

```
com.sise.orbitsongv1/
├── activities/              # Actividades principales
│   ├── HomeActivity.java
│   ├── LoginActivity.java
│   ├── RegisterActivity.java
│   ├── MusicPlayerActivity.java
│   └── LibraryActivity.java
├── adapters/                # Adaptadores RecyclerView
│   └── SongAdapter.java
├── models/                  # Modelos de datos
│   ├── Song.java
│   ├── User.java
│   └── SpotifySearchResponse.java
├── services/                # Servicios
│   ├── MusicPlayerService.java
│   └── RetrofitClient.java
├── viewmodels/              # ViewModels MVVM
│   └── RegisterViewModel.java
├── utils/                   # Utilidades
│   └── Constants.java
└── res/                     # Recursos
    ├── layout/
    ├── drawable/
    ├── values/
    └── menu/
```

## 🔧 Configuración Avanzada

### Variables de Entorno
```properties
# config.properties
SPOTIFY_CLIENT_ID=tu_client_id
SPOTIFY_CLIENT_SECRET=tu_client_secret
API_BASE_URL=https://api.tu-servidor.com
```

### Personalización
- **Temas:** Modifica `styles.xml` para cambiar colores
- **Animaciones:** Ajusta duraciones en `Constants.java`
- **API Endpoints:** Configura URLs en `RetrofitClient.java`

