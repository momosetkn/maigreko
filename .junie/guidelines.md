# Maigreko Development Guidelines

## Project Overview
Maigreko is a multi-module Kotlin database migration library supporting multiple database dialects (PostgreSQL, MySQL, Oracle, H2, MariaDB, SQL Server). The project uses a ServiceLoader pattern for dialect discovery and Testcontainers for database testing.

## Build/Configuration Instructions

### Prerequisites
- Java 21 (language version)
- JVM target: 17
- Gradle 8.14.3+ (uses wrapper)

### Module Structure
```
core/                    - Core APIs and base functionality
*-dialect/              - Database-specific implementations
integration-test/       - Cross-dialect integration tests
*-test-utils/          - Database-specific test utilities
test-utils/            - Common test utilities
```

### Build Commands
```bash
# Build entire project
./gradlew build

# Build specific module
./gradlew :core:build

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :core:test

# Code quality checks
./gradlew detekt ktlintCheck
```

### Key Dependencies
- **Komapper**: Database access framework with KSP annotation processing
- **Log4j2 + SLF4j**: Logging framework
- **Testcontainers**: Database testing with real database instances
- **Various JDBC drivers**: Database connectivity

## Testing Information

### Testing Framework
- **Primary**: Kotest with FunSpec style
- **Structure**: Use `context("description")` for grouping and `test("description")` for individual tests
- **Assertions**: Use `shouldBe` and other Kotest matchers

### Test Example
```kotlin
class ExampleTestSpec : FunSpec({
    
    context("Basic functionality") {
        test("should demonstrate simple assertion") {
            val result = "hello" + " world"
            result shouldBe "hello world"
        }
        
        test("should work with collections") {
            val list = listOf(1, 2, 3)
            list.size shouldBe 3
        }
    }
})
```

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :core:test

# Run with detailed output
./gradlew test --info

# Run integration tests (uses Testcontainers)
./gradlew :integration-test:test
```

### Database Testing with Testcontainers
- Integration tests use Testcontainers to spin up real database instances
- PostgreSQL and MySQL are primarily used for integration testing
- Each database dialect has its own test-utils module for setup helpers
- Test databases are automatically started/stopped during test execution

### Adding New Tests
1. Place tests in `src/test/kotlin/` following package structure
2. Use Kotest FunSpec style with descriptive context blocks
3. For database tests, use appropriate test-utils modules
4. Follow existing naming conventions: `*Spec.kt` for test files

## Code Style and Development Practices

### Code Quality Tools
- **ktlint**: Kotlin code formatting (version from libs.versions.toml)
- **detekt**: Static code analysis with baseline files
- **Configuration**: 
  - ktlint baseline: `core/config/ktlint/baseline.xml` (currently empty)
  - detekt baseline: `core/detekt-baseline.xml`

### Kotlin Conventions
- Use object declarations for factory classes (see `IntrospectorFactory`)
- Comprehensive KDoc comments for public APIs
- Proper exception handling with descriptive messages
- Use ServiceLoader pattern for plugin discovery
- Follow standard Kotlin naming conventions

### Module Dependencies
- Each dialect module registers services via `META-INF/services/`
- Core module provides base abstractions
- Dialect modules implement specific database logic
- Test utilities are separated into dedicated modules

### ServiceLoader Pattern
The project uses Java ServiceLoader for dynamic dialect discovery:
```kotlin
val builderLoader = ServiceLoader.load(IntrospectorBuilder::class.java)
```

Register implementations in `META-INF/services/` files.

### Development Workflow
1. Make changes in appropriate module
2. Run relevant tests: `./gradlew :module:test`
3. Check code style: `./gradlew ktlintCheck detekt`
4. Run integration tests if database changes: `./gradlew :integration-test:test`
5. Build entire project: `./gradlew build`

### Important Notes
- KSP annotation processing is used (ensure proper IDE setup)
- Testcontainers requires Docker for integration tests
- Each dialect module is independent but follows common patterns
- Log4j2 configuration in `src/main/resources/log4j2.xml`