package net.flyingfishflash.loremlist.domain.lrmlist.data

import net.flyingfishflash.loremlist.domain.LrmListTable
import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import org.jetbrains.exposed.sql.ResultRow

object LrmListConverter {
  fun toLrmList(resultRow: ResultRow): LrmList =
    LrmList(
      id = resultRow[LrmListTable.id].value,
      created = resultRow[LrmListTable.created],
      name = resultRow[LrmListTable.name],
      description = resultRow[LrmListTable.description],
    )

  fun toLrmList(
    lrmListRequest: LrmListRequest,
    lrmList: LrmList,
  ): LrmList {
    lrmList.name = lrmListRequest.name
    lrmList.description = lrmListRequest.description
    return lrmList
  }
}
