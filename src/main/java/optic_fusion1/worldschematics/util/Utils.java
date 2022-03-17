package optic_fusion1.worldschematics.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class Utils {

    private Utils() {
    }

    // used to copy files from the .jar file to the config folders
    public static void copy(InputStream in, File file) {
        try {
            try (in;  OutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
