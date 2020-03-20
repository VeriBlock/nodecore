// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.altchainmonitor.util

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

fun LocalDateTime.toJoda(): DateTime =
	DateTime(toInstant(ZoneOffset.UTC).toEpochMilli(), DateTimeZone.UTC)

fun LocalDate.toJoda(): DateTime =
	atStartOfDay().toJoda()

fun DateTime.toJavaDate(): LocalDate =
	LocalDateTime.ofInstant(withZoneRetainFields(DateTimeZone.UTC).toDate().toInstant(), ZoneOffset.UTC).toLocalDate()

fun DateTime.toJavaDateTime(): LocalDateTime =
	LocalDateTime.ofInstant(withZone(DateTimeZone.UTC).toDate().toInstant(), ZoneOffset.UTC)
