package com.fadlan.fireporter.utils.exceptions

class InvalidCurrencyCodeException(message: String = "Given currency is not registered or enabled on Firefly III instance."): IllegalArgumentException(message)