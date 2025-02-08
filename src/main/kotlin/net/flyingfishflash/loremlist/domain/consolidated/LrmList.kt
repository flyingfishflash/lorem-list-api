package net.flyingfishflash.loremlist.domain.consolidated

import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.Instant
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmListSuccinct
import java.util.UUID

// @Serializable
// @OptIn(ExperimentalSerializationApi::class)
data class LrmList
constructor(
//  @Serializable(with = UUIDSerializer::class)
  var id: UUID,
  var name: String,
  var description: String? = null,
//  @EncodeDefault var public: Boolean = false,
  var owner: String? = null,
  var created: Instant? = null,
  var creator: String? = null,
  var updated: Instant? = null,
  var updater: String? = null,
  val items: Set<LrmItem>? = null,
)

fun LrmList.succinct() = LrmListSuccinct(id = this.id, name = this.name)

sealed class LrmListSealed {
  abstract val id: UUID
  abstract val name: String
  abstract val description: String
  abstract val creator: String
  abstract val owner: String
  abstract val updater: String
  abstract val created: Instant
  abstract val updated: Instant

  data class Created(override val id: UUID, override val name: String, override val description: String, override val creator: String) :
    LrmListSealed() {
    override val owner = creator
    override val updater = creator
    override val created = now()
    override val updated = created
  }
}

fun createLrmList(): LrmListSealed = LrmListSealed.Created(id=UUID.randomUUID(), "Item", "Item Description", "Creator")

sealed class Order {
  abstract val id: OrderId
  data class Created(override val id: OrderId) : Order()
  data class Cancelled(override val id: OrderId, val cancellationReason: CancellationReason?, val cancellationDateTime: Instant) : Order()
  data class Confirmed(override val id: OrderId, val confirmationDateTime: Instant) : Order()
}
fun createOrder(): Order = Order.Created(OrderId(UUID.randomUUID()))
fun confirmOrder(order: Order, confirmationDateTime: Instant): Either<OrderError, Order> = when (order) {
  is Order.Cancelled -> Either.Left(OrderError("Cannot confirm already cancelled order"))
  is Order.Confirmed -> Either.Left(OrderError("Cannot confirm already confirmed order"))
  is Order.Created -> Either.Right(Order.Confirmed(order.id, confirmationDateTime))
}
