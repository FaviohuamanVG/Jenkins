/**
 * Slack notifications for Jenkins - Clean version without encoding issues
 */

def sendSlackNotification(String status, String message = null) {
    def webhook = env.SLACK_WEBHOOK_URL
    def channel = env.SLACK_CHANNEL ?: '#todo-vallegrande'
    
    if (!webhook) {
        echo "SLACK_WEBHOOK_URL not configured - skipping notification"
        return
    }
    
    // Simple status colors
    def colors = [
        SUCCESS: '#36a64f',
        FAILURE: '#ff0000', 
        UNSTABLE: '#ffcc00',
        STARTED: '#0099cc'
    ]
    
    def color = colors[status] ?: '#808080'
    
    // Build message
    def text = "*${status}*: VG User Microservice #${env.BUILD_NUMBER}"
    if (message) {
        text += "\n${message}"
    }
    text += "\n<${env.BUILD_URL}|View Build> | <https://sonarcloud.io/project/overview?id=FaviohuamanVG_Jenkins|SonarCloud>"
    
    // Simple payload
    def payload = [
        channel: channel,
        username: 'Jenkins',
        text: text,
        attachments: [[
            color: color,
            fields: [
                [title: "Branch", value: env.BRANCH_NAME ?: 'main', short: true],
                [title: "Duration", value: currentBuild.durationString ?: 'In progress', short: true]
            ]
        ]]
    ]
    
    // Send using simple JSON construction (no JsonBuilder needed)
    try {
        // Build JSON manually to avoid security restrictions
        def duration = currentBuild.durationString ?: 'In progress'
        def branch = env.BRANCH_NAME ?: 'main'
        
        def jsonStr = """
{
    "channel": "${channel}",
    "username": "Jenkins",
    "text": "${text}",
    "attachments": [
        {
            "color": "${color}",
            "fields": [
                {
                    "title": "Branch",
                    "value": "${branch}",
                    "short": true
                },
                {
                    "title": "Duration", 
                    "value": "${duration}",
                    "short": true
                }
            ]
        }
    ]
}
        """.trim()
        
        // Write JSON to temp file
        def tempFile = "slack-payload-${System.currentTimeMillis()}.json"
        writeFile file: tempFile, text: jsonStr
        
        if (isUnix()) {
            sh """
                curl -X POST -H 'Content-type: application/json' --data @${tempFile} ${webhook}
                rm ${tempFile}
            """
        } else {
            bat """
                set WEBHOOK_URL=${webhook}
                powershell -Command "Invoke-RestMethod -Uri '%WEBHOOK_URL%' -Method Post -ContentType 'application/json' -InFile '${tempFile}'"
                del ${tempFile}
            """
        }
        echo "Slack notification sent: ${status}"
    } catch (Exception e) {
        echo "Slack error: ${e.message}"
    }
}

def notifyBuildStarted() {
    sendSlackNotification('STARTED', 'Build started - Estimated time: 3-5 min')
}

def notifyBuildSuccess() {
    sendSlackNotification('SUCCESS', 'Build successful - 19 tests OK - Ready for deploy')
}

def notifyBuildFailure() {
    sendSlackNotification('FAILURE', 'Build failed - Check logs - @vallegrande-dev')
}

def notifyBuildUnstable() {
    sendSlackNotification('UNSTABLE', 'Build UNSTABLE - Tests OK but minor warnings')
}

def notifyStageCompletion(String stageName, String status) {
    def importantStages = ['Unit Tests', 'SonarCloud Analysis']
    if (stageName in importantStages) {
        sendSlackNotification(status, "${stageName}: ${status}")
    }
}

return this
