package net.flyingfishflash.loremlist.core.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.*

class UuidSetValidator : ConstraintValidator<ValidUuidSet, Collection<*>> {

  private var allowEmpty: Boolean = true

  override fun initialize(constraintAnnotation: ValidUuidSet) {
    this.allowEmpty = constraintAnnotation.allowEmpty
  }

  override fun isValid(value: Collection<*>?, context: ConstraintValidatorContext): Boolean {
    if (value == null) {
      return buildViolation(context = context, message = "UUID set must not be null")
    }

    if (!allowEmpty && value.isEmpty()) {
      return buildViolation(context = context, message = "UUID set must not be empty")
    }

    context.disableDefaultConstraintViolation()
    var isValid = true

    value.forEachIndexed { index, item ->
      when (item) {
        null -> {
          isValid = buildViolation(context = context, message = "Value at index $index is null")
        }
        is UUID -> {
          val regex = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"
          if (!item.toString().matches(Regex(regex))) {
            isValid = buildViolation(context = context, message = "Value '$item' at index $index is not a valid version 3 or version 4 UUID")
          }
        }
        else -> {
          isValid = buildViolation(context = context, message = "Value at index $index is not a UUID object")
        }
      }
    }
    return isValid
  }

  private fun buildViolation(context: ConstraintValidatorContext, message: String): Boolean {
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation()
    return false
  }
}
