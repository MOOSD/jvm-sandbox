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

    /**
     * 配置的key名称
     */
    private static final String KEY_MODULE_COVERAGE_PATTERN = "module.coverage.pattern";
    private static final String KEY_MODULE_TRACE_PATTERN = "module.trace.pattern";
    private static final String KEY_HK_SERVER_IP = "hk.server.ip";
    private static final String KEY_SERVER_IP = "server.ip";
    private static final String KEY_SERVER_PORT = "server.port";
    private static final String KEY_AGENT_NAME = "agent.name";
    private static final String KEY_HOST_NAME = "host.name";
    private static final String KEY_AGENT_ENV_NAME = "agent.env.name";

    /**
     * 支持的环境变量名称
     */
    private static final String HK_MODULE_COVERAGE_PATTERN = "MODULE_COV_PATTERN";
    private static final String HK_MODULE_TRACE_PATTERN = "MODULE_TRACE_PATTERN";
    private static final String HK_SERVER_IP_ENV_NAME = "HK_SERVER_IP";
    private static final String[] HK_AGENT_NAME_ENV_NAMES = {"HK_AGENT_NAME","SW_AGENT_NAME"};
    private static final String HK_ENV_NAME_ENV_NAME = "HK_ENV_NAME";

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

        // 读取配置文件/manifest文件中中的配置
        Properties configProperties = getAgentConfigProperties(agentJar);

        // 获取核心配置，核心配置优先级最高
        final String coreFeatureString = getCoreFeatureString(agentJar, featureString, configProperties);

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

    }

    /**
     * 获取核心配置字符串
     * 这里是一些涉及到Agent启动的核心配置
     */
    private static String getCoreFeatureString(File agentJar, String featureString, Properties configProperties) throws IOException {
        // 启动命令参数格式化为map
        Map<String, String> featureMap = StringUtils.toFeatureMap(featureString);
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
        appendNonnullFromFeatureMap(featureSB, KEY_HK_SERVER_IP, getHkServerIp(featureMap));

        // 将自身的ip信息添加进去
        appendNonnullFromFeatureMap(featureSB, KEY_SERVER_IP,  getSelfIp(featureMap));

        // 将自身的端口信息添加进去
        appendNonnullFromFeatureMap(featureSB, KEY_SERVER_PORT, featureMap.get(KEY_SERVER_PORT));

        // 添加环境信息
        appendNonnullFromFeatureMap(featureSB, KEY_AGENT_ENV_NAME, getEnvName(featureMap));

        // 添加agentName信息
        appendNonnullFromFeatureMap(featureSB, KEY_AGENT_NAME, getAgentName(configProperties.getProperty("mf.artifact-Id")));

        // 添加主机名信息
        appendNonnullFromFeatureMap(featureSB, KEY_HOST_NAME, getHostName());

        // 添加覆盖率模块的配置信息
        appendNonnullFromFeatureMap(featureSB, KEY_MODULE_COVERAGE_PATTERN, getCoveragePattern(featureMap));

        // 添加调用链路模块的配置信息
        appendNonnullFromFeatureMap(featureSB, KEY_MODULE_TRACE_PATTERN, getTracePattern(featureMap));
        return featureSB.toString();
    }

    private static String getEnvName(Map<String, String> featureMap) {
        return getConfigFromFeatureMapAndEnv(KEY_AGENT_ENV_NAME, HK_ENV_NAME_ENV_NAME, featureMap);
    }


    /**
     * agentName是服务的业务唯一标识
     */
    private static String getAgentName(String artifactId) {
        String agentName;
        // 从启动命令中获取
        for (String hkAgentNameEnvName : HK_AGENT_NAME_ENV_NAMES) {
            String agentNameFromENV = System.getenv(hkAgentNameEnvName);
            if(StringUtils.isNotBlankString(agentNameFromENV)){
                agentName = agentNameFromENV;
                return agentName;
            }
        }

        // 用项目的artifactId
        if(StringUtils.isNotBlankString(artifactId)){
            agentName = artifactId;
            return agentName;
        }
        // 用主机名
        String hostName = getHostName();
        if(StringUtils.isNotBlankString(hostName)){
            agentName = hostName;
            return agentName;
        }
        return null;
    }

    /**
     * 获取主机名
     */
    private static String getHostName() {
        String hostName = null;
        // 从主机名称中获取
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // do nothing
        }
        return hostName;
    }

    private static String getConfigFromFeatureMapAndEnv(String configKey, String envName,  Map<String, String> featureMap){
        // 优先命令行中获取
        String featureValue = featureMap.get(configKey);
        if (StringUtils.isNotBlankString(featureValue)){
            return featureValue;
        }

        // 从环境变量中获取
        String envValue = System.getenv(envName);
        if (StringUtils.isNotBlankString(envValue)){
            return envValue;
        }

        return null;
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


    private static String getCoveragePattern(Map<String, String> featureMap){
        return getConfigFromFeatureMapAndEnv(KEY_MODULE_COVERAGE_PATTERN, HK_MODULE_COVERAGE_PATTERN, featureMap);
    }

    private static String getTracePattern(Map<String, String> featureMap) {
        return getConfigFromFeatureMapAndEnv(KEY_MODULE_TRACE_PATTERN, HK_MODULE_TRACE_PATTERN, featureMap);
    }

    private static String getHkServerIp(Map<String, String> featureMap){
        return getConfigFromFeatureMapAndEnv(KEY_HK_SERVER_IP,HK_SERVER_IP_ENV_NAME, featureMap);
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
        if (!systemResources.hasMoreElements()) {
            System.out.println("未找到项目的清单文件（MANIFEST.MF）");
        }
        // 默认取第一个清单文件
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
