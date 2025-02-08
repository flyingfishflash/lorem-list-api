package net.flyingfishflash.loremlist.domain.consolidated

import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListPatchRequest

object LrmListConverter {
  fun toLrmList(lrmListRequest: LrmListPatchRequest, lrmList: LrmList): LrmList {
    lrmList.name = lrmListRequest.name
    lrmList.description = lrmListRequest.description
    lrmList.public = lrmListRequest.public
    return lrmList
  }
}
