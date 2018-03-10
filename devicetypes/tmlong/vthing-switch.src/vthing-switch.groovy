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
        capability "Actuator"
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
                attributeState "turningOn", label: "...", action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "turningOff", label: "...", action: "switch.on", icon: "st.switches.switch.on", backgroundColor: "#ffffff", nextState: "turningOn"
            }

            tileAttribute("device.switch", key: "SECONDARY_CONTROL") {
                attributeState "on", label: "all on"
                attributeState "off", label: "all off"
                attributeState "someOn", label: "some on"
                attributeState "turningOn", label: "turning on..."
                attributeState "turningOff", label: "turning off..."
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
    def switchesOn = switches.count { it.currentSwitch == _SwitchState.ON }

    // determine the switch state
    def switchState = switchesOn == switches.size() ? _SwitchState.ON
        : (!switchesOn ? _SwitchState.OFF : _SwitchState.SOME)

    log.debug "determineState() switchState: ${switchState}"

    switchState
}

//
// Determine the switch transition state.
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
def shouldTurnOn() {
    log.debug "shouldTurnOn()"

    (whenSomeOn && inState(_SwitchState.SOME))
}

//
// Check if the device is in the current switch state.
//
def inState(switchState) {
    log.debug "inState() switchState: ${switchState}"

    (switchState == device.currentValue("switch"))
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"
}

// handle commands
def on() {
    log.debug "on()"

    // check if we are already in an on state
    if (inState(_SwitchState.ON)) return

    // turn on the switches
    parent.doDelegation(_SwitchState.ON)
}

def off() {
    log.debug "off()"

    // check if we are already in an off state
    if (inState(_SwitchState.OFF)) return

    // turn off the switches
    shouldTurnOn() ? on() : parent.doDelegation(_SwitchState.OFF)
}

def refresh() {
    log.debug "refresh()"

    // send the current state event
    sendEvent(name: "switch", value: determineState(parent.delegates))
}