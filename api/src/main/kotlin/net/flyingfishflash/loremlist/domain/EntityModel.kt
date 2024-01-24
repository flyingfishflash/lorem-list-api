package net.flyingfishflash.loremlist.domain

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import java.time.OffsetDateTime

object LrmListTable : LongIdTable("list") {
  val created: Column<OffsetDateTime> = timestampWithTimeZone(name = "created")
  val name: Column<String> = varchar(name = "name", length = 64)
  val description: Column<String?> = varchar(name = "description", length = 2048).nullable()
}

object LrmListItemTable : LongIdTable("item") {
  val created: Column<OffsetDateTime> = timestampWithTimeZone(name = "created")
  val name: Column<String> = varchar("name", length = 64)
  val description: Column<String?> = varchar("description", length = 2048).nullable()
  val quantity: Column<Long?> = long("quantity").nullable()
}

object LrmListsItemsTable : Table("lists_items") {
  val list = reference("list_id", LrmListTable, onDelete = ReferenceOption.CASCADE)
  val item = reference("item_id", LrmListItemTable, onDelete = ReferenceOption.CASCADE)
}
