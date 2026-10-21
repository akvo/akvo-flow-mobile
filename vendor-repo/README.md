# vendor-repo

A small local Maven repository for build artifacts that are no longer
reachable from their original source and cannot be fetched at build time.

## mapbox-android-accounts 0.7.0

A transitive dependency of `com.mapbox.mapboxsdk:mapbox-android-sdk:9.2.0`.
It only ever lived on jcenter/bintray, was never published to Maven Central,
and has now dropped out of the sunset jcenter CDN, so a fresh dependency
resolution fails without it. This is the proximate cause of the mobile build
breaking with no code change.

The `.aar` here is the genuine jcenter artifact, verified three ways:

- sha256 `07b39c81c61fd97bcd56cb2b4b73d00aed96556ddd1133c7b0d5a308a47c222c`
- byte-identical from three independent mirrors (Aliyun public, Aliyun
  jcenter, Huawei Cloud)
- its sha1 `06751cb5d85a488a2c15e5ba7eede05c78bbbdfc` matches the `.sha1` the
  repositories publish alongside it

Contents: `R.txt`, a 255-byte `AndroidManifest.xml`, a 2.4 KB `classes.jar`.
No native code. Its one declared dependency, `androidx.annotation:annotation`,
resolves from Google's Maven as before.

To re-verify:

    sha256sum vendor-repo/com/mapbox/mapboxsdk/mapbox-android-accounts/0.7.0/mapbox-android-accounts-0.7.0.aar
