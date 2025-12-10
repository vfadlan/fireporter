package com.fadlan.fireporter.utils.exceptions

class InactiveAccountException(message: String = "No active transaction found in the specified period."): IllegalArgumentException(message)