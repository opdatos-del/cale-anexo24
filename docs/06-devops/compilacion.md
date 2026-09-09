# Compilación

## Requisitos

Java 21, Node.js LTS, gestor de paquetes bloqueado, Angular CLI compatible, Git y acceso a dependencias autorizadas.

## Proceso reproducible

1. Restaurar dependencias desde archivos de bloqueo.
2. Ejecutar pruebas unitarias y análisis estático.
3. Construir frontend de producción y empaquetar backend Spring Boot.
4. Publicar artefactos versionados junto con reporte de pruebas.

Los comandos concretos quedan en los `package.json` y configuración Maven/Gradle del código cuando exista; esta documentación no inventa una herramienta de build no entregada aún.
