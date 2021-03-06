plugins {
    base
    java
    id("jacoco-report-aggregation")
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("org.jetbrains.dokka") version "1.6.20"

    id("java-library")
    id("maven-publish")
    publishing
    signing
}

jacoco {
    toolVersion = "0.8.7"
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "org.jetbrains.dokka")

    group = "org.archguard.aaac"
    version = "2.0.0-alpha.7"
    java.sourceCompatibility = JavaVersion.VERSION_1_8
    java.targetCompatibility = JavaVersion.VERSION_1_8

    repositories {
        mavenCentral()
        mavenLocal()
    }

    dependencies {
        implementation(kotlin("stdlib"))

        // log
        implementation("io.github.microutils:kotlin-logging:2.1.21")

        // test
        implementation(kotlin("test"))
        implementation(kotlin("test-junit"))

        testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    }

    tasks.getByName<Test>("test") {
        useJUnitPlatform()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    tasks.test {
        finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test) // tests are required to run before generating the report
    }

    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
            csv.required.set(false)
            html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
        }
    }

    tasks.withType<JacocoReport> {
        afterEvaluate {
            classDirectories.setFrom(
                files(
                    classDirectories.files.map {
                        fileTree(it).apply {
                            exclude("dev.evolution")
                        }
                    }
                )
            )
        }
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")
    apply(plugin = "publishing")

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                versionMapping {
                    usage("java-api") {
                        fromResolutionOf("runtimeClasspath")
                    }
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }
                pom {
                    name.set("ArchGuard")
                    description.set(" ArchGuard is a architecture governance tool which can analysis architecture in container, component, code level, create architecture fitness functions, and anaysis system dependencies.. ")
                    url.set("https://archguard.org/")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://raw.githubusercontent.com/archguard/archguard/master/LICENSE")
                        }
                    }
                    developers {
                        developer {
                            id.set("Modernizing")
                            name.set("Modernizing Team")
                            email.set("h@phodal.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/archguard/scanner.git")
                        developerConnection.set("scm:git:ssh://github.com/archguard/scanner.git")
                        url.set("https://github.com/archguard/scanner/")
                    }
                }
            }
        }

        repositories {
            maven {
                val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

                credentials {
                    username =
                        (
                            if (project.findProperty("sonatypeUsername") != null) project.findProperty("sonatypeUsername") else System.getenv(
                                "MAVEN_USERNAME"
                            )
                            ).toString()
                    password =
                        (
                            if (project.findProperty("sonatypePassword") != null) project.findProperty("sonatypePassword") else System.getenv(
                                "MAVEN_PASSWORD"
                            )
                            ).toString()
                }
            }
        }
    }

    signing {
        sign(publishing.publications["mavenJava"])
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }
}

dependencies {
    jacocoAggregation(project(":"))

    jacocoAggregation(project(":dsl"))
    jacocoAggregation(project(":repl-api"))
}

reporting {
    reports {
        val jacocoRootReport by creating(JacocoCoverageReport::class) {
            testType.set(TestSuiteType.UNIT_TEST)
        }
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("jacocoRootReport"))
}
