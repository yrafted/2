COLECTIVOS ROSARIO - ANDROID
============================

App Android en modo oscuro para consultar transporte urbano de Rosario.

Funciones principales:
- Ubicación del teléfono para facilitar la búsqueda de paradas cercanas.
- Acceso a "Cuándo llega".
- Líneas, recorridos y paradas.
- Horarios publicados como respaldo.
- Búsqueda por número de parada.
- Interfaz Night Mode con textos de alto contraste.

FORMA MÁS FÁCIL DE GENERAR EL APK
---------------------------------
No necesitás Android Studio.

El proyecto incluye un workflow de GitHub Actions en:
.github/workflows/generar-apk.yml

Leé COMPILAR_SIN_ANDROID_STUDIO.txt para los pasos.

COMPILACIÓN LOCAL OPCIONAL
--------------------------
También se puede compilar con Android SDK + Java + Gradle desde línea de comandos,
pero la opción de GitHub Actions es más simple si Android Studio no se puede instalar.

FUENTES WEB UTILIZADAS POR LA APP
---------------------------------
- ¿Cómo llego? Rosario
- Ente de la Movilidad de Rosario
- Rosario Datos Abiertos

Nota: es una app no oficial y depende de la disponibilidad de las fuentes públicas.
