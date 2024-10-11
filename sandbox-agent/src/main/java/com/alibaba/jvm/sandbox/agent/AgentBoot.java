package com.alibaba.jvm.sandbox.agent;

import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.*;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

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

    private static final String MENI_FEST_PATH = "META-INF/MANIFEST.MF";
    // manifest文件中属性和properties中的属性映射
    private static final HashMap<String,String> MF_KEY_MAP;
    static {
        MF_KEY_MAP = new HashMap<>(6);
        MF_KEY_MAP.put("mf.group-id", "Group-Id");
        MF_KEY_MAP.put("mf.artifact-Id", "Artifact-Id");
        MF_KEY_MAP.put("mf.build-time", "Build-Time");
        MF_KEY_MAP.put("mf.git-branch", "Git-Branch");
        MF_KEY_MAP.put("mf.git-commit-id", "Git-Commit-Id");
        MF_KEY_MAP.put("mf.git-commit-message", "Git-Commit-Message");
        MF_KEY_MAP.put("mf.git-commit-time", "Git-Commit-Time");
        MF_KEY_MAP.put("mf.git-remote-url", "Git-Remote-Url");

    }
    private static final String KEY_HK_SERVER_IP = "hk.server.ip";
    private static final String KEY_SERVER_IP = "server.ip";
    private static final String KEY_SERVER_PORT = "server.port";
    private static final String HK_SERVER_IP_ENV_NAME = "HK_SERVER_IP";

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

        // 获取核心配置，核心配置优先级最高
        final String coreFeatureString = getCoreFeatureString(agentJar, featureString);

        // 获取配置
        Properties configProperties = getAgentConfigProperties(agentJar);
        final Class<?> classOfConfigure = classLoader.loadClass(CLASS_OF_CORE_CONFIGURE);
        final Object objectOfCoreConfigure = classOfConfigure.getMethod("toConfigure", String.class, Properties.class)
                .invoke(null, coreFeatureString, configProperties);

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
        System.out.println("agent服务器绑定地址:"+socketAddress);
    }

    /**
     * 获取核心配置字符串
     * 这里是一些涉及到Agent启动的核心配置
     */
    private static String getCoreFeatureString(File agentJar, String featureString) throws IOException {
        // 启动命令参数格式化为map
        Map<String, String> featureMap = StringUtils.toFeatureMap(featureString);
        // 获取精准化服务器ip
        String hkServerIp = getHkServerIp(featureMap);
        // 获取agent的服务端信息
        String selfServerIp = getSelfIp(featureMap);
        // sandbox目录获取
        String systemModulePath = JarUtils.getDirPath(agentJar, MODULE_JAR_PATH);
        String logConfigFilePath = getLogConfigFilePath(agentJar);
        String sandboxHome = JarUtils.getTempFilePath();
        // todo 暂时取消SPI
        String providerPath = "null";
        // todo 暂时取消用户模块的加载
        String userModulePath = "null";
        final StringBuilder featureSB = new StringBuilder(format(
                "system_module=%s;sandbox_home=%s;user_module=%s;provider=%s;namespace=%s;logback_config_path=%s;mode=agent;",
                systemModulePath,
                // SANDBOX_MODULE_PATH,
                sandboxHome,
                // SANDBOX_HOME,
                userModulePath,
                providerPath,
                // SANDBOX_PROVIDER_LIB_PATH,
                NAME_SPACE,
                logConfigFilePath
        ));
        // 将服务端ip信息添加进去
        appendNonnullFromFeatureMap(featureSB, KEY_HK_SERVER_IP, hkServerIp);

        // 将自身的ip信息添加进去
        appendNonnullFromFeatureMap(featureSB, KEY_SERVER_IP, selfServerIp);

        // 将自身的端口信息添加进去
        appendNonnullFromFeatureMap(featureSB, KEY_SERVER_PORT, featureMap.get(KEY_SERVER_PORT));

        return featureSB.toString();
    }

    private static String getSelfIp(Map<String, String> featureMap) {

        String featureServerIp = featureMap.get(KEY_SERVER_IP);
        if(StringUtils.isNotBlankString(featureServerIp)){
            return featureServerIp;
        }

        // 获取本机ip
        return "0.0.0.0";
    }

    private static void appendNonnullFromFeatureMap(final StringBuilder featureSB,
                                                    final String key,
                                                    final String value) {
        if (StringUtils.isNotBlankString(value)) {
            featureSB.append(format("%s=%s;", key,value));
        }
    }

    private static String getHkServerIp(Map<String, String> featureMap){
        String hkServerIp = null;
        // 从环境变量中读取
        String serverIpFromEnv = System.getenv(HK_SERVER_IP_ENV_NAME);
        if(StringUtils.isNotBlankString(serverIpFromEnv)){
            hkServerIp = serverIpFromEnv;
        }
        // 从启动命令中读取
        String serverIpFromFeature = featureMap.get(KEY_HK_SERVER_IP);
        if(StringUtils.isNotBlankString(serverIpFromFeature)){
            hkServerIp = serverIpFromFeature;
        }
        return hkServerIp;
    }

    private static String getLogConfigFilePath(File agentJar) throws IOException {
        return JarUtils.findFile(agentJar, LOG_CONFIG_NAME).getAbsolutePath();
    }

    private static Properties getAgentConfigProperties(File agentJar) throws IOException {
        File cfgFile = JarUtils.findFile(agentJar, CORE_CONFIG_NAME);
        if(cfgFile == null){
            return null;
        }
        final Properties properties = new Properties();
        try(FileReader cfgFileReader = new FileReader(cfgFile)){
            properties.load(cfgFileReader);
        }
        // 从manifest中加载jar包的信息
        getJarInfoFroManifest(properties);
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

    private static synchronized void getJarInfoFroManifest(Properties properties) throws IOException {
        Enumeration<URL> systemResources = ClassLoader.getSystemResources(MENI_FEST_PATH);
        if (systemResources.hasMoreElements()) {
            URL url = systemResources.nextElement();
            Manifest manifest = new Manifest(url.openStream());
            Attributes mainAttributes = manifest.getMainAttributes();
            MF_KEY_MAP.forEach((left,right)-> {
                String mfValue = mainAttributes.getValue(right);
                if (Objects.isNull(mfValue)){
                    return;
                }
                properties.setProperty(left,mfValue);
            });
        }
    }


}
