package net.flyingfishflash.loremlist.domain.lrmlistitem.data

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.SequenceGenerator
import kotlinx.serialization.Serializable
import net.flyingfishflash.loremlist.domain.lrmlist.data.LrmList

@Serializable
@Entity(name = "item")
class LrmListItem(
  @SequenceGenerator(name = "item_id_seq", sequenceName = "item_seq", allocationSize = 1)
  @GeneratedValue(generator = "item_id_seq")
  @Id
  var id: Long? = null,
//  @CreationTimestamp
//  @Column(updatable = false)
//  var createdInstant: Instant? = null,
  @Column
  var name: String,
  @Column
  var details: String? = null,
  @Column
  var quantity: Short? = null,
  @JsonIgnore
  @ManyToMany(
    fetch = FetchType.LAZY,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE],
    mappedBy = "items",
  )
  val lists: MutableSet<LrmList> = mutableSetOf(),
)
