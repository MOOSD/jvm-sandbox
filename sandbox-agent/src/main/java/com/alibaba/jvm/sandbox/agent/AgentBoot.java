package com.alibaba.jvm.sandbox.agent;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.*;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.JarFile;

import static java.lang.String.format;

public class AgentBoot {

    private static final ClassLoader BOOTSTRAP_CLASS_LOADER = null;
    private static final String CORE_JAR_PATH = "core/";
    private static final String SPY_JAR_PATH = "spy/";
    private static final String MODULE_JAR_PATH = "modules/";
    private static final String CORE_CONFIG_NAME = "sandbox.properties";
    private static final String LOG_CONFIG_NAME = "sandbox-logback.xml";
    private static final String NAME_SPACE = "default";
    private static final String CLASS_OF_CORE_CONFIGURE = "com.alibaba.jvm.sandbox.core.CoreConfigure";
    private static final String CLASS_OF_PROXY_CORE_SERVER = "com.alibaba.jvm.sandbox.core.server.ProxyCoreServer";

    public static void premain(String featureString, Instrumentation inst) throws Exception {
        final File agentJar = getArchiveFileContains();
        // 保险起见，清空临时目录
        JarUtils.clearTempFilePath();
        // 获取Spring的启动类加载器
        final JarFileArchive archive = new JarFileArchive(agentJar);
        ArrayList<URL> urls = nestArchiveUrls(archive, CORE_JAR_PATH);
        final ClassLoader classLoader = getClassLoader(urls.get(0));

        // 获取Spy类,将Spy注入到BootstrapClassLoader
        final ArrayList<URL> spyUrl = nestArchiveUrls(archive, SPY_JAR_PATH);
        JarFile nestedJarFile = JarUtils.getNestedJarFile(spyUrl.get(0));
        inst.appendToBootstrapClassLoaderSearch(nestedJarFile);

        // 获取核心配置字符串 todo 支持命令行参数添加配置
        final String coreFeatureString = getCoreFeatureString(agentJar);
        Properties configProperties = getCoreConfigProperties(agentJar);
        File logConfigFile = getLogConfigFile(agentJar);
        final Class<?> classOfConfigure = classLoader.loadClass(CLASS_OF_CORE_CONFIGURE);
        final Object objectOfCoreConfigure = classOfConfigure.getMethod("toConfigure", String.class, Properties.class, File.class)
                .invoke(null, coreFeatureString, configProperties, logConfigFile);

        // CoreServer类定义
        final Class<?> classOfProxyServer = classLoader.loadClass(CLASS_OF_PROXY_CORE_SERVER);

        // 获取CoreServer单例
        final Object objectOfProxyServer = classOfProxyServer
                .getMethod("getInstance")
                .invoke(null);

        // CoreServer.isBind()
        final boolean isBind = (Boolean) classOfProxyServer.getMethod("isBind").invoke(objectOfProxyServer);


        // 如果未绑定,则需要绑定一个地址
        if (!isBind) {
            try {
                classOfProxyServer
                        .getMethod("bind", classOfConfigure, Instrumentation.class)
                        .invoke(objectOfProxyServer, objectOfCoreConfigure, inst);
            } catch (Throwable t) {
                classOfProxyServer.getMethod("destroy").invoke(objectOfProxyServer);
                throw t;
            }

        }

        // 返回服务器绑定的地址
        InetSocketAddress socketAddress = (InetSocketAddress) classOfProxyServer
                .getMethod("getLocal")
                .invoke(objectOfProxyServer);
        System.out.println(socketAddress);
    }
    private static String getCoreFeatureString(File agentJar) throws IOException {
        String systemModulePath = JarUtils.getDirPath(agentJar, MODULE_JAR_PATH);

        final String sandboxHome = JarUtils.getTempFilePath();
        // todo 暂时取消SPI
        String providerPath = "null";
        // todo 暂时取消用户模块的加载
        String userModulePath = "null";
        final StringBuilder featureSB = new StringBuilder(
                format(
                        ";system_module=%s;mode=%s;sandbox_home=%s;user_module=%s;provider=%s;namespace=%s;",
                        systemModulePath,
                        // SANDBOX_MODULE_PATH,
                        "agent",
                        sandboxHome,
                        // SANDBOX_HOME,
                        userModulePath,
                        providerPath,
                        // SANDBOX_PROVIDER_LIB_PATH,
                        NAME_SPACE
                )
        );
        return featureSB.toString();
    }


    private static File getLogConfigFile(File agentJar) throws IOException {
        return JarUtils.findFile(agentJar, LOG_CONFIG_NAME);
    }

    private static Properties getCoreConfigProperties(File agentJar) throws IOException {
        File cfgFile = JarUtils.findFile(agentJar, CORE_CONFIG_NAME);
        if(cfgFile == null){
            return null;
        }
        final Properties properties = new Properties();
        try(FileReader cfgFileReader = new FileReader(cfgFile)){
            properties.load(cfgFileReader);
        }

        return properties;
    }

    /**
     * 获取归档文件对应的文件信息
     */
    private static File getArchiveFileContains() throws URISyntaxException {
        final ProtectionDomain protectionDomain = AgentLauncher.class.getProtectionDomain();
        final CodeSource codeSource = protectionDomain.getCodeSource();
        final URI location = (codeSource == null ? null : codeSource.getLocation().toURI());
        final String path = (location == null ? null : location.getSchemeSpecificPart());

        if (path == null) {
            throw new IllegalStateException("Unable to determine code source archive");
        }

        final File root = new File(path);
        if (!root.exists() || root.isDirectory()) {
            throw new IllegalStateException("Unable to determine code source archive from " + root);
        }
        return root;
    }

    /**
     * 获取归档文件中的对应文件的URL信息
     */
    private static ArrayList<URL> nestArchiveUrls(JarFileArchive archive, String prefix) throws IOException {
        Iterator<Archive> nestedArchives = archive.getNestedArchives(entry -> !entry.isDirectory() && entry.getName().startsWith(prefix),
                entry -> true
        );
        final ArrayList<URL> urls = new ArrayList<>();
        nestedArchives.forEachRemaining(item -> {
            try {
                urls.add(item.getUrl());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        });

        return urls;
    }

    /**
     * 获取ClassLoader
     */
    private static synchronized ClassLoader getClassLoader(URL urls){
        final CompoundableClassLoader classLoader;
        classLoader = new CompoundableClassLoader(urls, BOOTSTRAP_CLASS_LOADER);
        return classLoader;
    }


}
