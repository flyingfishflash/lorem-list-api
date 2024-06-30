package net.flyingfishflash.loremlist.core.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.UUID

class UuidValidator : ConstraintValidator<ValidUuid, UUID> {
  private val regex = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"

  override fun isValid(uuid: UUID, cxt: ConstraintValidatorContext): Boolean {
    return uuid.toString().matches(Regex(regex))
  }
}
