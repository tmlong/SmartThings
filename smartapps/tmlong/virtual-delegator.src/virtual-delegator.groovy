/**
 *  Virtual Delegator
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
        name: "Virtual Delegator",
        namespace: "tmlong",
        author: "Todd Long",
        description: "A virtual delegator for things.",
        category: "SmartThings Labs",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "pageCapability")
}

def pageCapability() {
    log.debug "pageCapability()"

    // define the available capabilites
    def capabilities = [
            outlet: [
                    type: "capability.outlet",
                    title: "Select Outlets"
            ],
            switch: [
                    type: "capability.switch",
                    title: "Select Switches"
            ]
    ]

    // configure the select metadata
    def select = [
            capabilities: [
                    values: capabilities.keySet().collect()
            ]
    ]

    dynamicPage(name: "pageCapability", title: "", install: true, uninstall: state.init) {
        section {
            input "capability", "enum", title: "Select Capability", required: true, submitOnChange: true, metadata: select.capabilities

            if (settings.capability) {
                def capability = capabilities[settings.capability]

                input "delegates", capability.type, title: capability.title, multiple: true
            }
        }

        section {
            label title: "Assign Name", required: false
        }
    }
}

def installed() {
    log.info "Installed with settings: ${settings}"

    if (!state.init) {
        state.init = true;
        state.deviceId = getHandlerId()
    }
}

def updated() {
    log.info "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "initialize()"

    // initialize the child handlers
    initializeHandlers()

    // subscribe to the capability events
    subscribe(settings.delegates, settings.capability, delegatesHandler)
}

def initializeHandlers() {
    log.debug "initializeHandlers()"

    def device = getChildDevice(state.deviceId)

    if (!device) {
        // add the child device
        device = addChildDevice(app.namespace, getHandlerName(), state.deviceId, null)

        log.debug "initializeHandlers() created ${device.displayName} with id: ${state.deviceId}"
    } else {
        log.debug "initializeHandlers() found ${device.displayName} with id: ${state.deviceId}"
    }

    // send the initial delegates
    delegatesHandler()
}

def delegatesHandler(event) {
    log.debug "delegatesHandler() event: ${event}"

    // get the child device
    def device = getChildDevice(state.deviceId)

    device.handleEvent()
}

def getDelegates()   { return settings.delegates }

def getHandlerName() { return "Virtual Switch" }
def getHandlerId()   { return "${settings.capability}.${UUID.randomUUID().toString()}" }