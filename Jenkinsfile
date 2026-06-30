/*
 * 嘉园外卖持续集成流水线配置文件
 * 
 * 运行环境要求：
 * - Jenkins 2.440+
 * - Maven 3.9+
 * - JDK 17
 * - Python 3.11+
 * - Allure 2.25+
 * 
 * 流水线阶段：
 * 1. 拉取代码
 * 2. 代码静态分析
 * 3. 单元测试
 * 4. 接口自动化测试
 * 5. 性能测试
 * 6. 生成测试报告
 * 7. 报告通知
 */

pipeline {
    agent any
    
    // 环境变量配置
    environment {
        // 代码仓库地址
        GIT_URL = 'https://github.com/xxx/jia-yuan-wai-mai.git'
        
        // 测试服务器配置
        TEST_SERVER = '192.168.1.100'
        TEST_PORT = '8081'
        
        // 报告目录
        ALLURE_RESULTS = 'allure-results'
        ALLURE_REPORT = 'allure-report'
        
        // 构建工具路径
        MAVEN_HOME = tool 'Maven3.9'
        JAVA_HOME = tool 'JDK17'
    }
    
    // 流水线参数
    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: '代码分支')
        string(name: 'TEST_ENV', defaultValue: 'test', description: '测试环境')
        booleanParam(name: 'RUN_PERF_TEST', defaultValue: true, description: '是否运行性能测试')
    }
    
    // 工具配置
    tools {
        maven 'Maven3.9'
        jdk 'JDK17'
    }
    
    stages {
        /*
         * 阶段1：拉取代码
         * 从Git仓库拉取指定分支的代码
         */
        stage('拉取代码') {
            steps {
                echo "====== 拉取代码 - 分支: ${params.BRANCH} ======"
                
                git branch: "${params.BRANCH}", 
                    url: "${GIT_URL}",
                    credentialsId: 'git-credentials'
                
                echo "代码拉取成功"
            }
        }
        
        /*
         * 阶段2：代码静态分析
         * 使用SonarQube进行代码质量检查
         */
        stage('代码静态分析') {
            steps {
                echo "====== 代码静态分析 ======"
                
                withSonarQubeEnv('SonarQube') {
                    sh """
                        ${MAVEN_HOME}/bin/mvn sonar:sonar \
                            -Dsonar.projectKey=jia-yuan-wai-mai \
                            -Dsonar.projectName=嘉园外卖 \
                            -Dsonar.projectVersion=1.0 \
                            -Dsonar.sources=server/src/main/java \
                            -Dsonar.java.binaries=server/target/classes
                    """
                }
                
                echo "静态分析完成"
            }
        }
        
        /*
         * 阶段3：单元测试
         * 执行JUnit5单元测试，生成Jacoco覆盖率报告
         */
        stage('单元测试') {
            steps {
                echo "====== 单元测试 ======"
                
                dir('hanye-take-out-springboot3/server') {
                    sh """
                        ${MAVEN_HOME}/bin/mvn test \
                            -Dtest=*Test \
                            -DfailIfNoTests=false \
                            -q
                    """
                }
                
                echo "单元测试完成"
            }
            
            post {
                always {
                    // 发布单元测试报告
                    junit 'hanye-take-out-springboot3/server/target/surefire-reports/*.xml'
                    
                    // 发布Jacoco覆盖率报告
                    jacoco(
                        execPattern: 'hanye-take-out-springboot3/server/target/jacoco.exec',
                        classPattern: 'hanye-take-out-springboot3/server/target/classes',
                        sourcePattern: 'hanye-take-out-springboot3/server/src/main/java'
                    )
                }
            }
        }
        
        /*
         * 阶段4：接口自动化测试
         * 执行Pytest接口自动化测试
         */
        stage('接口自动化测试') {
            steps {
                echo "====== 接口自动化测试 ======"
                
                // 安装Python依赖
                sh "pip install pytest requests allure-pytest pyyaml -q"
                
                // 运行接口测试
                sh """
                    cd d:/small-third/project1/hanye-take-out-main
                    python -m pytest test_api.py \
                        --alluredir=${ALLURE_RESULTS} \
                        -v \
                        --tb=short
                """
                
                echo "接口自动化测试完成"
            }
        }
        
        /*
         * 阶段5：性能测试
         * 根据参数决定是否执行JMeter性能测试
         */
        stage('性能测试') {
            when {
                expression { params.RUN_PERF_TEST }
            }
            
            steps {
                echo "====== 性能测试 ======"
                
                // 启动测试服务器（如果未启动）
                sh "start cmd /c 'java -jar hanye-take-out-springboot3/server/target/hanye-take-out.jar'"
                
                // 等待服务器启动
                sleep(time: 30, unit: 'SECONDS')
                
                // 运行JMeter性能测试
                sh """
                    cd d:/apache-jmeter-5.6.3/bin
                    jmeter -n \
                        -t 'd:/small-third/project1/hanye-take-out-main/hanye-take-out-springboot3/server/src/test/resources/嘉园外卖性能测试.jmx' \
                        -l result.jtl \
                        -e -o perf-report
                """
                
                echo "性能测试完成"
            }
            
            post {
                always {
                    // 发布性能测试报告
                    publishHTML(target: [
                        allowMissing: false,
                        alwaysLinkToLastBuild: false,
                        keepAll: true,
                        reportDir: 'perf-report',
                        reportFiles: 'index.html',
                        reportName: '性能测试报告'
                    ])
                }
            }
        }
        
        /*
         * 阶段6：生成测试报告
         * 生成Allure综合测试报告
         */
        stage('生成测试报告') {
            steps {
                echo "====== 生成测试报告 ======"
                
                // 生成Allure报告
                sh "allure generate ${ALLURE_RESULTS} -o ${ALLURE_REPORT} --clean"
                
                echo "测试报告生成完成"
            }
            
            post {
                always {
                    // 发布Allure报告
                    publishHTML(target: [
                        allowMissing: false,
                        alwaysLinkToLastBuild: false,
                        keepAll: true,
                        reportDir: "${ALLURE_REPORT}",
                        reportFiles: 'index.html',
                        reportName: 'Allure测试报告'
                    ])
                }
            }
        }
        
        /*
         * 阶段7：报告通知
         * 发送测试报告通知
         */
        stage('报告通知') {
            steps {
                echo "====== 报告通知 ======"
                
                // 获取测试结果统计
                def testResult = currentBuild.currentResult
                
                // 发送邮件通知
                emailext(
                    subject: "[嘉园外卖] 测试流水线执行结果 - ${testResult}",
                    body: """
                        <h2>嘉园外卖测试流水线执行完成</h2>
                        <p>构建编号: ${BUILD_NUMBER}</p>
                        <p>执行结果: ${testResult}</p>
                        <p>分支: ${params.BRANCH}</p>
                        <p>测试环境: ${params.TEST_ENV}</p>
                        <p>Allure报告: <a href="${BUILD_URL}allure">查看报告</a></p>
                        <p>构建日志: <a href="${BUILD_URL}console">查看日志</a></p>
                    """,
                    to: 'test@xxx.com, dev@xxx.com',
                    from: 'jenkins@xxx.com',
                    mimeType: 'text/html'
                )
                
                echo "报告通知发送完成"
            }
        }
    }
    
    /*
     * 后置处理
     */
    post {
        success {
            echo "====== 流水线执行成功 ======"
            slackSend channel: '#test', message: "✅ 嘉园外卖测试流水线执行成功 - 构建#${BUILD_NUMBER}"
        }
        
        failure {
            echo "====== 流水线执行失败 ======"
            slackSend channel: '#test', message: "❌ 嘉园外卖测试流水线执行失败 - 构建#${BUILD_NUMBER}"
        }
        
        always {
            echo "====== 流水线执行完毕 ======"
            
            // 清理工作空间
            deleteDir()
        }
    }
}
