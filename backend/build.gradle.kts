import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.jovycandy"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Seguridad
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Validación
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Base de datos
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("com.microsoft.sqlserver:mssql-jdbc")

    // Observabilidad
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

    // Pruebas
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")

    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:mssqlserver:1.20.4")
}

/**
 * Carga las variables del archivo .env para inyectarlas como
 * variables de entorno en las tareas bootRun y test.
 *
 * @return mapa clave-valor con las variables definidas en .env
 */
fun loadDotEnv(): Map<String, String> {
    val envFile = file(".env")
    if (!envFile.exists()) return emptyMap()
    return envFile.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .associate { line ->
            val idx = line.indexOf("=")
            line.substring(0, idx).trim() to line.substring(idx + 1).trim()
        }
}

tasks.named<BootRun>("bootRun") {
    environment(loadDotEnv())
}

tasks.withType<Test> {
    environment(loadDotEnv())
    useJUnitPlatform()
}
