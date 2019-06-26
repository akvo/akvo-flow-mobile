/*
 * Copyright (C) 2019 Stichting Akvo (Akvo Foundation)
 *
 * This file is part of Akvo Flow.
 *
 * Akvo Flow is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Akvo Flow is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Akvo Flow.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.akvo.flow.util

import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.specs.ShouldSpec

class Base32Test : ShouldSpec() {

    init {
        should("Base32Uuid contain 12 characters") {
            forAll(Generators.Base32Generator()) { base32: Base32 ->
                val base32Uuid = base32.base32Uuid()
                base32Uuid.length == 12
            }
        }

        should("Base32Uuid not contain l") {
            forAll(Generators.Base32Generator()) { base32: Base32 ->
                val base32Uuid = base32.base32Uuid()
                !base32Uuid.contains("l")
            }
        }

        should("Base32Uuid not contain o") {
            forAll(Generators.Base32Generator()) { base32: Base32 ->
                val base32Uuid = base32.base32Uuid()
                !base32Uuid.contains("o")
            }
        }

        should("Base32Uuid not contain i") {
            forAll(Generators.Base32Generator()) { base32: Base32 ->
                val base32Uuid = base32.base32Uuid()
                !base32Uuid.contains("i")
            }
        }
    }

    object Generators {

        open class Base32Generator : Gen<Base32> {

            override fun constants() = emptyList<Base32>()

            override fun random(): Sequence<Base32> = generateSequence { Base32() }
        }
    }
}