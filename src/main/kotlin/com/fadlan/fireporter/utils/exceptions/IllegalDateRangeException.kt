package com.fadlan.fireporter.utils.exceptions

class IllegalDateRangeException(message: String = "Start date of a period must be before end date with at least 1 day difference."): IllegalArgumentException(message)