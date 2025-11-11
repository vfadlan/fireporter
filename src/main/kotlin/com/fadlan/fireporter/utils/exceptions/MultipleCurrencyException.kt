package com.fadlan.fireporter.utils.exceptions

class MultipleCurrencyException(message: String = "Multi-currency transactions are not supported by this system."): UnsupportedOperationException(message)