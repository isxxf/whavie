# 🎬 Whavie

**Whavie** es una aplicación web moderna que te ayuda a encontrar la película perfecta para ver, ya sea solo o con amigos. Usa votación en tiempo real para que el grupo pueda llegar a un acuerdo sobre qué película ver.

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring--Boot-4.0.3-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue)

## ✨ Características

### 🎯 Funcionalidades Principales

- **Crear Salas de Votación**: Crea una sala privada con un código único para invitar a tus amigos
- **Votación Grupal en Tiempo Real**: Los participantes votan películas mediante WebSocket en tiempo real
- **Búsqueda Inteligente de Películas**: Filtra películas por:
  - Géneros
  - Idiomas
  - Plataformas de streaming
  - Clasificación por edad
  - Rango de años de lanzamiento
  - Actores y directores favoritos

- **Modo Individual**: Descubre películas recomendadas basadas en tus preferencias personalizadas
- **Participantes Registrados e Invitados**: Puedes usar la aplicación sin registrarte, pero los usuarios registrados tienen acceso a funciones adicionales como poder crear salas o utilizar el modo individual

### 🔐 Seguridad y Autenticación

- **Autenticación Local**: Registro e inicio de sesión con correo electrónico y contraseña
- **OAuth2 con Google**: Inicia sesión rápidamente con tu cuenta de Google
- **Gestión Segura de Sesiones**: Tokens de invitado para participantes sin cuenta
- **Control de Acceso**: Solo el creador de la sala puede iniciar la votación y aplicar filtros

### 🚀 Experiencia en Tiempo Real

- **WebSocket (STOMP)**: Comunicación bidireccional en tiempo real entre servidor y clientes
- **Actualización de Películas**: Los participantes ven las películas actualizarse sin recargar la página
- **Sincronización automática**: La sala se actualiza en vivo cuando los participantes entran o salen
- **Votación Responsiva**: Los resultados de la votación se actualizan instantáneamente

### 💾 Gestión de Datos

- **Base de Datos PostgreSQL**: Almacenamiento persistente y confiable
- **ORM con Hibernate/JPA**: Mapeo objeto-relacional eficiente
- **Preferencias de Usuario**: Guarda tus filtros y gustos para recomendaciones personalizadas

## 🛠️ Stack Tecnológico

### Backend
- **Java 17**
- **Spring Boot 4.0.3**
- **Spring Security**: Autenticación y autorización
- **Spring WebSocket**: Comunicación en tiempo real
- **Spring Data JPA**: Acceso a datos
- **Hibernate**: ORM
- **PostgreSQL**: Base de datos
- **Maven**: Gestor de dependencias

### Frontend
- **HTML5 / CSS3**: Estructura y estilos
- **JavaScript Vanilla**: Interactividad
- **Thymeleaf**: Template engine
- **WebSocket (STOMP)**: Cliente de tiempo real
- **SockJS**: Fallback para browsers antiguos

### Integraciones Externas
- **TMDB API**: Datos de películas
- **Google OAuth2**: Autenticación social
- **SendGrid SMTP**: Envío de correos
- **DiceBear API**: Generación de avatares

## 📱 Uso de la Aplicación

### Flujo para Salas Grupales

```
1. Registrarse o iniciar sesión
   ↓
2. Click en "Crear Sala"
   ↓
3. Configurar filtros (opcional)
   ↓
4. Obtener código de la sala
   ↓
5. Compartir código con amigos
   ↓
6. Amigos se unen con el código
   ↓
7. Click en "Empezar" para iniciar votación
   ↓
8. Todos votan las películas (SÍ/NO)
   ↓
9. La película con más votos gana
   ↓
10. Ver detalles de la película ganadora
```

### Flujo para Modo Individual

```
1. Iniciar sesión
   ↓
2. Click en "Empezar a descubrir"
   ↓
3. Configurar preferencias (géneros, idiomas, etc.)
   ↓
4. Click en "Buscar película"
   ↓
5. Ver película recomendada
   ↓
6. Marcar "Me gusta" o "No me gusta"
   ↓
7. Obtener siguiente recomendación
```

## 📞 Contacto

**GitHub** - [@isxxf](https://github.com/isxxf/)

**LinkedIn** - [LinkedIn](https://www.linkedin.com/in/isabella-ferraro-trivelli/)

---

**Hecho con ❤️ para encontrar la película perfecta**

