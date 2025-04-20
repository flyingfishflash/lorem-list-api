package net.flyingfishflash.loremlist.persistence

import kotlinx.datetime.Instant
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
  val owner: Column<String> = varchar("owner", length = 64)
  val created: Column<Instant> = timestamp(name = "created")
  val creator: Column<String> = varchar(name = "creator", length = 64)
  val updated: Column<Instant> = timestamp(name = "updated")
  val updater: Column<String> = varchar(name = "updater", length = 64)
  override val primaryKey = PrimaryKey(id, name = "list_pk")
}

object LrmItemTable : Table("item") {
  val id: Column<UUID> = uuid(name = "id")
  val name: Column<String> = varchar("name", length = 64)
  val description: Column<String?> = varchar("description", length = 2048).nullable()
  val owner: Column<String> = varchar(name = "owner", length = 64)
  val created: Column<Instant> = timestamp(name = "created")
  val creator: Column<String> = varchar(name = "creator", length = 64)
  val updated: Column<Instant> = timestamp(name = "updated")
  val updater: Column<String> = varchar(name = "updater", length = 64)
  override val primaryKey = PrimaryKey(id, name = "item_pk")
}

object LrmListItemsTable : Table("list_item") {
  val list = reference(name = "list_id", refColumn = LrmListTable.id, onDelete = ReferenceOption.CASCADE)
  val item = reference(name = "item_id", refColumn = LrmItemTable.id, onDelete = ReferenceOption.CASCADE)
  val itemQuantity: Column<Int> = integer("item_quantity")
  val itemIsSuppressed: Column<Boolean> = bool(name = "item_is_suppressed")
  override val primaryKey = PrimaryKey(list, item, name = "list_items_pk")
}
