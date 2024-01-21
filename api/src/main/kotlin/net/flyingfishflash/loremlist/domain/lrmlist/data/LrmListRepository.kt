package net.flyingfishflash.loremlist.domain.lrmlist.data

import net.flyingfishflash.loremlist.domain.lrmlist.data.dto.LrmListRequest
import net.flyingfishflash.loremlist.domain.lrmlist.exceptions.ListNotFoundException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Sequence
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.nextLongVal
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class LrmListRepository {
  private val repositoryTable = LrmListTable
  private val listSequence = Sequence("list_sequence")

  fun insert(lrmListRequest: LrmListRequest): LrmList {
    val id =
      repositoryTable
        .insertAndGetId {
          it[id] = listSequence.nextLongVal()
          it[created] = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
          it[name] = lrmListRequest.name
          it[description] = lrmListRequest.description
        }

    val lrmList =
      repositoryTable.selectAll()
        .where { repositoryTable.id eq id }
        .map { it.toListRecord() }
        // TODO: custom exception
        .singleOrNull() ?: throw ListNotFoundException()
    return lrmList
  }

  fun update(lrmList: LrmList): LrmList {
    val updatedCount =
      repositoryTable.update({ repositoryTable.id eq lrmList.id }) {
        it[name] = lrmList.name
        it[description] = lrmList.description
      }

    if (updatedCount > 0) {
      // TODO: custom exception
      return findByIdOrNull(lrmList.id) ?: throw ListNotFoundException()
    } else {
      // TODO: custom exception
      throw UnsupportedOperationException()
    }
  }

  fun deleteById(id: Long): Int {
    val deletedCount = repositoryTable.deleteWhere { repositoryTable.id eq id }
    // TODO: custom exception
    if (deletedCount > 1) throw UnsupportedOperationException() else return deletedCount
  }

  fun findAll(): MutableList<LrmList> =
    repositoryTable.selectAll()
      .map { it.toListRecord() }
      .toMutableList()

  fun findByIdOrNull(id: Long): LrmList? =
    repositoryTable.selectAll()
      .where { LrmListTable.id eq id }
      .map { it.toListRecord() }
      .firstOrNull()

  private fun ResultRow.toListRecord(): LrmList = LrmListTable.rowToLrmList(this)
}
