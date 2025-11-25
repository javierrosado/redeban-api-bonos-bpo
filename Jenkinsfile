pipeline {

    agent any

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Rama Git')
        choice(name: 'ENVIRONMENT', choices: ['desa','certi','prod'], description: 'Ambiente')
        string(name: 'VERSION', defaultValue: 'latest', description: 'Version a desplegar (obligatorio para PROD)')
        string(name: 'ROLLOUT_TIMEOUT', defaultValue: '300s', description: 'Timeout')
    }

    environment {

        APP_NAME   = "redeban-api-bonos-bpo"
        NAMESPACE  = "redeban-bonos-bpo"

        // Base image corporativa local (ya cargada en Jenkins host)
        BASE_IMAGE = "redeban-base-image/java21-runtime:1.0.0"
        DEFAULT_APP_CONTEXT = "/redeban/bonos"
        OCP_DESA_ROUTE_SUFFIX = "apps.mdetestcam.rbm.com.co"

        PLACEHOLDER_NAMESPACE   = "__NAMESPACE__"
        PLACEHOLDER_APP_NAME    = "__APP_NAME__"
        PLACEHOLDER_APP_CONTEXT = "__APP_CONTEXT__"
        PLACEHOLDER_VERSION     = "__VERSION__"
        PLACEHOLDER_REGISTRY    = "__REGISTRY__"
        PLACEHOLDER_REGPULL     = "__REGISTRY_PULL__"
        PLACEHOLDER_ROUTE_HOST  = "__ROUTE_HOST__"
    }

    stages {

        /* ===========================================================
                           CHECKOUT
           =========================================================== */
        stage('Checkout') {
            steps {
                script {
                    echo "Checkout de rama: ${params.BRANCH}"

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: params.BRANCH]],
                        userRemoteConfigs: [[
                            url: env.GIT_URL,
                            credentialsId: env.GIT_CREDENTIALS
                        ]]
                    ])

                    if (!fileExists('Dockerfile')) error("No existe Dockerfile.")
                    if (!fileExists('pom.xml')) error("No existe pom.xml.")

                    echo "? Checkout validado."
                }
            }
        }

        /* ===========================================================
                           COMPILACION
           =========================================================== */
        stage('Compile') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        /* ===========================================================
                        BUILD DOCKER (solo capas nuevas)
           =========================================================== */
        stage('Build Image') {
            steps {
                script {

                    def tag         = params.VERSION?.trim() ? params.VERSION.trim() : "latest"
                    def baseImage   = env["OCP_${params.ENVIRONMENT.toUpperCase()}_BASE_IMAGE"] ?: BASE_IMAGE
                    def baseHost    = baseImage.tokenize('/')[0]
                    def needsLogin  = baseHost.contains('.') || baseHost.contains(':')
                    def tokenId     = "OCP_${params.ENVIRONMENT.toUpperCase()}_TOKEN"

                    if (needsLogin) {
                        withCredentials([string(credentialsId: tokenId, variable: 'TOKEN')]) {
                            sh """
                                echo "${TOKEN}" | docker login ${baseHost} -u openshift --password-stdin
                                docker build \\
                                --pull=false \\
                                --build-arg BASE_IMAGE=${baseImage} \\
                                -t ${APP_NAME}:${tag} \\
                                -f Dockerfile .
                            """
                        }
                    } else {
                        sh """
                            docker build \\
                            --pull=false \\
                            --build-arg BASE_IMAGE=${baseImage} \\
                            -t ${APP_NAME}:${tag} \\
                            -f Dockerfile .
                        """
                    }
                }
            }
        }

        /* ===========================================================
                             PUSH DOCKER
           =========================================================== */
        stage('Push Image') {
            steps {
                script {
                    def registry = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGISTRY"]
                    def tokenId  = "OCP_${params.ENVIRONMENT.toUpperCase()}_TOKEN"
                    def tag      = params.VERSION?.trim() ? params.VERSION.trim() : "latest"

                    withCredentials([string(credentialsId: tokenId, variable: 'TOKEN')]) {

                        sh """
                            echo "${TOKEN}" | docker login ${registry} -u openshift --password-stdin

                            docker tag ${APP_NAME}:${tag} \\
                                       ${registry}/${NAMESPACE}/${APP_NAME}:${tag}

                            docker push ${registry}/${NAMESPACE}/${APP_NAME}:${tag}
                        """
                    }
                }
            }
        }

        /* ===========================================================
                               DEPLOY OCP
           =========================================================== */
        stage("Deploy") {
            steps {
                script {

                    def api            = env["OCP_${params.ENVIRONMENT.toUpperCase()}_API"]
                    def cred           = "OCP_${params.ENVIRONMENT.toUpperCase()}_TOKEN"
                    def registry       = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGISTRY"]
                    def registryPull   = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGPULL"]
                    def routeSuffix    = env["OCP_${params.ENVIRONMENT.toUpperCase()}_ROUTE_SUFFIX"]
                    def routeHost      = env["OCP_${params.ENVIRONMENT.toUpperCase()}_ROUTE_HOST"] ?: (routeSuffix ? "${APP_NAME}.${routeSuffix}" : "${APP_NAME}.${params.ENVIRONMENT}.apps")
                    def appContext     = env["OCP_${params.ENVIRONMENT.toUpperCase()}_APP_CONTEXT"] ?: DEFAULT_APP_CONTEXT
                    def manifestVersion = params.VERSION?.trim() ? params.VERSION.trim() : "latest"
                    if (params.ENVIRONMENT == 'prod' && !params.VERSION?.trim()) {
                        error("Debe especificar VERSION para despliegues en PROD.")
                    }

                    withCredentials([string(credentialsId: cred, variable: 'TOKEN')]) {
                        sh """
                            export KUBECONFIG=\$(mktemp)
                            oc login ${api} --token="${TOKEN}" --insecure-skip-tls-verify=true
                            oc whoami
                            oc project ${NAMESPACE}

                            echo "Validando YAML (dry-run)..."
                            for f in deploy/${params.ENVIRONMENT}/*.yaml; do
                                sed -e "s|${PLACEHOLDER_NAMESPACE}|${NAMESPACE}|g" \\
                                    -e "s|${PLACEHOLDER_APP_NAME}|${APP_NAME}|g" \\
                                    -e "s|${PLACEHOLDER_VERSION}|${manifestVersion}|g" \\
                                    -e "s|${PLACEHOLDER_REGISTRY}|${registry}|g" \\
                                    -e "s|${PLACEHOLDER_REGPULL}|${registryPull}|g" \\
                                    -e "s|${PLACEHOLDER_ROUTE_HOST}|${routeHost}|g" \\
                                    -e "s|${PLACEHOLDER_APP_CONTEXT}|${appContext}|g" \\
                                    "\$f" | oc apply --dry-run=client -f -
                            done

                            echo "Aplicando manifiestos..."
                            for f in deploy/${params.ENVIRONMENT}/*.yaml; do
                                sed -e "s|${PLACEHOLDER_NAMESPACE}|${NAMESPACE}|g" \\
                                    -e "s|${PLACEHOLDER_APP_NAME}|${APP_NAME}|g" \\
                                    -e "s|${PLACEHOLDER_VERSION}|${manifestVersion}|g" \\
                                    -e "s|${PLACEHOLDER_REGISTRY}|${registry}|g" \\
                                    -e "s|${PLACEHOLDER_REGPULL}|${registryPull}|g" \\
                                    -e "s|${PLACEHOLDER_ROUTE_HOST}|${routeHost}|g" \\
                                    -e "s|${PLACEHOLDER_APP_CONTEXT}|${appContext}|g" \\
                                    "\$f" | oc apply -f -
                            done

                            oc rollout restart deployment/${APP_NAME}
                            oc rollout status deployment/${APP_NAME} --timeout=${params.ROLLOUT_TIMEOUT}
                        """
                    }
                }
            }
        }
    }

    /* ===========================================================
                            ROLLBACK
       =========================================================== */
    post {
        failure {
            script {

                def api  = env["OCP_${params.ENVIRONMENT.toUpperCase()}_API"]
                def cred = "OCP_${params.ENVIRONMENT.toUpperCase()}_TOKEN"

                withCredentials([string(credentialsId: cred, variable: 'TOKEN')]) {

                    sh """
                        export KUBECONFIG=\$(mktemp)
                        echo "Rollback iniciado..."
                        oc login ${api} --token="${TOKEN}" --insecure-skip-tls-verify=true
                    """

                    if (fileExists("deploy/${params.ENVIRONMENT}")) {
                        sh "oc delete -f deploy/${params.ENVIRONMENT}/ --ignore-not-found=true"
                    } else {
                        echo "? No existe carpeta deploy/${params.ENVIRONMENT}, rollback parcial."
                    }
                }
            }
        }

        always {
            cleanWs()
        }
    }
}
