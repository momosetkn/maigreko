package momosetkn.maigreko

import momosetkn.maigreko.dialect.Dialect
import momosetkn.maigreko.dialect.DialectFactory
import momosetkn.maigreko.dsl.ChangeSetGroupDsl
import momosetkn.maigreko.dsl.MaigrekoMigration
import momosetkn.maigreko.introspector.IntrospectorBuilder
import momosetkn.maigreko.sql.MigrateEngine
import momosetkn.maigreko.versioning.Versioning
import javax.sql.DataSource

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
            override fun build(dataSource: javax.sql.DataSource): momosetkn.maigreko.introspector.Introspector {
                throw UnsupportedOperationException("Introspection is not available in this Maigreko constructor")
            }
        },
    )

    // block
    fun migrate(block: ChangeSetGroupDsl.() -> Unit) {
        val changeSetGroupDsl = ChangeSetGroupDsl("", forwardDslInterpreter)
        changeSetGroupDsl.block()
    }

    fun rollback(block: ChangeSetGroupDsl.() -> Unit) {
        val changeSetGroupDsl = ChangeSetGroupDsl("", rollbackDslInterpreter)
        changeSetGroupDsl.block()
    }

    fun dryRunForward(block: ChangeSetGroupDsl.() -> Unit): List<String> {
        val dryRunForwardDslInterpreter = DryRunForwardDslInterpreter(migrateEngine)
        val changeSetGroupDsl = ChangeSetGroupDsl("", dryRunForwardDslInterpreter)
        changeSetGroupDsl.block()
        return dryRunForwardDslInterpreter.getDdls()
    }

    fun dryRunRollback(block: ChangeSetGroupDsl.() -> Unit): List<String> {
        val dryRunRollbackDslInterpreter = DryRunRollbackDslInterpreter(migrateEngine)
        val changeSetGroupDsl = ChangeSetGroupDsl("", dryRunRollbackDslInterpreter)
        changeSetGroupDsl.block()
        return dryRunRollbackDslInterpreter.getDdls()
    }

    // MaigrekoMigration
    fun migrate(migration: MaigrekoMigration): Unit = migrate(migration.body)
    fun rollback(migration: MaigrekoMigration): Unit = rollback(migration.body)
    fun dryRunForward(migration: MaigrekoMigration): List<String> = dryRunForward(migration.body)
    fun dryRunRollback(migration: MaigrekoMigration): List<String> = dryRunRollback(migration.body)

    // class
    fun migrate(maigrekoMigrationClazz: Class<MaigrekoMigration>) {
        val instance = maigrekoMigrationClazz.constructors.first().newInstance() as MaigrekoMigration
        migrate(instance.body)
    }

    // package
    fun migrate(packageName: String) {
        val maigrekoMigrationClazzs = Class.forName(packageName).classes.filterIsInstance<Class<MaigrekoMigration>>()
        maigrekoMigrationClazzs.forEach { maigrekoMigration ->
            val instance = maigrekoMigration.constructors.first().newInstance() as MaigrekoMigration
            migrate(instance.body)
        }
    }
}
