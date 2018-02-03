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
    pausable: false)

preferences {
    page(name: "auth", title: "ecobee", nextPage: "", content: "authPage", uninstall: true, install: true)
}

mappings {
    path("/oauth/initialize") { action: [GET: "oauthInitUrl"] }
    path("/oauth/callback") { action: [GET: "callback"] }
}

def authPage() {
    log.debug "authPage()"

    if (!atomicState.accessToken) { //this is to access token for 3rd party to make a call to connect app
        atomicState.accessToken = createAccessToken()
    }

    def description
    def uninstallAllowed = false
    def oauthTokenProvided = false

    if (atomicState.authToken) {
        description = "You are connected."
        uninstallAllowed = true
        oauthTokenProvided = true
    } else {
        description = "Click to enter Ecobee Credentials"
    }

    def redirectUrl = buildRedirectUrl
    log.debug "RedirectUrl = ${redirectUrl}"
    // get rid of next button until the user is actually auth'd
    if (!oauthTokenProvided) {
        return dynamicPage(name: "auth", title: "Login", nextPage: "", uninstall:uninstallAllowed) {
            section() {
                paragraph "Tap below to log in to the ecobee service and authorize SmartThings access. Be sure to scroll down on page 2 and press the 'Allow' button."
                href url:redirectUrl, style:"embedded", required:true, title:"ecobee", description:description
            }
        }
    } else {
        def stats = getEcobeeThermostats()
        log.debug "thermostat list: $stats"
        log.debug "sensor list: ${sensorsDiscovered()}"
        return dynamicPage(name: "auth", title: "Select Your Thermostats", uninstall: true) {
            section("") {
                paragraph "Tap below to see the list of ecobee thermostats available in your ecobee account and select the ones you want to connect to SmartThings."
                input(name: "thermostats", title:"Select Your Thermostats", type: "enum", required:true, multiple:true, description: "Tap to choose", metadata:[values:stats])
            }

            def options = sensorsDiscovered() ?: []
            def numFound = options.size() ?: 0
            if (numFound > 0)  {
                section("") {
                    paragraph "Tap below to see the list of ecobee sensors available in your ecobee account and select the ones you want to connect to SmartThings."
                    input(name: "ecobeesensors", title: "Select Ecobee Sensors ({{numFound}} found)", messageArgs: [numFound: numFound], type: "enum", required:false, description: "Tap to choose", multiple:true, options:options)
                }
            }
        }
    }
}

def oauthInitUrl() {
    log.debug "oauthInitUrl with callback: ${callbackUrl}"

    atomicState.oauthInitState = UUID.randomUUID().toString()

    def oauthParams = [
        response_type: "code",
        scope: "smartRead,smartWrite",
        client_id: smartThingsClientId,
        state: atomicState.oauthInitState,
        redirect_uri: callbackUrl
    ]

    redirect(location: "${apiEndpoint}/authorize?${toQueryString(oauthParams)}")
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
