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
    page(name: "doInit")
}

def doInit() {
    log.debug "doInit()"

    def stats = parent.getEcobeeThermostats()
    log.debug "Ecobee thermostats: ${stats}"

    return dynamicPage(name: "doInit", title: "Presence Configuration", install: true, uninstall: state.init) {
        section {
            paragraph "Tap below to see the list of ecobee thermostats available in your ecobee account and select the ones you want to connect to SmartThings."
            input(name: "thermostats", title: "Select Your Thermostats", type: "enum", required: true, multiple: true, description: "Tap to choose", metadata: [values: stats])
        }

        // holdType: dateTime, nextTransition, indefinite, holdHours
        def holdTypes = [
            [ indefinite: "Indefinite" ],
            [ nextTransition: "Next Transition" ]
        ]

        section {
            paragraph "Tap below to see the list of hold types available and select the one you want to use on mode changes."
            input(name: "holdType", title: "Select Your Hold Type", type: "enum", required: true, multiple: false, description: "Tap to choose", metadata: [values: holdTypes])
        }

        // TODO map thermostat climates... includeProgram: true

        section {
            paragraph "Tap below to set whether or not to send an initial presence to the selected thermostats."
            input(name: "initHold", title: "Send Initial Presence", type: "bool", defaultValue: false)
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    if (!state.init) {
        state.init = true;

        if (settings.initHold) {
            modeChangeHandler([value: location.mode])
        }
    }
}

def updated() {
    // prevent updated() from being called twice
    if ((now() - (state.lastUpdated ?: 0)) < 5000) return

    state.lastUpdated = now()

    log.debug "Updated with settings: ${settings}"

    initialize()
}

def initialize() {
    log.debug "initialize()"

    log.debug "parent.atomicState: ${parent.atomicState}"

    // unsubscribe from previous events
    unsubscribe()

    // subscribe to "mode" events
    subscribe(location, "mode", modeChangeHandler)
}

def modeChangeHandler(evt) {
    log.debug "modeChangeHandler() mode: ${evt.value}"

    // get the thermostat ids
    def thermostatIds = getThermostatIdsForSelection(settings.thermostats)

    // get the mode
	def mode = evt.value

    // thermostat climate to location mode mapping
	def climate = [
        Away: "away",
        Home: "home",
        Nap: "home",
        Night: "sleep"
    ]

    holdClimate(thermostatIds, settings.holdType, climate[mode])
}

// @see https://www.ecobee.com/home/developer/api/documentation/v1/functions/SetHold.shtml
def holdClimate(thermostatIds, holdType, climate) {
    log.debug "holdClimate() thermostatIds: ${thermostatIds} holdType: ${holdType} climate: ${climate}"

    // define the hold climate payload
    def payload = [
        selection: [
            selectionType: "thermostats",
            selectionMatch: thermostatIds,
            includeRuntime: true
        ],
        functions: [[
            type: "setHold",
            params: [
                holdType: holdType,
                holdClimateRef: climate
            ]
        ]]
    ]

    // send the hold climate request
    def success = parent.sendCommandToEcobee(payload)

    log.debug "holdClimate() success? ${success}"
}

def getThermostatIdsForSelection(stats) {
    return stats.collect { it.split(/\./).last() }.join(',')
}

def toJson(Map m) {
    return groovy.json.JsonOutput.toJson(m)
}

def getApiEndpoint()         { return "https://api.ecobee.com" }

//sendTest([
//    selection: [
//        selectionType: "thermostats",
//        selectionMatch: state.thermostatIds,
//        includeProgram: true
//    ]
//])

def sendTest(Map bodyParams) {
    def json = toJson(bodyParams)
    log.debug "json: ${json}"
    
    def cmdParams = [
        uri: apiEndpoint,
        path: "/1/thermostat",
        headers: ["Content-Type": "application/json", "Authorization": "Bearer ${parent.atomicState.authToken}"],
        query: [format: 'json', body: json]
    ]

    httpGet(cmdParams) { resp ->
        log.debug "resp.status: ${resp.status}"
        log.debug "resp.data length: ${resp.data.size()}"
        for (i = 0; i < resp.data.size(); ) {
            def data = resp.data.substring(i, i+100)
            log.debug "data: ${data}"
            i+=100
        }
    }
}