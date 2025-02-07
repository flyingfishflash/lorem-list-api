import kotlinx.datetime.Instant
import java.util.*

data class LrmItem(
  val id: UUID,
  var name: String,
  var description: String? = null,
  var quantity: Int = 0,
  val created: Instant? = null,
  val createdBy: String? = null,
  var updated: Instant? = null,
  var updatedBy: String? = null,
) {

  private val visibilityMap = mutableMapOf<LrmList, Boolean>()

  fun setVisibilityInList(list: LrmList, isVisible: Boolean) {
    visibilityMap[list] = isVisible
  }

  fun getVisibilityInList(list: LrmList): Boolean {
    return visibilityMap.getOrDefault(list, false) // Default to false if not set
  }
}

data class LrmList(
  val id: UUID,
  var name: String,
  var description: String? = null,
  var public: Boolean = false,
  val created: Instant? = null,
  val createdBy: String? = null,
  var updated: Instant? = null,
  var updatedBy: String? = null,
)
