pipeline {
    environment {
        DOCKER_IMAGE = 'newgrand-up-registry.cn-hangzhou.cr.aliyuncs.com/newgrand_hawkeye/hawkeye-agent:latest'
        DOCKER_REGISTRY_URL = 'newgrand-up-registry.cn-hangzhou.cr.aliyuncs.com'
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
        stage('maven_build') {
            steps {
                sh ' echo "${pwd} 执行打包:"'
                sh ' mvn -B -U -DskipTests clean package '
            }
        }
        stage('docker_build') {
            steps {
                dir("./build") {
                    sh 'docker version'
                    sh 'echo "切换工作目录为:$(pwd)" '
                    sh 'DOCKER_BUILDKIT=0 docker build -t $DOCKER_IMAGE . '
                }
            }
        }
        stage('push'){
            steps {
                script {
                    // 使用 Jenkins 的 withCredentials 来读取 Docker 注册表的凭据
                    withCredentials([usernamePassword(credentialsId: DOCKER_REGISTRY_CREDENTIALS_ID,
                                                     usernameVariable: 'REGISTRY_USER',
                                                     passwordVariable: 'REGISTRY_PASS')]) {
                        sh "echo $REGISTRY_PASS | docker login -u $REGISTRY_USER --password-stdin $DOCKER_REGISTRY_URL"
                    }
                }
                sh 'echo "推送镜像"'
                sh 'docker push $DOCKER_IMAGE'
                sh 'docker logout $DOCKER_REGISTRY_URL'
            }
        }
    }
}