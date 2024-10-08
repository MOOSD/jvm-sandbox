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
                sh 'echo "$DOCKERHUB_CREDENTIALS_PSW"'
                sh 'echo $DOCKERHUB_CREDENTIALS_PSW | sudo docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin'
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