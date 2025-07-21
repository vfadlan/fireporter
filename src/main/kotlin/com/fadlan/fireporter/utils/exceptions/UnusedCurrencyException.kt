package com.fadlan.fireporter.utils.exceptions

class UnusedCurrencyException(message: String = "Given currency is never used in any transaction or account."): IllegalArgumentException(message)