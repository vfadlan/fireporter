package com.fadlan.fireporter.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.bigdecimal.shouldBeZero
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import java.math.BigDecimal
import java.util.Properties

class MiscTest: FunSpec({
    context("HashMap<String, BigDecimal>.getDefaultOrZero") {
        val hm = hashMapOf(
            "test" to BigDecimal(5.24),
            "sample" to BigDecimal(1.22)
        )
        test("should return correct number from hashmap") {
            hm.getOrZero("test").shouldBeEqual(BigDecimal(5.24))
        }
        test("should return correct number from hashmap") {
            hm.getOrZero("sample").shouldBeEqual(BigDecimal(1.22))
        }
        test("should return 0 for nonexistent index") {
            hm.getOrZero("nonexistent").shouldBeZero()
        }
    }

    context("loadProperties") {
        test("should return instance of java.util.properties") {
            loadProperties("config/app.properties").shouldBeInstanceOf<Properties>()
        }
        test("should return null for non-existent file") {
            loadProperties("nonexistent").shouldBeNull()
        }
    }

    context("getProperty") {
        test("should return \"Fireporter\"") {
            getProperty("config/app.properties", "app.name").shouldBeEqual("Fireporter")
        }
        test("should return \"Unknown\" for non-existent file") {
            getProperty("nonexistent", "app.name").shouldBeEqual("Unknown")
        }
        test("should return \"Unknown\" for non-existent property") {
            getProperty("config/app.properties", "app.nonexistent").shouldBeEqual("Unknown")
        }
    }
})