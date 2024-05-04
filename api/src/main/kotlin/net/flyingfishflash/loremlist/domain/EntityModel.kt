package net.flyingfishflash.loremlist.domain

import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import java.util.UUID

object LrmListTable : LongIdTable("list") {
  val uuid: Column<UUID> = uuid(name = "uuid").uniqueIndex("list_uuid")
  val created: Column<Instant> = timestamp(name = "created")
  val name: Column<String> = varchar(name = "name", length = 64)
  val description: Column<String?> = varchar(name = "description", length = 2048).nullable()
}

object LrmListItemTable : LongIdTable("item") {
  val uuid: Column<UUID> = uuid(name = "uuid").uniqueIndex("item_uuid")
  val created: Column<Instant> = timestamp(name = "created")
  val name: Column<String> = varchar("name", length = 64)
  val description: Column<String?> = varchar("description", length = 2048).nullable()
  val quantity: Column<Int> = integer("quantity")
}

object LrmListsItemsTable : Table("lists_items") {
  val list = reference("list_id", LrmListTable, onDelete = ReferenceOption.CASCADE)
  val item = reference("item_id", LrmListItemTable, onDelete = ReferenceOption.CASCADE)
}
