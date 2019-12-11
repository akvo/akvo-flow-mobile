/*
 * Copyright (C) 2017 Stichting Akvo (Akvo Foundation)
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
 *
 */

package org.akvo.flow.util.logging;

import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(PowerMockRunner.class)
public class SentryTreeTest {

    private static final String TEST_TAG = "Test Tag";
    private static final String TEST_MESSAGE = "dummy message";
    private static final IllegalArgumentException TEST_EXCEPTION = new IllegalArgumentException("");

    @Test
    public void log_shouldIgnoreExceptionIfNullThrowable() {
        SentryTree tree = spy(new SentryTree());

        tree.log(Log.ERROR, TEST_TAG, TEST_MESSAGE, null);

        verify(tree, times(0)).captureException(any(Throwable.class), anyString());
    }

    @Test
    public void log_shouldIgnoreExceptionIfLowerThanError() {
        SentryTree tree = spy(new SentryTree());

        tree.log(Log.DEBUG, TEST_TAG, TEST_MESSAGE, TEST_EXCEPTION);

        verify(tree, times(0)).captureException(any(Throwable.class), anyString());
    }

    @Test
    public void log_shouldIgnoreExceptionIfFilteredException() {
        SentryTree tree = spy(new SentryTree());

        tree.log(Log.ERROR, TEST_TAG, TEST_MESSAGE, new java.net.ConnectException());

        verify(tree, times(0)).captureException(any(Throwable.class), anyString());
    }

    @Test
    public void log_shouldIgnoreExceptionIfFilteredNestedException() {
        SentryTree tree = spy(new SentryTree());

        tree.log(Log.ERROR, TEST_TAG, TEST_MESSAGE, new Exception(new java.net.ConnectException()));

        verify(tree, times(0)).captureException(any(Throwable.class), anyString());
    }

    @Test
    public void log_shouldIgnoreExceptionIfFilteredMessage() {
        SentryTree tree = spy(new SentryTree());

        tree.log(Log.ERROR, TEST_TAG, "HTTP 500 Internal Server Error",
                new java.net.ConnectException());

        verify(tree, times(0)).captureException(any(Throwable.class), anyString());
    }

    @Test
    public void log_shouldCaptureAcceptedException() {
        SentryTree tree = spy(new SentryTree());
        doNothing().when(tree).captureException(any(Throwable.class), anyString());

        tree.log(Log.ERROR, TEST_TAG, TEST_MESSAGE, TEST_EXCEPTION);

        verify(tree, times(1)).captureException(any(Throwable.class), anyString());
    }
}
