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

        /* Placeholders en los YAML */
        PLACEHOLDER_NAMESPACE   = "__NAMESPACE__"
        PLACEHOLDER_APP_NAME    = "__APP_NAME__"
        PLACEHOLDER_APP_CONTEXT = "__APP_CONTEXT__"
        PLACEHOLDER_VERSION     = "__VERSION__"
        PLACEHOLDER_REGISTRY    = "__REGISTRY__"
        PLACEHOLDER_REGPULL     = "__REGISTRY_PULL__"
        PLACEHOLDER_ROUTE_HOST  = "__ROUTE_HOST__"

        /* Imagen base corporativa por ambiente */
        OCP_DESA_BASEIMAGE  = "image-registry.openshift-image-registry.svc:5000/redeban-base-image/java21-runtime:1.0.0"
        OCP_CERTI_BASEIMAGE = "image-registry.openshift-image-registry.svc:5000/redeban-base-image/java21-runtime:1.0.0"
        OCP_PROD_BASEIMAGE  = "image-registry.openshift-image-registry.svc:5000/redeban-base-image/java21-runtime:1.0.0"
    }

    stages {

        /* ===========================================================
                           CHECKOUT
           =========================================================== */
        stage('Checkout') {
            steps {
                script {
                    echo "📥 Haciendo checkout de la rama: ${params.BRANCH}"

                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: params.BRANCH]],
                        userRemoteConfigs: [[
                            url: env.GIT_URL,
                            credentialsId: env.GIT_CREDENTIALS
                        ]]
                    ])

                    if (!fileExists('Dockerfile')) {
                        error("❌ No existe Dockerfile en raíz.")
                    }

                    echo "✔ Checkout completado correctamente."
                }
            }
        }

        /* ===========================================================
                           COMPILACIÓN
           =========================================================== */
        stage('Compile') {
            when { expression { params.ENVIRONMENT != 'prod' } }
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        /* ===========================================================
                           BUILD DOCKER
           =========================================================== */
        stage('Build Image') {
            when { expression { params.ENVIRONMENT != 'prod' } }
            steps {
                script {

                    def registry  = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGISTRY"]
                    def baseImage = env["OCP_${params.ENVIRONMENT.toUpperCase()}_BASEIMAGE"]
                    def tag       = params.VERSION ?: "latest"

                    echo "🚧 Construyendo imagen con base: ${baseImage}"

                    sh """
                        docker build \
                            --build-arg BASE_IMAGE=${baseImage} \
                            -t ${registry}/${NAMESPACE}/${APP_NAME}:${tag} \
                            -f Dockerfile .
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

                    def registry = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGISTRY"]
                    def tokenId  = "OCP_${params.ENVIRONMENT.toUpperCase()}_TOKEN"
                    def tag      = params.VERSION ?: "latest"

                    withCredentials([string(credentialsId: tokenId, variable: 'OCP_TOKEN')]) {

                        echo "🔐 Login al registry: ${registry}"
                        sh """
                            echo "\${OCP_TOKEN}" | docker login ${registry} -u openshift --password-stdin
                        """

                        echo "📤 Enviando imagen al registry…"
                        sh """
                            docker push ${registry}/${NAMESPACE}/${APP_NAME}:${tag}
                        """
                    }
                }
            }
        }

        /* ===========================================================
                      PROMOTE CERTI → PROD
           =========================================================== */
        stage('Promote to PROD') {
            when { expression { params.ENVIRONMENT == 'prod' } }
            steps {
                script {

                    if (!params.VERSION?.trim()) {
                        error("❌ Para producción debe ingresar VERSION certificada.")
                    }

                    def regCerti = env["OCP_CERTI_REGISTRY"]
                    def regProd  = env["OCP_PROD_REGISTRY"]

                    echo "📦 Promoviendo imagen CERTI → PROD"

                    sh """
                        docker manifest inspect ${regCerti}/${NAMESPACE}/${APP_NAME}:${params.VERSION} || exit 1

                        docker pull ${regCerti}/${NAMESPACE}/${APP_NAME}:${params.VERSION}
                        docker tag  ${regCerti}/${NAMESPACE}/${APP_NAME}:${params.VERSION} ${regProd}/${NAMESPACE}/${APP_NAME}:${params.VERSION}
                        docker push ${regProd}/${NAMESPACE}/${APP_NAME}:${params.VERSION}
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

                    def api          = env["OCP_${params.ENVIRONMENT.toUpperCase()}_API"]
                    def cred         = "OCP_${params.ENVIRONMENT.toUpperCase()}_TOKEN"
                    def registry     = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGISTRY"]
                    def registryPull = env["OCP_${params.ENVIRONMENT.toUpperCase()}_REGPULL"]
                    def tag          = params.VERSION ?: "latest"

                    echo "🚀 Desplegando en OCP ambiente: ${params.ENVIRONMENT}"

                    withCredentials([string(credentialsId: cred, variable: 'TOKEN')]) {

                        sh """
                            oc logout || true
                            oc login ${api} --token="\${TOKEN}" --insecure-skip-tls-verify=true
                            oc project ${NAMESPACE}
                        """

                        echo "🔎 Validando manifiestos YAML"

                        sh """
                            for f in deploy/${params.ENVIRONMENT}/*.yaml; do
                                sed -e "s|${PLACEHOLDER_NAMESPACE}|${NAMESPACE}|g" \
                                    -e "s|${PLACEHOLDER_APP_NAME}|${APP_NAME}|g" \
                                    -e "s|${PLACEHOLDER_VERSION}|${tag}|g" \
                                    -e "s|${PLACEHOLDER_REGISTRY}|${registry}|g" \
                                    -e "s|${PLACEHOLDER_REGPULL}|${registryPull}|g" \
                                    "\$f" | oc apply --dry-run=client -f - || exit 1
                            done
                        """

                        echo "📡 Aplicando manifiestos en cluster"
                        sh """
                            for f in deploy/${params.ENVIRONMENT}/*.yaml; do
                                sed -e "s|${PLACEHOLDER_NAMESPACE}|${NAMESPACE}|g" \
                                    -e "s|${PLACEHOLDER_APP_NAME}|${APP_NAME}|g" \
                                    -e "s|${PLACEHOLDER_VERSION}|${tag}|g" \
                                    -e "s|${PLACEHOLDER_REGISTRY}|${registry}|g" \
                                    -e "s|${PLACEHOLDER_REGPULL}|${registryPull}|g" \
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
                def cred = "OCP_${params.ENVIRONMENT.toUpperCase()}_TOKEN"

                withCredentials([string(credentialsId: cred, variable: 'TOKEN')]) {

                    sh """
                        echo "Rollback iniciado…"
                        oc login ${api} --token="\${TOKEN}" --insecure-skip-tls-verify=true
                    """

                    if (fileExists("deploy/${params.ENVIRONMENT}")) {
                        sh "oc delete -f deploy/${params.ENVIRONMENT}/ --ignore-not-found=true"
                    } else {
                        echo "⚠ No existe carpeta deploy/${params.ENVIRONMENT}, rollback limitado."
                    }
                }
            }
        }

        always {
            cleanWs()
        }
    }
}
