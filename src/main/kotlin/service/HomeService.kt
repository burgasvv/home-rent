package org.burgas.service

import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.asFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import org.burgas.dao.HomeEntity
import org.burgas.dao.IdentityEntity
import org.burgas.database.DatabaseConnection
import org.burgas.database.HomeTable
import org.burgas.dto.EntityFileRequest
import org.burgas.dto.HomeRequest
import org.burgas.dto.HomeResponse
import org.burgas.redis.CacheHandler
import org.burgas.redis.RedisKey
import org.burgas.service.contract.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.sql.Connection
import kotlin.uuid.Uuid

class HomeService : CacheHandler<HomeResponse>, CollectService<HomeResponse>, ReadService<Uuid, HomeEntity>,
    FindService<Uuid, HomeResponse>, CreateService<HomeRequest, HomeResponse>, UpdateService<HomeRequest, HomeResponse>,
    DeleteService<Uuid> {

    private val redis = DatabaseConnection.redis
    private val imageService: ImageService
    private val videoService: VideoService

    constructor(imageService: ImageService, videoService: VideoService) {
        this.imageService = imageService
        this.videoService = videoService
    }

    override suspend fun handleCache(response: HomeResponse) {
        val homeKey = RedisKey.HOME_KEY.format(response.id)
        if (redis.exists(homeKey)) redis.del(homeKey)

        val lessorKey = RedisKey.IDENTITY_KEY.format(response.lessor.id)
        if (redis.exists(lessorKey)) redis.del(lessorKey)

        val tenantKey = RedisKey.IDENTITY_KEY.format(response.tenant?.id)
        if (redis.exists(tenantKey)) redis.del(tenantKey)

        response.meetings.forEach {
            val meetingKey = RedisKey.MEETING_KEY.format(it.id)
            if (redis.exists(meetingKey)) redis.del(meetingKey)
        }
    }

    override suspend fun findAll(): List<HomeResponse> = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        HomeEntity.all().with(
            HomeEntity::address, HomeEntity::lessor, HomeEntity::tenant, HomeEntity::image,
            HomeEntity::images, HomeEntity::videos, HomeEntity::meetings
        ).map { it.toResponse() }
    }

    suspend fun findAllFree(): List<HomeResponse> = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        HomeEntity.find { HomeTable.tenantId eq null }
            .with(
                HomeEntity::address, HomeEntity::lessor, HomeEntity::tenant, HomeEntity::image,
                HomeEntity::images, HomeEntity::videos, HomeEntity::meetings
            ).map { it.toResponse() }
    }

    override suspend fun readEntity(id: Uuid): HomeEntity = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        HomeEntity[id].load(
            HomeEntity::address, HomeEntity::lessor, HomeEntity::tenant, HomeEntity::image,
            HomeEntity::images, HomeEntity::videos, HomeEntity::meetings
        )
    }

    override suspend fun findById(id: Uuid): HomeResponse = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        val homeKey = RedisKey.HOME_KEY.format(id)
        if (redis.exists(homeKey)) {
            Json.decodeFromString(redis[homeKey])
        } else {
            val homeResponse = readEntity(id).toResponse()
            redis.set(homeKey, Json.encodeToString(homeResponse))
            homeResponse
        }
    }

    override suspend fun create(request: HomeRequest): HomeResponse = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val homeResponse = HomeEntity.new { this.insert(request) }.load(
            HomeEntity::address, HomeEntity::lessor, HomeEntity::tenant, HomeEntity::image,
            HomeEntity::images, HomeEntity::videos, HomeEntity::meetings
        ).toResponse()
        handleCache(homeResponse)
        val homeKey = RedisKey.HOME_KEY.format(homeResponse.id)
        redis.set(homeKey, Json.encodeToString(homeResponse))
        homeResponse
    }

    override suspend fun update(request: HomeRequest): HomeResponse = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val homeResponse = HomeEntity.findByIdAndUpdate(request.uuid!!) { it.update(request) }!!.load(
            HomeEntity::address, HomeEntity::lessor, HomeEntity::tenant, HomeEntity::image,
            HomeEntity::images, HomeEntity::videos, HomeEntity::meetings
        ).toResponse()
        handleCache(homeResponse)
        val homeKey = RedisKey.HOME_KEY.format(homeResponse.id)
        redis.set(homeKey, Json.encodeToString(homeResponse))
        homeResponse
    }

    override suspend fun delete(id: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val homeEntity = readEntity(id)
        homeEntity.address.delete()
        homeEntity.image?.delete()
        homeEntity.images.forEach { it.delete() }
        homeEntity.videos.forEach { it.delete() }
        homeEntity.delete()
        handleCache(homeEntity.toResponse())
    }

    suspend fun rent(homeRequest: HomeRequest) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val homeEntity = readEntity(homeRequest.uuid!!)
        homeRequest.tenantUuid!!.let { homeEntity.tenant = IdentityEntity[it] }
        handleCache(homeEntity.toResponse())
    }

    suspend fun cancel(homeId: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val homeEntity = readEntity(homeId)
        requireNotNull(homeEntity.tenant) { "Tenant is already null and can't cancel rent agreement" }
        homeEntity.tenant = null
        handleCache(homeEntity.toResponse())
    }

    suspend fun uploadPreviewImage(homeId: Uuid, multiPartData: MultiPartData) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val fileItem = multiPartData.asFlow().filterIsInstance<PartData.FileItem>().first()
        val homeEntity = readEntity(homeId)
        require(homeEntity.image == null) { "Home image already set" }
        homeEntity.image = imageService.upload(fileItem)
        handleCache(homeEntity.toResponse())
    }

    suspend fun removePreviewImage(homeId: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val homeEntity = readEntity(homeId)
        homeEntity.image?.delete()
        handleCache(homeEntity.toResponse())
    }

    suspend fun uploadImages(homeId: Uuid, multiPartData: MultiPartData) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val fileItems = multiPartData.asFlow().filterIsInstance<PartData.FileItem>().toList()
        val homeEntity = readEntity(homeId)
        val imageEntities = fileItems.map { imageService.upload(it) }
        homeEntity.images = SizedCollection(homeEntity.images + imageEntities)
        handleCache(homeEntity.toResponse())
    }

    suspend fun removeImages(entityFileRequest: EntityFileRequest) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val homeEntity = readEntity(entityFileRequest.entityUuid)
        homeEntity.images.filter { entityFileRequest.fileUuids.contains(it.id.value) }.forEach { it.delete() }
        handleCache(homeEntity.toResponse())
    }

    suspend fun uploadVideos(homeId: Uuid, multiPartData: MultiPartData) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val fileItems = multiPartData.asFlow().filterIsInstance<PartData.FileItem>().toList()
        val homeEntity = readEntity(homeId)
        val videoEntities = fileItems.map { videoService.upload(it) }
        homeEntity.videos = SizedCollection(homeEntity.videos + videoEntities)
        handleCache(homeEntity.toResponse())
    }

    suspend fun removeVideos(entityFileRequest: EntityFileRequest) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val homeEntity = readEntity(entityFileRequest.entityUuid)
        homeEntity.videos.filter { entityFileRequest.fileUuids.contains(it.id.value) }.forEach { it.delete() }
        handleCache(homeEntity.toResponse())
    }
}