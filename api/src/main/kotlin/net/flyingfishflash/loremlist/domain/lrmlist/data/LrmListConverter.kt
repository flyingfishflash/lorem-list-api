package net.flyingfishflash.loremlist.domain.lrmlist.data

import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest

object LrmListConverter {
  fun toLrmList(
    lrmListRequest: LrmListRequest,
    lrmList: LrmList,
  ): LrmList {
    lrmList.name = lrmListRequest.name
    lrmList.description = lrmListRequest.description
    return lrmList
  }
}
