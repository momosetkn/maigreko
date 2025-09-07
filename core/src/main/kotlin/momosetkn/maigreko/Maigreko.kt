package momosetkn.maigreko

import momosetkn.maigreko.dsl.ChangeSetGroupDsl
import momosetkn.maigreko.dsl.MaigrekoMigration
import momosetkn.maigreko.sql.MigrateEngine
import momosetkn.maigreko.sql.MigrateEngineFactory
import momosetkn.maigreko.versioning.Versioning
import javax.sql.DataSource

class Maigreko(
    private val dataSource: DataSource,
    private val migrateEngine: MigrateEngine,
) {
    private val versioning = Versioning(dataSource, migrateEngine)
    private val forwardDslInterpreter = ForwardDslInterpreter(versioning)
    private val rollbackDslInterpreter = RollbackDslInterpreter(versioning)

    constructor(dataSource: DataSource) : this(
        dataSource = dataSource,
        migrateEngine = MigrateEngineFactory.create(dataSource),
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
