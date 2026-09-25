# Ajuste de build (permitido por las reglas del encargo: "si algo no compila,
# la solución es ajustar el CMakeLists.txt o el build.gradle.kts, no reescribir el C").
# Termux master envía ndk-build (Android.mk: LOCAL_MODULE := libtermux,
# LOCAL_SRC_FILES := termux.c). Este CMakeLists.txt es el espejo exacto de ese
# Android.mk, para respetar el build.gradle.kts del encargo (externalNativeBuild.cmake).
# El código C (termux.c) NO fue modificado. (Actualizado Task 6: ver NOTA abajo.)
cmake_minimum_required(VERSION 3.22.1)
project(termux LANGUAGES C)

add_library(termux SHARED termux.c)

# NOTA (Task 6): termux.c recibió UN cambio quirúrgico y declarado: intento de
# setgroups(0, NULL) en el hijo pre-exec, activado solo con TH_CLEAR_SUPPL_GROUPS=1
# (corrige la causa de "groups: cannot find name for group ID"). Sin la variable,
# el comportamiento es idéntico al de termux-app original.

# TerminalHouse: termux.c ahora registra el resultado de setgroups() en logcat
# (diagnóstico del problema "groups: cannot find name for group ID").
find_library(log-lib log)
target_link_libraries(termux PRIVATE ${log-lib})
