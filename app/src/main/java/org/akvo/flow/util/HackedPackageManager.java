package org.akvo.flow.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by MelEnt on 2016-11-03.
 */
public class HackedPackageManager extends PackageManager
{
    public static PackageInfo globalInfo;

    private PackageManager pkgManager;

    public HackedPackageManager(PackageManager pkgManager)
    {
        this.pkgManager = pkgManager;
    }

    public static void installHack(Context context)
    {
        if (globalInfo == null)
        {
            try
            {
                PackageManager pkgMgr = context.getPackageManager();
                globalInfo = pkgMgr.getPackageInfo(context.getPackageName(), 0);
                if (globalInfo == null)
                    throw new NullPointerException("GlobalInfo is null");
                HackedPackageManager hackedPackageManager = new HackedPackageManager(pkgMgr);
                replaceAllFieldOccurances(context, context.getClass(), hackedPackageManager, PackageManager.class);
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private static void replaceAllFieldOccurances(Object object, Class<?> objectClass, Object replace, Class<?> replaceType) throws IllegalAccessException
    {
        // replace
        for (Field field : objectClass.getDeclaredFields())
        {
            if (field.getType().equals(replaceType))
            {
                field.setAccessible(true);
                field.set(object, replace);
            }
        }
        // recurse
        Class<?> parentClass = objectClass.getSuperclass();
        if (parentClass != null)
        {
            replaceAllFieldOccurances(object, parentClass, replace, replaceType);
        }
    }

    public PackageInfo getPackageInfo(String packageName, int flags) throws PackageManager.NameNotFoundException
    {
        return globalInfo;
    }

    public ResolveInfo resolveService(Intent intent, int flags)
    {
        return pkgManager.resolveService(intent, flags);
    }

    public boolean addPermissionAsync(PermissionInfo info)
    {
        return pkgManager.addPermissionAsync(info);
    }

    public XmlResourceParser getXml(String packageName, int resid, ApplicationInfo appInfo)
    {
        return pkgManager.getXml(packageName, resid, appInfo);
    }

    public Drawable getApplicationIcon(ApplicationInfo info)
    {
        return pkgManager.getApplicationIcon(info);
    }

    public void clearPackagePreferredActivities(String packageName)
    {
        pkgManager.clearPackagePreferredActivities(packageName);
    }

    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags)
    {
        return pkgManager.queryContentProviders(processName, uid, flags);
    }

    public int getComponentEnabledSetting(ComponentName componentName)
    {
        return pkgManager.getComponentEnabledSetting(componentName);
    }

    public ActivityInfo getActivityInfo(ComponentName component, int flags) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getActivityInfo(component, flags);
    }

    @Deprecated
    public void addPackageToPreferred(String packageName)
    {
        pkgManager.addPackageToPreferred(packageName);
    }

    public List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags)
    {
        return pkgManager.getPackagesHoldingPermissions(permissions, flags);
    }

    public Drawable getApplicationBanner(String packageName) throws PackageManager.NameNotFoundException
    {
        return null;
        //pkgManager.getApplicationBanner(packageName);
    }

    public Drawable getActivityLogo(ComponentName activityName) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getActivityLogo(activityName);
    }

    public ServiceInfo getServiceInfo(ComponentName component, int flags) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getServiceInfo(component, flags);
    }

    public Drawable getActivityLogo(Intent intent) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getActivityLogo(intent);
    }

    public Intent getLaunchIntentForPackage(String packageName)
    {
        return pkgManager.getLaunchIntentForPackage(packageName);
    }

    public String[] currentToCanonicalPackageNames(String[] names)
    {
        return pkgManager.currentToCanonicalPackageNames(names);
    }

    public Drawable getActivityIcon(ComponentName activityName) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getActivityIcon(activityName);
    }

    public Drawable getApplicationLogo(ApplicationInfo info)
    {
        return pkgManager.getApplicationLogo(info);
    }

    @Deprecated
    public void removePackageFromPreferred(String packageName)
    {
        pkgManager.removePackageFromPreferred(packageName);
    }

    public Drawable getApplicationLogo(String packageName) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getApplicationLogo(packageName);
    }

    public List<PackageInfo> getPreferredPackages(int flags)
    {
        return pkgManager.getPreferredPackages(flags);
    }

    public Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, Rect badgeLocation, int badgeDensity)
    {
        return null;//pkgManager.getUserBadgedDrawableForDensity(drawable, user, badgeLocation, badgeDensity);
    }

    public Drawable getActivityBanner(ComponentName activityName) throws PackageManager.NameNotFoundException
    {
        return null;//pkgManager.getActivityBanner(activityName);
    }

    public CharSequence getText(String packageName, int resid, ApplicationInfo appInfo)
    {
        return pkgManager.getText(packageName, resid, appInfo);
    }

    public Drawable getApplicationIcon(String packageName) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getApplicationIcon(packageName);
    }

    public Drawable getApplicationBanner(ApplicationInfo info)
    {
        return null;//pkgManager.getApplicationBanner(info);
    }

    public int checkPermission(String permName, String pkgName)
    {
        return pkgManager.checkPermission(permName, pkgName);
    }

    public Drawable getActivityBanner(Intent intent) throws PackageManager.NameNotFoundException
    {
        return null;//pkgManager.getActivityBanner(intent);
    }

    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags)
    {
        return pkgManager.queryBroadcastReceivers(intent, flags);
    }

    public boolean addPermission(PermissionInfo info)
    {
        return pkgManager.addPermission(info);
    }

    public Drawable getDefaultActivityIcon()
    {
        return pkgManager.getDefaultActivityIcon();
    }

    public int getApplicationEnabledSetting(String packageName)
    {
        return pkgManager.getApplicationEnabledSetting(packageName);
    }

    public int checkSignatures(String pkg1, String pkg2)
    {
        return pkgManager.checkSignatures(pkg1, pkg2);
    }

    public ActivityInfo getReceiverInfo(ComponentName component, int flags) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getReceiverInfo(component, flags);
    }

    public ProviderInfo resolveContentProvider(String name, int flags)
    {
        return pkgManager.resolveContentProvider(name, flags);
    }

    public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags)
    {
        return pkgManager.getPackageArchiveInfo(archiveFilePath, flags);
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getApplicationInfo(packageName, flags);
    }

    public int getPreferredActivities(List<IntentFilter> outFilters, List<ComponentName> outActivities, String packageName)
    {
        return pkgManager.getPreferredActivities(outFilters, outActivities, packageName);
    }

    public InstrumentationInfo getInstrumentationInfo(ComponentName className, int flags) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getInstrumentationInfo(className, flags);
    }

    public String[] canonicalToCurrentPackageNames(String[] names)
    {
        return pkgManager.canonicalToCurrentPackageNames(names);
    }

    public List<PackageInfo> getInstalledPackages(int flags)
    {
        return pkgManager.getInstalledPackages(flags);
    }

    public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags)
    {
        pkgManager.setComponentEnabledSetting(componentName, newState, flags);
    }

    public PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getPermissionGroupInfo(name, flags);
    }

    public List<PermissionInfo> queryPermissionsByGroup(String group, int flags) throws PackageManager.NameNotFoundException
    {
        return pkgManager.queryPermissionsByGroup(group, flags);
    }

    public int checkSignatures(int uid1, int uid2)
    {
        return pkgManager.checkSignatures(uid1, uid2);
    }

    public PackageInstaller getPackageInstaller()
    {
        return null;//.getPackageInstaller();
    }

    public void removePermission(String name)
    {
        pkgManager.removePermission(name);
    }

    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags)
    {
        return pkgManager.queryIntentActivities(intent, flags);
    }

    public void setInstallerPackageName(String targetPackage, String installerPackageName)
    {
        pkgManager.setInstallerPackageName(targetPackage, installerPackageName);
    }

    public List<ResolveInfo> queryIntentServices(Intent intent, int flags)
    {
        return pkgManager.queryIntentServices(intent, flags);
    }

    public List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags)
    {
        return pkgManager.queryInstrumentation(targetPackage, flags);
    }

    public Drawable getActivityIcon(Intent intent) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getActivityIcon(intent);
    }

    public List<PermissionGroupInfo> getAllPermissionGroups(int flags)
    {
        return pkgManager.getAllPermissionGroups(flags);
    }

    public ResolveInfo resolveActivity(Intent intent, int flags)
    {
        return pkgManager.resolveActivity(intent, flags);
    }

    public Drawable getDrawable(String packageName, int resid, ApplicationInfo appInfo)
    {
        return pkgManager.getDrawable(packageName, resid, appInfo);
    }

    public ProviderInfo getProviderInfo(ComponentName component, int flags) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getProviderInfo(component, flags);
    }

    public String[] getPackagesForUid(int uid)
    {
        return pkgManager.getPackagesForUid(uid);
    }

    public List<ApplicationInfo> getInstalledApplications(int flags)
    {
        return pkgManager.getInstalledApplications(flags);
    }

    public void setApplicationEnabledSetting(String packageName, int newState, int flags)
    {
        pkgManager.setApplicationEnabledSetting(packageName, newState, flags);
    }

    public String getNameForUid(int uid)
    {
        return pkgManager.getNameForUid(uid);
    }

    public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, Intent intent, int flags)
    {
        return pkgManager.queryIntentActivityOptions(caller, specifics, intent, flags);
    }

    public CharSequence getApplicationLabel(ApplicationInfo info)
    {
        return pkgManager.getApplicationLabel(info);
    }

    public String getInstallerPackageName(String packageName)
    {
        return pkgManager.getInstallerPackageName(packageName);
    }

    public FeatureInfo[] getSystemAvailableFeatures()
    {
        return pkgManager.getSystemAvailableFeatures();
    }

    @Deprecated
    public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity)
    {
        pkgManager.addPreferredActivity(filter, match, set, activity);
    }

    public Resources getResourcesForApplication(String appPackageName) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getResourcesForApplication(appPackageName);
    }

    public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags)
    {
        return null;//pkgManager.queryIntentContentProviders(intent, flags);
    }

    public CharSequence getUserBadgedLabel(CharSequence label, UserHandle user)
    {
        return null;//pkgManager.getUserBadgedLabel(label, user);
    }

    public String[] getSystemSharedLibraryNames()
    {
        return pkgManager.getSystemSharedLibraryNames();
    }

    public boolean hasSystemFeature(String name)
    {
        return pkgManager.hasSystemFeature(name);
    }

    public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay)
    {
        pkgManager.extendVerificationTimeout(id, verificationCodeAtTimeout, millisecondsToDelay);
    }

    public Drawable getUserBadgedIcon(Drawable icon, UserHandle user)
    {
        return null;//pkgManager.getUserBadgedIcon(icon, user);
    }

    public int[] getPackageGids(String packageName) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getPackageGids(packageName);
    }

    public PermissionInfo getPermissionInfo(String name, int flags) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getPermissionInfo(name, flags);
    }

    public Intent getLeanbackLaunchIntentForPackage(String packageName)
    {
        return null;//pkgManager.getLeanbackLaunchIntentForPackage(packageName);
    }

    public Resources getResourcesForActivity(ComponentName activityName) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getResourcesForActivity(activityName);
    }

    public boolean isSafeMode()
    {
        return pkgManager.isSafeMode();
    }

    public Resources getResourcesForApplication(ApplicationInfo app) throws PackageManager.NameNotFoundException
    {
        return pkgManager.getResourcesForApplication(app);
    }

    public void verifyPendingInstall(int id, int verificationCode)
    {
        pkgManager.verifyPendingInstall(id, verificationCode);
    }

}
