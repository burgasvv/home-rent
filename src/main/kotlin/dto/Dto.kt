package org.burgas.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.burgas.database.Authority
import org.burgas.database.BillingUnit
import org.burgas.database.HomeType
import kotlin.uuid.Uuid

interface Request {
    val id: Uuid?
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
data class IdentityRequest(
    override val id: Uuid?,
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
) : Request

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
    override val id: Uuid?,
    val country: String?,
    val city: String?,
    val street: String?,
    val building: String?,
    val apartment: String?
) : Request

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
    override val id: Uuid?,
    val homeType: HomeType?,
    val address: AddressRequest?,
    val description: String?,
    val billingUnit: BillingUnit?,
    val price: Double?,
    val lessorId: Uuid?,
    val tenantId: Uuid?
) : Request

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
    override val id: Uuid?,
    val homeId: Uuid?,
    val applicantId: Uuid?,
    val datetime: LocalDateTime?,
    val tookPlace: Boolean?
) : Request

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