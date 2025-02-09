package net.flyingfishflash.loremlist.domain

import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import java.util.*

object LrmListTable : Table("list") {
  val id: Column<UUID> = uuid(name = "id")
  val name: Column<String> = varchar(name = "name", length = 64)
  val description: Column<String?> = varchar(name = "description", length = 2048).nullable()
  val public: Column<Boolean> = bool("public")
  val created: Column<Instant> = timestamp(name = "created")
  val createdBy: Column<String> = varchar(name = "created_by", length = 64)
  val updated: Column<Instant> = timestamp(name = "updated")
  val updatedBy: Column<String> = varchar(name = "updated_by", length = 64)
  override val primaryKey = PrimaryKey(id, name = "list_pk")
}

object LrmListItemTable : Table("item") {
  val id: Column<UUID> = uuid(name = "id")
  val name: Column<String> = varchar("name", length = 64)
  val description: Column<String?> = varchar("description", length = 2048).nullable()
  val quantity: Column<Int> = integer("quantity")
  val created: Column<Instant> = timestamp(name = "created")
  val createdBy: Column<String> = varchar(name = "created_by", length = 64)
  val updated: Column<Instant> = timestamp(name = "updated")
  val updatedBy: Column<String> = varchar(name = "updated_by", length = 64)
  override val primaryKey = PrimaryKey(id, name = "item_pk")
}

object LrmListsItemsTable : UUIDTable("lists_items", "id") {
  val list = reference(name = "list_id", refColumn = LrmListTable.id, onDelete = ReferenceOption.CASCADE)
  val item = reference(name = "item_id", refColumn = LrmListItemTable.id, onDelete = ReferenceOption.CASCADE)
  init {
    uniqueIndex("list_item_index", list, item)
  }
}
