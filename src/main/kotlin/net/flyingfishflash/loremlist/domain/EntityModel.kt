package net.flyingfishflash.loremlist.domain

import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object LrmListTable : UUIDTable("list", "id") {
  val name: Column<String> = varchar(name = "name", length = 64)
  val description: Column<String?> = varchar(name = "description", length = 2048).nullable()
  val public: Column<Boolean> = bool("public")
  val created: Column<Instant> = timestamp(name = "created")
  val updated: Column<Instant> = timestamp(name = "updated")
}

object LrmListItemTable : UUIDTable("item", "id") {
  val name: Column<String> = varchar("name", length = 64)
  val description: Column<String?> = varchar("description", length = 2048).nullable()
  val quantity: Column<Int> = integer("quantity")
  val created: Column<Instant> = timestamp(name = "created")
  val updated: Column<Instant> = timestamp(name = "updated")
}

object LrmListsItemsTable : UUIDTable("lists_items", "id") {
  val list = reference("list_id", LrmListTable, onDelete = ReferenceOption.CASCADE)
  val item = reference("item_id", LrmListItemTable, onDelete = ReferenceOption.CASCADE)
  init {
    uniqueIndex("list_item_index", list, item)
  }
}
