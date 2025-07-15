package kr.co.aromit.tvagent.util

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateTimeUtil {
    private val MESSAGE_ID_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
    private val CWMP_TIME_FMT  = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun messageIdNow(): String =
        OffsetDateTime.now(ZoneOffset.systemDefault()).format(MESSAGE_ID_FMT)

    fun currentTimeNow(): String =
        OffsetDateTime.now(ZoneOffset.systemDefault()).format(CWMP_TIME_FMT)
}

