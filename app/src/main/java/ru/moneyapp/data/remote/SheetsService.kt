package ru.moneyapp.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface SheetsService {
    companion object {
        const val BASE_URL = "https://sheets.googleapis.com/"
        const val SPREADSHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"
        // Kept in sync with OAuth2Service.DRIVE_SCOPE (the OAuth request uses that one).
        const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.readonly"
    }

    @GET("v4/spreadsheets/{spreadsheetId}")
    suspend fun getSpreadsheet(@Path("spreadsheetId") id: String): SheetsSpreadsheet

    @POST("v4/spreadsheets")
    suspend fun createSpreadsheet(@Body body: CreateSpreadsheetRequest): SheetsSpreadsheet

    @GET("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun getValues(
        @Path("spreadsheetId") id: String,
        @Path("range") range: String
    ): ValueRange

    @PUT("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun updateValues(
        @Path("spreadsheetId") id: String,
        @Path("range") range: String,
        @Body body: ValueRange
    ): UpdateValuesResponse

    @POST("v4/spreadsheets/{spreadsheetId}/values/{range}/append")
    suspend fun appendValues(
        @Path("spreadsheetId") id: String,
        @Path("range") range: String,
        @Query("valueInputOption") valueInputOption: String = "RAW",
        @Body body: ValueRange
    ): AppendValuesResponse

    @PATCH("v4/spreadsheets/{spreadsheetId}")
    suspend fun batchUpdate(
        @Path("spreadsheetId") id: String,
        @Body body: BatchUpdateRequest
    ): BatchUpdateResponse

    @DELETE("v4/spreadsheets/{spreadsheetId}/values/{range}")
    suspend fun clearValues(
        @Path("spreadsheetId") id: String,
        @Path("range") range: String
    ): ClearValuesResponse
}

@JsonClass(generateAdapter = true)
data class SheetsSpreadsheet(
    val spreadsheetId: String,
    val spreadsheetUrl: String,
    val properties: SpreadsheetProperties?,
    val sheets: List<Sheet>?
)

@JsonClass(generateAdapter = true)
data class SpreadsheetProperties(
    val title: String,
    val locale: String
)

@JsonClass(generateAdapter = true)
data class Sheet(
    val properties: SheetProperties?
)

@JsonClass(generateAdapter = true)
data class SheetProperties(
    val sheetId: Int,
    val title: String
)

@JsonClass(generateAdapter = true)
data class ValueRange(
    val range: String?,
    val major: String?,
    val values: List<List<Any>>?
)

@JsonClass(generateAdapter = true)
data class UpdateValuesResponse(
    val updatedRows: Int,
    val updatedColumns: Int,
    val updatedCells: Int,
    val updatedRange: String
)

@JsonClass(generateAdapter = true)
data class AppendValuesResponse(
    val updates: AppendValuesResponseUpdates?
)

@JsonClass(generateAdapter = true)
data class AppendValuesResponseUpdates(
    val updatedRows: Int,
    val updatedColumns: Int,
    val updatedCells: Int,
    val updateRange: String
)

@JsonClass(generateAdapter = true)
data class CreateSpreadsheetRequest(
    val properties: CreateSpreadsheetProperties?,
    val sheets: List<CreateSheetRequest>?
)

@JsonClass(generateAdapter = true)
data class CreateSpreadsheetProperties(
    val title: String,
    val locale: String = "ru_RU"
)

@JsonClass(generateAdapter = true)
data class CreateSheetRequest(
    val properties: CreateSheetProperties?
)

@JsonClass(generateAdapter = true)
data class CreateSheetProperties(
    val sheetId: Int,
    val title: String
)

@JsonClass(generateAdapter = true)
data class BatchUpdateRequest(
    val requests: List<Map<String, Any>>
)

@JsonClass(generateAdapter = true)
data class BatchUpdateResponse(
    val replies: List<Any>?,
    val spreadsheetUrl: String?
)

@JsonClass(generateAdapter = true)
data class ClearValuesResponse(
    val clearedRange: String
)
