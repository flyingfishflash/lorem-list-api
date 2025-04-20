package net.flyingfishflash.loremlist.core.response.structure

import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.springframework.http.HttpStatus
import java.util.*

/**
 * Represents the disposition of any request from a client.
 *
 * <p>Every response to a client must include exactly one Disposition.
 */
interface Disposition {
  fun nameAsLowercase(): String
}

@Serializable
enum class DispositionOfSuccess : Disposition {
  /** Client request success */
  @SerialName("success")
  SUCCESS,
  ;

  /** Returns an enum constant name() in lowercase */
  @JsonValue
  override fun nameAsLowercase(): String {
    return name.lowercase(Locale.getDefault())
  }
}

@Serializable
enum class DispositionOfProblem : Disposition {
  /** Client request disposition of error: http 5xx - unanticipated problem in platform, framework or server */
  @SerialName("error")
  ERROR,

  /** Client request disposition of failure: http 4xx - anticipated problem */
  @SerialName("failure")
  FAILURE,

  /** Client request disposition of undefined: http 1xx->3xx */
  @SerialName("undefined")
  UNDEFINED,
  ;

  /** Returns an enum constant name() in lowercase */
  @JsonValue
  override fun nameAsLowercase(): String {
    return name.lowercase(Locale.getDefault())
  }

  companion object {
    /** Calculate the disposition of the API Event from the Http status */
    fun calcDisposition(httpStatus: HttpStatus) = when {
      httpStatus.is4xxClientError -> FAILURE
      httpStatus.is5xxServerError -> ERROR
      else -> UNDEFINED
    }
  }
}
