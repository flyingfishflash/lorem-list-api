package net.flyingfishflash.loremlist.core.response.structure

import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Represents the disposition of any request from a client.
 *
 * <p>Every response to a client must include no more than one Disposition.
 */
@Serializable
enum class Disposition {
  /** Client request error due to an unexpected platform or framework error */
  ERROR,

  /** Client request failure in an anticipated manner */
  FAILURE,

  /** Client request success */
  SUCCESS,

  ;

  /**
   * Returns an enum constant name() formatted in Camel Case (dromedaryCamelCase)
   *
   * @return An enum constant name() formatted in Camel Case (dromedaryCamelCase)
   * @JsonValue
   */
  fun jsonValue(): String {
    return name.lowercase(Locale.getDefault())
  }
}
