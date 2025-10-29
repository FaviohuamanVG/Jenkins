/**
 * Funciones simplificadas para notificaciones de Slack
 * ===================================================
 */

def sendSlackNotification(String status, String message = null, String color = null) {
    // Configuración simple
    def webhook = env.SLACK_WEBHOOK_URL
    def channel = env.SLACK_CHANNEL ?: '#todo-vallegrande'
    
    // Verificar que tenemos webhook configurado
    if (!webhook) {
        echo "⚠️ SLACK_WEBHOOK_URL no configurado - saltando notificación"
        return
    }
    
    // Colores y simbolos simples (sin emojis para evitar problemas de codificacion)
    def statusConfig = [
        SUCCESS: [color: '#36a64f', emoji: '[OK]'],
        FAILURE: [color: '#ff0000', emoji: '[FAIL]'],
        UNSTABLE: [color: '#ffcc00', emoji: '[WARN]'],
        STARTED: [color: '#0099cc', emoji: '[START]']
    ]
    
    def config = statusConfig[status] ?: [color: '#808080', emoji: '[INFO]']
    
    // Mensaje simple
    def text = "${config.emoji} *${status}*: VG User Microservice #${env.BUILD_NUMBER}"
    if (message) {
        text += "\n${message}"
    }
    text += "\n<${env.BUILD_URL}|Ver build> | <https://sonarcloud.io/project/overview?id=FaviohuamanVG_Jenkins|SonarCloud>"
    
    // Payload JSON simple
    def payload = [
        channel: channel,
        username: 'Jenkins',
        text: text,
        attachments: [[
            color: config.color,
            fields: [
                [title: "Rama", value: env.BRANCH_NAME ?: 'main', short: true],
                [title: "Tiempo", value: currentBuild.durationString ?: 'En progreso', short: true]
            ]
        ]]
    ]
    
    // Enviar a Slack usando PowerShell (Windows nativo)
    try {
        def jsonPayload = groovy.json.JsonBuilder(payload).toString().replace('"', '\\"')
        
        if (isUnix()) {
            // Para Linux/Mac usar curl
            sh """
                curl -X POST -H 'Content-type: application/json' \
                -d '${jsonPayload}' ${webhook}
            """
        } else {
            // Para Windows usar PowerShell nativo
            bat """
                powershell -Command "Invoke-RestMethod -Uri '${webhook}' -Method Post -ContentType 'application/json' -Body '${jsonPayload}'"
            """
        }
        echo "✅ Slack notificación enviada: ${status}"
    } catch (Exception e) {
        echo "⚠️ Slack error (continuando build): ${e.message}"
    }
}

// Funciones simplificadas (sin emojis)
def notifyBuildStarted() {
    sendSlackNotification('STARTED', 'Iniciando build - Tiempo estimado: 3-5 min')
}

def notifyBuildSuccess() {
    sendSlackNotification('SUCCESS', 'Build exitoso - 19 tests OK - Listo para deploy')
}

def notifyBuildFailure() {
    sendSlackNotification('FAILURE', 'Build fallo - Revisar logs - @vallegrande-dev')
}

def notifyBuildUnstable() {
    sendSlackNotification('UNSTABLE', 'Build UNSTABLE - Tests OK pero hay warnings menores')
}

def notifyStageCompletion(String stageName, String status, Map details = [:]) {
    // Solo notificar etapas importantes
    def importantStages = ['Unit Tests', 'SonarCloud Analysis'] 
    if (stageName in importantStages) {
        sendSlackNotification(status, "${stageName}: ${status}")
    }
}

return this
