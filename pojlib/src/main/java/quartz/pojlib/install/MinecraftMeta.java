package quartz.pojlib.install;

import com.google.gson.annotations.SerializedName;
import quartz.pojlib.util.APIHandler;
import quartz.pojlib.util.Constants;

public class MinecraftMeta {

    private static final APIHandler handler = new APIHandler(Constants.MOJANG_META_URL);

    public static class MinecraftVersions {
        @SerializedName("versions")
        public MinecraftVersion[] verisons;
    }

    public static class MinecraftVersion {
        @SerializedName("id")
        public String id;
        @SerializedName("sha1")
        public String sha1;
    }

    public static MinecraftVersion[] getVersions() {
        return handler.get("mc/game/version_manifest_v2.json", MinecraftVersions.class).verisons;
    }

    public static VersionInfo getVersionInfo(MinecraftVersion minecraftVersion) {
        return handler.get(String.format("v1/packages/%s/%s.json", minecraftVersion.sha1, minecraftVersion.id), VersionInfo.class);
    }
}