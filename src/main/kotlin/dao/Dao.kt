package org.burgas.dao

import io.ktor.http.content.*
import io.ktor.utils.io.jvm.javaio.*
import org.burgas.database.*
import org.burgas.dto.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.dao.UuidEntity
import org.jetbrains.exposed.v1.dao.UuidEntityClass
import org.mindrot.jbcrypt.BCrypt
import kotlin.uuid.Uuid

interface File

interface Dao

interface Uploader<in P : PartData> {
    fun upload(fileItem: PartData.FileItem)
}

interface Creator<in R : Request> {
    fun insert(request: R)
}

interface Modifier<in R : Request> {
    fun update(request: R)
}

interface DependencyMapper<out D : Dependency> {
    suspend fun toDependency(): D
}

interface ResponseMapper<out R : Response> {
    suspend fun toResponse(): R
}

class ImageEntity(id: EntityID<Uuid>) : UuidEntity(id), File, Uploader<PartData.FileItem>, DependencyMapper<ImageDependency> {
    companion object : UuidEntityClass<ImageEntity>(ImageTable)

    var name by ImageTable.name
    var contentType by ImageTable.contentType
    var data by ImageTable.data

    override fun upload(fileItem: PartData.FileItem) {
        this.name = fileItem.originalFileName
        this.contentType = "${fileItem.contentType!!.contentType}/${fileItem.contentType!!.contentSubtype}"
        this.data = ExposedBlob(fileItem.provider().toInputStream())
    }

    override suspend fun toDependency(): ImageDependency {
        return ImageDependency(
            id = this.id.value,
            name = this.name,
            contentType = this.contentType
        )
    }
}

class VideoEntity(id: EntityID<Uuid>) : UuidEntity(id), File, Uploader<PartData.FileItem>, DependencyMapper<VideoDependency> {
    companion object : UuidEntityClass<VideoEntity>(VideoTable)

    var name by VideoTable.name
    var contentType by VideoTable.contentType
    var data by VideoTable.data

    override fun upload(fileItem: PartData.FileItem) {
        this.name = fileItem.originalFileName
        this.contentType = "${fileItem.contentType!!.contentType}/${fileItem.contentType!!.contentSubtype}"
        this.data = ExposedBlob(fileItem.provider().toInputStream())
    }

    override suspend fun toDependency(): VideoDependency {
        return VideoDependency(
            id = this.id.value,
            name = this.name,
            contentType = this.contentType
        )
    }
}

class IdentityEntity(id: EntityID<Uuid>) : UuidEntity(id), Dao, Creator<IdentityRequest>, Modifier<IdentityRequest>,
    DependencyMapper<IdentityDependency>, ResponseMapper<IdentityResponse> {
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

    override fun insert(request: IdentityRequest) {
        request.email.takeIf { !it.isNullOrEmpty() }!!.let { this.email = it }
        request.password.takeIf { !it.isNullOrEmpty() }!!.let { this.password = BCrypt.hashpw(it, BCrypt.gensalt()) }
        request.phone.takeIf { !it.isNullOrEmpty() }!!.let { this.phone = it }
        request.telegram?.takeIf { it.isNotEmpty() }.let { this.telegram = it }
        request.whatsapp?.takeIf { it.isNotEmpty() }.let { this.whatsapp = it }
        request.max?.takeIf { it.isNotEmpty() }.let { this.max = it }
        request.firstname.takeIf { !it.isNullOrEmpty() }!!.let { this.firstname = it }
        request.lastname.takeIf { !it.isNullOrEmpty() }!!.let { this.lastname = it }
        request.patronymic.takeIf { !it.isNullOrEmpty() }!!.let { this.patronymic = it }
    }

    override fun update(request: IdentityRequest) {
        request.email.takeIf { !it.isNullOrEmpty() }?.let { this.email = it }
        request.phone.takeIf { !it.isNullOrEmpty() }?.let { this.phone = it }
        request.telegram?.takeIf { it.isNotEmpty() }.let { this.telegram = it }
        request.whatsapp?.takeIf { it.isNotEmpty() }.let { this.whatsapp = it }
        request.max?.takeIf { it.isNotEmpty() }.let { this.max = it }
        request.firstname.takeIf { !it.isNullOrEmpty() }?.let { this.firstname = it }
        request.lastname.takeIf { !it.isNullOrEmpty() }?.let { this.lastname = it }
        request.patronymic.takeIf { !it.isNullOrEmpty() }?.let { this.patronymic = it }
    }

    override suspend fun toDependency(): IdentityDependency {
        return IdentityDependency(
            id = this.id.value,
            email = this.email,
            phone = this.phone,
            telegram = this.telegram,
            whatsapp = this.whatsapp,
            max = this.max,
            firstname = this.firstname,
            lastname = this.lastname,
            patronymic = this.patronymic,
            image = this.image?.toDependency()
        )
    }

    override suspend fun toResponse(): IdentityResponse {
        return IdentityResponse(
            id = this.id.value,
            email = this.email,
            phone = this.phone,
            telegram = this.telegram,
            whatsapp = this.whatsapp,
            max = this.max,
            firstname = this.firstname,
            lastname = this.lastname,
            patronymic = this.patronymic,
            image = this.image?.toDependency(),
            homesByLessor = this.homesByLessor.map { it.toHomeDependencyWithTenant() },
            homesByTenant = this.homesByTenant.map { it.toHomeDependencyWithLessor() },
            meetings = this.meetings.map { it.toMeetingDependencyInApplicant() }
        )
    }
}

class AddressEntity(id: EntityID<Uuid>) : UuidEntity(id), Dao, Creator<AddressRequest>, Modifier<AddressRequest>,
    DependencyMapper<AddressDependency> {
    companion object : UuidEntityClass<AddressEntity>(AddressTable)

    var country by AddressTable.country
    var city by AddressTable.city
    var street by AddressTable.street
    var building by AddressTable.building
    var apartment by AddressTable.apartment

    override fun insert(request: AddressRequest) {
        request.country.takeIf { !it.isNullOrEmpty() }!!.let { this.country = it }
        request.city.takeIf { !it.isNullOrEmpty() }!!.let { this.city = it }
        request.street.takeIf { !it.isNullOrEmpty() }!!.let { this.street = it }
        request.building.takeIf { !it.isNullOrEmpty() }!!.let { this.building = it }
        request.apartment?.takeIf { it.isNotEmpty() }.let { this.apartment = it }
    }

    override fun update(request: AddressRequest) {
        request.country.takeIf { !it.isNullOrEmpty() }?.let { this.country = it }
        request.city.takeIf { !it.isNullOrEmpty() }?.let { this.city = it }
        request.street.takeIf { !it.isNullOrEmpty() }?.let { this.street = it }
        request.building.takeIf { !it.isNullOrEmpty() }?.let { this.building = it }
        request.apartment?.takeIf { it.isNotEmpty() }.let { this.apartment = it }
    }

    override suspend fun toDependency(): AddressDependency {
        return AddressDependency(
            id = this.id.value,
            country = this.country,
            city = this.city,
            street = this.street,
            building = this.building,
            apartment = this.apartment
        )
    }
}

class HomeEntity(id: EntityID<Uuid>) : UuidEntity(id), Dao, Creator<HomeRequest>, Modifier<HomeRequest>,
    DependencyMapper<HomeDependency>, ResponseMapper<HomeResponse> {
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

    override fun insert(request: HomeRequest) {
        request.homeType!!.let { this.homeType = it }
        request.address!!.let { this.address = AddressEntity.new { this.insert(it) } }
        request.description?.takeIf { it.isNotEmpty() }.let { this.description = it }
        request.billingUnit!!.let { this.billingUnit = it }
        request.price!!.let { this.price = it }
        request.lessorUuid!!.let { this.lessor = IdentityEntity[it] }
        request.tenantUuid?.let { this.tenant = IdentityEntity[it] }
    }

    override fun update(request: HomeRequest) {
        request.homeType?.let { this.homeType = it }
        request.address?.let {
            this.address = AddressEntity.findByIdAndUpdate(it.uuid!!)
            { addressEntity -> addressEntity.update(it) }!!
        }
        request.description?.takeIf { it.isNotEmpty() }.let { this.description = it }
        request.billingUnit?.let { this.billingUnit = it }
        request.price?.let { this.price = it }
        request.lessorUuid?.let { this.lessor = IdentityEntity[it] }
        request.tenantUuid?.let { this.tenant = IdentityEntity[it] }
    }

    suspend fun toHomeDependencyWithTenant(): HomeDependencyWithTenant {
        return HomeDependencyWithTenant(
            id = this.id.value,
            homeType = this.homeType,
            address = this.address.toDependency(),
            description = this.description,
            billingUnit = this.billingUnit,
            price = this.price,
            tenant = this.tenant?.toDependency(),
            image = this.image?.toDependency()
        )
    }

    suspend fun toHomeDependencyWithLessor(): HomeDependencyWithLessor {
        return HomeDependencyWithLessor(
            id = this.id.value,
            homeType = this.homeType,
            address = this.address.toDependency(),
            description = this.description,
            billingUnit = this.billingUnit,
            price = this.price,
            lessor = this.lessor.toDependency(),
            image = this.image?.toDependency()
        )
    }

    override suspend fun toDependency(): HomeDependency {
        return HomeDependency(
            id = this.id.value,
            homeType = this.homeType,
            address = this.address.toDependency(),
            description = this.description,
            billingUnit = this.billingUnit,
            price = this.price,
            lessor = this.lessor.toDependency(),
            tenant = this.tenant?.toDependency(),
            image = this.image?.toDependency()
        )
    }

    override suspend fun toResponse(): HomeResponse {
        return HomeResponse(
            id = this.id.value,
            homeType = this.homeType,
            address = this.address.toDependency(),
            description = this.description,
            billingUnit = this.billingUnit,
            price = this.price,
            lessor = this.lessor.toDependency(),
            tenant = this.tenant?.toDependency(),
            image = this.image?.toDependency(),
            images = this.images.map { it.toDependency() },
            videos = this.videos.map { it.toDependency() },
            meetings = this.meetings.map { it.toMeetingDependencyInHome() }
        )
    }
}

class MeetingEntity(id: EntityID<Uuid>) : UuidEntity(id), Dao, Creator<MeetingRequest>, Modifier<MeetingRequest>, ResponseMapper<MeetingResponse> {
    companion object : UuidEntityClass<MeetingEntity>(MeetingTable)

    var home by HomeEntity.referencedOn(MeetingTable.homeId)
    var applicant by IdentityEntity.referencedOn(MeetingTable.applicantId)
    var dateTime by MeetingTable.dateTime
    var tookPlace by MeetingTable.tookPlace

    override fun insert(request: MeetingRequest) {
        request.homeUuid!!.let { this.home = HomeEntity[it] }
        request.applicantUuid!!.let { this.applicant = IdentityEntity[it] }
        request.datetime!!.let { this.dateTime = it }
        request.tookPlace?.let { this.tookPlace = it }
    }

    override fun update(request: MeetingRequest) {
        request.homeUuid?.let { this.home = HomeEntity[it] }
        request.applicantUuid?.let { this.applicant = IdentityEntity[it] }
        request.datetime?.let { this.dateTime = it }
        request.tookPlace?.let { this.tookPlace = it }
    }

    suspend fun toMeetingDependencyInHome(): MeetingDependencyInHome {
        return MeetingDependencyInHome(
            id = this.id.value,
            applicant = this.applicant.toDependency(),
            datetime = this.dateTime,
            tookPlace = this.tookPlace
        )
    }

    suspend fun toMeetingDependencyInApplicant(): MeetingDependencyInApplicant {
        return MeetingDependencyInApplicant(
            id = this.id.value,
            home = this.home.toDependency(),
            datetime = this.dateTime,
            tookPlace = this.tookPlace
        )
    }

    override suspend fun toResponse(): MeetingResponse {
        return MeetingResponse(
            id = this.id.value,
            home = this.home.toDependency(),
            applicant = this.applicant.toDependency(),
            datetime = this.dateTime,
            tookPlace = this.tookPlace
        )
    }
}