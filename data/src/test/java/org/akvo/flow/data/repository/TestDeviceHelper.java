package org.akvo.flow.data.repository;

import org.akvo.flow.data.net.DeviceHelper;

public class TestDeviceHelper extends DeviceHelper {

    public TestDeviceHelper() {
        super(null);
    }

    @Override
    public String getPhoneNumber() {
        return "123";
    }

    @Override
    public String getImei() {
        return "123";
    }

    @Override
    public String getAndroidId() {
        return "123";
    }
}
