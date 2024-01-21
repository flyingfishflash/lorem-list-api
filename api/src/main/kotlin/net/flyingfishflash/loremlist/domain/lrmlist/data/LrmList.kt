package net.flyingfishflash.loremlist.domain.lrmlist.data

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.SequenceGenerator
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.core.configuration.InstantSerializer
import net.flyingfishflash.loremlist.domain.lrmlistitem.data.LrmListItem
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Serializable
@Entity(name = "list")
class LrmList(
  @SequenceGenerator(name = "list_id_seq", sequenceName = "list_seq", allocationSize = 1)
  @GeneratedValue(generator = "list_id_seq")
  @Id
  var id: Long? = null,
  @CreationTimestamp
  @Column(updatable = false)
  @Serializable(with = InstantSerializer::class)
  var createdInstant: Instant? = null,
  @Column(length = 64)
  var name: String,
  @Column(length = 2048)
  var description: String? = null,
  @ManyToMany(fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
  @JoinTable(
    name = "lists_items",
    joinColumns = [JoinColumn(name = "list_id")],
    inverseJoinColumns = [JoinColumn(name = "item_id")],
  )
  val items: MutableSet<LrmListItem> = mutableSetOf(),
) {
  fun addItem(lrmListItem: LrmListItem) {
    this.items.add(lrmListItem)
    lrmListItem.lists.add(this)
  }

  fun removeItem(itemId: Long) {
    val lrmListItem: LrmListItem = this.items.stream().filter { t -> t.id == itemId }.findFirst().orElse(null)
    this.items.remove(lrmListItem)
    lrmListItem.lists.remove(this)
  }
}
