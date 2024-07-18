package net.flyingfishflash.loremlist.domain

import java.util.UUID

interface SuccinctLrmComponent {
  var uuid: UUID
  val name: String
}
