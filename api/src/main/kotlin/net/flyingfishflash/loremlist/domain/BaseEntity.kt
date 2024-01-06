package net.flyingfishflash.loremlist.domain

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@MappedSuperclass
class BaseEntity {
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.UUID)
  var id: UUID? = null

  @CreationTimestamp
  @Column(updatable = false)
  var createdInstant: Instant? = null
}
