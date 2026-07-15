package com.example.vehicledata

import android.content.Context
import android.provider.Settings
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.Response as RetrofitResponse
import java.util.UUID
import java.util.concurrent.TimeUnit

// ==========================================
// MODELS
// ==========================================
data class VehicleResponse(
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("responseMessage") val responseMessage: List<ResponseMessage>,
    @SerializedName("dataModel") val dataModel: VehicleDataModel?
)

data class ResponseMessage(
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String,
    @SerializedName("fieldName") val fieldName: String?
)

data class VehicleDataModel(
    @SerializedName("energyConsumptionDrive") val energyConsumptionDrive: Double,
    @SerializedName("energyConsumptionHVAC") val energyConsumptionHVAC: Double,
    @SerializedName("energyConsumptionAUX") val energyConsumptionAUX: Double,
    @SerializedName("energyConsumptionBattCare") val energyConsumptionBattCare: Double,
    @SerializedName("energyRegnted") val energyRegnted: Double,
    @SerializedName("hvBattEffy") val hvBattEffy: Double,
    @SerializedName("hvBattSOH") val hvBattSOH: Int,
    @SerializedName("chargeCountAC") val chargeCountAC: Int,
    @SerializedName("chargeCountDC") val chargeCountDC: Int,
    @SerializedName("totalChrgCount") val totalChrgCount: Int,
    @SerializedName("energyInputAC") val energyInputAC: Double,
    @SerializedName("energyInputDC") val energyInputDC: Double,
    @SerializedName("totalEnergyInput") val totalEnergyInput: Double,
    @SerializedName("totalEnergyConsumption") val totalEnergyConsumption: Double
)

data class LikeResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("likes") val likes: Int
)

data class AppLoginResponse(
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("responseMessage") val responseMessage: AppLoginMessage?
)

data class AppLoginMessage(
    @SerializedName("message") val message: String,
    @SerializedName("type") val type: String
)

data class OtpResponse(
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String?
)

data class MyInfoResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: MyInfoData?
)

data class MyInfoData(
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("mobileNumber") val mobileNumber: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("district") val district: String?,
    @SerializedName("pinCode") val pinCode: String?,
    @SerializedName("address1") val address: String?,
    @SerializedName("rsaTel") val rsaTel: String?,
    @SerializedName("callTel") val callTel: String?,
    @SerializedName("vehicleList") val vehicleList: List<VehicleInfo>?
)

data class VehicleInfo(
    @SerializedName("vehicleName") val vehicleName: String?,
    @SerializedName("vinPlainText") val vinPlainText: String?,
    @SerializedName("color") val color: String?,
    @SerializedName("registrationNumber") val registrationNumber: String?,
    @SerializedName("batterySpecification") val batterySpecification: String?,
    @SerializedName("variant") val variant: String?,
    @SerializedName("modelName") val modelName: String?,
    @SerializedName("make") val make: String?,
    @SerializedName("odometer") val odometer: Int?,
    @SerializedName("insuranceExpiry") val insuranceExpiry: String?,
    @SerializedName("purchaseDate") val purchaseDate: String?,
    @SerializedName("motorPower") val motorPower: String?,
    @SerializedName("originalWarrantyEndDate") val originalWarrantyEndDate: String?,
    @SerializedName("primaryUserName") val primaryUserName: String?
)

// ==========================================
// API INTERFACE
// ==========================================
interface VehicleApi {
    @GET("/V1/dataanalytics-service/vehicle-performance/fetch-data")
    suspend fun fetchVehicleData(): VehicleResponse

    @POST("https://bev-likes.pixel3demog.workers.dev/")
    suspend fun sendLike(): LikeResponse

    @GET("/V1/auth/one-app/api/appLogin")
    suspend fun appLogin(
        @Query("mobileNumber") mobileNumber: String,
        @Query("appName") appName: String = "INGLOApp",
        @Query("countryId") countryId: String = "41"
    ): AppLoginResponse

    @GET("/V1/auth/login")
    suspend fun login(
        @Header("Source") source: String = "MMCApp",
        @Header("X-Auth-Username") username: String,
        @Header("X-Auth-Password") password: String,
        @Header("Applogin") applogin: String = "INGLOApp"
    ): RetrofitResponse<okhttp3.ResponseBody>

    @GET("/V1/requestaccess/generateOtpAppIdMapping")
    suspend fun generateOtp(): OtpResponse

    @POST("/V1/auth/validate/otp")
    suspend fun validateOtp(
        @Header("Otp") otp: String
    ): RetrofitResponse<Unit>

    @GET("/V1/vehicleservice/myInfo")
    suspend fun myInfo(): MyInfoResponse
}

// ==========================================
// RETROFIT INSTANCE
// ==========================================
object RetrofitInstance {
    private const val BASE_URL = "https://interface.mahindramobilitysolution.com"

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val urlString = originalRequest.url.toString()
        
        // Skip interceptor for external URLs
        if (!urlString.contains("mahindramobilitysolution.com")) {
            return@Interceptor chain.proceed(originalRequest)
        }

        val requestBuilder = originalRequest.newBuilder()
            .header("Host", "interface.mahindramobilitysolution.com")
            .header("Content-Type", "application/json")
            .header("Connection", "Keep-Alive")
            .header("User-Agent", "okhttp/4.12.0")

        // Add dynamic tokens only if it's not appLogin or login
        if (!urlString.contains("appLogin") && !urlString.contains("auth/login")) {
            val authRepo = U4MeApp.authRepository
            val token = authRepo.jwtToken ?: authRepo.mfaToken
            if (token != null) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        }

        // Add Appuniqueid and Countryid unconditionally
        val authRepo = U4MeApp.authRepository
        requestBuilder.header("Appuniqueid", authRepo.getAppUniqueId())
        requestBuilder.header("Countryid", "41")

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
        level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: VehicleApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VehicleApi::class.java)
    }
}

// ==========================================
// REPOSITORY
// ==========================================
class AuthRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    var jwtToken: String?
        get() = prefs.getString("jwt_token", null)
        set(value) {
            prefs.edit().putString("jwt_token", value).apply()
        }

    var mfaToken: String?
        get() = prefs.getString("mfa_token", null)
        set(value) {
            prefs.edit().putString("mfa_token", value).apply()
        }

    var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) {
            prefs.edit().putString("refresh_token", value).apply()
        }

    fun getAppUniqueId(): String {
        var id = prefs.getString("app_unique_id", null)
        if (id == null) {
            id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (id.isNullOrEmpty()) {
                id = UUID.randomUUID().toString().replace("-", "").substring(0, 16)
            }
            if (id!!.length >= 16) {
                val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
                val randomChar = chars.random()
                id = id.substring(0, id.length - 1) + randomChar
            }
            prefs.edit().putString("app_unique_id", id).apply()
        }
        return id!!
    }

    fun clearTokens() {
        prefs.edit().remove("jwt_token").remove("mfa_token").remove("refresh_token").apply()
    }

    fun clearAppUniqueId() {
        prefs.edit().remove("app_unique_id").apply()
    }
    
    fun isLoggedIn(): Boolean {
        return jwtToken != null
    }
}
