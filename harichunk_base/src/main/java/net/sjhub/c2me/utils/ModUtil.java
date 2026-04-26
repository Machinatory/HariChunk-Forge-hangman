package net.sjhub.c2me.utils;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;

import java.util.List;

public class ModUtil {

    public static boolean isModLoaded(String modName) {
        return FMLLoader.getLoadingModList().getModFileById(modName) != null;
    }

    public static ArtifactVersion getModVersion(String modName) {
        return FMLLoader.getLoadingModList().getModFileById(modName).getMods().get(0).getVersion();
    }

    public static List<ModInfo> getModInfoList() {
        return FMLLoader.getLoadingModList().getMods();
    }
}
