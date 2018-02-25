/**
 *  Virtual Switch
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
metadata {
    definition (name: "Virtual Switch", namespace: "tmlong", author: "Todd Long") {
        capability "Refresh"
        capability "Switch"
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "switch", type: "generic", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label: "on", action: "switch.off", icon: "st.switches.switch.off", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "off", label: "off", action: "switch.on", icon: "st.switches.switch.on", backgroundColor: "#ffffff", nextState: "turningOn"
                attributeState "someOn", label: "some on", action: "switch.off", icon: "st.switches.switch.off", backgroundColor: "#79b821", nextState: "turningOff"
                attributeState "turningOn", label: "turning on", action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "turningOff", label: "turning off", action: "switch.on", icon: "st.switches.switch.on", backgroundColor: "#ffffff", nextState: "turningOn"
            }
        }

        standardTile("on", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "default", label: "on", backgroundColor: "#00A0DC", action: "switch.on", icon: "st.switches.switch.on"
        }

        standardTile("off", "device.switch", width: 2, height: 2, decoration: "flat") {
            state "default", label: "off", backgroundColor: "#ffffff", action: "switch.off", icon: "st.switches.switch.off"
        }

        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", action: "refresh.refresh", icon: "st.secondary.refresh"
        }

        main "switch"
        details(["switch", "on", "off", "refresh"])
    }

    preferences {
        input "whenSomeOn", "bool", title: "Turn off lights when some are on?", defaultValue: true
    }
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
}

// handle parent event
def handleEvent(Map event) {
    log.debug "handleEvent() event: ${event}"

    // determine the switches state
    def switchesState = partiallyOn(parent.delegates)

    log.debug "handleEvent() switches: ${parent.delegates}, switchesState: ${switchesState}"

    sendEvent(name: "switch", value: switchesState)
}

def partiallyOn(switches) {
    // determine which switches are turned on
    def switchesOn = switches.findAll {
        it.currentSwitch == "on"
    }

    return (switchesOn.size() == switches.size()) ? "on" : (!switchesOn.size() ? "off" : "someOn")
}

// handle commands
def on() {
    log.debug "on()"

    // turn on the switches
    parent.delegates.on()
    sendEvent(name: "switch", value: "on")
}

def off() {
    log.debug "off()"

    // determine the switches state
    def switchesState = partiallyOn(parent.delegates)

    log.debug "off() switchesState: ${switchesState}, whenSomeOn: ${whenSomeOn}"

    if (switchesState == "someOn" && whenSomeOn == false) {
        on()
    } else {
        // turn off the switches
        parent.delegates.off()
        sendEvent(name: "switch", value: "off")
    }
}

def refresh() {
    log.debug "refresh()"
}