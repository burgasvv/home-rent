package org.burgas.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.burgas.database.Authority
import org.burgas.database.BillingUnit
import org.burgas.database.HomeType
import kotlin.uuid.Uuid

interface Request {
    val id: String?
}

interface Dependency {
    val id: Uuid
}

interface Response {
    val id: Uuid
}

@Serializable
data class ExceptionResponse(
    val status: String,
    val code: Int,
    val message: String?
)

@Serializable
data class IdentityPrincipal(
    val id: Uuid,
    val authority: Authority
)

@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

@Serializable
data class ImageDependency(
    override val id: Uuid,
    val name: String?,
    val contentType: String
) : Dependency

@Serializable
data class VideoDependency(
    override val id: Uuid,
    val name: String?,
    val contentType: String
) : Dependency

@Serializable
data class EntityFileRequest(
    val entityId: String,
    val filesIds: List<String>
) {
    val entityUuid: Uuid
        get() = entityId.let { Uuid.parse(it) }
    val fileUuids: List<Uuid>
        get() = filesIds.map { Uuid.parse(it) }
}

@Serializable
data class IdentityRequest(
    override val id: String?,
    val authority: Authority?,
    val email: String?,
    val password: String?,
    val status: Boolean?,
    val phone: String?,
    val telegram: String?,
    val whatsapp: String?,
    val max: String?,
    val firstname: String?,
    val lastname: String?,
    val patronymic: String?
) : Request {
    val uuid: Uuid?
        get() = id?.let { Uuid.parse(it) }
}

@Serializable
data class IdentityDependency(
    override val id: Uuid,
    val email: String,
    val phone: String,
    val telegram: String?,
    val whatsapp: String?,
    val max: String?,
    val firstname: String,
    val lastname: String,
    val patronymic: String,
    val image: ImageDependency?
) : Dependency

@Serializable
data class IdentityResponse(
    override val id: Uuid,
    val email: String,
    val phone: String,
    val telegram: String?,
    val whatsapp: String?,
    val max: String?,
    val firstname: String,
    val lastname: String,
    val patronymic: String,
    val image: ImageDependency?,
    val homesByLessor: List<HomeDependencyWithTenant>,
    val homesByTenant: List<HomeDependencyWithLessor>?,
    val meetings: List<MeetingDependencyInApplicant>
) : Response

@Serializable
data class AddressRequest(
    override val id: String?,
    val country: String?,
    val city: String?,
    val street: String?,
    val building: String?,
    val apartment: String?
) : Request {
    val uuid: Uuid?
        get() = id?.let { Uuid.parse(it) }
}

@Serializable
data class AddressDependency(
    override val id: Uuid,
    val country: String,
    val city: String,
    val street: String,
    val building: String,
    val apartment: String?
) : Dependency

@Serializable
data class HomeRequest(
    override val id: String?,
    val homeType: HomeType?,
    val address: AddressRequest?,
    val description: String?,
    val billingUnit: BillingUnit?,
    val price: Double?,
    val lessorId: String?,
    val tenantId: String?
) : Request {
    val uuid: Uuid?
        get() = id?.let { Uuid.parse(it) }
    val lessorUuid: Uuid?
        get() = lessorId?.let { Uuid.parse(it) }
    val tenantUuid: Uuid?
        get() = tenantId?.let { Uuid.parse(it) }
}

@Serializable
data class HomeDependencyWithTenant(
    override val id: Uuid,
    val homeType: HomeType,
    val address: AddressDependency,
    val description: String?,
    val billingUnit: BillingUnit,
    val price: Double,
    val tenant: IdentityDependency?,
    val image: ImageDependency?
) : Dependency

@Serializable
data class HomeDependencyWithLessor(
    override val id: Uuid,
    val homeType: HomeType,
    val address: AddressDependency,
    val description: String?,
    val billingUnit: BillingUnit,
    val price: Double,
    val lessor: IdentityDependency,
    val image: ImageDependency?
) : Dependency

@Serializable
data class HomeDependency(
    override val id: Uuid,
    val homeType: HomeType,
    val address: AddressDependency,
    val description: String?,
    val billingUnit: BillingUnit,
    val price: Double,
    val lessor: IdentityDependency,
    val tenant: IdentityDependency?,
    val image: ImageDependency?
) : Dependency

@Serializable
data class HomeResponse(
    override val id: Uuid,
    val homeType: HomeType,
    val address: AddressDependency,
    val description: String?,
    val billingUnit: BillingUnit,
    val price: Double,
    val lessor: IdentityDependency,
    val tenant: IdentityDependency?,
    val image: ImageDependency?,
    val images: List<ImageDependency>,
    val videos: List<VideoDependency>,
    val meetings: List<MeetingDependencyInHome>
) : Response

@Serializable
data class MeetingRequest(
    override val id: String?,
    val homeId: String?,
    val applicantId: String?,
    val datetime: LocalDateTime?,
    val tookPlace: Boolean?
) : Request {
    val uuid: Uuid?
        get() = id?.let { Uuid.parse(it) }
    val homeUuid: Uuid?
        get() = homeId?.let { Uuid.parse(it) }
    val applicantUuid: Uuid?
        get() = applicantId?.let { Uuid.parse(it) }
}

@Serializable
data class MeetingDependencyInHome(
    override val id: Uuid,
    val applicant: IdentityDependency,
    val datetime: LocalDateTime,
    val tookPlace: Boolean
) : Dependency

@Serializable
data class MeetingDependencyInApplicant(
    override val id: Uuid,
    val home: HomeDependency,
    val datetime: LocalDateTime,
    val tookPlace: Boolean
) : Dependency

@Serializable
data class MeetingResponse(
    override val id: Uuid,
    val home: HomeDependency,
    val applicant: IdentityDependency,
    val datetime: LocalDateTime,
    val tookPlace: Boolean
) : Response