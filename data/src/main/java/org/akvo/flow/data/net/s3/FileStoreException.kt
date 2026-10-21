/*
 * Copyright (C) 2026 Stichting Akvo (Akvo Foundation)
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
package org.akvo.flow.data.net.s3

import retrofit2.HttpException

/**
 * An upload that the file store itself refused, as opposed to one the processor
 * refused. The distinction matters for 404: the processor is asked for the form by
 * id and answers 404 when the form has been deleted, while the file store knows
 * nothing about forms and a 404 from it is a transport fault.
 */
class FileStoreException(cause: HttpException) : Exception(cause)
