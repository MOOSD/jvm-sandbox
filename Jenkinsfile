pipeline {
    environment {
        DOCKER_IMAGE = 'newgrand-up-registry.cn-hangzhou.cr.aliyuncs.com/newgrand_hawkeye/hawkeye-agent:latest'
        DOCKER_REGISTRY_URL = 'newgrand-up-registry.cn-hangzhou.cr.aliyuncs.com'
        DOCKERHUB_CREDENTIALS = credentials('3f24bd3d-b035-4656-a4f5-99f4214127bd')
    }
    tools {
        jdk 'JDK8'  // 在 Global Tool Configuration 中定义的 JDK 名称
    }
    //使用带有bugkiller标签的资源执行流水线
    agent {
        node {
            label 'bugkiller'
        }
    }
    stages {
        //构建阶段，执行跳过测试的打包
        stage('maven build') {
            steps {
                sh ' echo "${pwd} 执行打包:"'
                sh ' mvn -B -U -DskipTests clean package '
            }
        }
        stage('jar issue') {
                    steps {
                        dir("./build") {
                            sh 'echo "当前工作目录:$(pwd)"'
                            sshPublisher(
                            publishers: [
                                sshPublisherDesc(
                                    configName: 'bugkiller-dev(172)',
                                    transfers: [
                                        sshTransfer(
                                            cleanRemote: false,
                                            excludes: '',
                                            execCommand: '''
                                                # 移动文件
                                                echo "文件移动成功"

                                                # 切换到文件所在目录
                                                cd /var/release/agent

                                                # 查找最新的 JAR 文件（按时间排序或按版本号排序）
                                                LATEST_JAR=$(ls -t *.jar | head -n 1)

                                                # 创建符号链接，指向最新的 JAR 文件
                                                ln -sf $LATEST_JAR hawkeye-agent-latest.jar

                                                # 验证符号链接
                                                ls -l hawkeye-agent-latest.jar
                                            ''',
                                            execTimeout: 120000,
                                            flatten: false,
                                            makeEmptyDirs: false,
                                            noDefaultExcludes: false,
                                            patternSeparator: '[, ]+',
                                            remoteDirectory: '/var/release/agent/',
                                            remoteDirectorySDF: false,
                                            removePrefix: 'target',
                                            sourceFiles: 'target/*.jar'  // 允许上传任意 JAR 文件
                                        )
                                    ],
                                    usePromotionTimestamp: false,
                                    useWorkspaceInPromotion: false,
                                    verbose: true
                                )
                                ]
                            )
                        }
                    }
        }

        stage('docker build') {
            steps {
                dir("./build") {
                    sh 'docker version'
                    sh 'echo "切换工作目录为:$(pwd)" '
                    sh 'DOCKER_BUILDKIT=0 docker build -t $DOCKER_IMAGE . '
                }
            }
        }

        stage('docker login'){
            steps {

                sh 'echo "docker login"'
                sh 'echo "$DOCKERHUB_CREDENTIALS_USR"'
                sh 'echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR $DOCKER_REGISTRY_URL --password-stdin'
            }
        }
        stage('image push'){
            steps {
                sh 'docker push $DOCKER_IMAGE'
                sh 'docker logout $DOCKER_REGISTRY_URL'
            }
        }
    }
}