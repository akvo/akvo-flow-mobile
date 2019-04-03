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
 *
 */

package org.akvo.flow.data.net.s3;

import android.util.Base64;

import org.akvo.flow.data.entity.S3File;
import org.akvo.flow.data.net.S3User;
import org.akvo.flow.data.net.SignatureHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
public class AmazonAuthHelperTest {

    private static final String PAYLOAD_PUT_PUBLIC = "PUT\n%s\n%s\n%s\nx-amz-acl:public-read\n/%s/%s";

    @Mock
    SignatureHelper mockSignatureHelper;

    @Test
    public void shouldFormatPublicPayloadCorrectly() {
        AmazonAuthHelper helper = new AmazonAuthHelper(null, new S3User("bucket", null, null));
        S3File s3File = spy(new S3File(new File("dir", "name.png"), true, "dir", "action", "md564","md5hex"));
        when(s3File.getContentType()).thenReturn("type");

        String formattedPayload = helper.formatPayloadForPut("12-12-2009", PAYLOAD_PUT_PUBLIC, s3File);

        assertEquals("PUT\nmd564\ntype\n12-12-2009\nx-amz-acl:public-read\n/bucket/dir/name.png", formattedPayload);
    }

    @Test(expected = NullPointerException.class)
    public void shouldFormatPayloadFailIfNullPayload() {
        AmazonAuthHelper helper = new AmazonAuthHelper(null, new S3User("bucket", null, null));
        S3File s3File = spy(new S3File(new File("dir", "name.png"), true, "dir", "action", "md564","md5hex"));
        when(s3File.getContentType()).thenReturn("type");

        helper.formatPayloadForPut("12-12-2009", null, s3File);
    }

    @Test(expected = NullPointerException.class)
    public void shouldFormatPayloadFailIfNullS3File() {
        AmazonAuthHelper helper = new AmazonAuthHelper(null, new S3User("bucket", null, null));

        helper.formatPayloadForPut("12-12-2009", PAYLOAD_PUT_PUBLIC, null);
    }

    @Test
    public void shouldMapSignatureCorrectly() {
        final S3User s3User = new S3User("bucket", "key", "secret");
        S3File s3File = new S3File(new File("dir", "name.png"), true, "dir", "action", "md564","md5hex");
        AmazonAuthHelper helper = spy(new AmazonAuthHelper(mockSignatureHelper, s3User));
        when(mockSignatureHelper.getAuthorization("payload", "secret", Base64.NO_WRAP)).thenReturn("auth");
        when(helper.formatPayloadForPut("12-12-2009", "payload", s3File)).thenReturn("payload");

        String auth = helper.getAmazonAuthForPut("12-12-2009", "payload", s3File);

        assertEquals("AWS key:auth", auth);
    }
}
