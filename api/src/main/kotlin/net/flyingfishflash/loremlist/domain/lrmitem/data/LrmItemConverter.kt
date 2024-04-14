package net.flyingfishflash.loremlist.domain.lrmitem.data

import net.flyingfishflash.loremlist.domain.LrmListItemTable
import org.jetbrains.exposed.sql.ResultRow

object LrmItemConverter {
  fun toLrmItem(resultRow: ResultRow): LrmItem = LrmItem(
    id = resultRow[LrmListItemTable.id].value,
    created = resultRow[LrmListItemTable.created],
    name = resultRow[LrmListItemTable.name],
    description = resultRow[LrmListItemTable.description],
    quantity = resultRow[LrmListItemTable.quantity],
  )
}
