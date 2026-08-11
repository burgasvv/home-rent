package org.burgas.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import redis.clients.jedis.Jedis
import java.sql.Connection

object DatabaseConnection {

    private val config = ApplicationConfig("application.yaml")

    private val hikariConfig = HikariConfig()
    init {
        hikariConfig.jdbcUrl = config.property("postgres.url").getString()
        hikariConfig.username = config.property("postgres.user").getString()
        hikariConfig.password = config.property("postgres.password").getString()
        hikariConfig.driverClassName = "org.postgresql.Driver"
        hikariConfig.schema = "public"
        hikariConfig.minimumIdle = 1
        hikariConfig.maximumPoolSize = 1000
        hikariConfig.validate()
    }

    val postgres = Database.connect(
        datasource = HikariDataSource(hikariConfig),
        databaseConfig = DatabaseConfig { explicitDialect = PostgreSQLDialect() }
    )

    val redis = Jedis(
        config.property("redis.host").getString(),
        config.property("redis.port").getString().toInt()
    )
}

object ImageTable : UuidTable("image") {
    val name = varchar("name", 250).nullable()
    val contentType = varchar("content_type", 100).check { it like "image/%" }
    val data = blob("data")
}

object VideoTable : UuidTable("video") {
    val name = varchar("name", 250).nullable()
    val contentType = varchar("content_type", 100).check { it like "video/%" }
    val data = blob("data")
}

@Suppress("unused")
enum class Authority {
    ADMIN, USER
}

object IdentityTable : UuidTable("identity") {
    val authority = enumerationByName<Authority>("authority", 10).default(Authority.USER)
    val email = varchar("email", 100).uniqueIndex()
        .check { it regexp "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$" }
    val password = varchar("password", 250)
    val status = bool("status").default(true)
    val phone = varchar("phone", 20).uniqueIndex().check { it regexp "^\\+?[0-9\\s\\-\\(\\)]{10,20}$" }
    val telegram = varchar("telegram", 50).uniqueIndex().nullable()
    val whatsapp = varchar("whatsapp", 50).uniqueIndex().nullable()
    val max = varchar("max", 50).uniqueIndex().nullable()
    val firstname = varchar("firstname", 50)
    val lastname = varchar("lastname", 50)
    val patronymic = varchar("patronymic", 50)
    val imageId = optReference(
        name = "image_id", refColumn = ImageTable.id,
        onDelete = ReferenceOption.SET_NULL, onUpdate = ReferenceOption.CASCADE
    ).uniqueIndex()
}

@Suppress("unused")
enum class HomeType {
    APARTMENT, HOUSE, CONDO, TOWNHOUSE, MANOR
}

@Suppress("unused")
enum class BillingUnit {
    HOURLY, DAILY, MONTHLY, YEARLY
}

object AddressTable : UuidTable("address") {
    val country = varchar("country", 100)
    val city = varchar("city", 100)
    val street = varchar("street", 100)
    val building = varchar("building", 100)
    val apartment = varchar("apartment", 100).nullable()
}

object HomeTable : UuidTable("home") {
    val homeType = enumerationByName<HomeType>("home_type", 50)
    val addressId = reference(
        name = "address_id", refColumn = AddressTable.id,
        onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE
    ).uniqueIndex()
    val description = text("description").nullable()
    val billingUnit = enumerationByName<BillingUnit>("billing_unit", 50)
    val price = double("price").default(0.0).check { it greaterEq 0.0 }
    val lessorId = reference(
        name = "lessor_id", refColumn = IdentityTable.id,
        onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE
    )
    val tenantId = optReference(
        name = "tenant_id", refColumn = IdentityTable.id,
        onDelete = ReferenceOption.SET_NULL, onUpdate = ReferenceOption.CASCADE
    ).check { it neq lessorId }
    val imageId = optReference(
        name = "image_id", refColumn = ImageTable.id,
        onDelete = ReferenceOption.SET_NULL, onUpdate = ReferenceOption.CASCADE
    ).uniqueIndex()
}

object HomeImageTable : Table("home_image") {
    val homeId = reference(
        name = "home_id", refColumn = HomeTable.id,
        onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE
    )
    val imageId = reference(
        name = "image_id", refColumn = ImageTable.id,
        onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE
    )
    override val primaryKey: PrimaryKey
        get() = PrimaryKey(arrayOf(homeId, imageId))
}

object HomeVideoTable : Table("home_video") {
    val homeId = reference(
        name = "home_id", refColumn = HomeTable.id,
        onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE
    )
    val videoId = reference(
        name = "video_id", refColumn = VideoTable.id,
        onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE
    )
    override val primaryKey: PrimaryKey
        get() = PrimaryKey(arrayOf(homeId, videoId))
}

object MeetingTable : UuidTable("meeting") {
    val homeId = reference(
        name = "home_id", refColumn = HomeTable.id,
        onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE
    )
    val applicantId = reference(
        name = "applicant_id", refColumn = IdentityTable.id,
        onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE
    )
    val dateTime = datetime("date_time")
    val tookPlace = bool("took_place").default(false)
    init {
        index(isUnique = true, columns = arrayOf(homeId, dateTime))
        index(isUnique = true, columns = arrayOf(applicantId, dateTime))
    }
}

suspend fun configureDatabase() = suspendTransaction(
    db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
) {
    SchemaUtils.create(
        ImageTable, VideoTable, IdentityTable, AddressTable,
        HomeTable, HomeImageTable, HomeVideoTable, MeetingTable
    )
}