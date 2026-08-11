package org.burgas.dao

import org.burgas.database.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import kotlin.uuid.Uuid

class ImageEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<ImageEntity>(ImageTable)

    var name by ImageTable.name
    var contentType by ImageTable.contentType
    var data by ImageTable.data
}

class VideoEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<VideoEntity>(VideoTable)

    var name by VideoTable.name
    var contentType by VideoTable.contentType
    var data by VideoTable.data
}

class IdentityEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<IdentityEntity>(IdentityTable)

    var authority by IdentityTable.authority
    var email by IdentityTable.email
    var password by IdentityTable.password
    var status by IdentityTable.status
    var phone by IdentityTable.phone
    var telegram by IdentityTable.telegram
    var whatsapp by IdentityTable.whatsapp
    var max by IdentityTable.max
    var firstname by IdentityTable.firstname
    var lastname by IdentityTable.lastname
    var patronymic by IdentityTable.patronymic
    var image by ImageEntity.optionalReferencedOn(IdentityTable.imageId)
    val homesByLessor by HomeEntity.referrersOn(HomeTable.lessorId)
    val homesByTenant by HomeEntity.optionalReferrersOn(HomeTable.tenantId)
    val meetings by MeetingEntity.referrersOn(MeetingTable.applicantId)
}

class AddressEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<AddressEntity>(AddressTable)

    var country by AddressTable.country
    var city by AddressTable.city
    var street by AddressTable.street
    var building by AddressTable.building
    var apartment by AddressTable.apartment
}

class HomeEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<HomeEntity>(HomeTable)

    var homeType by HomeTable.homeType
    var address by AddressEntity.referencedOn(HomeTable.addressId)
    var description by HomeTable.description
    var billingUnit by HomeTable.billingUnit
    var price by HomeTable.price
    var lessor by IdentityEntity.referencedOn(HomeTable.lessorId)
    var tenant by IdentityEntity.optionalReferencedOn(HomeTable.tenantId)
    var image by ImageEntity.optionalReferencedOn(HomeTable.imageId)
    var images by ImageEntity.via(HomeImageTable.homeId, HomeImageTable.imageId)
    var videos by VideoEntity.via(HomeVideoTable.homeId, HomeVideoTable.videoId)
    val meetings by MeetingEntity.referrersOn(MeetingTable.homeId)
}

class MeetingEntity(id: EntityID<Uuid>) : UuidEntity(id) {
    companion object : UuidEntityClass<MeetingEntity>(MeetingTable)

    var home by HomeEntity.referencedOn(MeetingTable.homeId)
    var applicant by IdentityEntity.referencedOn(MeetingTable.applicantId)
    var dateTime by MeetingTable.dateTime
    var tookPlace by MeetingTable.tookPlace
}