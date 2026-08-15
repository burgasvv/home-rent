package org.burgas.service

import kotlinx.serialization.json.Json
import org.burgas.dao.MeetingEntity
import org.burgas.database.DatabaseConnection
import org.burgas.dto.MeetingRequest
import org.burgas.dto.MeetingResponse
import org.burgas.redis.CacheHandler
import org.burgas.redis.RedisKey
import org.burgas.service.contract.CreateService
import org.burgas.service.contract.DeleteService
import org.burgas.service.contract.FindService
import org.burgas.service.contract.ReadService
import org.burgas.service.contract.UpdateService
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import java.sql.Connection
import kotlin.uuid.Uuid

class MeetingService : CacheHandler<MeetingResponse>, ReadService<Uuid, MeetingEntity>,
    FindService<Uuid, MeetingResponse>, CreateService<MeetingRequest, MeetingResponse>,
    UpdateService<MeetingRequest, MeetingResponse>, DeleteService<Uuid> {

    private val redis = DatabaseConnection.redis

    override suspend fun handleCache(response: MeetingResponse) {
        val meetingKey = RedisKey.MEETING_KEY.format(response.id)
        if (redis.exists(meetingKey)) redis.del(meetingKey)

        val homeKey = RedisKey.HOME_KEY.format(response.home.id)
        if (redis.exists(homeKey)) redis.del(homeKey)

        val applicantKey = RedisKey.IDENTITY_KEY.format(response.applicant.id)
        if (redis.exists(applicantKey)) redis.del(applicantKey)
    }

    override suspend fun readEntity(id: Uuid): MeetingEntity = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        MeetingEntity[id].load(MeetingEntity::home, MeetingEntity::applicant)
    }

    override suspend fun findById(id: Uuid): MeetingResponse = suspendTransaction(
        db = DatabaseConnection.postgres, readOnly = true
    ) {
        val meetingKey = RedisKey.MEETING_KEY.format(id)
        if (redis.exists(meetingKey)) {
            Json.decodeFromString(redis[meetingKey])
        } else {
            val meetingResponse = readEntity(id).toResponse()
            redis.set(meetingKey, Json.encodeToString(meetingResponse))
            meetingResponse
        }
    }

    override suspend fun create(request: MeetingRequest): MeetingResponse = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val meetingResponse = MeetingEntity.new { this.insert(request) }
            .load(MeetingEntity::home, MeetingEntity::applicant).toResponse()
        handleCache(meetingResponse)
        val meetingKey = RedisKey.MEETING_KEY.format(meetingResponse.id)
        redis.set(meetingKey, Json.encodeToString(meetingResponse))
        meetingResponse
    }

    override suspend fun update(request: MeetingRequest): MeetingResponse = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        val meetingResponse = MeetingEntity.findByIdAndUpdate(request.uuid!!) { it.update(request) }!!
            .load(MeetingEntity::home, MeetingEntity::applicant).toResponse()
        handleCache(meetingResponse)
        val meetingKey = RedisKey.MEETING_KEY.format(meetingResponse.id)
        redis.set(meetingKey, Json.encodeToString(meetingResponse))
        meetingResponse
    }

    override suspend fun delete(id: Uuid) = suspendTransaction(
        db = DatabaseConnection.postgres, transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
    ) {
        readEntity(id).delete()
    }
}