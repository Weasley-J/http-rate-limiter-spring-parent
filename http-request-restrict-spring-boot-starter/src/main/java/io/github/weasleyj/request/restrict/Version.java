package io.github.weasleyj.request.restrict;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Version
 */
public final class Version {

    private Version() {
    }

    public static String getVersion() {
        return determineVersion();
    }

    private static String determineVersion() {
        final Package pkg = Version.class.getPackage();
        if (null != pkg && null != pkg.getImplementationVersion()) {
            return pkg.getImplementationVersion();
        }
        URL srcLocation = Version.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            URLConnection connection = srcLocation.openConnection();
            if (connection instanceof JarURLConnection) {
                return getVersion(((JarURLConnection) connection).getJarFile());
            }
            try (JarFile jarFile = new JarFile(new File(srcLocation.toURI()))) {
                return getVersion(jarFile);
            }
        } catch (Exception ex) {
            return "";
        }
    }

    private static String getVersion(JarFile jarFile) throws IOException {
        return jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
    }

}
