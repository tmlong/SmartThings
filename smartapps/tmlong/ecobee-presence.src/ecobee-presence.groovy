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
    description: "Notify ecobee about presence.",
    category: "SmartThings Labs",
    parent: "smartthings:Ecobee (Connect)",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png",
    singleInstance: true,
    pausable: false)

preferences {
    page(name: "authInit")
}

def authInit() {
    log.debug "authInit()"

    def stats = parent.getEcobeeThermostats()
    log.debug "Available thermostats: ${stats}"

    return dynamicPage(name: "authInit", title: "Select Your Thermostats", install: true, uninstall: state.init) {
        section {
            paragraph "Tap below to see the list of ecobee thermostats available in your ecobee account and select the ones you want to connect to SmartThings."
            input(name: "thermostats", title: "Select Your Thermostats", type: "enum", required: true, multiple: true, description: "Tap to choose", metadata: [values:stats])
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

//    initialize()

    if (!state.init) {
        state.init = true;
    }
}

def updated() {
    if ((now() - (state.lastUpdated ?: 0)) < 5000) return

    state.lastUpdated = now()

    log.debug "Updated with settings: ${settings}"

    initialize()
}

def initialize() {
    log.debug "Selected thermostats: ${thermostats}"

    // unsubscribe from previous events
    unsubscribe()

    // subscribe to "mode" events
    subscribe(location, "mode", modeChangeHandler)
}

def modeChangeHandler(evt) {
    log.debug "Handle mode change: ${location.mode}"

    // away, home, sleep

	// holdType: dateTime, nextTransition, indefinite, holdHours

    def deviceIdsString = getChildDeviceIdsString()

    annoyEcobee(deviceIdsString, location.mode.toLowerCase())
}

def annoyEcobee(deviceId, climate) {
    log.debug "Annoying ecobee for deviceId: ${deviceId}"

    def payload = [
        selection: [
            selectionType: "thermostats",
            selectionMatch: deviceId,
            includeRuntime: true
        ],
        functions: [[
            type: "setHold",
            params: [
                holdType: "indefinite",
                holdClimateRef: climate
            ]
        ]]
    ]

    def success = parent.sendCommandToEcobee(payload)

    log.debug "Annoying ecobee was a success? ${success}"
}

def getChildDeviceIdsString() {
    return thermostats.collect { it.split(/\./).last() }.join(',')
}