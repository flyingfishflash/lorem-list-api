package net.flyingfishflash.loremlist.domain.lrmlist.data

import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import org.springframework.stereotype.Component

@Component
class LrmListMapper {
  fun mapRequestModelToEntityModel(
    lrmListRequest: LrmListRequest,
    lrmList: LrmList,
  ): LrmList {
    lrmList.name = lrmListRequest.name
    lrmList.description = lrmListRequest.description
    return lrmList
  }
}
