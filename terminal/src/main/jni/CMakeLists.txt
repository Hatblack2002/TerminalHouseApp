# Ajuste de build (permitido por las reglas del encargo: "si algo no compila,
# la solución es ajustar el CMakeLists.txt o el build.gradle.kts, no reescribir el C").
# Termux master envía ndk-build (Android.mk: LOCAL_MODULE := libtermux,
# LOCAL_SRC_FILES := termux.c). Este CMakeLists.txt es el espejo exacto de ese
# Android.mk, para respetar el build.gradle.kts del encargo (externalNativeBuild.cmake).
# El código C (termux.c) NO fue modificado.
cmake_minimum_required(VERSION 3.22.1)
project(termux LANGUAGES C)

add_library(termux SHARED termux.c)
