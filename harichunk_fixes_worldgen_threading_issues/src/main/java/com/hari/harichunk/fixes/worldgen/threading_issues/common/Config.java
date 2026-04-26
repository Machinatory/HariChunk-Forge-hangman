package com.hari.harichunk.fixes.worldgen.threading_issues.common;

import com.hari.harichunk.base.common.config.ConfigSystem;

public class Config {

    public static final boolean enforceSafeWorldRandomAccess = new ConfigSystem.ConfigAccessor()
            .key("fixes.enforceSafeWorldRandomAccess")
            .comment("""
                        Enforces safe world random access. \s
                        This feature detects unsafe off-thread world random access, helping to find the causes \s
                        of mysterious "Accessing LegacyRandomSource from multiple threads" crash. \s
                        The default behavior is to fail hard when such bad things happens. \s
                        Disabling this option will replace this behavior with a warning. \s
                        
                        It is generally not recommended to disable this settings unless you know what you are doing \s
                        
                        """)
            .getBoolean(true,true);

    public static void init() {
    }

}
