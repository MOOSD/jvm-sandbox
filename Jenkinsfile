pipeline {
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
        stage('Build') {
            steps {
                sh ' echo "${pwd} 执行打包:"'
                sh ' mvn -B -U -DskipTests clean package '
            }
        }
        stage('build') {
            steps {
                dir("./build") {
                    sh 'echo "切换工作目录为:$(pwd)"'
                    sh 'docker build -t hawkeye-agent:latest . '
                    sh 'docker tag hawkeye-agent:latest newgrand-up-registry.cn-hangzhou.cr.aliyuncs.com/newgrand_hawkeye/hawkeye-agent:latest'
                }
            }
        }
        stage('push'){
            steps {
                sh 'echo "推送镜像"'
                sh 'docker push newgrand-up-registry.cn-hangzhou.cr.aliyuncs.com/newgrand_hawkeye/hawkeye-agent:latest'
            }
        }
    }
}