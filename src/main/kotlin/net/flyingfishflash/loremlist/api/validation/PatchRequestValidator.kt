package net.flyingfishflash.loremlist.api.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class PatchRequestValidator : ConstraintValidator<ValidPatchRequest, Map<String, Any>> {

  override fun isValid(value: Map<String, Any>?, context: ConstraintValidatorContext): Boolean {
    if (value == null) {
      return buildViolation(context, "patch request cannot be null")
    }

    context.disableDefaultConstraintViolation()
    var isValid = true

    for ((key, rawValue) in value) {
      if (!validKey(key, context)) isValid = false
      if (!validValue(key, rawValue, context)) isValid = false
    }
    return isValid
  }

  private fun validKey(key: String, context: ConstraintValidatorContext): Boolean {
    return if (key.length !in 1..64) {
      buildViolation(context, "Key '$key' must be 1 to 64 characters long")
    } else {
      true
    }
  }

  private fun validValue(key: String, value: Any?, context: ConstraintValidatorContext): Boolean {
    if (value == null) {
      return buildViolation(context, "Value for key '$key' must not be null")
    }

    return when (value) {
      is Boolean -> true
      is String -> validStringValue(key, value, context)
      else -> {
        buildViolation(context, "Value for key '$key' must be a boolean or string")
      }
    }
  }

  private fun validStringValue(key: String, value: String, context: ConstraintValidatorContext): Boolean {
    if (value.isBlank()) {
      return buildViolation(context, "String value for key '$key' must not be blank or whitespace")
    }

    val maxLength = when (key) {
      "description" -> 2048
      else -> 64 // Default for all other keys including "name"
    }

    if (value.length !in 1..maxLength) {
      return buildViolation(context, "Value for field '$key' must be a string from 1 to $maxLength characters long")
    }

    return true
  }

  private fun buildViolation(context: ConstraintValidatorContext, message: String): Boolean {
    context.buildConstraintViolationWithTemplate(message).addConstraintViolation()
    return false
  }
}
