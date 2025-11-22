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

        // Placeholder exactos del proyecto
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
                    echo "Realizando checkout de la rama: ${params.BRANCH}"

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: params.BRANCH]],
                        userRemoteConfigs: [[
                            url: env.GIT_URL,
                            credentialsId: env.GIT_CREDENTIALS
                        ]]
                    ])

                    if (!fileExists('Dockerfile')) {
                        error("ERROR: No se encontró Dockerfile en el checkout.")
                    }

                    if (!fileExists('pom.xml') && !fileExists('build.gradle')) {
                        error("ERROR: No existe pom.xml ni build.gradle. El checkout del código ha fallado.")
                    }

                    echo "✔ Checkout validado correctamente."
                }
            }
        }

        /* ===========================================================
                         COMPILACIÓN (solo desa/certi)
           =========================================================== */
        stage('Compile') {
            when { expression { params.ENVIRONMENT != 'prod' } }
            steps {
                sh '''
                    mvn clean package -DskipTests
                '''
            }
        }

        /* ===========================================================
                        BUILD DOCKER (solo desa/certi)
           =========================================================== */
        stage('Build Image') {
            when { expression { params.ENVIRONMENT != 'prod' } }
            steps {
                script {
                    def registry = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGISTRY"]

                    sh """
                        docker build -f Dockerfile -t ${registry}/${APP_NAME}:${params.VERSION ?: "latest"} .
                    """
                }
            }
        }

        /* ===========================================================
                               PUSH DOCKER
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

                        sh '''
                            echo "$REG_PWD" | docker login '"${registry}"' -u "$REG_USR" --password-stdin
                            docker push '"${registry}"'/'"${APP_NAME}"':'"${params.VERSION ?: "latest"}"'
                        '''
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
                        error("ERROR: Para producción debe ingresar VERSION certificada.")
                    }

                    def regCerti = env["OCP_CERTI_REGISTRY"]
                    def regProd  = env["OCP_PROD_REGISTRY"]
                    def version  = params.VERSION

                    sh """
                        echo "Validando imagen certificada..."
                        docker manifest inspect ${regCerti}/${APP_NAME}:${version} || exit 1

                        docker pull ${regCerti}/${APP_NAME}:${version}
                        docker tag  ${regCerti}/${APP_NAME}:${version} ${regProd}/${APP_NAME}:${version}
                        docker push ${regProd}/${APP_NAME}:${version}
                    """
                }
            }
        }

        /* ===========================================================
                         DESPLIEGUE OPENSHIFT
           =========================================================== */
        stage("Deploy") {
            steps {
                script {

                    def api         = env["OCP_${params.ENVIRONMENT.toUpperCase()}_API"]
                    def cred        = env["OCP_${params.ENVIRONMENT.toUpperCase()}_CRED"]
                    def registry    = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGISTRY"]
                    def registryPull = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGPULL"]

                    echo "Validando namespace: ${NAMESPACE}"

                    withCredentials([
                        usernamePassword(credentialsId: cred, usernameVariable: 'USR', passwordVariable: 'PWD')
                    ]) {

                        sh '''
                            oc logout || true
                            oc login '"${api}"' -u "$USR" -p "$PWD" --insecure-skip-tls-verify=true
                        '''

                        sh """
                            oc get project ${NAMESPACE} || exit 1
                        """

                        sh "echo Namespace validado: ${NAMESPACE}"

                        sh """
                            for f in deploy/${params.ENVIRONMENT}/*.yaml; do
                                echo \"Validando YAML: \$f\";
                                sed -e \"s|${PLACEHOLDER_NAMESPACE}|${NAMESPACE}|g\" \
                                    -e \"s|${PLACEHOLDER_APP_NAME}|${APP_NAME}|g\" \
                                    -e \"s|${PLACEHOLDER_VERSION}|${params.VERSION ?: 'latest'}|g\" \
                                    -e \"s|${PLACEHOLDER_REGISTRY}|${registry}|g\" \
                                    -e \"s|${PLACEHOLDER_REGPULL}|${registryPull}|g\" \
                                    -e \"s|${PLACEHOLDER_APP_CONTEXT}|${APP_NAME}|g\" \
                                    -e \"s|${PLACEHOLDER_ROUTE_HOST}|${APP_NAME}.${params.ENVIRONMENT}.apps|g\" \
                                    \"\$f\" | oc apply --dry-run=client -f - || exit 1
                            done
                        """

                        sh """
                            for f in deploy/${params.ENVIRONMENT}/*.yaml; do
                                sed -e \"s|${PLACEHOLDER_NAMESPACE}|${NAMESPACE}|g\" \
                                    -e \"s|${PLACEHOLDER_APP_NAME}|${APP_NAME}|g\" \
                                    -e \"s|${PLACEHOLDER_VERSION}|${params.VERSION ?: 'latest'}|g\" \
                                    -e \"s|${PLACEHOLDER_REGISTRY}|${registry}|g\" \
                                    -e \"s|${PLACEHOLDER_REGPULL}|${registryPull}|g\" \
                                    -e \"s|${PLACEHOLDER_APP_CONTEXT}|${APP_NAME}|g\" \
                                    -e \"s|${PLACEHOLDER_ROUTE_HOST}|${APP_NAME}.${params.ENVIRONMENT}.apps|g\" \
                                    \"\$f\" | oc apply -f -
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
                        ROLLBACK AUTOMÁTICO
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

                    sh '''
                        echo "⚠️ Error detectado: ejecutando rollback…";

                        oc login '"${api}"' -u "$USR" -p "$PWD" --insecure-skip-tls-verify=true
                    '''

                    sh """
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
