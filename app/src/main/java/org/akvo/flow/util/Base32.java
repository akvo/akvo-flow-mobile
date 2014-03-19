package org.akvo.flow.util;

import java.util.UUID;

public class Base32 {
    
    /* Creates a base32 version of a UUID. in the output, it replaces the following letters:
     * l, o, i are replace by w, x, y, to avoid confusion with 1 and 0
     * we don't use the z as it can easily be confused with 2, especially in handwriting.
     * If we can't form the base32 version, we return an empty string.
     */
    public static String base32Uuid(){
        final String uuid = UUID.randomUUID().toString();
        String strippedUUID = (uuid.substring(0,13) + uuid.substring(24,27)).replace("-", "");
        String result = null;
        try {
            Long id = Long.parseLong(strippedUUID,16);
            result = Long.toString(id,32).replace("l","w").replace("o","x").replace("i","y");
        } catch (NumberFormatException e){
            // if we can't create the base32 UUID string, return the original uuid.
            result = uuid;
        }
        
        return result;
    }

}
