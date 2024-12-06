package uk.gov.justice.digital.hmpps.subjectaccessrequestapi.repository

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneId

@Converter(autoApply = true)
class LocalDateTimeAttributeConverter : AttributeConverter<LocalDateTime?, Timestamp> {
  override fun convertToDatabaseColumn(localDateTime: LocalDateTime?): Timestamp? {
    return localDateTime?.let { Timestamp.valueOf(localDateTime) }
  }

  override fun convertToEntityAttribute(timestamp: Timestamp?): LocalDateTime? {
    return timestamp?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
  }
}
