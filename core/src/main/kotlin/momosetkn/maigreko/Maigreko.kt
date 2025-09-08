package momosetkn.maigreko

import io.github.classgraph.ClassGraph
import momosetkn.maigreko.change.ChangeSet
import momosetkn.maigreko.dialect.Dialect
import momosetkn.maigreko.dialect.DialectFactory
import momosetkn.maigreko.dsl.ChangeSetGroupDsl
import momosetkn.maigreko.dsl.MaigrekoMigration
import momosetkn.maigreko.introspector.Introspector
import momosetkn.maigreko.introspector.IntrospectorBuilder
import momosetkn.maigreko.sql.MigrateEngine
import momosetkn.maigreko.versioning.Versioning
import javax.sql.DataSource
import kotlin.reflect.KClass

@Suppress("TooManyFunctions")
class Maigreko(
    private val dataSource: DataSource,
    private val migrateEngine: MigrateEngine,
    private val introspectorBuilder: IntrospectorBuilder,
) {
    private val versioning = Versioning(dataSource, migrateEngine)
    private val forwardDslInterpreter = ForwardDslInterpreter(versioning)
    private val rollbackDslInterpreter = RollbackDslInterpreter(versioning)

    constructor(dataSource: DataSource) : this(
        dataSource = dataSource,
        dialect = DialectFactory.create(dataSource),
    )

    constructor(dataSource: DataSource, dialect: Dialect) : this(
        dataSource = dataSource,
        migrateEngine = dialect.migrateEngine,
        introspectorBuilder = dialect.introspectorBuilder,
    )

    // Backward-compatible constructor used in tests where only MigrateEngine is provided.
    constructor(dataSource: DataSource, migrateEngine: MigrateEngine) : this(
        dataSource = dataSource,
        migrateEngine = migrateEngine,
        introspectorBuilder = object : IntrospectorBuilder {
            override fun build(dataSource: DataSource): Introspector {
                throw UnsupportedOperationException("Introspection is not available in this Maigreko constructor")
            }
        },
    )

    // block
    fun migrate(migrationClassName: String, block: ChangeSetGroupDslBlock) {
        val changeSetGroupDsl = ChangeSetGroupDsl(migrationClassName, forwardDslInterpreter)
        changeSetGroupDsl.block()
    }

    fun dryRunMigrate(migrationClassName: String, block: ChangeSetGroupDslBlock): List<DryRunResult> {
        val dryRunForwardDslInterpreter = DryRunForwardDslInterpreter(migrateEngine)
        val changeSetGroupDsl = ChangeSetGroupDsl(migrationClassName, dryRunForwardDslInterpreter)
        changeSetGroupDsl.block()
        return dryRunForwardDslInterpreter.getResults()
    }

    fun rollback(migrationClassName: String, block: ChangeSetGroupDslBlock) {
        val changeSetGroupDsl = ChangeSetGroupDsl(migrationClassName, rollbackDslInterpreter)
        changeSetGroupDsl.block()
    }

    fun dryRunForward(migrationClassName: String, block: ChangeSetGroupDslBlock): List<DryRunResult> {
        val dryRunForwardDslInterpreter = DryRunForwardDslInterpreter(migrateEngine)
        val changeSetGroupDsl = ChangeSetGroupDsl(migrationClassName, dryRunForwardDslInterpreter)
        changeSetGroupDsl.block()
        return dryRunForwardDslInterpreter.getResults()
    }

    fun dryRunRollback(migrationClassName: String, block: ChangeSetGroupDslBlock): List<DryRunResult> {
        val dryRunRollbackDslInterpreter = DryRunRollbackDslInterpreter(migrateEngine)
        val changeSetGroupDsl = ChangeSetGroupDsl(migrationClassName, dryRunRollbackDslInterpreter)
        changeSetGroupDsl.block()
        return dryRunRollbackDslInterpreter.getResults()
    }

    // MaigrekoMigration
    fun migrate(migration: MaigrekoMigration): Unit =
        migrate(migration::class.javaClassName, migration.body)

    fun rollback(migration: MaigrekoMigration): Unit =
        rollback(migration::class.javaClassName, migration.body)

    fun dryRunForward(migration: MaigrekoMigration): List<DryRunResult> =
        dryRunForward(migration::class.javaClassName, migration.body)

    fun dryRunRollback(migration: MaigrekoMigration): List<DryRunResult> =
        dryRunRollback(migration::class.javaClassName, migration.body)

    // class
    fun migrate(maigrekoMigrationClazz: Class<MaigrekoMigration>) {
        val instance = maigrekoMigrationClazz.constructors.first().newInstance() as MaigrekoMigration
        migrate(maigrekoMigrationClazz.javaClassName, instance.body)
    }

    // package
    fun migrate(packageName: String) {
        val maigrekoMigrationClazzs = findListClassesByPackage(packageName)
        maigrekoMigrationClazzs.forEach { maigrekoMigration ->
            val instance = maigrekoMigration.constructors.first().newInstance() as MaigrekoMigration
            migrate(maigrekoMigration.javaClassName, instance.body)
        }
    }

    fun dryRunMigrate(packageName: String): List<DryRunResult> {
        val maigrekoMigrationClazzs = findListClassesByPackage(packageName)
        return maigrekoMigrationClazzs.flatMap { maigrekoMigration ->
            val instance = maigrekoMigration.constructors.first().newInstance() as MaigrekoMigration
            dryRunMigrate(maigrekoMigration.javaClassName, instance.body)
        }
    }

    private fun findListClassesByPackage(packageName: String): List<Class<MaigrekoMigration>> {
        val maigrekoMigrationClazzs = ClassGraph()
            .acceptPackages(packageName)
            .scan()
            .getSubclasses(MaigrekoMigration::class.java)
            .loadClasses(MaigrekoMigration::class.java)
        return maigrekoMigrationClazzs
    }

    fun rollback(packageName: String) {
        val maigrekoMigrationClazzs = findListClassesByPackage(packageName)
        maigrekoMigrationClazzs.forEach { maigrekoMigration ->
            val instance = maigrekoMigration.constructors.first().newInstance() as MaigrekoMigration
            migrate(maigrekoMigration.javaClassName, instance.body)
        }
    }

    fun dryRunRollback(packageName: String): List<DryRunResult> {
        val maigrekoMigrationClazzs = findListClassesByPackage(packageName)
        return maigrekoMigrationClazzs.flatMap { maigrekoMigration ->
            val instance = maigrekoMigration.constructors.first().newInstance() as MaigrekoMigration
            dryRunRollback(maigrekoMigration.javaClassName, instance.body)
        }
    }

    private val KClass<*>.javaClassName: String
        get() = this.java.name

    private val Class<*>.javaClassName: String
        get() = this.name
}

typealias ChangeSetGroupDslBlock = ChangeSetGroupDsl.() -> Unit

data class DryRunResult(
    val migrationClassName: String,
    val changeSet: ChangeSet,
    val ddls: List<String>,
    val direction: Direction,
) {
    enum class Direction {
        FORWARD,
        BACKWARD,
    }
}
