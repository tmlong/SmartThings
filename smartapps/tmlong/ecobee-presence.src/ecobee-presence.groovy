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
    page(name: "pageMode")
    page(name: "pageClimate")
}

def pageMode() {
    // configure debug
    state.debug = [
        enabled: false
    ]

    log.debug "pageMode() debug: ${state.debug}, settings: ${settings}"

    // initialize thermostats
    state.thermostats = getThermostats([
        selection: [
            selectionType: "registered",
            selectionMatch: "",
            includeProgram: true
        ]
    ])

    def select = [
        thermostats: state.thermostats.collectEntries { k,v -> [(k): v.name] },

        // holdType: dateTime, nextTransition, indefinite, holdHours
        holdTypes: [
            [ indefinite: "Indefinite" ],
            [ nextTransition: "Next Transition" ]
        ]
    ]

    log.debug "pageMode() select: ${select}"

    dynamicPage(name: "pageMode", title: "", nextPage: "pageClimate", install: false, uninstall: state.init) {
        section {
            paragraph "Tap below to see the list of ecobee thermostats available and select the ones you want to connect to SmartThings."
            input(name: "thermostats", title: "Select Thermostats", type: "enum", required: true, multiple: true, description: "Tap to choose", metadata: [values: select.thermostats])
        }

        section {
            paragraph "Tap below to see the list of hold types available and select the one you want to use with mode changes."
            input(name: "holdType", title: "Select Hold Type", type: "enum", required: true, multiple: false, description: "Tap to choose", metadata: [values: select.holdTypes])
        }

        section {
            paragraph "Tap below to see the list of modes available and select the ones you want to listen for."
            input "modes", "mode", title: "Select Mode(s)", multiple: true
        }
    }
}

def pageClimate() {
    log.debug "pageClimate() settings: ${settings}"

    def select = [
        climates: [:]
    ]

    state.thermostats.each { id,thermostat ->
        thermostat.climates.each { ref,name ->
            select.climates[ref] = name
        }
    }

    log.debug "pageClimate() select: ${select}"

    dynamicPage(name: "pageClimate", title: "", install: true, uninstall: state.init) {
        section {
            paragraph "Tap below to see the list of ecobee climates available and select the one you want to associate with each mode."
        }

        settings.modes.each { mode ->
            section {
                input(name: "${mode}", title: "Select \"${mode}\" Climate", type: "enum", required: true, multiple: false, description: "Tap to choose", metadata: [values: select.climates])
            }
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    if (!state.init) {
        state.init = true;
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

    // initialize thermostat ids for selection (i.e. request)
    state.thermostatIds = getThermostatIdsForSelection(settings.thermostats)

    // initialize the hold type
    state.holdType = settings.holdType

    // initialize the mode to climate map
    state.climate = settings.modes.collectEntries {
        [(it): settings[it]]
    }

    log.debug "Initialized with state: ${state}"

    // send the initial mode change
    modeChangeHandler([value: location.mode])

    // unsubscribe from previous events
    unsubscribe()

    // subscribe to "mode" events
    subscribe(location, "mode", modeChangeHandler)
}

def modeChangeHandler(event) {
    log.debug "modeChangeHandler() event: ${event}"

    // get the mode
	def mode = event.value

    log.debug "modeChangeHandler() mode: ${mode}"

    holdClimate(state.thermostatIds, state.holdType, state.climate[mode])
}

// @see https://www.ecobee.com/home/developer/api/documentation/v1/operations/get-thermostats.shtml
def getThermostats(Map query) {
    log.debug "getThermostats() query: ${query}"

    if (state.debug.enabled) {
        def thermostats = [
            12345: [
                name: "My Thermo",
                climates: [
                    away: "Away",
                    home: "Home",
                    sleep: "Sleep"
                ]
            ]
        ]

        log.debug "(debug) getThermostats() return: ${thermostats}"

        return thermostats
    }

    def params = [
        uri: apiEndpoint,
        path: "/1/thermostat",
        headers: ["Content-Type": "application/json", Authorization: "Bearer ${parent.atomicState.authToken}"],
        query: [format: 'json', body: toJson(query)]
    ]

    def thermostats = [:]

    httpGet(params) { resp ->
        if (resp.status == 200) {
            resp.data.thermostatList.each { stat ->
                def climates = [:]

                stat.program.climates.each { climate ->
                    climates[climate.climateRef] = climate.name
                }

                thermostats[stat.identifier] = [
                    name: stat.name,
                    climates: climates
                ]
            }
        } else {
            log.debug "http status: ${resp.status}"
        }
    }

    log.debug "getThermostats() return: ${thermostats}"

    return thermostats
}

// @see https://www.ecobee.com/home/developer/api/documentation/v1/functions/SetHold.shtml
def holdClimate(thermostatIds, holdType, climate) {
    log.debug "holdClimate() thermostatIds: ${thermostatIds}, holdType: ${holdType}, climate: ${climate}"

    if (state.debug.enabled) {
        log.debug "(debug) holdClimate() return: true"
        return true
    }

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
    log.debug "holdClimate() return: ${success}"

    return success
}

def getThermostatIdsForSelection(thermostats) {
    return thermostats.collect { it.split(/\./).last() }.join(',')
}

def toJson(Map m) { return groovy.json.JsonOutput.toJson(m) }

def getApiEndpoint() { return "https://api.ecobee.com" }