plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.shop"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

val querydslGenerated = layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main")

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
    options.generatedSourceOutputDirectory.set(querydslGenerated.get().asFile)
}

tasks.bootRun {
    systemProperty("file.encoding", "UTF-8")
    systemProperty("stdout.encoding", "UTF-8")
    systemProperty("stderr.encoding", "UTF-8")
}

repositories {
    mavenCentral()
}

// CVE 대응: Jackson core 수정 버전 고정 (GHSA-72hv-8253-57qq 등)
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "tools.jackson.core" && requested.name == "jackson-core") {
            useVersion("3.1.0")
            because("GHSA-72hv-8253-57qq")
        }
        if (requested.group == "com.fasterxml.jackson.core" && requested.name == "jackson-core") {
            useVersion("2.21.1")
            because("CVE jackson-core 2.x")
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.springframework.boot:spring-boot-jackson2")
    implementation("org.apache.poi:poi-ooxml:5.3.0")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    implementation("com.h2database:h2")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
    implementation("com.bucket4j:bucket4j-core:8.10.1")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")         // Thymeleaf Template
    implementation("org.apache.poi:poi:5.4.0")
    implementation("org.apache.poi:poi-ooxml:5.4.0")
    implementation("com.opencsv:opencsv:5.10")
    implementation("org.apache.tika:tika-core:2.9.1")

    /* querydsl */
    implementation("io.github.openfeign.querydsl:querydsl-jpa:6.10.1")
    annotationProcessor("io.github.openfeign.querydsl:querydsl-apt:6.10.1:jpa")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

// *IT 는 실제 dev DB·외부 API에 연결되는 수동 실행용이므로 기본 test 태스크에서 제외한다.
tasks.test {
    exclude("**/*IT.class")
}

// 실행: ./gradlew integrationTest (dev DB 및 필요한 환경 변수가 준비된 상태에서만)
tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "실제 dev 환경에 연결되는 *IT 테스트를 실행한다."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/*IT.class")
    // 벤치마크 성격이라 캐시된 결과를 재사용하지 않는다.
    outputs.upToDateWhen { false }
}
