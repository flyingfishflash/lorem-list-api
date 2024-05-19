package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.core.exceptions.AbstractApiException
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleteResponse
import org.springframework.http.HttpStatus

class ItemDeleteWithListAssociationException(
  id: Long,
  val associationDetail: LrmItemDeleteResponse,
) : AbstractApiException(
  httpStatus = HTTP_STATUS,
  title = TITLE,
  detail = defaultMessage(id, associationDetail),
  responseMessage = defaultMessage(id, associationDetail),
) {
  companion object {
    const val TITLE = "Cannot Delete an Item Having List Associations"
    val HTTP_STATUS = HttpStatus.UNPROCESSABLE_ENTITY
    fun defaultMessage(id: Long, associationDetail: LrmItemDeleteResponse) =
      "Item id $id could not be deleted because it's associated with ${associationDetail.countItemToListAssociations} list(s). " +
        "First remove the item from each list."
  }
}
