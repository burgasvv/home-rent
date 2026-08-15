package org.burgas.service

import io.ktor.http.content.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import org.burgas.dao.IdentityEntity
import org.burgas.database.Authority
import org.burgas.database.DatabaseConnection
import org.burgas.dto.IdentityRequest
import org.burgas.dto.IdentityResponse
import org.burgas.redis.CacheHandler
import org.burgas.redis.RedisKey
import org.burgas.service.contract.*
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.mindrot.jbcrypt.BCrypt
import java.sql.Connection
import kotlin.uuid.Uuid

class IdentityService : CacheHandler<IdentityResponse>, CollectService<IdentityResponse>,
    ReadService<Uuid, IdentityEntity>, FindService<Uuid, IdentityResponse>,
    CreateService<IdentityRequest, IdentityResponse>, UpdateService<IdentityRequest, IdentityResponse>,
    DeleteService<Uuid> {

    private val redis = DatabaseConnection.redis
    private val imageService: ImageService

    constructor(imageService: ImageService) {
        this.imageService = imageService
    }

    override suspend fun handleCache(response: IdentityResponse) {
        val identityKey = RedisKey.IDENTITY_KEY.format(response.id)
        if (redis.exists(identityKey)) redis.del(identityKey)

        response.homesByLessor.forEach {
            val homeKey = RedisKey.HOME_KEY.format(it.id)
            if (redis.exists(homeKey)) redis.del(homeKey)
        }
        response.homesByTenant?.forEach {
            val homeKey = RedisKey.HOME_KEY.format(it.id)
            if (redis.exists(homeKey)) redis.del(homeKey)
        }
        response.meetings.forEach {
            val meetingKey = RedisKey.MEETING_KEY.format(it.id)
            if (redis.exists(meetingKey)) redis.del(meetingKey)
        }
    }

    override suspend fun findAll(): List<IdentityResponse> = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        IdentityEntity.all().with(
            IdentityEntity::image, IdentityEntity::meetings,
            IdentityEntity::homesByLessor, IdentityEntity::homesByTenant
        ).map { it.toResponse() }
    }

    override suspend fun readEntity(id: Uuid): IdentityEntity = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        IdentityEntity[id].load(
            IdentityEntity::image, IdentityEntity::meetings,
            IdentityEntity::homesByLessor, IdentityEntity::homesByTenant
        )
    }

    override suspend fun findById(id: Uuid): IdentityResponse = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        val identityKey = RedisKey.IDENTITY_KEY.format(id)
        if (redis.exists(identityKey)) {
            Json.decodeFromString(redis[identityKey])
        } else {
            val identityResponse = readEntity(id).toResponse()
            redis.set(identityKey, Json.encodeToString(identityResponse))
            identityResponse
        }
    }

    override suspend fun create(request: IdentityRequest): IdentityResponse = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityResponse = IdentityEntity.new { this.insert(request) }.load(
            IdentityEntity::image, IdentityEntity::meetings,
            IdentityEntity::homesByLessor, IdentityEntity::homesByTenant
        ).toResponse()
        handleCache(identityResponse)
        val identityKey = RedisKey.IDENTITY_KEY.format(identityResponse.id)
        redis.set(identityKey, Json.encodeToString(identityResponse))
        identityResponse
    }

    suspend fun createAdmin(identityRequest: IdentityRequest) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityEntity = IdentityEntity.new { this.insert(identityRequest) }.load(
            IdentityEntity::image, IdentityEntity::meetings,
            IdentityEntity::homesByLessor, IdentityEntity::homesByTenant
        )
        identityEntity.authority = Authority.ADMIN
        val identityResponse = identityEntity.toResponse()
        handleCache(identityResponse)
        val identityKey = RedisKey.IDENTITY_KEY.format(identityResponse.id)
        redis.set(identityKey, Json.encodeToString(identityResponse))
        identityResponse
    }

    override suspend fun update(request: IdentityRequest): IdentityResponse = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityResponse = IdentityEntity.findByIdAndUpdate(request.uuid!!) { it.update(request) }!!.load(
            IdentityEntity::image, IdentityEntity::meetings,
            IdentityEntity::homesByLessor, IdentityEntity::homesByTenant
        ).toResponse()
        handleCache(identityResponse)
        val identityKey = RedisKey.IDENTITY_KEY.format(identityResponse.id)
        redis.set(identityKey, Json.encodeToString(identityResponse))
        identityResponse
    }

    override suspend fun delete(id: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityEntity = readEntity(id)
        identityEntity.image?.delete()
        identityEntity.delete()
        handleCache(identityEntity.toResponse())
    }

    suspend fun changePassword(identityRequest: IdentityRequest) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityEntity = readEntity(identityRequest.uuid!!)
        require(!BCrypt.checkpw(identityRequest.password!!, identityEntity.password)) {
            "Input and old passwords matched"
        }
        identityRequest.password.takeIf { it.isNotEmpty() }!!.let {
            identityEntity.password = BCrypt.hashpw(it, BCrypt.gensalt())
        }
        handleCache(identityEntity.toResponse())
    }

    suspend fun changeStatus(identityRequest: IdentityRequest) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityEntity = readEntity(identityRequest.uuid!!)
        require(identityEntity.status != identityRequest.status!!) { "Input and identity statuses matched" }
        identityRequest.status.let { identityEntity.status = it }
        handleCache(identityEntity.toResponse())
    }

    suspend fun uploadImage(identityId: Uuid, multiPartData: MultiPartData) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val fileItem = multiPartData.asFlow().filterIsInstance<PartData.FileItem>().first()
        val identityEntity = readEntity(identityId)
        require(identityEntity.image == null) { "Identity image already set" }
        identityEntity.image = imageService.upload(fileItem)
        handleCache(identityEntity.toResponse())
    }

    suspend fun removeImage(identityId: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val identityEntity = readEntity(identityId)
        identityEntity.image?.delete()
        handleCache(identityEntity.toResponse())
    }
}