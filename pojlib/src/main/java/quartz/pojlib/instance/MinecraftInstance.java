package quartz.pojlib.instance;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import quartz.pojlib.account.MinecraftAccount;
import quartz.pojlib.install.*;
import quartz.pojlib.util.GsonUtils;
import quartz.pojlib.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MinecraftInstance {

    public String instanceName;
    public String versionName;
    public String versionType;
    public String classpath;
    public String gameDir;
    public String assetIndex;
    public String assetsDir;
    public String mainClass;

    //WIP!!!!!!
    //creates a new instance of a minecraft version, install game + mod loader, stores non login related launch info to json
    public static MinecraftInstance create(String instanceName, String gameDir, MinecraftMeta.MinecraftVersion minecraftVersion, int modLoader) throws IOException {
        Logger.log(Logger.INFO, "Creating new instance: " + instanceName);

        MinecraftInstance instance = new MinecraftInstance();
        instance.instanceName = instanceName;
        instance.versionName = minecraftVersion.id;
        instance.gameDir = new File(gameDir + "/instances/" + instanceName).getAbsolutePath();
        new File(instance.gameDir).mkdirs();

        VersionInfo minecraftVersionInfo = MinecraftMeta.getVersionInfo(minecraftVersion);
        instance.versionType = minecraftVersionInfo.type;
        VersionInfo modLoaderVersionInfo = null;

        // Get mod loader info
        if (modLoader == 0) {
            instance.mainClass = minecraftVersionInfo.mainClass;
        }

        else if (modLoader == 1) {
            FabricMeta.FabricVersion fabricVersion = FabricMeta.getLatestStableVersion();
            if (fabricVersion != null) {
                modLoaderVersionInfo = FabricMeta.getVersionInfo(fabricVersion, minecraftVersion);
                instance.mainClass = modLoaderVersionInfo.mainClass;
            }
        }

        else if (modLoader == 2) {
            QuiltMeta.QuiltVersion quiltVersion = QuiltMeta.getLatestVersion();
            if (quiltVersion != null) {
                modLoaderVersionInfo = QuiltMeta.getVersionInfo(quiltVersion, minecraftVersion);
                instance.mainClass = modLoaderVersionInfo.mainClass;
            }
        }

        else if (modLoader == 3) {
            throw new RuntimeException("Forge not yet implemented\nExiting...");
        }

        if (modLoaderVersionInfo == null) throw new RuntimeException("Error fetching mod loader data");

        // Install minecraft
        String clientClasspath = Installer.installClient(minecraftVersionInfo, gameDir);
        String minecraftClasspath = Installer.installLibraries(minecraftVersionInfo, gameDir);
        String modLoaderClasspath = Installer.installLibraries(modLoaderVersionInfo, gameDir);
        instance.classpath = clientClasspath + File.pathSeparator + minecraftClasspath + File.pathSeparator + modLoaderClasspath;

        instance.assetsDir = Installer.installAssets(minecraftVersionInfo, gameDir);
        instance.assetIndex = minecraftVersionInfo.assetIndex.id;

        // Write instance to json file
        List<MinecraftInstance> instances = new ArrayList<>();
        File instancesFile = new File(gameDir + "/instances/instances.json");

        if (instancesFile.exists()) {
            MinecraftInstance[] instancesFromFile = GsonUtils.jsonFileToObject(instancesFile.getAbsolutePath(), MinecraftInstance[].class);
            if (instancesFromFile == null) throw new RuntimeException("Error saving instance: " + instanceName);
            instances.addAll(Arrays.asList(instancesFromFile));
        }

        instances.add(instance);
        GsonUtils.objectToJsonFile(instancesFile.getAbsolutePath(), instances);
        return instance;
    }

    public static MinecraftInstance[] getInstances(String gameDir) {
        return GsonUtils.jsonFileToObject(new File(gameDir + "/instances/instances.json").getAbsolutePath(), MinecraftInstance[].class);
    }

    // Return true if instance was deleted
    public static boolean delete(String instanceName, String gameDir) {
        List<MinecraftInstance> instancesToSave = new ArrayList<>();

        for (MinecraftInstance instance : getInstances(gameDir)) {
            if (!instance.instanceName.equals(instanceName)) instancesToSave.add(instance);
        }

        GsonUtils.objectToJsonFile(new File(gameDir + "/instances/instances.json").getAbsolutePath(), instancesToSave);
        return new File(gameDir + "/instances/" + instanceName).delete();
    }

    public List<String> generateLaunchArgs(MinecraftAccount account) {
        String[] mcArgs = {"--username", account.username, "--version", versionName, "--gameDir", gameDir,
                "--assetsDir", assetsDir, "--assetIndex", assetIndex, "--uuid", account.uuid,
                "--accessToken", account.accessToken, "--userType", account.userType, "--versionType", versionType};

        List<String> allArgs = new ArrayList<>(Arrays.asList("-cp", classpath));
        allArgs.add(mainClass);
        allArgs.addAll(Arrays.asList(mcArgs));
        return allArgs;
    }
}