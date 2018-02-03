/**
 *  Ecobee (Presence)
 *
 *  Copyright 2018 Todd Long
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Ecobee (Presence)",
    namespace: "tmlong",
    author: "Todd Long",
    description: "Notify Ecobee about presence.",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
    singleInstance: true,
    usesThirdPartyAuthentication: true,
    pausable: false
) {
    appSetting "clientId"
}

preferences {
    page(name: "authInit", install: true, uninstall: true)
}

mappings {
    path("/oauth/initialize") { action: [GET: "oauthInitUrl"] }
    path("/oauth/callback") { action: [GET: "callback"] }
}

def getServerUrl()           { return "https://graph.api.smartthings.com" }
def getShardUrl()            { return getApiServerUrl() }
def getCallbackUrl()         { return "https://graph.api.smartthings.com/oauth/callback" }
def getBuildRedirectUrl()    { return "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${shardUrl}" }
def getApiEndpoint()         { return "https://api.ecobee.com" }
def getSmartThingsClientId() { return appSettings.clientId }

def authInit() {
    log.debug "authInit()"

    if (!state.accessToken) { //this is to access token for 3rd party to make a call to connect app
        createAccessToken()
        log.debug "state.accessToken = ${state.accessToken}"
    }

    if (!state.authToken) {
        def redirectUrl = buildRedirectUrl
	    log.debug "redirectUrl = ${redirectUrl}"

        return dynamicPage(name: "authInit", title: "Login") {
            section {
                paragraph "Tap below to log in to the ecobee service and authorize SmartThings access. Be sure to scroll down on page 2 and press the 'Allow' button."
                href url: redirectUrl, style: "embedded", required: true, title: "ecobee", description: "Click to enter Ecobee Credentials"
            }
        }
    } else {
        def stats = getEcobeeThermostats()
        log.debug "thermostat list: $stats"

        return dynamicPage(name: "authInit", title: "Select Your Thermostats", uninstall: true) {
            section {
                paragraph "Tap below to see the list of ecobee thermostats available in your ecobee account and select the ones you want to connect to SmartThings."
                input(name: "thermostats", title: "Select Your Thermostats", type: "enum", required: true, multiple: true, description: "Tap to choose", metadata:[values:stats])
            }
        }
    }
}

def oauthInitUrl() {
    log.debug "oauthInitUrl with callback: ${callbackUrl}"

    state.oauthInitState = UUID.randomUUID().toString()
    log.debug "state.oauthInitState: ${state.oauthInitState}"

    def oauthParams = [
        response_type: "code",
        scope: "smartRead,smartWrite",
        client_id: smartThingsClientId,
        state: state.oauthInitState,
        redirect_uri: callbackUrl
    ]
    log.debug "oauthParams: ${oauthParams}"

    redirect(location: "${apiEndpoint}/authorize?${toQueryString(oauthParams)}")
}

def callback() {
    log.debug "callback()>> params: $params, params.code ${params.code}"

    def code = params.code
    def oauthState = params.state

    if (oauthState == state.oauthInitState) {
        def tokenParams = [
            grant_type: "authorization_code",
            code: code,
            client_id : smartThingsClientId,
            redirect_uri: callbackUrl
        ]

        def tokenUrl = "https://api.ecobee.com/token?${toQueryString(tokenParams)}"

        httpPost(uri: tokenUrl) { resp ->
            state.refreshToken = resp.data.refresh_token
            state.authToken = resp.data.access_token
        }

        state.authToken ? success() : fail()
    } else {
        log.error "callback() failed oauthState != state.oauthInitState"
    }
}

def success() {
    def message = """
        <p>Your ecobee Account is now connected to SmartThings!</p>
        <p>Click 'Done' to finish setup.</p>
    """
    connectionStatus(message)
}

def fail() {
    def message = """
        <p>The connection could not be established!</p>
        <p>Click 'Done' to return to the menu.</p>
    """
    connectionStatus(message)
}

def connectionStatus(message, redirectUrl = null) {
    def redirectHtml = ""
    if (redirectUrl) {
        redirectHtml = """
            <meta http-equiv="refresh" content="3; url=${redirectUrl}" />
        """
    }

	def html = """
        <!DOCTYPE html>
        <html>
            <head>
                <meta name="viewport" content="width=640">
                <title>Ecobee & SmartThings connection</title>
                <style type="text/css">
                    @font-face {
                        font-family: 'Swiss 721 W01 Thin';
                        src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
                        src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
                        url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
                        url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
                        url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
                        font-weight: normal;
                        font-style: normal;
                    }
                    @font-face {
                        font-family: 'Swiss 721 W01 Light';
                        src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
                        src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
                        url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
                        url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
                        url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
                        font-weight: normal;
                        font-style: normal;
                    }
                    .container {
                        width: 90%;
                        padding: 4%;
                        text-align: center;
                    }
                    img {
                        vertical-align: middle;
                    }
                    p {
                        font-size: 2.2em;
                        font-family: 'Swiss 721 W01 Thin';
                        text-align: center;
                        color: #666666;
                        padding: 0 40px;
                        margin-bottom: 0;
                    }
                    span {
                        font-family: 'Swiss 721 W01 Light';
                    }
                </style>
            </head>
        <body>
            <div class="container">
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/ecobee%402x.png" alt="ecobee icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
                ${message}
            </div>
        </body>
    </html>
    """

    render contentType: 'text/html', data: html
}

def getEcobeeThermostats() {
    log.debug "getting device list"
    state.remoteSensors = []

    def bodyParams = [
        selection: [
            selectionType: "registered",
            selectionMatch: "",
            includeRuntime: true,
            includeSensors: true
        ]
    ]

    def deviceListParams = [
        uri: apiEndpoint,
        path: "/1/thermostat",
        headers: ["Content-Type": "text/json", "Authorization": "Bearer ${state.authToken}"],
        // TODO - the query string below is not consistent with the Ecobee docs:
        // https://www.ecobee.com/home/developer/api/documentation/v1/operations/get-thermostats.shtml
        query: [format: 'json', body: toJson(bodyParams)]
    ]

    def stats = [:]
    try {
        httpGet(deviceListParams) { resp ->
            if (resp.status == 200) {
                resp.data.thermostatList.each { stat ->
                    state.remoteSensors = state.remoteSensors == null ? stat.remoteSensors : state.remoteSensors <<  stat.remoteSensors
                    def dni = [app.id, stat.identifier].join('.')
                    stats[dni] = getThermostatDisplayName(stat)
                }
            } else {
                log.debug "http status: ${resp.status}"
            }
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        log.trace "Exception polling children: " + e.response.data.status
        if (e.response.data.status.code == 14) {
            state.action = "getEcobeeThermostats"
            log.debug "Refreshing your auth_token!"
            refreshAuthToken()
        }
    }
    state.thermostats = stats
    return stats
}

def getThermostatDisplayName(stat) {
    if(stat?.name) {
        return stat.name.toString()
    }
    return (getThermostatTypeName(stat) + " (${stat.identifier})").toString()
}

def getThermostatTypeName(stat) {
	return stat.modelNumber == "siSmart" ? "Smart Si" : "Smart"
}

def toJson(Map m) {
    return groovy.json.JsonOutput.toJson(m)
}

def toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
//    subscribe(location, "mode", modeChangeHandler)
}

def modeChangeHandler(evt) {
    log.debug "Handle mode change: ${location.mode}"

    def deviceIdsString = parent.getChildDeviceIdsString()

    log.debug "deviceIdsString: ${deviceIdsString}"

    // away, home, sleep

    annoyEcobee(deviceIdsString)
}

def annoyEcobee(deviceId) {
    def payload = [
        selection: [
            selectionType: "thermostats",
            selectionMatch: deviceId,
            includeRuntime: true
        ],
        functions: [[
            type: "setHold",
            params: [
                holdType: "nextTransition",
                holdClimateRef: "away"
            ]
        ]]
    ]

    parent.sendCommandToEcobee(payload)
}