/**
 *  vThing Switch
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
    definition (name: "vThing Switch", namespace: "tmlong", author: "Todd Long") {
        capability "Refresh"
        capability "Switch"
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles(scale: 2) {
        standardTile("main", "device.switch") {
            state "on", label: "on", action: "switch.off", icon: "st.switches.switch.off", backgroundColor: "#00A0DC", nextState: "turningOff"
            state "off", label: "off", action: "switch.on", icon: "st.switches.switch.on", backgroundColor: "#ffffff", nextState: "turningOn"
            state "someOn", label: "some on", action: "switch.off", icon: "st.switches.switch.off", backgroundColor: "#79b821", nextState: "turningOff"
            state "turningOn", label: "turning on", action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState: "turningOff"
            state "turningOff", label: "turning off", action: "switch.on", icon: "st.switches.switch.on", backgroundColor: "#ffffff", nextState: "turningOn"
        }

        multiAttributeTile(name: "details", type: "generic", width: 6, height: 4) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label: "on", action: "switch.off", icon: "st.switches.switch.off", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "off", label: "off", action: "switch.on", icon: "st.switches.switch.on", backgroundColor: "#ffffff", nextState: "turningOn"
                attributeState "someOn", label: "on", action: "switch.off", icon: "st.switches.switch.off", backgroundColor: "#79b821", nextState: "turningOff"
                attributeState "turningOn", label: "on", action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "turningOff", label: "off", action: "switch.on", icon: "st.switches.switch.on", backgroundColor: "#ffffff", nextState: "turningOn"
            }

            tileAttribute("device.switch", key: "SECONDARY_CONTROL") {
                attributeState "on", label: "all on", action: "switch.off", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "off", label: "all off", action: "switch.on", backgroundColor: "#ffffff", nextState: "turningOn"
                attributeState "someOn", label: "some on", action: "switch.off", backgroundColor: "#79b821", nextState: "turningOff"
                attributeState "turningOn", label: "turning on...", action: "switch.off", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "turningOff", label: "turning off...", action: "switch.on", backgroundColor: "#ffffff", nextState: "turningOn"
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

        main "main"
        details(["details", "on", "off", "refresh"])
    }

    preferences {
        input "whenSomeOn", "bool", title: "How should switches be controlled when some are on? By default, they will be turned off.", defaultValue: true
    }
}

def get_SwitchState() {
    [ ON: "on", OFF: "off", SOME: "someOn" ]
}

def get_TransitionState() {
    [ ON: "turningOn", OFF: "turningOff" ]
}

//
// Determine the switch state.
//
def determineState(switches) {
    log.debug "determineState() switches: ${switches}"

    // determine which switches are turned on
    def switchesOn = switches.findAll {
        it.currentSwitch == "on"
    }

    def switchState = (switchesOn.size() == switches.size()) ? _SwitchState.ON
        : (!switchesOn.size() ? _SwitchState.OFF : _SwitchState.SOME)

    log.debug "determineState() switchState: ${switchState}"

    switchState
}

//
// Determine the transition state.
//
def determineTransitionState(command) {
    log.debug "determineTransitionState() command: ${command}"

    switch (command) {
        case _SwitchState.ON: _TransitionState.ON; break
        case _SwitchState.OFF: _TransitionState.OFF; break
    }
}

//
// Determine if the switches should be turned on.
//
def shouldTurnOn(switches) {
    log.debug "shouldTurnOn() switches: ${switches}"

    // determine the switch state
    def switchState = determineState(switches)
    def shouldTurnOn = (switchState == _SwitchState.SOME && whenSomeOn == false)

    log.debug "shouldTurnOn() switchState: ${switchState}, shouldTurnOn: ${shouldTurnOn}"

    return shouldTurnOn
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
}

// handle commands
def on() {
    log.debug "on()"

    // turn on the switches
    parent.doDelegation(_SwitchState.ON)
}

def off() {
    log.debug "off()"

    if (shouldTurnOn(parent.delegates)) {
        on()
    } else {
        // turn off the switches
        parent.doDelegation(_SwitchState.OFF)
    }
}

def refresh() {
    log.debug "refresh()"
}