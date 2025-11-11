package com.fadlan.fireporter.utils.exceptions

class InactiveAccountException(message: String = "No active account found in the specified period."): IllegalArgumentException(message)