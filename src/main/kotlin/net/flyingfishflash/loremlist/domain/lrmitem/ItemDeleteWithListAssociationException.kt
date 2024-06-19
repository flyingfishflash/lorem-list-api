package net.flyingfishflash.loremlist.domain.lrmitem

import net.flyingfishflash.loremlist.core.exceptions.ApiException
import net.flyingfishflash.loremlist.domain.lrmitem.data.LrmItemDeleteResponse
import org.springframework.http.HttpStatus

class ItemDeleteWithListAssociationException(
  id: Long,
  associationDetail: LrmItemDeleteResponse,
) : ApiException(
  httpStatus = HTTP_STATUS,
  title = "ItemDeleteWithListAssociationException",
  message = defaultMessage(id, associationDetail),
) {
  companion object {
    val HTTP_STATUS = HttpStatus.UNPROCESSABLE_ENTITY
    fun defaultMessage(id: Long, associationDetail: LrmItemDeleteResponse) =
      "Item id $id could not be deleted because it's associated with ${associationDetail.countItemToListAssociations} list(s). " +
        "First remove the item from each list."
  }
}
