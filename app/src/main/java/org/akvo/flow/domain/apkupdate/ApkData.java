package org.akvo.flow.domain.apkupdate;

public class ApkData {

    private final String version;
    private final String fileUrl;
    private final String md5Checksum;

    public ApkData(String version, String fileUrl, String md5Checksum) {
        this.version = version;
        this.fileUrl = fileUrl;
        this.md5Checksum = md5Checksum;
    }

    public String getVersion() {
        return version;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getMd5Checksum() {
        return md5Checksum;
    }
}
