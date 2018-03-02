/**
 *  vThing Delegator
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
        name: "vThing Delegator",
        namespace: "tmlong",
        author: "Todd Long",
        description: "A virtual delegator of things.",
        singleInstance: true,
        category: "SmartThings Labs",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "pageCheck")
    page(name: "pageMain")
    page(name: "pageCapability")
    page(name: "pageSettings")
}

def pageCheck() {
    isParent ? pageMain() : pageCapability()
}

def pageMain() {
    log.debug "pageMain()"

    dynamicPage(name: "pageMain", title: "", install: true, uninstall: false) {
        section(title: hasThings ? "My vThings" : "") {
        }

        section(title: hasThings ? " " : "") {
            app(name: "vThing", appName: app.name, namespace: "tmlong", title: "Add vThing...", multiple: true, uninstall: false)
        }

        section(title: "App Info") {
            href "pageSettings", title: "Settings", description: ""
        }
    }
}

def pageCapability() {
    log.debug "pageCapability()"

    // configure the select metadata
    def select = [
        capabilities: [
            values: _Capability.keySet().collect()
        ]
    ]

    dynamicPage(name: "pageCapability", title: "", install: true, uninstall: false) {
        section {
            input "capability", "enum", title: "Select Capability", required: true, submitOnChange: true, metadata: select.capabilities

            if (capability) {
                input "delegates", capability.type, title: capability.title, multiple: true
            }
        }

        section {
            label title: "Assign Name", required: false, defaultValue: nextThingName
        }
    }
}

def pageSettings() {
    log.debug "pageSettings()"

    dynamicPage(name: "pageSettings", title: "", install: false, uninstall: true) {
        section {
            paragraph "Caution: You are about to uninstall \"${app.name}\" and all of your configured \"vThings\". This action cannot be undone. If you would like to proceed, tap the \"Remove\" button below."
        }
    }
}

def installed() {
    log.info "Installed with settings: ${settings}"
}

def updated() {
    // prevent updated() from being called twice
    if ((now() - (state.lastUpdated ?: 0)) < 5000) return

    state.lastUpdated = now()

    log.info "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "initialize() isParent: ${isParent}"

    if (!isParent) {
        initializeThing()
    }
}

def initializeThing() {
    log.debug "initializeThing()"

    // initialize the device id
    state.deviceId = state.deviceId ?: handlerId

    // initialize the device handler
    initializeHandler()

    // subscribe to the delegates capability event
    subscribe(delegates, capability.event, delegatesHandler)
}

def initializeHandler() {
    log.debug "initializeHandler()"

    def device = getChildDevice(state.deviceId)

    if (!device) {
        // add the device handler
        device = addChildDevice(app.namespace, handlerName, state.deviceId, null, [label: app.label])

        log.debug "initializeHandler() created ${device.displayName} with id: ${state.deviceId}"
    } else {
        log.debug "initializeHandler() found ${device.displayName} with id: ${state.deviceId}"
    }

    // delete the device handlers that are no longer used
    def devicesToDelete = getChildDevices().findAll { state.deviceId != it.deviceNetworkId }

    if (devicesToDelete) {
        log.warn "initializeHandler() devices to delete: ${devicesToDelete}"

        devicesToDelete.each { deleteChildDevice(it.deviceNetworkId) }
    }

    // send the initial event
    delegatesHandler([])
}

def delegatesHandler(event) {
    log.debug "delegatesHandler() event: ${event.name}"

    // get the device handler
    def device = getChildDevice(state.deviceId)

    // determine the device state
    def deviceState = device.determineState(delegates)

    // check if we are currently working
    if (state.working) {
        if (deviceState == state.working) {
            state.working = null
        } else {
            return
        }
    }

    log.debug "delegatesHandler() delegates: ${delegates}, deviceState: ${deviceState}"

    device.sendEvent(name: capability.event, value: deviceState)
}

def doDelegation(command) {
    log.debug "doDelegation()"

    state.working = command
    delegates."${command}"()
}

def getCapability() {
    _Capability."${settings.capability}"
}

def getNextThingName() {
    def i = 1

    parent.childApps.any {
        def childApp = parent.childApps.any { it.label == "vThing #${i}" }
        if (!childApp) return true
        i++
        return false
    }

    "vThing #${i}"
}

def getDelegates() {
    settings.delegates
}

def getHandlerName() {
    capability.handler
}

def getHandlerId() {
    "${capability.event}.${UUID.randomUUID().toString()}"
}

def getIsInstalled() {
    (app.installationState == "COMPLETE")
}

def getHasThings() {
    app.childApps
}

def getIsParent() {
    (parent == null)
}

def get_Capability() {
    [
        outlet: [
            event: "switch",
            type: "capability.outlet",
            title: "Select Outlets",
            handler: "vThing Switch"
        ],
        switch: [
            event: "switch",
            type: "capability.switch",
            title: "Select Switches",
            handler: "vThing Switch"
        ]
    ]
}