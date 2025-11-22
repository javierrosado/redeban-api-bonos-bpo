pipeline {

    agent any

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'Rama Git a desplegar')
        choice(name: 'ENVIRONMENT', choices: ['desa','certi','prod'], description: 'Ambiente de despliegue')
        string(name: 'VERSION', defaultValue: '', description: 'Versión certificada (obligatorio para PROD)')
        string(name: 'ROLLOUT_TIMEOUT', defaultValue: '300s', description: 'Timeout del rollout')
    }

    environment {

        APP_NAME   = "redeban-api-bonos-bpo"
        NAMESPACE  = "redeban-bonos-bpo"

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
                          CHECKOUT DEL CÓDIGO
           =========================================================== */
        stage('Checkout') {
            steps {
                script {
                    echo "Haciendo checkout de la rama: ${params.BRANCH}"

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: params.BRANCH]],
                        userRemoteConfigs: [[
                            url: env.GIT_URL,
                            credentialsId: env.GIT_CREDENTIALS
                        ]]
                    ])

                    if (!fileExists('Dockerfile')) {
                        error("No existe Dockerfile en raíz.")
                    }

                    if (!fileExists('pom.xml') && !fileExists('build.gradle')) {
                        error("Código inválido: no existe pom.xml ni build.gradle.")
                    }

                    echo "✔ Checkout completado y validado."
                }
            }
        }

        /* ===========================================================
                           COMPILACIÓN MAVEN
           =========================================================== */
        stage('Compile') {
            when { expression { params.ENVIRONMENT != 'prod' } }
            steps {
                sh """
                    mvn clean package -DskipTests
                """
            }
        }

        /* ===========================================================
                          BUILD DOCKER IMAGE
           =========================================================== */
        stage('Build Image') {
            when { expression { params.ENVIRONMENT != 'prod' } }
            steps {
                script {
                    def registry = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGISTRY"]

                    sh """
                        docker build \
                            -t ${registry}/${APP_NAME}:${params.VERSION ?: "latest"} \
                            -f Dockerfile .
                    """
                }
            }
        }

        /* ===========================================================
                           PUSH DOCKER IMAGE
           =========================================================== */
        stage('Push Image') {
            when { expression { params.ENVIRONMENT != 'prod' } }
            steps {
                script {

                    def registry     = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGISTRY"]
                    def registryCred = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGCRED"]

                    withCredentials([
                        usernamePassword(credentialsId: registryCred,
                        usernameVariable: 'REG_USR', passwordVariable: 'REG_PWD')
                    ]) {
                        sh """
                            echo "${REG_PWD}" | docker login ${registry} -u "${REG_USR}" --password-stdin
                            docker push ${registry}/${APP_NAME}:${params.VERSION ?: "latest"}
                        """
                    }
                }
            }
        }

        /* ===========================================================
                     PROMOVER IMAGEN CERTIFICADA A PROD
           =========================================================== */
        stage('Promote to PROD') {
            when { expression { params.ENVIRONMENT == 'prod' } }
            steps {
                script {

                    if (!params.VERSION?.trim()) {
                        error("ERROR: Para PROD debe ingresar una VERSION certificada.")
                    }

                    def regCerti = env["OCP_CERTI_REGISTRY"]
                    def regProd  = env["OCP_PROD_REGISTRY"]

                    sh """
                        docker manifest inspect ${regCerti}/${APP_NAME}:${params.VERSION} || exit 1

                        docker pull ${regCerti}/${APP_NAME}:${params.VERSION}
                        docker tag  ${regCerti}/${APP_NAME}:${params.VERSION} ${regProd}/${APP_NAME}:${params.VERSION}
                        docker push ${regProd}/${APP_NAME}:${params.VERSION}
                    """
                }
            }
        }

        /* ===========================================================
                              DEPLOY OCP
           =========================================================== */
        stage("Deploy") {
            steps {
                script {

                    def api         = env["OCP_${params.ENVIRONMENT.toUpperCase()}_API"]
                    def cred        = env["OCP_${params.ENVIRONMENT.toUpperCase()}_CRED"]
                    def registry    = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGISTRY"]
                    def registryPull = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGPULL"]

                    echo "Validando namespace destino: ${NAMESPACE}"

                    withCredentials([
                        usernamePassword(credentialsId: cred,
                        usernameVariable: 'USR', passwordVariable: 'PWD')
                    ]) {

                        sh """
                            oc logout || true
                            oc login ${api} -u "${USR}" -p "${PWD}" --insecure-skip-tls-verify=true
                            oc get project ${NAMESPACE}
                        """

                        sh """
                            echo "Validando YAML con dry-run..."
                        """

                        sh """
                            for f in deploy/${params.ENVIRONMENT}/*.yaml; do
                                echo "Validando: \$f";

                                sed -e "s|${PLACEHOLDER_NAMESPACE}|${NAMESPACE}|g" \
                                    -e "s|${PLACEHOLDER_APP_NAME}|${APP_NAME}|g" \
                                    -e "s|${PLACEHOLDER_VERSION}|${params.VERSION ?: 'latest'}|g" \
                                    -e "s|${PLACEHOLDER_REGISTRY}|${registry}|g" \
                                    -e "s|${PLACEHOLDER_REGPULL}|${registryPull}|g" \
                                    -e "s|${PLACEHOLDER_APP_CONTEXT}|${APP_NAME}|g" \
                                    -e "s|${PLACEHOLDER_ROUTE_HOST}|${APP_NAME}.${params.ENVIRONMENT}.apps|g" \
                                    "\$f" | oc apply --dry-run=client -f - || exit 1
                            done
                        """

                        sh """
                            echo "Aplicando YAML a OpenShift..."
                            for f in deploy/${params.ENVIRONMENT}/*.yaml; do
                                sed -e "s|${PLACEHOLDER_NAMESPACE}|${NAMESPACE}|g" \
                                    -e "s|${PLACEHOLDER_APP_NAME}|${APP_NAME}|g" \
                                    -e "s|${PLACEHOLDER_VERSION}|${params.VERSION ?: 'latest'}|g" \
                                    -e "s|${PLACEHOLDER_REGISTRY}|${registry}|g" \
                                    -e "s|${PLACEHOLDER_REGPULL}|${registryPull}|g" \
                                    -e "s|${PLACEHOLDER_APP_CONTEXT}|${APP_NAME}|g" \
                                    -e "s|${PLACEHOLDER_ROUTE_HOST}|${APP_NAME}.${params.ENVIRONMENT}.apps|g" \
                                    "\$f" | oc apply -f -
                            done
                        """

                        sh """
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
                def cred = env["OCP_${params.ENVIRONMENT.toUpperCase()}_CRED"]

                withCredentials([
                    usernamePassword(credentialsId: cred,
                    usernameVariable: 'USR', passwordVariable: 'PWD')
                ]) {

                    sh """
                        echo 'Rollback iniciado...'
                        oc login ${api} -u "${USR}" -p "${PWD}" --insecure-skip-tls-verify=true
                        oc delete -f deploy/${params.ENVIRONMENT}/ --ignore-not-found=true
                    """
                }
            }
        }

        always {
            cleanWs()
        }
    }
}
